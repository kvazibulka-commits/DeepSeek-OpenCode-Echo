package ru.echo.radar.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class EchoUrlTest {

    @Test
    fun `private address gets http scheme`() {
        assertEquals("http://192.168.1.5:8080", EchoUrl.normalizeHost("192.168.1.5:8080"))
        assertEquals("http://localhost:8080", EchoUrl.normalizeHost("localhost:8080/"))
        assertEquals("http://10.0.0.7:8000", EchoUrl.normalizeHost("10.0.0.7:8000"))
        assertEquals("http://172.20.1.2:8080", EchoUrl.normalizeHost("172.20.1.2:8080"))
    }

    @Test
    fun `public tunnel keeps https`() {
        assertEquals(
            "https://coming-podcasts.trycloudflare.com",
            EchoUrl.normalizeHost("coming-podcasts.trycloudflare.com"),
        )
        assertEquals("https://echo.example.com", EchoUrl.normalizeHost("https://echo.example.com/"))
    }

    @Test
    fun `explicit scheme is preserved`() {
        assertEquals("http://echo.local:8080", EchoUrl.normalizeHost("http://echo.local:8080"))
        assertEquals("https://echo.local", EchoUrl.normalizeHost("https://echo.local/"))
    }

    @Test
    fun `blank host stays blank`() {
        assertEquals("", EchoUrl.normalizeHost("   "))
        assertEquals("", EchoUrl.endpoint("", "/api/state"))
    }

    @Test
    fun `endpoint joins base and path`() {
        assertEquals(
            "http://192.168.1.5:8080/api/state",
            EchoUrl.endpoint("192.168.1.5:8080", "/api/state"),
        )
        assertEquals(
            "http://192.168.1.5:8080/api/state",
            EchoUrl.endpoint("http://192.168.1.5:8080/", "api/state"),
        )
    }

    @Test
    fun `relative cover resolves against base`() {
        assertEquals(
            "http://192.168.1.5:8080/api/cover/sig_1",
            EchoUrl.resolve("192.168.1.5:8080", "/api/cover/sig_1"),
        )
        assertEquals("https://cdn.example.com/a.jpg", EchoUrl.resolve("192.168.1.5:8080", "https://cdn.example.com/a.jpg"))
        assertEquals("", EchoUrl.resolve("192.168.1.5:8080", ""))
    }
}
