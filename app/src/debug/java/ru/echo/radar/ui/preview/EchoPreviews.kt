package ru.echo.radar.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.echo.radar.domain.model.EchoCapsule
import ru.echo.radar.domain.model.EchoConnection
import ru.echo.radar.domain.model.EchoSignal
import ru.echo.radar.domain.model.EchoZone
import ru.echo.radar.ui.components.EchoExpressiveHeader
import ru.echo.radar.ui.components.EchoHeroSonarCard
import ru.echo.radar.ui.components.EchoMetric
import ru.echo.radar.ui.components.EchoPillButton
import ru.echo.radar.ui.components.EchoPillNav
import ru.echo.radar.ui.components.EchoSectionLabel
import ru.echo.radar.ui.components.EchoSegmentedControl
import ru.echo.radar.ui.components.EchoSignalRow
import ru.echo.radar.ui.components.EchoStatusLine
import ru.echo.radar.ui.components.EchoVinylHero
import ru.echo.radar.ui.components.ResonanceBeam
import ru.echo.radar.ui.components.pluralRu
import ru.echo.radar.ui.theme.EchoColors
import ru.echo.radar.ui.theme.EchoTheme

private val demoSignals = listOf(
    EchoSignal(
        id = "sig_1",
        artist = "Молчат Дома",
        title = "Судно",
        sourceName = "Яндекс Музыка",
        personaTitle = "Ловец ночных фонарей",
        personaObservation = "Знает, как звучит пустой двор после полуночи.",
        moodLabel = "пост-панк",
        resonanceStory = "Один жанр: пост-панк. Совпали тональности Camelot.",
        compatibility = 87,
        bpm = 118,
        camelotKey = "9A",
        genre = "post-punk",
        genreLabel = "пост-панк",
        genreOrigin = "player",
        genreOriginLabel = "жанр из плеера",
        distanceMeters = 140,
        angleDegrees = 42f,
        distanceRatio = 0.63f,
        zoneName = "Сретенка",
        colorHex = 0xFF38BDF8,
        avatarPetals = 7,
        isGhost = false,
        ageMinutes = 1,
        encountersCount = 3,
        coverUrl = null,
    ),
    EchoSignal(
        id = "sig_2",
        artist = "Burial",
        title = "Archangel",
        sourceName = "Spotify",
        personaTitle = "Звонарь сводов собора",
        personaObservation = "Знает, как звучит эхо под куполом.",
        moodLabel = "электроника",
        resonanceStory = "Соседние жанры: электроника и пост-панк.",
        compatibility = 74,
        bpm = 132,
        camelotKey = "4A",
        genre = "electronic",
        genreLabel = "электроника",
        genreOrigin = "catalog",
        genreOriginLabel = "жанр по каталогу ЭХО",
        distanceMeters = 240,
        angleDegrees = 128f,
        distanceRatio = 0.48f,
        zoneName = "Набережная",
        colorHex = 0xFFA855F7,
        avatarPetals = 5,
        isGhost = false,
        ageMinutes = 2,
        encountersCount = 1,
        coverUrl = null,
    ),
    EchoSignal(
        id = "sig_3",
        artist = "Кино",
        title = "Группа крови",
        sourceName = "ЭХО",
        personaTitle = "Молчальник панельных дворов",
        personaObservation = "Призрачный след: звучал 14 мин назад.",
        moodLabel = "Призрачный след",
        resonanceStory = "Тёплый вечерний резонанс: траектории пересеклись.",
        compatibility = 68,
        bpm = 126,
        camelotKey = "6A",
        genre = "post-punk",
        genreLabel = "пост-панк",
        genreOrigin = "catalog",
        genreOriginLabel = "жанр по каталогу ЭХО",
        distanceMeters = 410,
        angleDegrees = 250f,
        distanceRatio = 0.78f,
        zoneName = "Покровка",
        colorHex = 0xFFFFB800,
        avatarPetals = 6,
        isGhost = true,
        ageMinutes = 14,
        encountersCount = 2,
        coverUrl = null,
    ),
)

private val demoCapsules = listOf(
    EchoCapsule(
        id = "cap_1",
        creatorId = "dev_1",
        artist = "Кино",
        title = "Группа крови",
        moodLabel = "Тёплый тайник",
        personaTitle = "Кофейня «Эхо»",
        zoneName = "Покровка",
        secretMessage = "Тёплый вечер в стекле.",
        distanceMeters = 88,
        angleDegrees = 120f,
        distanceRatio = 0.5f,
        compatibility = 91,
        isUnlocked = true,
        isVenue = true,
        venueName = "Кофейня «Эхо»",
        minResonance = 85,
        secretPerk = "кофе",
        createdAt = 0L,
    ),
)

private val demoZones = listOf(
    EchoZone("Сретенка", "пост-панк", "Кино", 4, 0xFF38BDF8, 600),
    EchoZone("Покровка", "синти", "Молчат Дома", 2, 0xFFA855F7, 520),
    EchoZone("Набережная", "эмбиент", "Burial", 3, 0xFF34D399, 480),
)

@Preview(
    name = "ЭХО · Эфир 0.2",
    widthDp = 390,
    heightDp = 844,
    showBackground = true,
    backgroundColor = 0xFF07090E,
)
@Composable
private fun AirPreview() {
    EchoTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EchoColors.Background)
                .padding(top = 8.dp, bottom = 8.dp),
        ) {
            EchoExpressiveHeader(
                eyebrow = "Сонар · Сретенка",
                title = "Эфир",
                subtitle = "Музыка, которая живёт вокруг",
                onOpenSettings = {},
            )
            EchoMetric(
                value = demoSignals.size.toString(),
                caption = "${demoSignals.size} ${pluralRu(demoSignals.size, "голос", "голоса", "голосов")} рядом · резонанс до 87%",
            )
            EchoStatusLine(
                connection = EchoConnection.Online,
                totalObjects = demoSignals.size + demoCapsules.size,
            )
            Spacer(modifier = Modifier.height(10.dp))
            EchoSegmentedControl(
                options = listOf("СОНАР", "ТАЙНИКИ"),
                selectedIndex = 0,
                onSelect = {},
            )
            Spacer(modifier = Modifier.height(12.dp))
            EchoHeroSonarCard(
                signals = demoSignals,
                capsules = demoCapsules,
                thermalPatches = emptyList(),
                lagrange = null,
                syncedTargets = emptySet(),
                myColorHex = 0xFF38BDF8,
                isDawn = false,
                zoneCount = demoZones.size,
                onSignalClick = {},
                zoom = 1f,
                onZoom = {},
                beam = ResonanceBeam(
                    angleDegrees = 42f,
                    distanceRatio = 0.63f,
                    progress = { 0.72f },
                ),
            )
            Spacer(modifier = Modifier.height(10.dp))
            EchoSectionLabel("Рядом · 3")
            demoSignals.take(2).forEachIndexed { index, signal ->
                EchoSignalRow(signal = signal, index = index, onClick = {})
            }
        }
    }
}

@Preview(
    name = "ЭХО · Дека 0.2",
    widthDp = 390,
    heightDp = 844,
    showBackground = true,
    backgroundColor = 0xFF07090E,
)
@Composable
private fun DeckPreview() {
    EchoTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EchoColors.Background)
                .padding(top = 8.dp),
        ) {
            EchoExpressiveHeader(
                eyebrow = "Дека · 33⅓",
                title = "Дека",
                subtitle = "Сейчас на игле",
                onOpenSettings = {},
            )
            EchoStatusLine(
                connection = EchoConnection.Online,
                totalObjects = demoSignals.size + demoCapsules.size,
            )
            Spacer(modifier = Modifier.height(8.dp))
            EchoVinylHero(
                labelColorHex = 0xFF38BDF8,
                coverUrl = null,
                isKnown = true,
                topChip = "33⅓ · живой винил",
                bottomChip = "Яндекс Музыка",
                modifier = Modifier.padding(horizontal = 18.dp),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Судно",
                style = MaterialTheme.typography.headlineMedium,
                color = EchoColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Молчат Дома",
                style = MaterialTheme.typography.titleMedium,
                color = EchoColors.SkyBlue,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EchoPillButton(
                    label = "ВОЛНА В ЭФИР",
                    onClick = {},
                    modifier = Modifier.weight(1.6f),
                )
                EchoPillButton(
                    label = "ТЕПЛО",
                    onClick = {},
                    primary = false,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
            ) {
                Text(
                    text = "118 BPM · 9A · ~140 М · СРЕТЕНКА",
                    style = MaterialTheme.typography.labelSmall,
                    color = EchoColors.TextDim,
                )
            }
        }
    }
}

@Preview(
    name = "ЭХО · Навигация 0.2",
    widthDp = 390,
    heightDp = 96,
    showBackground = true,
    backgroundColor = 0xFF07090E,
)
@Composable
private fun NavPreview() {
    EchoTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EchoColors.Background),
            contentAlignment = Alignment.BottomCenter,
        ) {
            EchoPillNav(currentRoute = "air", onSelect = {})
        }
    }
}
