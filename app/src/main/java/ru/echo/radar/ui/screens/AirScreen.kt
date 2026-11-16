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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import ru.echo.radar.ui.components.EchoExpressiveHeader
import ru.echo.radar.ui.components.EchoHeroSonarCard
import ru.echo.radar.ui.components.EchoMetric
import ru.echo.radar.ui.components.EchoSectionLabel
import ru.echo.radar.ui.components.EchoSegmentedControl
import ru.echo.radar.ui.components.EchoSignalRow
import ru.echo.radar.ui.components.EchoSignalSheet
import ru.echo.radar.ui.components.EchoStatusLine
import ru.echo.radar.ui.components.pluralRu
import ru.echo.radar.ui.theme.EchoColors

@Composable
fun AirScreen(repository: EchoRepository, onOpenSettings: () -> Unit) {
    val snapshot by repository.snapshot.collectAsStateWithLifecycle()
    val config by repository.config.collectAsStateWithLifecycle()

    var selectedSignal by remember { mutableStateOf<EchoSignal?>(null) }
    var selectedCapsule by remember { mutableStateOf<EchoCapsule?>(null) }
    var mode by rememberSaveable { mutableStateOf(0) }

    val topResonance = snapshot.signals.maxOfOrNull { it.compatibility } ?: 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 18.dp),
    ) {
        item {
            EchoExpressiveHeader(
                eyebrow = "Сонар · ${config.zoneName}",
                title = "Эфир",
                subtitle = "Музыка, которая живёт вокруг",
                onOpenSettings = onOpenSettings,
            )
            Spacer(modifier = Modifier.height(4.dp))
            EchoMetric(
                value = snapshot.signals.size.toString(),
                caption = if (topResonance > 0) {
                    "${snapshot.signals.size} ${pluralRu(snapshot.signals.size, "голос", "голоса", "голосов")} рядом · резонанс до $topResonance%"
                } else {
                    "сонар слушает тишину"
                },
            )
            EchoStatusLine(
                connection = snapshot.connection,
                totalObjects = snapshot.totalObjects,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            EchoSegmentedControl(
                options = listOf("СОНАР", "ТАЙНИКИ"),
                selectedIndex = mode,
                onSelect = { mode = it },
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (mode == 0) {
            item {
                EchoHeroSonarCard(
                    signals = snapshot.signals,
                    capsules = snapshot.capsules,
                    thermalPatches = snapshot.thermalPatches,
                    lagrange = snapshot.lagrange,
                    syncedTargets = snapshot.syncedTargets,
                    myColorHex = snapshot.myEcho.colorHex,
                    isDawn = snapshot.isDawn,
                    zoneCount = snapshot.zones.size,
                    onSignalClick = { selectedSignal = it },
                )
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                EchoSectionLabel("Рядом · ${snapshot.signals.size}")
            }

            if (snapshot.signals.isEmpty()) {
                item {
                    EchoEmptyState(
                        title = "В эфире тихо",
                        body = "Включите музыку на телефоне — и город ответит волной. Пока сонар слышит только тишину.",
                    )
                }
            } else {
                itemsIndexed(snapshot.signals, key = { _, signal -> signal.id }) { index, signal ->
                    EchoSignalRow(
                        signal = signal,
                        index = index,
                        onClick = { selectedSignal = signal },
                    )
                }
            }

            if (snapshot.zones.isNotEmpty()) {
                item { ZoneStrip(snapshot.zones) }
            }
        } else {
            item { EchoSectionLabel("Тайники в эфире · ${snapshot.capsules.size}") }

            if (snapshot.capsules.isEmpty()) {
                item {
                    EchoEmptyState(
                        title = "Тайников пока нет",
                        body = "Спрячьте первый трек с посланием — или дождитесь, пока кто-то оставит капсулу в вашем квартале.",
                    )
                }
            } else {
                items(snapshot.capsules, key = { it.id }) { capsule ->
                    EchoCapsuleRow(capsule = capsule, onClick = { selectedCapsule = capsule })
                }
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
private fun ZoneStrip(zones: List<EchoZone>) {
    Column(modifier = Modifier.padding(top = 10.dp)) {
        EchoSectionLabel("Кварталы")
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            zones.forEach { zone ->
                Row(
                    modifier = Modifier
                        .background(EchoColors.SurfaceTonal, RoundedCornerShape(14.dp))
                        .border(1.dp, EchoColors.Line, RoundedCornerShape(14.dp))
                        .padding(horizontal = 11.dp, vertical = 9.dp),
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
    }
}
