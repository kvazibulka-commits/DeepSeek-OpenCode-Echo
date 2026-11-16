package ru.echo.radar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.echo.radar.data.repository.EchoRepository
import ru.echo.radar.domain.model.EchoCapsule
import ru.echo.radar.domain.model.EchoSignal
import ru.echo.radar.domain.model.EchoZone
import ru.echo.radar.ui.components.EchoCapsuleRow
import ru.echo.radar.ui.components.EchoCapsuleSheet
import ru.echo.radar.ui.components.EchoEmptyState
import ru.echo.radar.ui.components.EchoScreenHeader
import ru.echo.radar.ui.components.EchoSectionLabel
import ru.echo.radar.ui.components.EchoSignalCard
import ru.echo.radar.ui.components.EchoSignalSheet
import ru.echo.radar.ui.components.EchoSonar
import ru.echo.radar.ui.components.EchoStatusLine
import ru.echo.radar.ui.components.formatDistance
import ru.echo.radar.ui.theme.EchoColors

@Composable
fun AirScreen(repository: EchoRepository, onOpenSettings: () -> Unit) {
    val snapshot by repository.snapshot.collectAsStateWithLifecycle()
    var selectedSignal by remember { mutableStateOf<EchoSignal?>(null) }
    var selectedCapsule by remember { mutableStateOf<EchoCapsule?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 18.dp),
    ) {
        item {
            EchoScreenHeader(
                title = "ЭФИР",
                subtitle = "Сонар музыкального присутствия",
                onOpenSettings = onOpenSettings,
            )
            EchoStatusLine(
                connection = snapshot.connection,
                totalObjects = snapshot.totalObjects,
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                EchoSonar(
                    signals = snapshot.signals,
                    capsules = snapshot.capsules,
                    thermalPatches = snapshot.thermalPatches,
                    lagrange = snapshot.lagrange,
                    syncedTargets = snapshot.syncedTargets,
                    myColorHex = snapshot.myEcho.colorHex,
                    isDawn = snapshot.isDawn,
                    onSignalClick = { selectedSignal = it },
                )
            }
        }

        item {
            SonarLegend(
                signalCount = snapshot.signals.size,
                capsuleCount = snapshot.capsules.size,
                hasLagrange = snapshot.lagrange != null,
                isDawn = snapshot.isDawn,
            )
        }

        if (snapshot.zones.isNotEmpty()) {
            item { ZoneStrip(snapshot.zones) }
        }

        item { EchoSectionLabel("СИГНАЛЫ РЯДОМ") }

        if (snapshot.signals.isEmpty()) {
            item {
                EchoEmptyState(
                    title = "В эфире тихо",
                    body = "Включите музыку на телефоне — и город ответит волной. Пока сонар слышит только тишину.",
                )
            }
        } else {
            items(snapshot.signals, key = { it.id }) { signal ->
                EchoSignalCard(signal = signal, onClick = { selectedSignal = signal })
            }
        }

        if (snapshot.capsules.isNotEmpty()) {
            item { EchoSectionLabel("ТАЙНИКИ В ЭФИРЕ") }
            items(snapshot.capsules, key = { it.id }) { capsule ->
                EchoCapsuleRow(capsule = capsule, onClick = { selectedCapsule = capsule })
            }
        }
    }

    selectedSignal?.let { signal ->
        EchoSignalSheet(
            signal = signal,
            onDismiss = { selectedSignal = null },
            onWave = {
                repository.sendWave(signal.id)
                selectedSignal = null
            },
            onNeedle = {
                repository.syncNeedle(signal.id)
                selectedSignal = null
            },
            onReact = {
                repository.react(signal.id)
                selectedSignal = null
            },
            onWarmth = {
                repository.sendWarmth(signal.id, 0.5f, 0.5f)
                selectedSignal = null
            },
        )
    }

    selectedCapsule?.let { capsule ->
        EchoCapsuleSheet(capsule = capsule, onDismiss = { selectedCapsule = null })
    }
}

@Composable
private fun SonarLegend(
    signalCount: Int,
    capsuleCount: Int,
    hasLagrange: Boolean,
    isDawn: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LegendItem(label = "СИГНАЛЫ $signalCount", color = EchoColors.SkyBlue)
        LegendItem(label = "ТАЙНИКИ $capsuleCount", color = EchoColors.Amber)
        if (hasLagrange) {
            LegendItem(label = "ТОЧКА ЛАГРАНЖА", color = EchoColors.Gold)
        }
        if (isDawn) {
            LegendItem(label = "ШУМ РАССВЕТА", color = EchoColors.Gold)
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(color, CircleShape),
        )
        Spacer(modifier = Modifier.size(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
            color = EchoColors.TextDim,
            maxLines = 1,
        )
    }
}

@Composable
private fun ZoneStrip(zones: List<EchoZone>) {
    Column {
        EchoSectionLabel("КВАРТАЛЫ")
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            zones.forEach { zone ->
                Row(
                    modifier = Modifier
                        .background(EchoColors.Surface, RoundedCornerShape(12.dp))
                        .border(1.dp, EchoColors.Line, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color(zone.fogColorHex), CircleShape),
                    )
                    Spacer(modifier = Modifier.size(7.dp))
                    Column {
                        Text(
                            text = zone.placeName,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                            color = EchoColors.TextPrimary,
                            maxLines = 1,
                        )
                        Text(
                            text = "${zone.listenersCount} · ${zone.dominantVibe}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                            color = EchoColors.TextDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun DistanceHint(distance: Int) {
    Text(
        text = formatDistance(distance),
        style = MaterialTheme.typography.labelSmall,
        color = EchoColors.TextDim,
    )
}
