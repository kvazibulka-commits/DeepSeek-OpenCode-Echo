package ru.echo.radar.di

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import ru.echo.radar.data.remote.EchoApiClient
import ru.echo.radar.data.repository.EchoRepository
import ru.echo.radar.data.settings.DeviceIdentity
import ru.echo.radar.data.settings.EchoSettingsStore
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
        deviceId = DeviceIdentity.getOrCreate(appContext),
        scope = appScope,
    )
}
