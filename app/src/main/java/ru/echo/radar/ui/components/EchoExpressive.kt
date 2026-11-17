package ru.echo.radar.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.echo.radar.domain.model.EchoCapsule
import ru.echo.radar.domain.model.EchoSignal
import ru.echo.radar.domain.model.LagrangePoint
import ru.echo.radar.domain.model.ThermalPatch
import ru.echo.radar.ui.theme.EchoColors

/**
 * Заголовок в духе ЭХО 0.2: мелкий надзаголовок, крупное имя экрана,
 * спокойная строка смысла. Один и тот же ритм на всех вкладках.
 */
@Composable
fun EchoExpressiveHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 6.dp, top = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.8.sp),
                color = EchoColors.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = EchoColors.TextPrimary,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = EchoColors.TextSecondary,
                maxLines = 2,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Настройки",
                tint = EchoColors.TextDim,
            )
        }
    }
}

/** Крупная метрика: цифра — главный герой, подпись — второй голос. */
@Composable
fun EchoMetric(
    value: String,
    caption: String,
    color: Color = EchoColors.SignalCyan,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            color = color,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = EchoColors.TextSecondary,
            modifier = Modifier.padding(bottom = 5.dp),
            maxLines = 2,
        )
    }
}

/** Пилюльный переключатель разделов: один активный сегмент, мягкий переход. */
@Composable
fun EchoSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(EchoColors.Line.copy(alpha = 0.30f))
            .border(1.dp, EchoColors.Line, RoundedCornerShape(999.dp))
            .padding(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val background by animateColorAsState(
                targetValue = if (selected) EchoColors.TextPrimary else Color.Transparent,
                animationSpec = tween(durationMillis = 200),
                label = "segment-bg",
            )
            val foreground by animateColorAsState(
                targetValue = if (selected) Color(0xFF0B0E14) else EchoColors.TextSecondary,
                animationSpec = tween(durationMillis = 200),
                label = "segment-fg",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(background)
                    .clickable { onSelect(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Default,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    ),
                    color = foreground,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Главная кнопка экрана: мягкий градиент, чёрный текст, высота 48. */
@Composable
fun EchoPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
) {
    val backgroundModifier = when {
        !enabled -> Modifier.background(EchoColors.Line.copy(alpha = 0.35f))
        primary -> Modifier.background(
            Brush.horizontalGradient(
                listOf(EchoColors.SignalCyan, EchoColors.SkyBlue, EchoColors.Purple),
            ),
        )
        else -> Modifier
            .background(EchoColors.Line.copy(alpha = 0.35f))
            .border(1.dp, EchoColors.LineStrong, RoundedCornerShape(999.dp))
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(999.dp))
            .then(backgroundModifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Default,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.9.sp,
            ),
            color = when {
                !enabled -> EchoColors.TextDim
                primary -> Color(0xFF0B0E14)
                else -> EchoColors.SkyBlue
            },
            maxLines = 1,
        )
    }
}

/** Плавающая метка поверх сонара: резонанс, кварталы, тайник. */
@Composable
fun EchoGlowChip(
    text: String,
    color: Color = EchoColors.SkyBlue,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(EchoColors.Background.copy(alpha = 0.82f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Default,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
            ),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Герой-сонар в тональной карточке. Метки по краям дают мгновенный контекст,
 * пока сам радар остаётся центром внимания.
 */
@Composable
fun EchoHeroSonarCard(
    signals: List<EchoSignal>,
    capsules: List<EchoCapsule>,
    thermalPatches: List<ThermalPatch>,
    lagrange: LagrangePoint?,
    syncedTargets: Set<String>,
    myColorHex: Long,
    isDawn: Boolean,
    onSignalClick: (EchoSignal) -> Unit,
    modifier: Modifier = Modifier,
    zoom: Float = 1f,
    onZoom: (Float) -> Unit = {},
    beams: List<ResonanceBeam> = emptyList(),
) {
    val ranked = signals.sortedByDescending { it.compatibility }
    val strongest = ranked.firstOrNull()
    val second = ranked.drop(1).firstOrNull()
    val nearestCapsule = capsules.minByOrNull { it.distanceMeters }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(344.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.verticalGradient(
                    listOf(EchoColors.SurfaceTonal, EchoColors.Surface),
                ),
            )
            .border(1.dp, EchoColors.Line, RoundedCornerShape(30.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, _, gestureZoom, _ ->
                    onZoom(gestureZoom)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        EchoSonar(
            signals = signals,
            capsules = capsules,
            thermalPatches = thermalPatches,
            lagrange = lagrange,
            syncedTargets = syncedTargets,
            myColorHex = myColorHex,
            isDawn = isDawn,
            onSignalClick = onSignalClick,
            modifier = Modifier.fillMaxWidth(0.82f),
            zoom = zoom,
            beams = beams,
        )

        if (strongest != null) {
            EchoGlowChip(
                text = "${strongest.compatibility}% · ${strongest.title}",
                color = Color(strongest.colorHex),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = 14.dp),
            )
        }
        if (second != null) {
            EchoGlowChip(
                text = "${second.compatibility}% · ${second.title}",
                color = Color(second.colorHex),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 14.dp, top = 52.dp),
            )
        }
        EchoGlowChip(
            text = nearestCapsule?.let { "тайник · ${it.distanceMeters} м" } ?: "тайников нет",
            color = EchoColors.Amber,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 24.dp),
        )
    }
}

/**
 * Герой деки: винил в тональной карточке с двумя плавающими метками.
 * Вынесен отдельно, чтобы экран и превью показывали одно и то же.
 */
@Composable
fun EchoVinylHero(
    labelColorHex: Long,
    coverUrl: String?,
    isKnown: Boolean,
    topChip: String,
    bottomChip: String,
    modifier: Modifier = Modifier,
    coverBitmap: Bitmap? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.verticalGradient(
                    listOf(EchoColors.SurfaceTonal, EchoColors.Surface),
                ),
            )
            .border(1.dp, EchoColors.Line, RoundedCornerShape(30.dp)),
        contentAlignment = Alignment.Center,
    ) {
        EchoVinyl(
            labelColorHex = labelColorHex,
            coverUrl = coverUrl,
            isKnown = isKnown,
            coverBitmap = coverBitmap,
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .aspectRatio(1f),
        )
        EchoGlowChip(
            text = topChip,
            color = EchoColors.Gold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 14.dp),
        )
        EchoGlowChip(
            text = bottomChip,
            color = EchoColors.SkyBlue,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 16.dp),
        )
    }
}

/**
 * Редакционная строка сигнала: номер, архетип, трек антиквой, резонанс справа.
 * Карточки остаются в паспорте и тайниках, список — дышит.
 */
@Composable
fun EchoSignalRow(
    signal: EchoSignal,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = Color(signal.colorHex)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 18.dp),
            thickness = 1.dp,
            color = EchoColors.Line,
        )
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = (index + 1).toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, letterSpacing = 0.6.sp),
                color = EchoColors.TextDim,
                modifier = Modifier
                    .width(22.dp)
                    .padding(top = 3.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${signal.artist} — ${signal.title}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    ),
                    color = EchoColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val facts = buildString {
                    if (signal.album.isNotBlank()) append(signal.album)
                    if (signal.hasTrustedGenre) {
                        if (isNotEmpty()) append(" · ")
                        append(signal.genreLabel.ifBlank { signal.genre })
                    }
                }
                if (facts.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = facts,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                        color = EchoColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(signal.personaTitle.uppercase())
                        if (signal.isGhost) append(" · ПРИЗРАК")
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, letterSpacing = 0.8.sp),
                    color = if (signal.isGhost) EchoColors.Amber else accent.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = signal.compatibility.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = accent,
                )
                Text(
                    text = "РЕЗОНАНС",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.5.sp, letterSpacing = 0.7.sp),
                    color = EchoColors.TextDim,
                )
            }
        }
    }
}
