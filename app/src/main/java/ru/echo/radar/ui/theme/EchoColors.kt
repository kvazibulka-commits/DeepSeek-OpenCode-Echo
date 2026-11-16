package ru.echo.radar.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Токены ЭХО. Правило: сначала токен, потом значение.
 * Сигнальный циан — только для «звучит прямо сейчас», небесный — для интерфейса.
 */
object EchoColors {
    val Background = Color(0xFF07090E)
    val Surface = Color(0xFF0D121B)
    val SurfaceElevated = Color(0xFF151E2A)
    val SurfaceTonal = Color(0xFF121826)
    val Line = Color(0xFF1E293B)
    val LineStrong = Color(0xFF334155)

    val TextPrimary = Color(0xFFF2F5F7)
    val TextSecondary = Color(0xFF85909E)
    val TextDim = Color(0xFF64748B)

    val SignalCyan = Color(0xFF00F0FF)
    val SkyBlue = Color(0xFF38BDF8)
    val Purple = Color(0xFFA855F7)
    val Amber = Color(0xFFFFB800)
    val Gold = Color(0xFFFFD700)
    val Green = Color(0xFF34D399)
    val Danger = Color(0xFFFF5533)
    val DeepCyan = Color(0xFF0284C7)
}
