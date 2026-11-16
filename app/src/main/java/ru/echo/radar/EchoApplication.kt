package ru.echo.radar

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import ru.echo.radar.di.AppContainer
import ru.echo.radar.widget.EchoWidgetProvider
import ru.echo.radar.widget.EchoWidgetStore
import ru.echo.radar.widget.WidgetTrack

class EchoApplication : Application(), DefaultLifecycleObserver {

    lateinit var container: AppContainer
        private set

    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super<Application>.onCreate()
        container = AppContainer(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        observeWidget()
    }

    override fun onStart(owner: LifecycleOwner) {
        container.repository.start()
    }

    override fun onStop(owner: LifecycleOwner) {
        container.repository.pause()
    }

    /** Виджет живёт на последнем известном треке, без сети и сервисов. */
    private fun observeWidget() {
        widgetScope.launch {
            combine(container.repository.track, container.repository.snapshot) { track, snapshot ->
                WidgetTrack(
                    artist = track.artist,
                    title = track.title,
                    persona = snapshot.myEcho.personaTitle,
                    colorHex = snapshot.myEcho.colorHex,
                )
            }
                .distinctUntilChanged()
                .collect { track ->
                    EchoWidgetStore.save(this@EchoApplication, track)
                    EchoWidgetProvider.refresh(this@EchoApplication)
                }
        }
    }
}
