package ru.echo.radar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.echo.radar.data.repository.EchoRepository
import ru.echo.radar.data.settings.EchoSettingsStore
import ru.echo.radar.media.NotificationAccess
import ru.echo.radar.ui.components.EchoActionRow
import ru.echo.radar.ui.components.EchoChip
import ru.echo.radar.ui.components.EchoExpressiveHeader
import ru.echo.radar.ui.components.EchoPillButton
import ru.echo.radar.ui.components.EchoQuote
import ru.echo.radar.ui.components.EchoSheetTitle
import ru.echo.radar.ui.components.EchoStatusLine
import ru.echo.radar.ui.components.EchoVinylHero
import ru.echo.radar.ui.theme.EchoColors

@Composable
fun DeckScreen(
    repository: EchoRepository,
    settingsStore: EchoSettingsStore,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snapshot by repository.snapshot.collectAsStateWithLifecycle()
    val track by repository.track.collectAsStateWithLifecycle()
    val config by repository.config.collectAsStateWithLifecycle()

    var mediaAccess by remember { mutableStateOf(NotificationAccess.isEnabled(context)) }
    LifecycleResumeEffect(Unit) {
        mediaAccess = NotificationAccess.isEnabled(context)
        onPauseOrDispose { }
    }

    var showManualDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        EchoExpressiveHeader(
            eyebrow = "Дека · 33⅓",
            title = "Дека",
            subtitle = if (track.isKnown) "Сейчас на игле" else "Игла ждёт трек",
            onOpenSettings = onOpenSettings,
        )
        EchoStatusLine(connection = snapshot.connection, totalObjects = snapshot.totalObjects)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            EchoVinylHero(
                labelColorHex = snapshot.myEcho.colorHex,
                coverUrl = null,
                isKnown = track.isKnown,
                topChip = "33⅓ · живой винил",
                bottomChip = track.sourceApp.ifBlank { "игла ждёт" },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = track.displayTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = EchoColors.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = track.displayArtist,
            style = MaterialTheme.typography.titleMedium,
            color = EchoColors.SkyBlue,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            EchoChip(config.zoneName)
            Spacer(modifier = Modifier.padding(horizontal = 3.dp))
            if (snapshot.myEcho.moodLabel.isNotBlank()) {
                EchoChip(snapshot.myEcho.moodLabel, Color(snapshot.myEcho.colorHex))
            } else {
                EchoChip(track.sourceApp.ifBlank { "Системный перехват Android" })
            }
        }

        if (snapshot.myEcho.personaTitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            DeckCard {
                EchoSheetTitle("Архетип вечера")
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = snapshot.myEcho.personaTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(snapshot.myEcho.colorHex),
                )
                if (snapshot.myEcho.observation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    EchoQuote(snapshot.myEcho.observation)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        DeckCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Присутствие в эфире",
                        style = MaterialTheme.typography.titleSmall,
                        color = EchoColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (config.presenceEnabled) {
                            "Соседи слышат ваш сигнал, пока играет музыка"
                        } else {
                            "Сигнал скрыт — сонар вас не видит"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = EchoColors.TextDim,
                    )
                }
                Switch(
                    checked = config.presenceEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { settingsStore.savePresence(enabled) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EchoColors.Background,
                        checkedTrackColor = EchoColors.SignalCyan,
                        uncheckedThumbColor = EchoColors.TextDim,
                        uncheckedTrackColor = EchoColors.Line,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (!mediaAccess) {
            DeckCard {
                Text(
                    text = "ЭХО слушает плеер",
                    style = MaterialTheme.typography.titleSmall,
                    color = EchoColors.TextPrimary,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Разрешите доступ к уведомлениям — и треки из Яндекс Музыки, VK и Spotify будут попадать в эфир сами.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EchoColors.TextSecondary,
                    lineHeight = 16.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EchoPillButton(
                        label = "РАЗРЕШИТЬ ДОСТУП",
                        onClick = { NotificationAccess.openSettings(context) },
                        modifier = Modifier.weight(1.6f),
                    )
                    EchoPillButton(
                        label = "ВРУЧНУЮ",
                        onClick = { showManualDialog = true },
                        modifier = Modifier.weight(1f),
                        primary = false,
                    )
                }
            }
        } else {
            DeckCard {
                EchoSheetTitle("Действия")
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EchoPillButton(
                        label = "ВОЛНА В ЭФИР",
                        onClick = { repository.sendWave("broadcast") },
                        modifier = Modifier.weight(1.6f),
                    )
                    EchoPillButton(
                        label = "ТЕПЛО",
                        onClick = { repository.sendWarmth("broadcast", 0.5f, 0.5f) },
                        modifier = Modifier.weight(1f),
                        primary = false,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                EchoActionRow(
                    actions = listOf(
                        "Вручную" to { showManualDialog = true },
                        "Обновить" to { repository.refreshNow() },
                    ),
                )
            }
        }
    }

    if (showManualDialog) {
        ManualBroadcastDialog(
            onDismiss = { showManualDialog = false },
            onSubmit = { text ->
                repository.publishManual(text)
                showManualDialog = false
            },
        )
    }
}

@Composable
private fun DeckCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .background(EchoColors.SurfaceTonal, RoundedCornerShape(22.dp))
            .border(1.dp, EchoColors.Line, RoundedCornerShape(22.dp))
            .padding(15.dp),
        content = content,
    )
}

@Composable
private fun ManualBroadcastDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EchoColors.SurfaceElevated,
        titleContentColor = EchoColors.TextPrimary,
        textContentColor = EchoColors.TextSecondary,
        title = { Text("Трансляция вручную") },
        text = {
            Column {
                Text(
                    text = "Формат: Исполнитель — Трек",
                    style = MaterialTheme.typography.bodySmall,
                    color = EchoColors.TextDim,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = { Text("Молчат Дома — Судно") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onSubmit(text) },
                enabled = text.isNotBlank(),
            ) {
                Text("В ЭФИР", color = EchoColors.SignalCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = EchoColors.TextSecondary)
            }
        },
    )
}
