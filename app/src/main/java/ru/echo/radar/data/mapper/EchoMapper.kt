package ru.echo.radar.data.mapper

import ru.echo.radar.data.remote.BannerDto
import ru.echo.radar.data.remote.CapsuleDto
import ru.echo.radar.data.remote.EchoStateDto
import ru.echo.radar.data.remote.EchoUrl
import ru.echo.radar.data.remote.LagrangeDto
import ru.echo.radar.data.remote.ReactionDto
import ru.echo.radar.data.remote.SignalDto
import ru.echo.radar.data.remote.ThermalDto
import ru.echo.radar.data.remote.ZoneDto
import ru.echo.radar.domain.model.EchoCapsule
import ru.echo.radar.domain.model.EchoConnection
import ru.echo.radar.domain.model.EchoReaction
import ru.echo.radar.domain.model.EchoSignal
import ru.echo.radar.domain.model.EchoSnapshot
import ru.echo.radar.domain.model.EchoZone
import ru.echo.radar.domain.model.IncomingEcho
import ru.echo.radar.domain.model.LagrangePoint
import ru.echo.radar.domain.model.MyEcho
import ru.echo.radar.domain.model.ThermalPatch

object EchoMapper {

    fun toSnapshot(dto: EchoStateDto, baseUrl: String, previous: EchoSnapshot): EchoSnapshot {
        val nowSec = System.currentTimeMillis() / 1000
        return previous.copy(
            connection = EchoConnection.Online,
            lastSyncAt = nowSec,
            serverTime = dto.serverTime,
            signals = dto.signals.map { it.toDomain(baseUrl) },
            capsules = dto.capsules.map { it.toDomain() },
            zones = dto.cityZones.map { it.toDomain() },
            reactions = dto.reactionsHistory.map { it.toDomain() },
            traces = dto.eveningTraces,
            myEcho = MyEcho(
                personaTitle = dto.myCuratedPersona,
                observation = dto.myCuratedObservation,
                moodLabel = dto.myRefinedMood,
                colorHex = dto.myColorHex,
            ),
            lagrange = dto.lagrangePoint?.takeIf { it.active }?.toDomain(),
            thermalPatches = dto.thermalPatches.map { it.toDomain() },
            syncedTargets = dto.needleSyncedTargets.toSet(),
            isDawn = dto.isDawnMode,
        )
    }

    fun toIncoming(dto: BannerDto): IncomingEcho = IncomingEcho(
        id = dto.id,
        symbol = dto.symbol,
        senderId = dto.senderId,
        message = dto.message.ifBlank { "Вам отправлен импульс из эфира" },
        timeSec = dto.time.toLong(),
    )

    private fun SignalDto.toDomain(baseUrl: String): EchoSignal = EchoSignal(
        id = id,
        artist = artist.ifBlank { "Неизвестный исполнитель" },
        title = title.ifBlank { "Без названия" },
        album = album,
        sourceName = sourceName.ifBlank { "ЭХО" },
        personaTitle = personaTitle.ifBlank { "Архивариус забытых кассет" },
        personaObservation = personaObservation,
        moodLabel = moodLabel,
        resonanceStory = resonanceStory,
        compatibility = compatibilityPercent.coerceIn(0, 100),
        bpm = bpm,
        camelotKey = camelotKey,
        genre = genre,
        distanceMeters = distanceMeters,
        angleDegrees = angleDegrees,
        distanceRatio = distanceRatio.coerceIn(0.12f, 1f),
        zoneName = zoneName,
        colorHex = colorHex,
        avatarPetals = avatarPetals.coerceIn(3, 9),
        isGhost = isGhost,
        ageMinutes = ageMinutes,
        encountersCount = encountersCount.coerceAtLeast(1),
        coverUrl = resolveCover(baseUrl, coverUrl, artist, title),
    )

    private fun CapsuleDto.toDomain(): EchoCapsule = EchoCapsule(
        id = id,
        creatorId = creatorId,
        artist = artist.ifBlank { "Неизвестный автор" },
        title = title.ifBlank { "Без названия" },
        moodLabel = moodLabel,
        personaTitle = personaTitle,
        zoneName = zoneName,
        secretMessage = secretMessage,
        distanceMeters = distanceMeters,
        angleDegrees = angleDegrees,
        distanceRatio = distanceRatio.coerceIn(0.12f, 1f),
        compatibility = compatibilityPercent.coerceIn(0, 100),
        isUnlocked = isUnlocked && (minResonance <= 0 || compatibilityPercent >= minResonance),
        isVenue = isVenue,
        venueName = venueName,
        minResonance = minResonance,
        secretPerk = secretPerk,
        createdAt = createdAt.toLong(),
    )

    private fun ZoneDto.toDomain(): EchoZone = EchoZone(
        placeName = placeName,
        dominantVibe = dominantVibe,
        recentArtists = recentArtists,
        listenersCount = listenersCount,
        fogColorHex = fogColorHex,
        fogRadius = fogRadius,
    )

    private fun ReactionDto.toDomain(): EchoReaction = EchoReaction(
        id = id,
        targetId = targetId,
        senderId = senderId,
        symbol = symbol,
        message = message,
        timeSec = time.toLong(),
    )

    private fun ThermalDto.toDomain(): ThermalPatch = ThermalPatch(
        id = id,
        targetId = targetId,
        senderId = senderId,
        x = x.coerceIn(0f, 1f),
        y = y.coerceIn(0f, 1f),
        ageSec = ageSec,
    )

    private fun LagrangeDto.toDomain(): LagrangePoint = LagrangePoint(
        angleDegrees = angleDegrees,
        distanceRatio = distanceRatio,
        secretTrack = secretTrack,
        secretMessage = secretMessage,
        participants = participants,
    )

    private fun resolveCover(baseUrl: String, coverUrl: String?, artist: String, title: String): String? {
        if (coverUrl.isNullOrBlank()) return null
        val full = EchoUrl.resolve(baseUrl, coverUrl)
        if (full.isEmpty()) return null
        return if (full.contains("/api/cover/")) "$full?v=${(artist + title).hashCode()}" else full
    }
}
