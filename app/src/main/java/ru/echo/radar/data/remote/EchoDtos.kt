package ru.echo.radar.data.remote

import kotlinx.serialization.Serializable

/**
 * Точное отражение ответа GET /api/state.
 * Все поля имеют значения по умолчанию: сервер развивается, клиент не должен падать
 * из-за нового или пропавшего поля.
 */
@Serializable
data class EchoStateDto(
    val status: String = "ok",
    val serverTime: Long = 0L,
    val signals: List<SignalDto> = emptyList(),
    val capsules: List<CapsuleDto> = emptyList(),
    val eveningTraces: List<String> = emptyList(),
    val cityZones: List<ZoneDto> = emptyList(),
    val incomingBanner: BannerDto? = null,
    val reactionsHistory: List<ReactionDto> = emptyList(),
    val myRefinedMood: String = "",
    val myCuratedPersona: String = "",
    val myCuratedObservation: String = "",
    val myColorHex: Long = 0xFF38BDF8L,
    val lagrangePoint: LagrangeDto? = null,
    val thermalPatches: List<ThermalDto> = emptyList(),
    val needleSyncedTargets: List<String> = emptyList(),
    val isDawnMode: Boolean = false,
)

@Serializable
data class SignalDto(
    val id: String = "",
    val artist: String = "Неизвестный исполнитель",
    val title: String = "Без названия",
    val album: String = "",
    val resonanceStory: String = "",
    val sourceName: String = "ЭХО",
    val distanceMeters: Int = 50,
    val angleDegrees: Float = 45f,
    val distanceRatio: Float = 0.45f,
    val personaTitle: String = "Архивариус забытых кассет",
    val personaObservation: String = "",
    val personaSource: String = "",
    val moodLabel: String = "Теплый винил",
    val compatibilityPercent: Int = 80,
    val bpm: Int = 120,
    val camelotKey: String = "8A",
    val genre: String = "indie",
    val genreLabel: String = "",
    val genreOrigin: String = "",
    val genreOriginLabel: String = "",
    val resonanceDays: Int = 1,
    val encountersCount: Int = 1,
    val colorHex: Long = 0xFF38BDF8L,
    val avatarPetals: Int = 6,
    val zoneName: String = "Вечерняя улица",
    val updatedAt: Double = 0.0,
    val isGhost: Boolean = false,
    val ageMinutes: Int = 0,
    val coverUrl: String? = null,
)

@Serializable
data class CapsuleDto(
    val id: String = "",
    val creatorId: String = "",
    val artist: String = "Неизвестный автор",
    val title: String = "Без названия",
    val moodLabel: String = "Теплый тайник",
    val personaTitle: String = "Музыкальный тайник",
    val zoneName: String = "Вечерняя улица",
    val secretMessage: String = "",
    val distanceMeters: Int = 65,
    val angleDegrees: Float = 120f,
    val distanceRatio: Float = 0.5f,
    val compatibilityPercent: Int = 90,
    val isUnlocked: Boolean = true,
    val isVenue: Boolean = false,
    val venueName: String = "",
    val minResonance: Int = 0,
    val secretPerk: String = "",
    val createdAt: Double = 0.0,
)

@Serializable
data class ZoneDto(
    val placeName: String = "",
    val dominantVibe: String = "",
    val recentArtists: String = "",
    val listenersCount: Int = 0,
    val fogColorHex: Long = 0xFF38BDF8L,
    val fogRadius: Int = 450,
)

@Serializable
data class BannerDto(
    val id: Long = 0L,
    val targetId: String = "",
    val senderId: String = "",
    val symbol: String = "✦",
    val message: String = "",
    val time: Double = 0.0,
)

@Serializable
data class ReactionDto(
    val id: Long = 0L,
    val targetId: String = "",
    val senderId: String = "",
    val symbol: String = "✦",
    val message: String = "",
    val time: Double = 0.0,
)

@Serializable
data class LagrangeDto(
    val active: Boolean = false,
    val angleDegrees: Float = 0f,
    val distanceRatio: Float = 0.5f,
    val secretTrack: String = "",
    val secretMessage: String = "",
    val participants: List<String> = emptyList(),
)

@Serializable
data class ThermalDto(
    val id: String = "",
    val targetId: String = "",
    val senderId: String = "",
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val ageSec: Float = 0f,
)
