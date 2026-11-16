package ru.echo.radar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import ru.echo.radar.data.repository.EchoRepository
import ru.echo.radar.data.discovery.EchoDiscovery
import ru.echo.radar.data.settings.EchoSettingsStore
import ru.echo.radar.domain.model.ECHO_ZONES
import ru.echo.radar.domain.model.EchoConnection
import ru.echo.radar.ui.components.EchoChoiceChip
import ru.echo.radar.ui.components.EchoSectionLabel
import ru.echo.radar.ui.components.EchoStatusLine
import ru.echo.radar.ui.theme.EchoColors

@Composable
fun SettingsScreen(
    repository: EchoRepository,
    settingsStore: EchoSettingsStore,
    onBack: () -> Unit,
    onReplayWelcome: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snapshot by repository.snapshot.collectAsStateWithLifecycle()
    val config by repository.config.collectAsStateWithLifecycle()

    var host by rememberSaveable(config.host) { mutableStateOf(config.host) }
    var token by rememberSaveable(config.token) { mutableStateOf(config.token) }
    var zone by rememberSaveable(config.zoneName) { mutableStateOf(config.zoneName) }
    var tokenVisible by rememberSaveable { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    val versionName = remember {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "0.1.0"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = EchoColors.TextSecondary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "НАСТРОЙКИ",
                    style = MaterialTheme.typography.titleLarge,
                    color = EchoColors.TextPrimary,
                )
                Text(
                    text = "Адрес, доступ и зона",
                    style = MaterialTheme.typography.bodySmall,
                    color = EchoColors.TextSecondary,
                )
            }
        }

        EchoStatusLine(connection = snapshot.connection, totalObjects = snapshot.totalObjects)

        EchoSectionLabel("СЕРВЕР")
        Column(modifier = Modifier.padding(horizontal = 18.dp)) {
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text("Адрес") },
                placeholder = { Text("192.168.1.5:8080 или https://…trycloudflare.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Домашняя сеть — http, туннель — https. Схему можно указать вручную.",
                style = MaterialTheme.typography.bodySmall,
                color = EchoColors.TextDim,
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Токен доступа") },
                singleLine = true,
                visualTransformation = if (tokenVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    Text(
                        text = if (tokenVisible) "СКРЫТЬ" else "ПОКАЗАТЬ",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = EchoColors.SkyBlue,
                        modifier = Modifier
                            .clickable { tokenVisible = !tokenVisible }
                            .padding(6.dp),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "В домашней сети токен не обязателен. Вне её — обязателен, иначе сервер ответит 401.",
                style = MaterialTheme.typography.bodySmall,
                color = EchoColors.TextDim,
            )
        }

        EchoSectionLabel("ЗОНА ГОРОДА")
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ECHO_ZONES.forEach { name ->
                EchoChoiceChip(
                    text = name,
                    selected = zone == name,
                    onClick = { zone = name },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Присутствие в эфире",
                    style = MaterialTheme.typography.titleSmall,
                    color = EchoColors.TextPrimary,
                )
                Text(
                    text = "Выключите, чтобы исчезнуть из сонара, не закрывая музыку.",
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

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    scope.launch {
                        settingsStore.saveConnection(host, token)
                        settingsStore.saveZone(zone)
                        repository.refreshNow()
                        statusText = "Сохранено — сонар перестраивается"
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EchoColors.SignalCyan,
                    contentColor = Color.Black,
                ),
            ) {
                Text("СОХРАНИТЬ", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        isChecking = true
                        val result = repository.verifyConnection(host, token)
                        isChecking = false
                        statusText = when (result) {
                            EchoConnection.Online -> "Линия живая — сервер отвечает"
                            EchoConnection.Unauthorized -> "Токен не принят — проверьте доступ"
                            EchoConnection.Offline -> "Сервер не отвечает — проверьте адрес"
                            EchoConnection.Connecting -> "Проверяем…"
                            EchoConnection.Idle -> "Сначала укажите адрес"
                        }
                    }
                },
                enabled = !isChecking,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = if (isChecking) "ПРОВЕРЯЕМ…" else "ПРОВЕРИТЬ",
                    style = MaterialTheme.typography.labelMedium,
                    color = EchoColors.SkyBlue,
                )
            }
        }

        statusText?.let { text ->
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = EchoColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 18.dp),
            )
        }

        snapshot.lastError?.let { detail ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Последняя ошибка: $detail",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = EchoColors.Danger,
                maxLines = 3,
                modifier = Modifier.padding(horizontal = 18.dp),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                scope.launch {
                    isChecking = true
                    statusText = "Ищу сервер в Wi-Fi…"
                    val found = EchoDiscovery.findServer()
                    isChecking = false
                    if (found != null) {
                        host = found
                        settingsStore.saveConnection(found, token)
                        repository.refreshNow()
                        statusText = "Найден сервер: $found"
                    } else {
                        statusText = "Сервер не найден — проверьте Wi-Fi и run-server.bat"
                    }
                }
            },
            enabled = !isChecking,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
        ) {
            Text(
                text = "НАЙТИ СЕРВЕР В WI-FI",
                style = MaterialTheme.typography.labelMedium,
                color = EchoColors.SkyBlue,
            )
        }

        EchoSectionLabel("О ПРИЛОЖЕНИИ")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .background(EchoColors.Surface, RoundedCornerShape(16.dp))
                .border(1.dp, EchoColors.Line, RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            Text(
                text = "ЭХО · $versionName",
                style = MaterialTheme.typography.labelMedium,
                color = EchoColors.SkyBlue,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Сонар музыкального присутствия. Он показывает не людей, а музыку, которая сейчас живёт вокруг, и позволяет тихо на неё откликнуться.",
                style = MaterialTheme.typography.bodySmall,
                color = EchoColors.TextSecondary,
                lineHeight = 17.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Координаты размываются до зоны. Точное место не показывается никогда. Нет лайков, профилей и рекламы.",
                style = MaterialTheme.typography.bodySmall,
                color = EchoColors.TextDim,
                lineHeight = 17.sp,
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            onClick = onReplayWelcome,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
        ) {
            Text(
                text = "ПОКАЗАТЬ ЗНАКОМСТВО",
                style = MaterialTheme.typography.labelMedium,
                color = EchoColors.SkyBlue,
            )
        }
    }
}
