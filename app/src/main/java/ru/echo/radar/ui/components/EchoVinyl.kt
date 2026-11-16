package ru.echo.radar.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.echo.radar.ui.theme.EchoColors

/**
 * Виниловая дека: диск, дорожки, тонарм. Вращение непрерывное и не сбрасывается.
 */
@Composable
fun EchoVinyl(
    labelColorHex: Long,
    coverUrl: String?,
    isKnown: Boolean,
    modifier: Modifier = Modifier,
    coverBitmap: Bitmap? = null,
) {
    val transition = rememberInfiniteTransition(label = "vinyl")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "vinyl-angle",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = if (isKnown) angle else 0f },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2f
                drawCircle(color = Color(0xFF0A0E15), radius = radius)
                drawCircle(
                    color = EchoColors.LineStrong.copy(alpha = 0.5f),
                    radius = radius - 1.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx()),
                )

                repeat(7) { index ->
                    drawCircle(
                        color = EchoColors.LineStrong.copy(alpha = 0.30f),
                        radius = radius * (0.44f + index * 0.075f),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }

                drawCircle(
                    color = Color(labelColorHex).copy(alpha = 0.85f),
                    radius = radius * 0.30f,
                )
                drawCircle(
                    color = EchoColors.Background,
                    radius = radius * 0.035f,
                )
            }

            if (coverBitmap != null || coverUrl != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                ) {
                    if (coverBitmap != null) {
                        Image(
                            bitmap = coverBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(10.dp)
                            .background(EchoColors.Background, CircleShape),
                    )
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val pivot = androidx.compose.ui.geometry.Offset(size.width * 0.88f, size.height * 0.12f)
            val tip = androidx.compose.ui.geometry.Offset(size.width * 0.67f, size.height * 0.44f)
            drawLine(
                color = EchoColors.LineStrong,
                start = pivot,
                end = tip,
                strokeWidth = 4.dp.toPx(),
            )
            drawCircle(color = EchoColors.Amber, radius = 4.5.dp.toPx(), center = pivot)
            drawCircle(color = EchoColors.TextDim, radius = 2.2.dp.toPx(), center = tip)
        }
    }
}
