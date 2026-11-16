package ru.echo.radar.widget

import android.content.Context

data class WidgetTrack(
    val artist: String = "",
    val title: String = "",
    val persona: String = "",
    val colorHex: Long = 0xFF38BDF8L,
)

/** Снимок текущего трека для виджета: без сети и без сервисов. */
object EchoWidgetStore {

    private const val PREFS = "echo_widget_prefs"
    private const val KEY_ARTIST = "artist"
    private const val KEY_TITLE = "title"
    private const val KEY_PERSONA = "persona"
    private const val KEY_COLOR = "color"

    fun save(context: Context, track: WidgetTrack) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ARTIST, track.artist)
            .putString(KEY_TITLE, track.title)
            .putString(KEY_PERSONA, track.persona)
            .putLong(KEY_COLOR, track.colorHex)
            .apply()
    }

    fun read(context: Context): WidgetTrack {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return WidgetTrack(
            artist = prefs.getString(KEY_ARTIST, "").orEmpty(),
            title = prefs.getString(KEY_TITLE, "").orEmpty(),
            persona = prefs.getString(KEY_PERSONA, "").orEmpty(),
            colorHex = prefs.getLong(KEY_COLOR, 0xFF38BDF8L),
        )
    }
}
