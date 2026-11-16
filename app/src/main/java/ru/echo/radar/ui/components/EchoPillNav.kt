package ru.echo.radar.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.echo.radar.ui.navigation.EchoTab
import ru.echo.radar.ui.theme.EchoColors

/**
 * Плавающая пилюльная навигация ЭХО 0.2: активная вкладка раскрывает подпись,
 * остальные остаются иконками. Живёт поверх контента, не съедая экран.
 */
@Composable
fun EchoPillNav(
    currentRoute: String?,
    onSelect: (EchoTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(EchoColors.SurfaceElevated.copy(alpha = 0.96f))
                .border(1.dp, EchoColors.Line, RoundedCornerShape(999.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EchoTab.entries.forEach { tab ->
                val selected = currentRoute == tab.route
                val background by animateColorAsState(
                    targetValue = if (selected) EchoColors.SignalCyan else Color.Transparent,
                    animationSpec = tween(durationMillis = 200),
                    label = "nav-bg",
                )
                val foreground by animateColorAsState(
                    targetValue = if (selected) Color(0xFF0B0E14) else EchoColors.TextDim,
                    animationSpec = tween(durationMillis = 200),
                    label = "nav-fg",
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(background)
                        .clickable { onSelect(tab) }
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = foreground,
                        modifier = Modifier.size(17.dp),
                    )
                    if (selected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            color = foreground,
                        )
                    }
                }
            }
        }
    }
}
