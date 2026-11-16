package ru.echo.radar.di

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import ru.echo.radar.BuildConfig
import ru.echo.radar.data.discovery.EchoDiscovery
import ru.echo.radar.data.remote.EchoApiClient
import ru.echo.radar.data.repository.EchoRepository
import ru.echo.radar.data.settings.DeviceIdentity
import ru.echo.radar.data.settings.EchoSettingsStore
import ru.echo.radar.data.settings.SavedTracksStore
import java.util.concurrent.TimeUnit

/**
 * Ручной DI-контейнер: приложение маленькое, Hilt здесь только замедлит сборку.
 * Всё создаётся один раз на процесс.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings = EchoSettingsStore(appContext)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val api = EchoApiClient(httpClient)

    val repository = EchoRepository(
        api = api,
        settingsStore = settings,
        savedTracksStore = SavedTracksStore(appContext),
        deviceId = DeviceIdentity.getOrCreate(appContext),
        scope = appScope,
    )

    init {
        appScope.launch {
            val initial = settings.settings.first()
            if (initial.host.isNotBlank() && initial.token.isNotBlank()) return@launch

            val host = initial.host.ifBlank {
                BuildConfig.ECHO_DEFAULT_HOST.ifBlank { EchoDiscovery.findServer().orEmpty() }
            }
            val token = initial.token.ifBlank { BuildConfig.ECHO_DEFAULT_TOKEN }

            if (host.isNotBlank() && (host != initial.host || token != initial.token)) {
                settings.saveConnection(host, token)
            }
        }
    }
}
