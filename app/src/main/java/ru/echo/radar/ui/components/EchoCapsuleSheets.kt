package ru.echo.radar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import ru.echo.radar.domain.model.EchoCapsule
import ru.echo.radar.ui.theme.EchoColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchoCapsuleSheet(capsule: EchoCapsule, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                CapsuleDiamond(isVenue = capsule.isVenue)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (capsule.isVenue) "ТАЙНИК ЗАВЕДЕНИЯ" else "МУЗЫКАЛЬНЫЙ ТАЙНИК",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = EchoColors.Amber,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = capsule.venueName.ifBlank { capsule.personaTitle },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${capsule.artist} — ${capsule.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = EchoColors.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                EchoChip(formatDistance(capsule.distanceMeters))
                EchoChip(capsule.zoneName)
                EchoChip("Резонанс ${capsule.compatibility}%", Color(capsule.compatibilityColorHex()))
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (capsule.isUnlocked) {
                EchoSheetTitle("ПОСЛАНИЕ")
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = capsule.secretMessage.ifBlank { "Здесь оставлена музыкальная память." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = EchoColors.TextPrimary,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                )
                if (capsule.secretPerk.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    EchoChip("Бонус гостям: ${capsule.secretPerk}", EchoColors.Gold)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EchoColors.Background, RoundedCornerShape(14.dp))
                        .border(1.dp, EchoColors.Gold.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                ) {
                    Column {
                        Text(
                            text = "ЗВУКОВОЙ ЗАМОК · ${capsule.minResonance}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = EchoColors.Gold,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Послание откроется созвучному треку. Сейчас резонанс ${capsule.compatibility}% — не хватает ${(capsule.minResonance - capsule.compatibility).coerceAtLeast(1)}%.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EchoColors.TextSecondary,
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                        )
                    }
                }
            }

            if (capsule.moodLabel.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                EchoQuote("${capsule.moodLabel} · ${capsule.personaTitle}")
            }
        }
    }
}

@Composable
fun EchoCapsuleRow(capsule: EchoCapsule, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 5.dp)
            .background(EchoColors.Surface, RoundedCornerShape(16.dp))
            .border(1.dp, EchoColors.Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CapsuleDiamond(isVenue = capsule.isVenue)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (capsule.isVenue) "ТАЙНИК ЗАВЕДЕНИЯ" else "МУЗЫКАЛЬНЫЙ ТАЙНИК",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = EchoColors.Amber,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${capsule.artist} — ${capsule.title}",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.5.sp),
                    color = EchoColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = capsule.secretMessage.ifBlank { "Здесь оставлена музыкальная память." },
                    style = MaterialTheme.typography.bodySmall,
                    color = EchoColors.TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDistance(capsule.distanceMeters),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = EchoColors.TextDim,
                )
                if (!capsule.isUnlocked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "замок ${capsule.minResonance}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = EchoColors.Gold,
                    )
                }
            }
        }
    }
}

@Composable
fun CapsuleDiamond(isVenue: Boolean) {
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(28.dp)
                .background(EchoColors.Amber.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                .border(1.dp, EchoColors.Amber, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isVenue) "◆" else "◇",
                style = MaterialTheme.typography.titleMedium,
                color = EchoColors.Amber,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchoDropCapsuleSheet(
    zoneName: String,
    onDismiss: () -> Unit,
    onDrop: (
        artist: String,
        title: String,
        message: String,
        isVenue: Boolean,
        venueName: String,
        minResonance: Int,
        perk: String,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var artist by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var isVenue by rememberSaveable { mutableStateOf(false) }
    var venueName by rememberSaveable { mutableStateOf("") }
    var perk by rememberSaveable { mutableStateOf("") }
    var minResonance by rememberSaveable { mutableStateOf(0) }

    val canDrop = artist.isNotBlank() && title.isNotBlank()

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
                .imePadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp),
        ) {
            Text(
                text = "ОСТАВИТЬ ТАЙНИК",
                style = MaterialTheme.typography.labelMedium,
                color = EchoColors.TextDim,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Спрячьте трек с посланием. Место размыто до зоны «$zoneName» — точных координат не будет.",
                style = MaterialTheme.typography.bodySmall,
                color = EchoColors.TextSecondary,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = artist,
                onValueChange = { artist = it },
                label = { Text("Исполнитель") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Трек") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Послание в тайнике") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Тайник заведения",
                        style = MaterialTheme.typography.titleSmall,
                        color = EchoColors.TextPrimary,
                    )
                    Text(
                        text = "Бонус гостям вместо баннера",
                        style = MaterialTheme.typography.bodySmall,
                        color = EchoColors.TextDim,
                    )
                }
                Switch(
                    checked = isVenue,
                    onCheckedChange = { isVenue = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EchoColors.Background,
                        checkedTrackColor = EchoColors.SignalCyan,
                        uncheckedThumbColor = EchoColors.TextDim,
                        uncheckedTrackColor = EchoColors.Line,
                    ),
                )
            }

            if (isVenue) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = venueName,
                    onValueChange = { venueName = it },
                    label = { Text("Название заведения") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ЗВУКОВОЙ ЗАМОК",
                    style = MaterialTheme.typography.labelSmall,
                    color = EchoColors.TextDim,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    listOf(0, 70, 85, 95).forEach { value ->
                        EchoChoiceChip(
                            text = if (value == 0) "Открыт всем" else "от $value%",
                            selected = minResonance == value,
                            onClick = { minResonance = value },
                            accent = EchoColors.Gold,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = perk,
                    onValueChange = { perk = it },
                    label = { Text("Бонус гостям: кофе, скидка, секретный трек") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            EchoPillButton(
                label = "СПРЯТАТЬ ТАЙНИК",
                onClick = { onDrop(artist, title, message, isVenue, venueName, minResonance, perk) },
                modifier = Modifier.fillMaxWidth(),
                enabled = canDrop,
            )
        }
    }
}

private fun EchoCapsule.compatibilityColorHex(): Long = when {
    compatibility >= 90 -> 0xFFFFD700
    compatibility >= 75 -> 0xFF34D399
    else -> 0xFF38BDF8
}
