package ru.echo.radar.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import ru.echo.radar.domain.model.EchoCapsule
import ru.echo.radar.domain.model.EchoSignal
import ru.echo.radar.domain.model.LagrangePoint
import ru.echo.radar.domain.model.ThermalPatch
import ru.echo.radar.ui.theme.EchoColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Сонар присутствия: кольца, луч, сигналы, тайники, тепловые пятна.
 * Рисуется на canvas, поэтому одинаково дёшев на любом телефоне.
 */
@Composable
fun EchoSonar(
    signals: List<EchoSignal>,
    capsules: List<EchoCapsule>,
    thermalPatches: List<ThermalPatch>,
    lagrange: LagrangePoint?,
    syncedTargets: Set<String>,
    myColorHex: Long,
    isDawn: Boolean,
    onSignalClick: (EchoSignal) -> Unit,
    modifier: Modifier = Modifier,
    zoom: Float = 1f,
    beam: ResonanceBeam? = null,
) {
    val transition = rememberInfiniteTransition(label = "sonar")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(signals) {
                detectTapGestures { tap ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxR = min(size.width, size.height) * 0.42f * zoom
                    val hit = signals.firstOrNull { signal ->
                        val position = sonarPosition(
                            signal.angleDegrees,
                            signal.distanceRatio,
                            center,
                            maxR,
                        )
                        (position - tap).getDistance() <= size.width * 0.07f
                    }
                    if (hit != null) onSignalClick(hit)
                }
            },
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxR = min(size.width, size.height) * 0.42f * zoom
        val myColor = Color(myColorHex)

        if (isDawn) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(EchoColors.Gold.copy(alpha = 0.10f), Color.Transparent),
                    center = center,
                    radius = maxR * 1.35f,
                ),
                radius = maxR * 1.35f,
                center = center,
            )
        }

        listOf(0.3f, 0.55f, 0.78f, 1f).forEach { factor ->
            drawCircle(
                color = if (factor == 1f) EchoColors.SignalCyan.copy(alpha = 0.22f) else EchoColors.Line,
                radius = maxR * factor,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        drawLine(
            color = EchoColors.Line,
            start = Offset(center.x - maxR, center.y),
            end = Offset(center.x + maxR, center.y),
            strokeWidth = 1.dp.toPx(),
        )
        drawLine(
            color = EchoColors.Line,
            start = Offset(center.x, center.y - maxR),
            end = Offset(center.x, center.y + maxR),
            strokeWidth = 1.dp.toPx(),
        )

        thermalPatches.forEach { patch ->
            val position = Offset(
                center.x + (patch.x - 0.5f) * 2f * maxR,
                center.y + (patch.y - 0.5f) * 2f * maxR,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFB923C).copy(alpha = 0.22f), Color.Transparent),
                    center = position,
                    radius = maxR * 0.25f,
                ),
                radius = maxR * 0.25f,
                center = position,
            )
        }

        rotate(degrees = sweep, pivot = center) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        EchoColors.SignalCyan.copy(alpha = 0.35f),
                        Color.Transparent,
                    ),
                    center = center,
                ),
                startAngle = -16f,
                sweepAngle = 32f,
                useCenter = true,
                topLeft = Offset(center.x - maxR, center.y - maxR),
                size = Size(maxR * 2f, maxR * 2f),
            )
        }

        signals.forEach { signal ->
            val color = Color(signal.colorHex)
            val position = sonarPosition(signal.angleDegrees, signal.distanceRatio, center, maxR)

            drawCircle(color = color.copy(alpha = 0.12f), radius = 12.dp.toPx(), center = position)
            drawCircle(color = color, radius = 5.dp.toPx(), center = position)
            drawCircle(color = Color.White, radius = 1.6.dp.toPx(), center = position)

            if (signal.isGhost) {
                drawCircle(
                    color = color.copy(alpha = 0.55f),
                    radius = 9.dp.toPx(),
                    center = position,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 4f)),
                    ),
                )
            }
            if (signal.id in syncedTargets) {
                drawCircle(
                    color = EchoColors.Gold,
                    radius = 10.dp.toPx(),
                    center = position,
                    style = Stroke(width = 1.2.dp.toPx()),
                )
            }
        }

        capsules.forEach { capsule ->
            val position = sonarPosition(capsule.angleDegrees, capsule.distanceRatio, center, maxR)
            val radius = if (capsule.isVenue) 7.dp.toPx() else 5.5.dp.toPx()
            val diamond = Path().apply {
                moveTo(position.x, position.y - radius)
                lineTo(position.x + radius, position.y)
                lineTo(position.x, position.y + radius)
                lineTo(position.x - radius, position.y)
                close()
            }
            drawPath(diamond, color = EchoColors.Amber)
            if (capsule.isVenue) {
                drawPath(diamond, color = Color.White, style = Stroke(width = 1.dp.toPx()))
            }
        }

        lagrange?.let { point ->
            val position = sonarPosition(point.angleDegrees, point.distanceRatio, center, maxR)
            drawCircle(
                color = EchoColors.Gold.copy(alpha = 0.85f),
                radius = 20.dp.toPx(),
                center = position,
                style = Stroke(
                    width = 1.4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                ),
            )
        }

        beam?.let { pulse ->
            val target = sonarPosition(pulse.angleDegrees, pulse.distanceRatio, center, maxR)
            val progress = pulse.progress().coerceIn(0f, 1f)
            // Мягкое появление и затухание: луч не вспыхивает и не мельтешит.
            val fadeIn = (progress / 0.16f).coerceIn(0f, 1f)
            val fadeOut = ((1f - progress) / 0.22f).coerceIn(0f, 1f)
            val alpha = minOf(fadeIn, fadeOut)
            if (alpha > 0.01f) {
                val tailProgress = (progress - 0.26f).coerceAtLeast(0f)
                val head = Offset(
                    center.x + (target.x - center.x) * progress,
                    center.y + (target.y - center.y) * progress,
                )
                val tail = Offset(
                    center.x + (target.x - center.x) * tailProgress,
                    center.y + (target.y - center.y) * tailProgress,
                )
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            EchoColors.SignalCyan.copy(alpha = 0.10f * alpha),
                            EchoColors.SignalCyan.copy(alpha = 0.60f * alpha),
                            EchoColors.Gold.copy(alpha = alpha),
                        ),
                        start = tail,
                        end = head,
                    ),
                    start = tail,
                    end = head,
                    strokeWidth = 2.6.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = EchoColors.Gold.copy(alpha = 0.22f * alpha),
                    radius = 9.dp.toPx(),
                    center = head,
                )
                drawCircle(
                    color = EchoColors.Gold.copy(alpha = alpha),
                    radius = 3.dp.toPx(),
                    center = head,
                )
                if (progress > 0.70f) {
                    val flare = ((progress - 0.70f) / 0.30f).coerceIn(0f, 1f)
                    drawCircle(
                        color = EchoColors.Gold.copy(alpha = (1f - flare) * 0.75f * alpha),
                        radius = 10.dp.toPx() + flare * 16.dp.toPx(),
                        center = target,
                        style = Stroke(width = 1.4.dp.toPx()),
                    )
                }
            }
        }

        drawCircle(color = myColor.copy(alpha = 0.30f), radius = 11.dp.toPx(), center = center)
        drawCircle(color = myColor, radius = 3.5.dp.toPx(), center = center)
    }
}

private fun sonarPosition(
    angleDegrees: Float,
    distanceRatio: Float,
    center: Offset,
    maxR: Float,
): Offset {
    val radians = Math.toRadians(angleDegrees.toDouble())
    val radius = maxR * distanceRatio
    return Offset(
        center.x + (sin(radians) * radius).toFloat(),
        center.y - (cos(radians) * radius).toFloat(),
    )
}

data class ResonanceBeam(
    val angleDegrees: Float,
    val distanceRatio: Float,
    val progress: () -> Float,
)
