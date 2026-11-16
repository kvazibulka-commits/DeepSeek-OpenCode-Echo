package ru.echo.radar.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import ru.echo.radar.MainActivity
import ru.echo.radar.R

/**
 * Виджет «Сейчас играет»: RemoteViews + SharedPreferences.
 * Никакой сети, никаких сервисов — только последний известный трек.
 */
class EchoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onEnabled(context: Context) {
        refresh(context)
    }

    companion object {

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, EchoWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { widgetId ->
                updateWidget(context, manager, widgetId)
            }
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
        ) {
            val track = EchoWidgetStore.read(context)
            val hasTrack = track.title.isNotBlank()

            val views = RemoteViews(context.packageName, R.layout.echo_widget)
            views.setTextViewText(
                R.id.widget_track,
                if (hasTrack) track.title else "Игла ждёт трек",
            )
            views.setTextViewText(
                R.id.widget_artist,
                if (hasTrack) track.artist else "Включите музыку",
            )
            views.setTextViewText(
                R.id.widget_persona,
                track.persona.ifBlank { "Сонар музыкального присутствия" },
            )
            views.setTextColor(R.id.widget_persona, track.colorHex.toInt())

            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            manager.updateAppWidget(widgetId, views)
        }
    }
}
