package ru.echo.radar.data.settings

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.echo.radar.domain.model.SavedTrack

/** Сохранённые треки: небольшой JSON в SharedPreferences, без БД и сети. */
class SavedTracksStore(context: Context) {

    private val prefs = context.getSharedPreferences("echo_saved", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<SavedTrack> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<SavedTrackDto>>(raw)
                .map { it.toDomain() }
                .sortedByDescending { it.savedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun toggle(track: SavedTrack): List<SavedTrack> {
        val current = load().toMutableList()
        val existing = current.indexOfFirst { it.id == track.id }
        if (existing >= 0) {
            current.removeAt(existing)
        } else {
            current.add(0, track)
        }
        persist(current)
        return current
    }

    fun remove(id: String): List<SavedTrack> {
        val current = load().filterNot { it.id == id }
        persist(current)
        return current
    }

    private fun persist(tracks: List<SavedTrack>) {
        val dto = tracks.map { SavedTrackDto.from(it) }
        prefs.edit().putString(KEY, json.encodeToString(dto)).apply()
    }

    @Serializable
    private data class SavedTrackDto(
        val id: String,
        val artist: String,
        val title: String,
        val album: String = "",
        val coverUrl: String? = null,
        val persona: String = "",
        val savedAt: Long = 0L,
    ) {
        fun toDomain(): SavedTrack = SavedTrack(
            id = id,
            artist = artist,
            title = title,
            album = album,
            coverUrl = coverUrl,
            persona = persona,
            savedAt = savedAt,
        )

        companion object {
            fun from(track: SavedTrack): SavedTrackDto = SavedTrackDto(
                id = track.id,
                artist = track.artist,
                title = track.title,
                album = track.album,
                coverUrl = track.coverUrl,
                persona = track.persona,
                savedAt = track.savedAt,
            )
        }
    }

    private companion object {
        const val KEY = "tracks_json"
    }
}
