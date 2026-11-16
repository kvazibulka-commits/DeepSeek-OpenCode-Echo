package ru.echo.radar.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import ru.echo.radar.domain.model.EchoSignal
import ru.echo.radar.domain.model.EchoZone
import ru.echo.radar.domain.model.SavedTrack
import ru.echo.radar.domain.model.toSavedTrack
import ru.echo.radar.ui.components.EchoChoiceChip
import ru.echo.radar.ui.components.EchoEmptyState
import ru.echo.radar.ui.components.EchoExpressiveHeader
import ru.echo.radar.ui.components.EchoHeroSonarCard
import ru.echo.radar.ui.components.EchoMetric
import ru.echo.radar.ui.components.EchoSectionLabel
import ru.echo.radar.ui.components.EchoSignalRow
import ru.echo.radar.ui.components.EchoSignalSheet
import ru.echo.radar.ui.components.EchoStatusLine
import ru.echo.radar.ui.components.ResonanceBeam
import ru.echo.radar.ui.components.pluralRu
import ru.echo.radar.ui.theme.EchoColors
import kotlinx.coroutines.launch

private const val RESONANCE_THRESHOLD = 85
private const val BEAM_COOLDOWN_MS = 10_000L

@Composable
fun AirScreen(repository: EchoRepository, onOpenSettings: () -> Unit) {
    val snapshot by repository.snapshot.collectAsStateWithLifecycle()
    val config by repository.config.collectAsStateWithLifecycle()
    val track by repository.track.collectAsStateWithLifecycle()
    val savedTracks by repository.savedTracks.collectAsStateWithLifecycle()

    var selectedSignal by remember { mutableStateOf<EchoSignal?>(null) }
    var showCaches by rememberSaveable { mutableStateOf(true) }
    var zoom by remember { mutableStateOf(1f) }
    val beamProgress = remember { Animatable(0f) }
    var beamSignal by remember { mutableStateOf<EchoSignal?>(null) }
    var knownSignalIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var previousCompatibility by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var lastBeamAt by remember { mutableStateOf(0L) }
    var lastTrackKey by remember { mutableStateOf("") }
    val beamScope = rememberCoroutineScope()

    val topResonance = snapshot.signals.maxOfOrNull { it.compatibility } ?: 0
    val trackKey = "${track.artist}|${track.title}"

    LaunchedEffect(snapshot.signals, trackKey) {
        if (trackKey != lastTrackKey) {
            lastTrackKey = trackKey
            knownSignalIds = emptySet()
            previousCompatibility = emptyMap()
        }

        if (!track.isKnown) {
            knownSignalIds = snapshot.signals.map { it.id }.toSet()
            previousCompatibility = snapshot.signals.associate { it.id to it.compatibility }
            return@LaunchedEffect
        }

        val now = System.currentTimeMillis()
        val newcomers = snapshot.signals.filter {
            it.id !in knownSignalIds && it.compatibility >= RESONANCE_THRESHOLD && !it.isGhost
        }
        val crossed = snapshot.signals.filter { signal ->
            val previous = previousCompatibility[signal.id]
            previous != null && previous < RESONANCE_THRESHOLD &&
                signal.compatibility >= RESONANCE_THRESHOLD && !signal.isGhost
        }

        knownSignalIds = snapshot.signals.map { it.id }.toSet()
        previousCompatibility = snapshot.signals.associate { it.id to it.compatibility }

        val candidate = (newcomers + crossed).maxByOrNull { it.compatibility }
        if (candidate != null && beamSignal == null && now - lastBeamAt >= BEAM_COOLDOWN_MS) {
            beamSignal = candidate
            lastBeamAt = now
            beamScope.launch {
                beamProgress.snapTo(0f)
                beamProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
                )
                beamSignal = null
            }
        }
    }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                EchoChoiceChip(
                    text = if (showCaches) "СКРЫТЬ ТАЙНИКИ" else "ПОКАЗАТЬ ТАЙНИКИ",
                    selected = !showCaches,
                    onClick = { showCaches = !showCaches },
                    accent = EchoColors.Amber,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            EchoHeroSonarCard(
                signals = snapshot.signals,
                capsules = if (showCaches) snapshot.capsules else emptyList(),
                thermalPatches = snapshot.thermalPatches,
                lagrange = snapshot.lagrange,
                syncedTargets = snapshot.syncedTargets,
                myColorHex = snapshot.myEcho.colorHex,
                isDawn = snapshot.isDawn,
                zoneCount = snapshot.zones.size,
                onSignalClick = { selectedSignal = it },
                zoom = zoom,
                onZoom = { factor -> zoom = (zoom * factor).coerceIn(1f, 2.5f) },
                beam = beamSignal?.let { signal ->
                    ResonanceBeam(
                        angleDegrees = signal.angleDegrees,
                        distanceRatio = signal.distanceRatio,
                        progress = { beamProgress.value },
                    )
                },
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
    }

    selectedSignal?.let { signal ->
        EchoSignalSheet(
            signal = signal,
            onDismiss = { selectedSignal = null },
            isSaved = savedTracks.any {
                it.id == SavedTrack.makeId(signal.artist, signal.title)
            },
            onToggleSave = { repository.toggleSaved(signal.toSavedTrack()) },
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
