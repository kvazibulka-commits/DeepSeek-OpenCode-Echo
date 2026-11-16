package ru.echo.radar

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import ru.echo.radar.di.AppContainer

class EchoApplication : Application(), DefaultLifecycleObserver {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super<Application>.onCreate()
        container = AppContainer(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        container.repository.start()
    }

    override fun onStop(owner: LifecycleOwner) {
        container.repository.pause()
    }
}
