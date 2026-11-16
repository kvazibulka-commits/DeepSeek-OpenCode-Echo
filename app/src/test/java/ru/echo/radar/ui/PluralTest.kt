package ru.echo.radar.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.echo.radar.ui.components.pluralRu

class PluralTest {

    @Test
    fun `russian plural forms`() {
        assertEquals("квартал", pluralRu(1, "квартал", "квартала", "кварталов"))
        assertEquals("квартала", pluralRu(2, "квартал", "квартала", "кварталов"))
        assertEquals("кварталов", pluralRu(5, "квартал", "квартала", "кварталов"))
        assertEquals("кварталов", pluralRu(11, "квартал", "квартала", "кварталов"))
        assertEquals("квартал", pluralRu(21, "квартал", "квартала", "кварталов"))
        assertEquals("квартала", pluralRu(102, "квартал", "квартала", "кварталов"))
        assertEquals("кварталов", pluralRu(0, "квартал", "квартала", "кварталов"))
    }

    @Test
    fun `voices and capsules`() {
        assertEquals("голос", pluralRu(1, "голос", "голоса", "голосов"))
        assertEquals("голоса", pluralRu(4, "голос", "голоса", "голосов"))
        assertEquals("голосов", pluralRu(12, "голос", "голоса", "голосов"))
        assertEquals("капсула", pluralRu(1, "капсула", "капсулы", "капсул"))
        assertEquals("капсул", pluralRu(8, "капсула", "капсулы", "капсул"))
    }
}
