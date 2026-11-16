package ru.echo.radar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.echo.radar.domain.model.EchoSignal
import ru.echo.radar.ui.theme.EchoColors

@Composable
fun EchoSignalCard(signal: EchoSignal, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = Color(signal.colorHex)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(EchoColors.Surface)
            .border(1.dp, EchoColors.Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(accent),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${signal.artist} — ${signal.title}",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = EchoColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (signal.isGhost) {
                        Spacer(modifier = Modifier.width(6.dp))
                        EchoChip("призрак", EchoColors.Amber)
                    }
                }

                if (signal.album.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = signal.album,
                        style = MaterialTheme.typography.bodySmall,
                        color = EchoColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    EchoChip("${signal.compatibility}%", accent)
                    if (signal.hasTrustedGenre) {
                        EchoChip(signal.genreLabel.ifBlank { signal.genre })
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = signal.personaTitle,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = accent.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(
                modifier = Modifier.padding(top = 12.dp, end = 12.dp),
                horizontalAlignment = Alignment.End,
            ) {
                EchoAvatar(petals = signal.avatarPetals, colorHex = signal.colorHex, size = 40.dp)
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = formatDistance(signal.distanceMeters),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = EchoColors.TextDim,
                    maxLines = 1,
                )
            }
        }
    }
}
