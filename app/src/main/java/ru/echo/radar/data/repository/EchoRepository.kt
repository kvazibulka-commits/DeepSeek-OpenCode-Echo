package ru.echo.radar.data.repository

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import ru.echo.radar.data.mapper.EchoMapper
import ru.echo.radar.data.remote.ApiError
import ru.echo.radar.data.remote.ApiResult
import ru.echo.radar.data.remote.BannerDto
import ru.echo.radar.data.remote.EchoApiClient
import ru.echo.radar.data.remote.EchoUrl
import ru.echo.radar.data.settings.EchoSettingsStore
import ru.echo.radar.domain.SourceApps
import ru.echo.radar.domain.TrackSanitizer
import ru.echo.radar.domain.TrackText
import ru.echo.radar.domain.model.EchoConnection
import ru.echo.radar.domain.model.EchoEvent
import ru.echo.radar.domain.model.EchoSettings
import ru.echo.radar.domain.model.EchoSnapshot
import ru.echo.radar.domain.model.PlayingTrack

/**
 * Единственный источник состояния ЭХО.
 *
 * Правила:
 *  - опрос живёт только пока приложение на экране (start/pause из Application);
 *  - все запросы сериализуются одним Mutex, чтобы не плодить дубли;
 *  - после каждого действия — один внеочередной опрос, без локальных догадок о резонансе.
 */
class EchoRepository(
    private val api: EchoApiClient,
    private val settingsStore: EchoSettingsStore,
    val deviceId: String,
    private val scope: CoroutineScope,
) {

    private val _snapshot = MutableStateFlow(EchoSnapshot())
    val snapshot: StateFlow<EchoSnapshot> = _snapshot.asStateFlow()

    private val _track = MutableStateFlow(PlayingTrack())
    val track: StateFlow<PlayingTrack> = _track.asStateFlow()

    private val _cover = MutableStateFlow<Bitmap?>(null)
    val cover: StateFlow<Bitmap?> = _cover.asStateFlow()

    private val _config = MutableStateFlow(EchoSettings())
    val config: StateFlow<EchoSettings> = _config.asStateFlow()

    private val _events = MutableSharedFlow<EchoEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<EchoEvent> = _events.asSharedFlow()

    private val refreshMutex = Mutex()
    private var pollJob: Job? = null
    private var failures = 0
    private var lastBannerId = 0L
    private var lastSentTrack: Pair<String, String>? = null

    init {
        scope.launch {
            settingsStore.settings.collect { settings ->
                val previous = _config.value
                _config.value = settings

                if (previous.presenceEnabled != settings.presenceEnabled) {
                    if (settings.presenceEnabled) {
                        lastSentTrack?.let { sent ->
                            sendSignal(sent.first, sent.second, _track.value.sourceApp.ifBlank { "Android ЭХО" })
                        }
                    } else {
                        deleteSignal()
                    }
                }

                if (previous.host != settings.host || previous.token != settings.token) {
                    failures = 0
                    pollOnce()
                }
            }
        }
        start()
    }

    fun start() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (isActive) {
                pollOnce()
                delay(if (failures >= 2) RETRY_DELAY_MS else POLL_DELAY_MS)
            }
        }
    }

    fun pause() {
        pollJob?.cancel()
        pollJob = null
    }

    fun refreshNow() {
        scope.launch { pollOnce() }
    }

    fun dismissIncoming() {
        _snapshot.value = _snapshot.value.copy(incoming = null)
    }

    // ── Музыка ──────────────────────────────────────────────────────────────

    /** Вызывается медиа-службой при смене трека. Дубли отсекаются здесь же. */
    fun onSystemTrackDetected(
        artist: String,
        title: String,
        packageName: String,
        cover: Bitmap? = null,
    ) {
        if (TrackSanitizer.isJunk(artist, title)) return

        val cleanArtist = artist.trim().ifBlank { "Неизвестный исполнитель" }
        val cleanTitle = title.trim()
        val source = SourceApps.labelFor(packageName)
        val current = _track.value

        if (current.artist == cleanArtist && current.title == cleanTitle) {
            // Обложка часто приходит вторым колбэком уже после названия трека.
            if (cover != null && _cover.value == null) {
                _cover.value = cover
                scope.launch { sendSignal(cleanArtist, cleanTitle, source) }
            }
            return
        }

        _track.value = PlayingTrack(cleanArtist, cleanTitle, source)
        _cover.value = cover

        if (lastSentTrack == Pair(cleanArtist, cleanTitle)) return
        lastSentTrack = Pair(cleanArtist, cleanTitle)
        scope.launch { sendSignal(cleanArtist, cleanTitle, source) }
    }

    /** Ручная трансляция: «Поделиться → ЭХО» или диалог на ДЕКЕ. */
    fun publishManual(rawText: String) {
        val parsed = TrackText.parseLoose(rawText)
        if (parsed == null) {
            emit(EchoEvent("Не удалось разобрать строку трека", true))
            return
        }
        val (artist, title) = parsed
        if (TrackSanitizer.isJunk(artist, title)) {
            emit(EchoEvent("Похоже на видео или служебное сообщение — не берём в эфир", true))
            return
        }
        _track.value = PlayingTrack(artist, title, "Вручную")
        _cover.value = null
        lastSentTrack = Pair(artist, title)
        scope.launch { sendSignal(artist, title, "Вручную") }
    }

    // ── Действия в эфире ────────────────────────────────────────────────────

    fun react(targetId: String, symbol: String = "✦") {
        val payload = buildJsonObject {
            put("targetId", targetId)
            put("senderId", deviceId)
            put("symbol", symbol)
            put("message", "Спасибо за созвучие")
        }
        perform("/api/react", payload, "Отклик ушёл в эфир")
    }

    fun sendWave(targetId: String) {
        val payload = buildJsonObject {
            put("targetId", targetId)
            put("senderId", deviceId)
        }
        perform("/api/wave_impulse", payload, "Волна ушла в ночной эфир")
    }

    fun syncNeedle(targetId: String) {
        val payload = buildJsonObject {
            put("targetId", targetId)
            put("senderId", deviceId)
        }
        perform("/api/needle_sync", payload, "Игла синхронизирована")
    }

    fun sendWarmth(targetId: String, x: Float, y: Float) {
        val payload = buildJsonObject {
            put("targetId", targetId)
            put("senderId", deviceId)
            put("x", x)
            put("y", y)
        }
        perform("/api/send_warmth", payload, "Тёплый след остался на радаре")
    }

    fun bumpEncounter(targetId: String) {
        val payload = buildJsonObject {
            put("targetId", targetId)
        }
        perform("/api/bump_encounter", payload, "Встреча записана в летопись")
    }

    fun dropCapsule(
        artist: String,
        title: String,
        secretMessage: String,
        zoneName: String,
        isVenue: Boolean,
        venueName: String,
        minResonance: Int,
        secretPerk: String,
    ) {
        if (artist.isBlank() || title.isBlank()) {
            emit(EchoEvent("Нужны исполнитель и трек", true))
            return
        }
        val payload = buildJsonObject {
            put("creatorId", deviceId)
            put("artist", artist.trim())
            put("title", title.trim())
            put("zoneName", zoneName)
            put("secretMessage", secretMessage.trim())
            put("isVenue", isVenue)
            put("venueName", venueName.trim())
            // Сервер трактует minResonance=0 у заведения как «по умолчанию 75».
            // 1% — честный «открыт всем»: любой трек проходит замок.
            put("minResonance", if (isVenue) minResonance.coerceAtLeast(1) else 0)
            put("secretPerk", secretPerk.trim())
        }
        perform("/api/drop_capsule", payload, "Тайник спрятан в эфире")
    }

    fun leaveAir() {
        scope.launch { deleteSignal() }
    }

    suspend fun verifyConnection(host: String, token: String): EchoConnection {
        if (host.isBlank()) return EchoConnection.Idle
        return when (val result = api.fetchState(host, token, deviceId)) {
            is ApiResult.Success -> EchoConnection.Online
            is ApiResult.Failure ->
                if (result.error is ApiError.Unauthorized) EchoConnection.Unauthorized else EchoConnection.Offline
        }
    }

    // ── Внутренняя механика ─────────────────────────────────────────────────

    private suspend fun pollOnce() {
        val settings = _config.value
        if (settings.host.isBlank()) {
            setConnection(EchoConnection.Idle)
            return
        }

        refreshMutex.withLock {
            if (_snapshot.value.connection != EchoConnection.Online) {
                setConnection(EchoConnection.Connecting)
            }

            when (val result = api.fetchState(settings.host, settings.token, deviceId)) {
                is ApiResult.Success -> {
                    failures = 0
                    val base = EchoUrl.normalizeHost(settings.host)
                    val mapped = EchoMapper.toSnapshot(result.value, base, _snapshot.value)
                    _snapshot.value = applyIncomingBanner(mapped, result.value.incomingBanner)
                        .copy(lastError = null)
                }
                is ApiResult.Failure -> registerFailure(result.error)
            }
        }
    }

    private fun applyIncomingBanner(snapshot: EchoSnapshot, banner: BannerDto?): EchoSnapshot {
        if (banner == null) return snapshot
        if (banner.id <= lastBannerId) return snapshot
        lastBannerId = banner.id
        return snapshot.copy(incoming = EchoMapper.toIncoming(banner))
    }

    private fun registerFailure(error: ApiError) {
        failures += 1
        val connection = if (error is ApiError.Unauthorized) {
            EchoConnection.Unauthorized
        } else {
            EchoConnection.Offline
        }
        _snapshot.value = _snapshot.value.copy(
            connection = connection,
            lastError = technicalMessage(error),
        )
    }

    private fun technicalMessage(error: ApiError): String = when (error) {
        is ApiError.Unauthorized -> "HTTP 401: токен не принят"
        is ApiError.Http -> "HTTP ${error.code}"
        is ApiError.Network -> "network: ${error.detail ?: "unknown"}"
        is ApiError.Parse -> "parse: ${error.detail ?: "unknown"}"
        is ApiError.Unexpected -> "error: ${error.detail ?: "unknown"}"
    }

    private fun setConnection(connection: EchoConnection) {
        val current = _snapshot.value
        if (current.connection != connection) {
            _snapshot.value = current.copy(connection = connection)
        }
    }

    private suspend fun sendSignal(artist: String, title: String, sourceName: String) {
        val settings = _config.value
        if (!settings.presenceEnabled || settings.host.isBlank()) return

        val payload = buildJsonObject {
            put("id", deviceId)
            put("artist", artist)
            put("title", title)
            put("sourceName", sourceName)
            put("zoneName", settings.zoneName)
            encodeCover(_cover.value)?.let { encoded -> put("coverBase64", encoded) }
        }

        when (val result = api.post(settings.host, settings.token, deviceId, "/api/signal", payload)) {
            is ApiResult.Success -> pollOnce()
            is ApiResult.Failure -> registerFailure(result.error)
        }
    }

    private suspend fun deleteSignal() {
        val settings = _config.value
        if (settings.host.isBlank()) return

        val payload = buildJsonObject {
            put("targetId", deviceId)
        }
        api.post(settings.host, settings.token, deviceId, "/api/delete_signal", payload)
        pollOnce()
    }

    private fun perform(path: String, payload: JsonObject, successMessage: String) {
        val settings = _config.value
        if (settings.host.isBlank()) {
            emit(EchoEvent("Сначала укажите адрес сервера в настройках", true))
            return
        }

        scope.launch {
            when (val result = api.post(settings.host, settings.token, deviceId, path, payload)) {
                is ApiResult.Success -> {
                    emit(EchoEvent(successMessage))
                    pollOnce()
                }
                is ApiResult.Failure -> emit(EchoEvent(userMessage(result.error), true))
            }
        }
    }

    private fun userMessage(error: ApiError): String = when (error) {
        is ApiError.Unauthorized -> "Токен не принят — проверьте доступ в настройках"
        is ApiError.Network -> "Сервер не отвечает — проверьте адрес в настройках"
        is ApiError.Http -> when (error.code) {
            403 -> "Сервер закрыл доступ к этому действию"
            413 -> "Послание слишком длинное"
            429 -> "Слишком часто — подождите минуту"
            else -> "Сервер ответил ошибкой ${error.code}"
        }
        is ApiError.Parse -> "Сервер прислал неожиданный ответ"
        is ApiError.Unexpected -> "Что-то пошло не так — попробуйте ещё раз"
    }

    private fun emit(event: EchoEvent) {
        _events.tryEmit(event)
    }

    /** Обложка для сервера: не больше 640 px, JPEG 82 %, чтобы влезала в один POST. */
    private fun encodeCover(bitmap: Bitmap?): String? {
        if (bitmap == null) return null
        return try {
            val maxSide = 640
            val largest = maxOf(bitmap.width, bitmap.height)
            val scaled = if (largest > maxSide) {
                val ratio = maxSide.toFloat() / largest
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt().coerceAtLeast(1),
                    (bitmap.height * ratio).toInt().coerceAtLeast(1),
                    true,
                )
            } else {
                bitmap
            }
            val stream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 82, stream)
            Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val POLL_DELAY_MS = 2500L
        const val RETRY_DELAY_MS = 7000L
    }
}
