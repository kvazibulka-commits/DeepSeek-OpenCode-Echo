package ru.echo.radar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.echo.radar.data.repository.EchoRepository
import ru.echo.radar.domain.model.EchoReaction
import ru.echo.radar.ui.components.EchoEmptyState
import ru.echo.radar.ui.components.EchoScreenHeader
import ru.echo.radar.ui.components.EchoSectionLabel
import ru.echo.radar.ui.components.EchoStatusLine
import ru.echo.radar.ui.components.formatAgo
import ru.echo.radar.ui.theme.EchoColors

@Composable
fun EchoesScreen(repository: EchoRepository, onOpenSettings: () -> Unit) {
    val snapshot by repository.snapshot.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 18.dp),
    ) {
        item {
            EchoScreenHeader(
                title = "ОТЗВУКИ",
                subtitle = "Следы вечера и чужие волны",
                onOpenSettings = onOpenSettings,
            )
            EchoStatusLine(connection = snapshot.connection, totalObjects = snapshot.totalObjects)
        }

        item { EchoSectionLabel("ОТКЛИКИ ИЗ ЭФИРА") }

        val reactions = snapshot.reactions.reversed()
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

        item { EchoSectionLabel("ЛЕТОПИСЬ ВЕЧЕРА") }

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
