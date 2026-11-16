package ru.echo.radar.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackTextTest {

    @Test
    fun `parses long dash`() {
        assertEquals("Молчат Дома" to "Судно", TrackText.parse("Молчат Дома — Судно"))
    }

    @Test
    fun `parses short dash and en dash`() {
        assertEquals("Кино" to "Группа крови", TrackText.parse("Кино - Группа крови"))
        assertEquals("Кино" to "Группа крови", TrackText.parse("Кино – Группа крови"))
    }

    @Test
    fun `returns null for a single line`() {
        assertNull(TrackText.parse("Просто трек без исполнителя"))
    }

    @Test
    fun `loose parse keeps track without artist`() {
        assertEquals("Неизвестный исполнитель" to "Просто трек", TrackText.parseLoose("Просто трек"))
        assertNull(TrackText.parseLoose("   "))
    }

    @Test
    fun `junk filter catches video and service messages`() {
        assertTrue(TrackSanitizer.isJunk("Кто-то", "Концерт.mp4"))
        assertTrue(TrackSanitizer.isJunk("Кто-то", "Прямой эфир"))
        assertTrue(TrackSanitizer.isJunk("Кто-то", "Голосовое сообщение"))
        assertTrue(TrackSanitizer.isJunk("Кто-то", "123456789"))
        assertTrue(TrackSanitizer.isJunk("Кто-то", "   "))
    }

    @Test
    fun `junk filter lets real music through`() {
        assertFalse(TrackSanitizer.isJunk("Молчат Дома", "Судно"))
        assertFalse(TrackSanitizer.isJunk("Burial", "Archangel"))
        assertFalse(TrackSanitizer.isJunk("", "Судно"))
    }

    @Test
    fun `music packages are strict`() {
        assertTrue(SourceApps.isMusicPackage("ru.yandex.music"))
        assertFalse(SourceApps.isMusicPackage("ru.yandex.maps"))
        assertFalse(SourceApps.isMusicPackage(null))
        assertEquals("Яндекс Музыка", SourceApps.labelFor("ru.yandex.music"))
        assertEquals("Плеер Android", SourceApps.labelFor("com.unknown.player"))
    }
}
