package ru.echo.radar.data.discovery

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketTimeoutException

/**
 * Сервер ЭХО каждые 3 секунды шлёт в LAN UDP-пакет
 * `ECHO_SERVER_DISCOVERY:<port>` на 255.255.255.255:8888.
 * Слушаем его, чтобы не вбивать IP вручную после смены сети.
 */
object EchoDiscovery {

    private const val DISCOVERY_PORT = 8888
    private const val PREFIX = "ECHO_SERVER_DISCOVERY:"

    fun parsePayload(payload: String, host: String): String? {
        val text = payload.trim()
        if (!text.startsWith(PREFIX)) return null
        if (host.isBlank()) return null
        val port = text.removePrefix(PREFIX).toIntOrNull() ?: 8080
        return "$host:$port"
    }

    suspend fun findServer(timeoutMs: Long = 5000): String? = withContext(Dispatchers.IO) {
        val socket = try {
            DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                bind(InetSocketAddress(DISCOVERY_PORT))
                soTimeout = 400
            }
        } catch (e: Exception) {
            return@withContext null
        }

        val deadline = System.currentTimeMillis() + timeoutMs
        val buffer = ByteArray(256)
        try {
            while (System.currentTimeMillis() < deadline) {
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(packet)
                } catch (e: SocketTimeoutException) {
                    continue
                }
                val text = String(packet.data, 0, packet.length)
                val found = parsePayload(text, packet.address?.hostAddress.orEmpty())
                if (found != null) return@withContext found
            }
            null
        } finally {
            socket.close()
        }
    }
}
