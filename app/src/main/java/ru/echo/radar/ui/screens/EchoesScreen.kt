package ru.echo.radar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import ru.echo.radar.data.repository.EchoRepository
import ru.echo.radar.domain.model.EchoReaction
import ru.echo.radar.domain.model.SavedTrack
import ru.echo.radar.ui.components.EchoEmptyState
import ru.echo.radar.ui.components.EchoExpressiveHeader
import ru.echo.radar.ui.components.EchoMetric
import ru.echo.radar.ui.components.EchoSectionLabel
import ru.echo.radar.ui.components.EchoSegmentedControl
import ru.echo.radar.ui.components.EchoStatusLine
import ru.echo.radar.ui.components.formatAgo
import ru.echo.radar.ui.theme.EchoColors

@Composable
fun EchoesScreen(repository: EchoRepository, onOpenSettings: () -> Unit) {
    val snapshot by repository.snapshot.collectAsStateWithLifecycle()
    val savedTracks by repository.savedTracks.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(0) }

    val reactions = snapshot.reactions.reversed()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 18.dp),
    ) {
        item {
            EchoExpressiveHeader(
                eyebrow = "Вечер · летопись",
                title = "Отзвуки",
                subtitle = "Следы вечера и чужие волны",
                onOpenSettings = onOpenSettings,
            )
            if (mode == 0) {
                EchoMetric(
                    value = reactions.size.toString(),
                    caption = "откликов за вечер",
                    color = EchoColors.Gold,
                )
            } else {
                EchoMetric(
                    value = savedTracks.size.toString(),
                    caption = "треков сохранено",
                    color = EchoColors.SkyBlue,
                )
            }
            EchoStatusLine(connection = snapshot.connection, totalObjects = snapshot.totalObjects)
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            EchoSegmentedControl(
                options = listOf("ОТКЛИКИ", "СОХРАНЕНО"),
                selectedIndex = mode,
                onSelect = { mode = it },
            )
        }

        if (mode == 0) {
            item { EchoSectionLabel("Отклики из эфира") }

            if (reactions.isEmpty()) {
                item {
                    EchoEmptyState(
                        title = "Пока тихо",
                        body = "Когда кто-то рядом ответит на вашу волну, отклик появится здесь — без счётчиков и лайков.",
                    )
                }
            } else {
                items(reactions, key = { it.id }) { reaction ->
                    ReactionRow(reaction)
                }
            }

            item { EchoSectionLabel("Летопись вечера") }

            if (snapshot.traces.isEmpty()) {
                item {
                    EchoEmptyState(
                        title = "Летопись пуста",
                        body = "Каждый новый трек оставляет строку в вечерней хронике. Включите музыку — и она начнётся.",
                    )
                }
            } else {
                items(snapshot.traces) { trace ->
                    TraceRow(trace)
                }
            }
        } else {
            item { EchoSectionLabel("Сохранённые треки · ${savedTracks.size}") }

            if (savedTracks.isEmpty()) {
                item {
                    EchoEmptyState(
                        title = "Пока ничего не сохранено",
                        body = "Сохраняйте треки из паспорта сигнала или с деки — они соберутся здесь, как кассеты в коробке.",
                    )
                }
            } else {
                items(savedTracks, key = { it.id }) { track ->
                    SavedTrackRow(
                        track = track,
                        onRemove = { repository.removeSaved(track.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReactionRow(reaction: EchoReaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 5.dp)
            .background(EchoColors.Surface, RoundedCornerShape(14.dp))
            .border(1.dp, EchoColors.Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = reaction.symbol,
            style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.sp, fontSize = 20.sp),
            color = EchoColors.Gold,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reaction.message.ifBlank { "Спасибо за созвучие" },
                style = MaterialTheme.typography.bodyMedium,
                color = EchoColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${reaction.senderId} · ${formatAgo(reaction.timeSec)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = EchoColors.TextDim,
            )
        }
    }
}

@Composable
private fun SavedTrackRow(track: SavedTrack, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(EchoColors.Surface)
            .border(1.dp, EchoColors.Line, RoundedCornerShape(16.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(EchoColors.SkyBlue, EchoColors.Purple),
                    ),
                ),
        ) {
            if (track.coverUrl != null) {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.artist.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, letterSpacing = 0.8.sp),
                color = EchoColors.SkyBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                ),
                color = EchoColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = listOf(
                    track.album,
                    "сохранено ${formatAgo(track.savedAt / 1000L)}",
                ).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = EchoColors.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = "✕",
            style = MaterialTheme.typography.bodyMedium,
            color = EchoColors.TextDim,
            modifier = Modifier
                .clickable(onClick = onRemove)
                .padding(8.dp),
        )
    }
}

@Composable
private fun TraceRow(trace: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(34.dp)
                .background(EchoColors.Amber.copy(alpha = 0.45f), RoundedCornerShape(1.dp)),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = trace,
            style = MaterialTheme.typography.bodySmall,
            color = EchoColors.TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
