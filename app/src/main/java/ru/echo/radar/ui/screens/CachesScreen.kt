package ru.echo.radar.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.echo.radar.data.repository.EchoRepository
import ru.echo.radar.domain.model.EchoCapsule
import ru.echo.radar.ui.components.EchoCapsuleRow
import ru.echo.radar.ui.components.EchoCapsuleSheet
import ru.echo.radar.ui.components.EchoChoiceChip
import ru.echo.radar.ui.components.EchoDropCapsuleSheet
import ru.echo.radar.ui.components.EchoEmptyState
import ru.echo.radar.ui.components.EchoScreenHeader
import ru.echo.radar.ui.components.EchoSectionLabel
import ru.echo.radar.ui.components.EchoStatusLine
import ru.echo.radar.ui.theme.EchoColors

@Composable
fun CachesScreen(repository: EchoRepository, onOpenSettings: () -> Unit) {
    val snapshot by repository.snapshot.collectAsStateWithLifecycle()
    val config by repository.config.collectAsStateWithLifecycle()

    var radius by rememberSaveable { mutableStateOf(1000) }
    var selectedCapsule by remember { mutableStateOf<EchoCapsule?>(null) }
    var showDropSheet by remember { mutableStateOf(false) }

    val visibleCapsules = snapshot.capsules.filter { it.distanceMeters <= radius }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 18.dp),
    ) {
        item {
            EchoScreenHeader(
                title = "ТАЙНИКИ",
                subtitle = "Музыкальные капсулы в тумане кварталов",
                onOpenSettings = onOpenSettings,
            )
            EchoStatusLine(connection = snapshot.connection, totalObjects = snapshot.totalObjects)
        }

        item {
            EchoSectionLabel("РАДИУС СЛЫШИМОСТИ · $radius м")
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(250, 500, 1000).forEach { value ->
                    EchoChoiceChip(
                        text = "$value м",
                        selected = radius == value,
                        onClick = { radius = value },
                    )
                }
            }
        }

        item { EchoSectionLabel("НАЙДЕНО · ${visibleCapsules.size}") }

        if (visibleCapsules.isEmpty()) {
            item {
                EchoEmptyState(
                    title = "В этом радиусе тихо",
                    body = "Тайники прячутся дальше или ещё не появились. Расширьте радиус — или спрячьте первый сами.",
                )
            }
        } else {
            items(visibleCapsules, key = { it.id }) { capsule ->
                EchoCapsuleRow(capsule = capsule, onClick = { selectedCapsule = capsule })
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showDropSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EchoColors.Amber,
                    contentColor = Color.Black,
                ),
            ) {
                Text("СПРЯТАТЬ ТАЙНИК", style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    selectedCapsule?.let { capsule ->
        EchoCapsuleSheet(capsule = capsule, onDismiss = { selectedCapsule = null })
    }

    if (showDropSheet) {
        EchoDropCapsuleSheet(
            zoneName = config.zoneName,
            onDismiss = { showDropSheet = false },
            onDrop = { artist, title, message, isVenue, venueName, minResonance, perk ->
                repository.dropCapsule(
                    artist = artist,
                    title = title,
                    secretMessage = message,
                    zoneName = config.zoneName,
                    isVenue = isVenue,
                    venueName = venueName,
                    minResonance = minResonance,
                    secretPerk = perk,
                )
                showDropSheet = false
            },
        )
    }
}
