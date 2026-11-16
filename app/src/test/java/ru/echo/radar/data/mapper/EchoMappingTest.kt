package ru.echo.radar.data.mapper

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.echo.radar.data.remote.EchoStateDto
import ru.echo.radar.domain.model.EchoConnection
import ru.echo.radar.domain.model.EchoSnapshot

class EchoMappingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val stateJson = """
        {
          "status": "ok",
          "serverTime": 1730000000,
          "signals": [
            {
              "id": "sig_1",
              "artist": "Молчат Дома",
              "title": "Судно",
              "personaTitle": "Пилигрим неоновых панелек",
              "personaObservation": "Слушает пост-панк в тумане.",
              "moodLabel": "Холодный синти",
              "resonanceStory": "Редкое созвучие Camelot и фазовый синхрон темпа.",
              "compatibilityPercent": 87,
              "bpm": 118,
              "camelotKey": "9A",
              "genre": "post-punk",
              "distanceMeters": 140,
              "angleDegrees": 42.5,
              "distanceRatio": 0.63,
              "zoneName": "Сретенка",
              "colorHex": 4281908728,
              "avatarPetals": 7,
              "isGhost": false,
              "ageMinutes": 1,
              "encountersCount": 3,
              "coverUrl": "/api/cover/sig_1",
              "unknownField": "сервер может добавить поле"
            }
          ],
          "capsules": [
            {
              "id": "cap_1",
              "creatorId": "dev_1",
              "artist": "Кино",
              "title": "Группа крови",
              "zoneName": "Покровка",
              "secretMessage": "Тёплый вечер в стекле",
              "distanceMeters": 88,
              "compatibilityPercent": 91,
              "minResonance": 85,
              "isUnlocked": true,
              "isVenue": true,
              "venueName": "Кофейня «Эхо»",
              "secretPerk": "кофе"
            }
          ],
          "cityZones": [
            {
              "placeName": "Сретенка",
              "dominantVibe": "пост-панк",
              "recentArtists": "Кино",
              "listenersCount": 4,
              "fogColorHex": 4281908728,
              "fogRadius": 600
            }
          ],
          "incomingBanner": {
            "id": 12,
            "targetId": "android_me",
            "senderId": "sig_1",
            "symbol": "✦",
            "message": "Спасибо за волну",
            "time": 1730000000.5
          },
          "reactionsHistory": [
            {
              "id": 1,
              "targetId": "android_me",
              "senderId": "sig_1",
              "symbol": "✦",
              "message": "Тёплый отклик",
              "time": 1730000000.0
            }
          ],
          "myCuratedPersona": "Архивариус забытых кассет",
          "myRefinedMood": "Ночной винил",
          "myCuratedObservation": "Слушает тихо.",
          "myColorHex": 4281908728,
          "isDawnMode": false,
          "lagrangePoint": {
            "active": true,
            "angleDegrees": 90.0,
            "distanceRatio": 0.6,
            "secretTrack": "Burial — Archangel",
            "secretMessage": "Акустическое затмение",
            "participants": ["sig_1"]
          }
        }
    """.trimIndent()

    @Test
    fun `parses full state payload`() {
        val dto = json.decodeFromString<EchoStateDto>(stateJson)
        val snapshot = EchoMapper.toSnapshot(dto, "192.168.1.5:8080", EchoSnapshot())

        assertEquals(EchoConnection.Online, snapshot.connection)
        assertEquals(1, snapshot.signals.size)

        val signal = snapshot.signals.first()
        assertEquals("sig_1", signal.id)
        assertEquals("Молчат Дома", signal.artist)
        assertEquals("Судно", signal.title)
        assertEquals(87, signal.compatibility)
        assertEquals(118, signal.bpm)
        assertEquals("9A", signal.camelotKey)
        assertEquals(140, signal.distanceMeters)
        assertEquals(42.5f, signal.angleDegrees, 0.001f)
        assertEquals(0xFF38BDF8L, signal.colorHex)
        assertEquals("Пилигрим неоновых панелек", signal.personaTitle)
        assertTrue(signal.coverUrl?.startsWith("http://192.168.1.5:8080/api/cover/sig_1?v=") == true)

        val capsule = snapshot.capsules.first()
        assertTrue(capsule.isVenue)
        assertEquals("Кофейня «Эхо»", capsule.venueName)
        assertEquals("кофе", capsule.secretPerk)

        assertEquals("Архивариус забытых кассет", snapshot.myEcho.personaTitle)
        assertEquals("Ночной винил", snapshot.myEcho.moodLabel)
        assertEquals(1, snapshot.reactions.size)
        assertEquals(1, snapshot.zones.size)
        assertEquals(1, snapshot.lagrange?.participants?.size)
        assertNotNull(dto.incomingBanner)
        assertEquals(12L, dto.incomingBanner?.id)
    }

    @Test
    fun `incoming banner is mapped with defaults`() {
        val dto = json.decodeFromString<EchoStateDto>(stateJson)
        val incoming = EchoMapper.toIncoming(dto.incomingBanner!!)
        assertEquals("Спасибо за волну", incoming.message)
        assertEquals("sig_1", incoming.senderId)
        assertEquals(1730000000L, incoming.timeSec)
    }

    @Test
    fun `empty payload is safe`() {
        val dto = json.decodeFromString<EchoStateDto>("{}")
        val snapshot = EchoMapper.toSnapshot(dto, "", EchoSnapshot())

        assertTrue(snapshot.signals.isEmpty())
        assertTrue(snapshot.capsules.isEmpty())
        assertEquals(EchoConnection.Online, snapshot.connection)
    }

    @Test
    fun `mapper clamps impossible values`() {
        val payload = """
            {
              "signals": [
                {
                  "id": "sig_x",
                  "compatibilityPercent": 150,
                  "distanceRatio": 0.01,
                  "avatarPetals": 42
                }
              ]
            }
        """.trimIndent()

        val snapshot = EchoMapper.toSnapshot(json.decodeFromString<EchoStateDto>(payload), "", EchoSnapshot())
        val signal = snapshot.signals.first()

        assertEquals(100, signal.compatibility)
        assertEquals(0.12f, signal.distanceRatio, 0.0001f)
        assertEquals(9, signal.avatarPetals)
    }
}
