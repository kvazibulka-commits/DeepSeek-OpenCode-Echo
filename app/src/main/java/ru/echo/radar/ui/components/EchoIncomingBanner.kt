package ru.echo.radar.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.echo.radar.domain.model.IncomingEcho
import ru.echo.radar.ui.theme.EchoColors

/** Отклик из эфира: показывается поверх любого экрана, живёт 5 секунд. */
@Composable
fun EchoIncomingBanner(
    incoming: IncomingEcho?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = incoming != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = modifier,
    ) {
        val item = incoming ?: return@AnimatedVisibility

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(EchoColors.SurfaceElevated.copy(alpha = 0.98f))
                .border(1.dp, EchoColors.Gold.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                .clickable(onClick = onDismiss)
                .padding(horizontal = 14.dp, vertical = 11.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.symbol,
                    fontSize = 22.sp,
                    color = EchoColors.Gold,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ОТКЛИК ИЗ ЭФИРА",
                        style = MaterialTheme.typography.labelSmall,
                        color = EchoColors.Gold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.message,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = EchoColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.senderId} · ${formatAgo(item.timeSec)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = EchoColors.TextDim,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EchoColors.TextDim,
                )
            }
        }
    }
}
