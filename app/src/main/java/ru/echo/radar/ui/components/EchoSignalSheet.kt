package ru.echo.radar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ru.echo.radar.domain.model.EchoSignal
import ru.echo.radar.ui.theme.EchoColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchoSignalSheet(
    signal: EchoSignal,
    onDismiss: () -> Unit,
    onWave: () -> Unit,
    onNeedle: () -> Unit,
    onReact: () -> Unit,
    onWarmth: () -> Unit,
    isSaved: Boolean = false,
    onToggleSave: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = Color(signal.colorHex)
    var menuOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = EchoColors.SurfaceElevated,
        contentColor = EchoColors.TextPrimary,
        dragHandle = { EchoSheetHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "ПАСПОРТ СИГНАЛА",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                    color = EchoColors.TextDim,
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onToggleSave) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = if (isSaved) "Убрать из сохранённого" else "Сохранить в отзвуки",
                        tint = if (isSaved) EchoColors.Gold else EchoColors.TextDim,
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Другие действия",
                            tint = EchoColors.TextDim,
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Отклик ✦") },
                            onClick = {
                                menuOpen = false
                                onReact()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Тёплое пятно") },
                            onClick = {
                                menuOpen = false
                                onWarmth()
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(EchoColors.Surface)
                    .border(1.dp, EchoColors.Line, RoundedCornerShape(20.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(accent.copy(alpha = 0.28f), EchoColors.SurfaceTonal),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (signal.coverUrl != null) {
                        AsyncImage(
                            model = signal.coverUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        EchoAvatar(
                            petals = signal.avatarPetals,
                            colorHex = signal.colorHex,
                            size = 76.dp,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = signal.personaTitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = signal.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                        color = EchoColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = signal.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = EchoColors.SkyBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = signal.album.ifBlank { "Альбом не передан" },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (signal.album.isBlank()) EchoColors.TextDim else EchoColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${signal.sourceName} · ${formatDistance(signal.distanceMeters)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                        color = EchoColors.TextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                EchoChip("Резонанс ${signal.compatibility}%", accent)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "совпадение с вашим треком",
                    style = MaterialTheme.typography.bodySmall,
                    color = EchoColors.TextDim,
                    maxLines = 1,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            EchoSheetTitle("О ТРЕКЕ")
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${signal.zoneName} · ${signal.moodLabel} · ${signal.genre}",
                style = MaterialTheme.typography.bodyMedium,
                color = EchoColors.TextSecondary,
            )

            if (signal.resonanceStory.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                EchoSheetTitle("ПОЧЕМУ СОВПАЛО")
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = signal.resonanceStory,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EchoColors.TextSecondary,
                )
            }

            if (signal.personaObservation.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                EchoQuote(signal.personaObservation)
            }

            if (signal.encountersCount > 1 || signal.isGhost) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (signal.encountersCount > 1) {
                        EchoChip("Встреч: ${signal.encountersCount}", EchoColors.Gold)
                    }
                    if (signal.isGhost) {
                        EchoChip("Призрачный след · ${signal.ageMinutes} мин", EchoColors.Amber)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EchoPillButton(
                    label = "ВОЛНА",
                    onClick = onWave,
                    modifier = Modifier.weight(1.6f),
                )
                EchoPillButton(
                    label = "ИГЛА",
                    onClick = onNeedle,
                    modifier = Modifier.weight(1f),
                    primary = false,
                )
            }
        }
    }
}
