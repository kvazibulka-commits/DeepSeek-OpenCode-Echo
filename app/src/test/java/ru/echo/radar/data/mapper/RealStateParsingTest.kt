package ru.echo.radar.data.mapper

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.echo.radar.data.remote.EchoStateDto
import ru.echo.radar.domain.model.EchoSnapshot

/**
 * Ответ старого сервера содержит дробные `updatedAt` и `createdAt`.
 * Этот фикстур — реальный `/api/state`, чтобы клиент не падал на таких полях.
 */
class RealStateParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    @Test
    fun `parses real server response`() {
        val text = javaClass.classLoader!!
            .getResourceAsStream("state_real.json")!!
            .bufferedReader()
            .use { it.readText() }

        val dto = json.decodeFromString<EchoStateDto>(text)
        assertTrue(dto.signals.isNotEmpty())
        assertTrue(dto.capsules.isNotEmpty())

        val snapshot = EchoMapper.toSnapshot(dto, "192.168.0.214:8080", EchoSnapshot())
        assertEquals(dto.signals.size, snapshot.signals.size)
        assertEquals(dto.capsules.size, snapshot.capsules.size)
        assertTrue(snapshot.capsules.first().createdAt > 0L)
        assertTrue(snapshot.signals.first().title.isNotBlank())
    }
}
