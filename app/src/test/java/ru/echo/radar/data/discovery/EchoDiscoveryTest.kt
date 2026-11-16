package ru.echo.radar.data.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EchoDiscoveryTest {

    @Test
    fun `parses server broadcast`() {
        assertEquals(
            "192.168.0.214:8080",
            EchoDiscovery.parsePayload("ECHO_SERVER_DISCOVERY:8080", "192.168.0.214"),
        )
        assertEquals(
            "10.0.0.7:9090",
            EchoDiscovery.parsePayload("ECHO_SERVER_DISCOVERY:9090\n", "10.0.0.7"),
        )
    }

    @Test
    fun `missing port falls back to 8080`() {
        assertEquals(
            "192.168.0.214:8080",
            EchoDiscovery.parsePayload("ECHO_SERVER_DISCOVERY:", "192.168.0.214"),
        )
    }

    @Test
    fun `foreign or empty packets are ignored`() {
        assertNull(EchoDiscovery.parsePayload("SOMETHING_ELSE:8080", "192.168.0.214"))
        assertNull(EchoDiscovery.parsePayload("ECHO_SERVER_DISCOVERY:8080", ""))
        assertNull(EchoDiscovery.parsePayload("", "192.168.0.214"))
    }
}
