package ru.echo.radar.data.remote

/**
 * Приводит введённый пользователем адрес к полному URL.
 *
 * Домашняя сеть живёт по http, туннель Cloudflare — по https. Поэтому схему
 * угадываем по признакам локального адреса, а не по «красивости» строки.
 */
object EchoUrl {

    private val localPrefixes = listOf(
        "localhost",
        "127.",
        "0.0.0.0",
        "10.",
        "192.168.",
        "169.254.",
    )

    private val httpPorts = listOf(":8080", ":8000", ":5000", ":3000", ":8888")

    fun normalizeHost(raw: String): String {
        val host = raw.trim().trimEnd('/')
        if (host.isEmpty()) return ""
        if (host.startsWith("http://") || host.startsWith("https://")) return host
        return if (isLocalHost(host)) "http://$host" else "https://$host"
    }

    fun endpoint(base: String, path: String): String {
        val root = normalizeHost(base)
        if (root.isEmpty()) return ""
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return root + cleanPath
    }

    fun resolve(base: String, url: String): String {
        if (url.isBlank()) return ""
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        return endpoint(base, url)
    }

    private fun isLocalHost(host: String): Boolean {
        if (localPrefixes.any { host.startsWith(it) }) return true
        if (httpPorts.any { host.contains(it) }) return true
        return isPrivate172(host)
    }

    private fun isPrivate172(host: String): Boolean {
        val withoutScheme = host.substringAfter("://")
        val octets = withoutScheme.substringBefore('/').substringBefore(':').split('.')
        if (octets.size != 4 || octets[0] != "172") return false
        val second = octets[1].toIntOrNull() ?: return false
        return second in 16..31
    }
}
