package ru.echo.radar.media

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import ru.echo.radar.EchoApplication
import ru.echo.radar.domain.SourceApps
import ru.echo.radar.domain.TrackText

/**
 * Слушает медиа-сессии плееров и отдаёт трек в репозиторий.
 *
 * Два канала: MediaController.Callback (мгновенно) и опрос раз в 2.5 с (страховка).
 * Яндекс Музыка не всегда обновляет уведомление при смене трека, поэтому опрос обязателен.
 */
class EchoMediaListenerService : NotificationListenerService() {

    private val handler = Handler(Looper.getMainLooper())
    private val bindings = mutableMapOf<String, SessionBinding>()

    private val scanRunnable = object : Runnable {
        override fun run() {
            scanActiveSessions()
            handler.postDelayed(this, SCAN_INTERVAL_MS)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        handler.post(scanRunnable)
    }

    override fun onListenerDisconnected() {
        handler.removeCallbacks(scanRunnable)
        releaseBindings()
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        handler.removeCallbacks(scanRunnable)
        releaseBindings()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val packageName = sbn.packageName ?: return
        if (!SourceApps.isMusicPackage(packageName)) return

        val extras = notification.extras ?: return

        @Suppress("DEPRECATION")
        val token = extras.getParcelable(Notification.EXTRA_MEDIA_SESSION) as? MediaSession.Token
        if (token != null) {
            handleSession(packageName, MediaController(this, token))
            return
        }

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val parsed = TrackText.parse(text) ?: TrackText.parse(title)
        if (parsed != null) {
            publish(packageName, parsed.first, parsed.second)
        } else if (!title.isNullOrBlank() && !text.isNullOrBlank()) {
            publish(packageName, subText ?: "Неизвестный исполнитель", text)
        }
    }

    private fun scanActiveSessions() {
        val manager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
        val component = ComponentName(this, EchoMediaListenerService::class.java)
        val sessions = try {
            manager.getActiveSessions(component)
        } catch (e: SecurityException) {
            emptyList()
        }

        sessions.forEach { controller ->
            val packageName = controller.packageName ?: return@forEach
            if (!SourceApps.isMusicPackage(packageName)) return@forEach
            handleSession(packageName, controller)
        }
    }

    private fun handleSession(packageName: String, controller: MediaController) {
        bind(controller)
        val metadata = controller.metadata ?: return
        publish(packageName, metadata)
    }

    private fun bind(controller: MediaController) {
        val packageName = controller.packageName ?: return
        if (bindings.containsKey(packageName)) return

        val callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                if (metadata != null) publish(packageName, metadata)
            }

            override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) {
                val metadata = controller.metadata ?: return
                publish(packageName, metadata)
            }
        }

        controller.registerCallback(callback, handler)
        bindings[packageName] = SessionBinding(controller, callback)
    }

    private fun releaseBindings() {
        bindings.values.forEach { binding ->
            try {
                binding.controller.unregisterCallback(binding.callback)
            } catch (e: Exception) {
                // Сессия могла умереть раньше нас — это нормально.
            }
        }
        bindings.clear()
    }

    private fun publish(packageName: String, metadata: MediaMetadata) {
        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: ""
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: return
        publish(packageName, artist, title)
    }

    private fun publish(packageName: String, artist: String, title: String) {
        if (artist.isBlank()) {
            val parsed = TrackText.parse(title)
            if (parsed != null) {
                publish(packageName, parsed.first, parsed.second)
                return
            }
        }

        val app = application as? EchoApplication ?: return
        val cleanArtist = artist.ifBlank { "Неизвестный исполнитель" }
        app.container.repository.onSystemTrackDetected(cleanArtist, title, packageName)
    }

    private data class SessionBinding(
        val controller: MediaController,
        val callback: MediaController.Callback,
    )

    private companion object {
        const val SCAN_INTERVAL_MS = 2500L
    }
}
