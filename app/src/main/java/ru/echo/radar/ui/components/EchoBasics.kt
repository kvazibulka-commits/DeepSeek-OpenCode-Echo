package ru.echo.radar.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.echo.radar.domain.model.EchoConnection
import ru.echo.radar.ui.theme.EchoColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EchoScreenHeader(
    title: String,
    subtitle: String,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = EchoColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = EchoColors.TextSecondary,
                maxLines = 2,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Настройки",
                tint = EchoColors.TextSecondary,
            )
        }
    }
}

@Composable
fun EchoSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = EchoColors.TextDim,
        modifier = modifier.padding(horizontal = 18.dp, vertical = 8.dp),
    )
}

@Composable
fun EchoChip(
    text: String,
    color: Color = EchoColors.TextSecondary,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(EchoColors.Line.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
            .border(1.dp, EchoColors.LineStrong, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Default,
                fontSize = 10.sp,
                letterSpacing = 0.2.sp,
            ),
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
fun EchoChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    accent: Color = EchoColors.SignalCyan,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = if (selected) accent else EchoColors.Line.copy(alpha = 0.4f),
                shape = RoundedCornerShape(999.dp),
            )
            .border(
                width = 1.dp,
                color = if (selected) accent else EchoColors.LineStrong,
                shape = RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Default,
                fontSize = 10.5.sp,
                letterSpacing = 0.2.sp,
            ),
            color = if (selected) Color.Black else EchoColors.TextSecondary,
            maxLines = 1,
        )
    }
}

@Composable
fun EchoStatusLine(connection: EchoConnection, totalObjects: Int, modifier: Modifier = Modifier) {
    val (text, color) = when (connection) {
        EchoConnection.Online -> "$totalObjects в эфире · линия живая" to EchoColors.Green
        EchoConnection.Connecting -> "Сонар настраивается…" to EchoColors.SkyBlue
        EchoConnection.Offline -> "Сервер не отвечает — эфир замер" to EchoColors.Danger
        EchoConnection.Unauthorized -> "Токен не принят — проверьте доступ" to EchoColors.Danger
        EchoConnection.Idle -> "Укажите адрес сервера в настройках" to EchoColors.TextDim
    }

    Row(
        modifier = modifier.padding(horizontal = 18.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape),
        )
        Spacer(modifier = Modifier.size(7.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = EchoColors.TextSecondary,
        )
    }
}

@Composable
fun EchoEmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 20.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = EchoColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = EchoColors.TextDim,
            lineHeight = 17.sp,
        )
    }
}

@Composable
fun EchoDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 18.dp),
        thickness = 1.dp,
        color = EchoColors.Line,
    )
}

/** Генеративный аватар: лепестки цвета архетипа вместо лица. */
@Composable
fun EchoAvatar(
    petals: Int,
    colorHex: Long,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    val color = Color(colorHex)
    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        drawCircle(color = EchoColors.Background, radius = radius)
        drawCircle(
            color = color.copy(alpha = 0.30f),
            radius = radius - 0.5.dp.toPx(),
            style = Stroke(width = 1.dp.toPx()),
        )

        val petalCount = petals.coerceIn(3, 9)
        val step = 360f / petalCount
        repeat(petalCount) { index ->
            rotate(degrees = index * step) {
                drawOval(
                    color = color.copy(alpha = 0.50f),
                    topLeft = Offset(center.x - radius * 0.16f, center.y - radius * 0.86f),
                    size = Size(radius * 0.32f, radius * 0.56f),
                )
            }
        }

        drawCircle(color = color.copy(alpha = 0.22f), radius = radius * 0.34f)
        drawCircle(color = EchoColors.Background, radius = radius * 0.07f)
    }
}

@Composable
fun EchoQuote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Normal,
        ),
        color = EchoColors.TextSecondary,
        lineHeight = 17.sp,
        modifier = modifier,
    )
}

@Composable
fun EchoActionRow(
    actions: List<Pair<String, () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        actions.forEach { (label, action) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(EchoColors.Line.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .border(1.dp, EchoColors.LineStrong, RoundedCornerShape(10.dp))
                    .clickable(onClick = action),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = EchoColors.SkyBlue,
                    maxLines = 1,
                )
            }
        }
    }
}

fun formatDistance(meters: Int): String = when {
    meters < 40 -> "~$meters м · рядом"
    meters < 150 -> "~$meters м"
    else -> "~$meters м · размыто"
}
