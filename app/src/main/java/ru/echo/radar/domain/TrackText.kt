package ru.echo.radar.domain

/**
 * Разбор текста «Исполнитель — Трек» из системного «Поделиться» и уведомлений.
 * Живёт в domain, чтобы покрываться обычными JVM-тестами.
 */
object TrackText {

    private val separators = listOf(" — ", " – ", " - ", " —", " –", " - ")

    fun parse(raw: String?): Pair<String, String>? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null

        for (separator in separators) {
            val index = text.indexOf(separator)
            if (index <= 0) continue
            val artist = text.substring(0, index).trim()
            val title = text.substring(index + separator.length).trim()
            if (artist.isNotEmpty() && title.isNotEmpty()) return artist to title
        }
        return null
    }

    /**
     * Одинокая строка без разделителя считается треком без исполнителя:
     * лучше показать «Неизвестный исполнитель», чем потерять музыку.
     */
    fun parseLoose(raw: String?): Pair<String, String>? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        return parse(text) ?: ("Неизвестный исполнитель" to text)
    }
}

/**
 * Фильтр мусора: видео, голосовые, «прямые эфиры», числовые ID файлов.
 * Правило работает и на клиенте, и в тестах — сервер дополнительно чистит свою сторону.
 */
object TrackSanitizer {

    private val junkMarkers = listOf(
        "прямой эфир",
        "прямая трансляция",
        "голосовое сообщение",
        "видеосообщение",
        "mash",
        "топор",
        "livestream",
        "live stream",
        "video message",
        "voice message",
    )

    private val videoExtensions = listOf(".mp4", ".mkv", ".webm", ".mov", ".avi", ".flv", ".wmv")

    private val numericIdRegex = Regex("^\\d{5,}$")

    fun isJunk(artist: String?, title: String?): Boolean {
        val titleText = title?.trim().orEmpty()
        if (titleText.isEmpty()) return true

        val probe = (artist.orEmpty() + " " + titleText).lowercase()
        if (videoExtensions.any { probe.contains(it) }) return true
        if (junkMarkers.any { probe.contains(it) }) return true
        if (numericIdRegex.matches(titleText)) return true
        if (titleText.length > 180) return true

        return false
    }
}

/**
 * Пакеты плееров, которые ЭХО умеет слушать.
 * Никаких подстрок вроде «yandex»: иначе Яндекс Карты попадут в эфир.
 */
object SourceApps {

    private val labels = linkedMapOf(
        "ru.yandex.music" to "Яндекс Музыка",
        "com.vkontakte.android" to "VK Музыка",
        "com.vk.music" to "VK Музыка",
        "com.spotify.music" to "Spotify",
        "com.google.android.apps.youtube.music" to "YouTube Music",
        "com.apple.android.music" to "Apple Music",
        "ru.zvuk.android" to "Звук",
        "com.sber.zvuk" to "Звук",
        "com.deezer.android" to "Deezer",
        "com.aspiro.tidal" to "Tidal",
        "com.soundcloud.android" to "SoundCloud",
        "com.maxmpz.audioplayer" to "Poweramp",
        "com.aimp.player" to "AIMP",
    )

    val musicPackages: Set<String> = labels.keys

    fun isMusicPackage(packageName: String?): Boolean =
        packageName != null && labels.containsKey(packageName)

    fun labelFor(packageName: String?): String =
        labels[packageName] ?: "Плеер Android"
}
