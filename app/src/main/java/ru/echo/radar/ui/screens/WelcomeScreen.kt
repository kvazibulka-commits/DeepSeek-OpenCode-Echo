package ru.echo.radar.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.echo.radar.domain.model.ECHO_DEFAULT_COLOR
import ru.echo.radar.ui.components.EchoPillButton
import ru.echo.radar.ui.components.EchoSonar
import ru.echo.radar.ui.theme.EchoColors

/** Первый экран: что такое ЭХО и почему здесь нет людей и лайков. */
@Composable
fun WelcomeScreen(onEnter: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(EchoColors.SurfaceTonal, EchoColors.Background),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 30.dp),
        ) {
            Text(
                text = "ЭХО",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 42.sp,
                    letterSpacing = 8.sp,
                ),
                color = EchoColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Сонар музыкального присутствия",
                style = MaterialTheme.typography.bodySmall,
                color = EchoColors.TextSecondary,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                EchoSonar(
                    signals = emptyList(),
                    capsules = emptyList(),
                    thermalPatches = emptyList(),
                    lagrange = null,
                    syncedTargets = emptySet(),
                    myColorHex = ECHO_DEFAULT_COLOR,
                    isDawn = false,
                    onSignalClick = {},
                    modifier = Modifier.fillMaxWidth(0.9f),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            WelcomePoint(
                title = "Слышать город",
                body = "Сонар показывает музыку рядом: резонанс, темп, настроение. Без имён, лиц и профилей.",
                color = EchoColors.SkyBlue,
            )
            WelcomePoint(
                title = "Отвечать тихо",
                body = "Волна, тёплое пятно, синхрон иглы — способы дать знать, что вы рядом.",
                color = EchoColors.SignalCyan,
            )
            WelcomePoint(
                title = "Оставлять след",
                body = "Тайники с посланиями и бонусами живут в кварталах. Найдите или спрячьте первый.",
                color = EchoColors.Amber,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EchoColors.Surface, RoundedCornerShape(18.dp))
                    .border(1.dp, EchoColors.Line, RoundedCornerShape(18.dp))
                    .padding(15.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "◌",
                    style = MaterialTheme.typography.titleLarge,
                    color = EchoColors.Green,
                )
                Column {
                    Text(
                        text = "Приватность по умолчанию",
                        style = MaterialTheme.typography.titleSmall,
                        color = EchoColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Координаты размываются до зоны. Точное место не показывается никогда. Нет лайков, рекламы и подписчиков.",
                        style = MaterialTheme.typography.bodySmall,
                        color = EchoColors.TextDim,
                        lineHeight = 17.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            EchoPillButton(
                label = "ВОЙТИ В ЭФИР",
                onClick = onEnter,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Адрес сервера и токен уже сохранены. Их можно изменить в ⚙ Настройки.",
                style = MaterialTheme.typography.bodySmall,
                color = EchoColors.TextDim,
            )
        }
    }
}

@Composable
private fun WelcomePoint(title: String, body: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .background(color, RoundedCornerShape(3.dp)),
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = EchoColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = EchoColors.TextSecondary,
                lineHeight = 17.sp,
            )
        }
    }
}
