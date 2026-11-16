package ru.echo.radar.domain.model

/** Палитра живого сигнала: используется, когда сервер не прислал цвет. */
const val ECHO_DEFAULT_COLOR: Long = 0xFF38BDF8L

/** Сигнальный циан — только для того, что звучит прямо сейчас. */
const val ECHO_SIGNAL_COLOR: Long = 0xFF00F0FFL

enum class EchoConnection {
    Idle,
    Connecting,
    Online,
    Unauthorized,
    Offline,
}

data class EchoSignal(
    val id: String,
    val artist: String,
    val title: String,
    val sourceName: String,
    val personaTitle: String,
    val personaObservation: String,
    val moodLabel: String,
    val resonanceStory: String,
    val compatibility: Int,
    val bpm: Int,
    val camelotKey: String,
    val genre: String,
    val distanceMeters: Int,
    val angleDegrees: Float,
    val distanceRatio: Float,
    val zoneName: String,
    val colorHex: Long,
    val avatarPetals: Int,
    val isGhost: Boolean,
    val ageMinutes: Int,
    val encountersCount: Int,
    val coverUrl: String?,
)

data class EchoCapsule(
    val id: String,
    val creatorId: String,
    val artist: String,
    val title: String,
    val moodLabel: String,
    val personaTitle: String,
    val zoneName: String,
    val secretMessage: String,
    val distanceMeters: Int,
    val angleDegrees: Float,
    val distanceRatio: Float,
    val compatibility: Int,
    val isUnlocked: Boolean,
    val isVenue: Boolean,
    val venueName: String,
    val minResonance: Int,
    val secretPerk: String,
    val createdAt: Long,
)

data class EchoZone(
    val placeName: String,
    val dominantVibe: String,
    val recentArtists: String,
    val listenersCount: Int,
    val fogColorHex: Long,
    val fogRadius: Int,
)

data class EchoReaction(
    val id: Long,
    val targetId: String,
    val senderId: String,
    val symbol: String,
    val message: String,
    val timeSec: Long,
)

data class IncomingEcho(
    val id: Long,
    val symbol: String,
    val senderId: String,
    val message: String,
    val timeSec: Long,
)

data class ThermalPatch(
    val id: String,
    val targetId: String,
    val senderId: String,
    val x: Float,
    val y: Float,
    val ageSec: Float,
)

data class LagrangePoint(
    val angleDegrees: Float,
    val distanceRatio: Float,
    val secretTrack: String,
    val secretMessage: String,
    val participants: List<String>,
)

data class MyEcho(
    val personaTitle: String = "",
    val observation: String = "",
    val moodLabel: String = "",
    val colorHex: Long = ECHO_DEFAULT_COLOR,
)

data class PlayingTrack(
    val artist: String = "",
    val title: String = "",
    val sourceApp: String = "",
) {
    val isKnown: Boolean get() = artist.isNotBlank() && title.isNotBlank()
    val displayArtist: String get() = artist.ifBlank { "Игла ещё не опущена" }
    val displayTitle: String get() = title.ifBlank { "Включите трек — ЭХО подхватит его из плеера" }
}

data class EchoSnapshot(
    val connection: EchoConnection = EchoConnection.Idle,
    val lastSyncAt: Long = 0L,
    val serverTime: Long = 0L,
    val signals: List<EchoSignal> = emptyList(),
    val capsules: List<EchoCapsule> = emptyList(),
    val zones: List<EchoZone> = emptyList(),
    val reactions: List<EchoReaction> = emptyList(),
    val traces: List<String> = emptyList(),
    val myEcho: MyEcho = MyEcho(),
    val incoming: IncomingEcho? = null,
    val lagrange: LagrangePoint? = null,
    val thermalPatches: List<ThermalPatch> = emptyList(),
    val syncedTargets: Set<String> = emptySet(),
    val isDawn: Boolean = false,
) {
    val totalObjects: Int get() = signals.size + capsules.size
}

data class EchoEvent(
    val message: String,
    val isError: Boolean = false,
)

data class EchoSettings(
    val host: String = "",
    val token: String = "",
    val zoneName: String = ECHO_ZONES.first(),
    val presenceEnabled: Boolean = true,
) {
    val isConfigured: Boolean get() = host.isNotBlank()
}

val ECHO_ZONES: List<String> = listOf(
    "Вечерняя улица",
    "Сретенка",
    "Камерные бары",
    "Парк Горького",
    "Набережная",
    "Покровка",
    "Ночные кофейни",
    "Северный лесопарк",
)
