package ru.echo.radar.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.echo.radar.domain.model.PlayingTrack
import ru.echo.radar.ui.theme.EchoColors

/** Паспорт альбома, который играет на телефоне: обложка, название, альбом, архетип. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EchoAlbumSheet(
    track: PlayingTrack,
    cover: Bitmap?,
    persona: String,
    mood: String,
    source: String,
    zoneName: String,
    isSaved: Boolean = false,
    onToggleSave: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val albumLine = listOf(track.album, track.year)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

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
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(EchoColors.Surface)
                        .border(1.dp, EchoColors.Line, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (cover != null) {
                        Image(
                            bitmap = cover.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text = "ЭХО",
                            style = MaterialTheme.typography.labelSmall,
                            color = EchoColors.TextDim,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "АЛЬБОМ",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
                        color = EchoColors.TextDim,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = track.displayTitle,
                        style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.sp),
                        color = EchoColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.displayArtist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = EchoColors.SkyBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = albumLine.ifBlank { "Альбом не передан" },
                        style = MaterialTheme.typography.bodySmall,
                        color = EchoColors.TextSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                EchoChip(source.ifBlank { "Плеер Android" })
                Spacer(modifier = Modifier.width(6.dp))
                EchoChip(zoneName)
                if (mood.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    EchoChip(mood, EchoColors.SkyBlue)
                }
            }

            if (track.genre.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                EchoChip(track.genre)
            }

            if (persona.isNotBlank()) {
                Spacer(modifier = Modifier.height(18.dp))
                EchoSheetTitle("Архетип вечера")
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = persona,
                    style = MaterialTheme.typography.titleSmall,
                    color = EchoColors.SkyBlue,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            EchoPillButton(
                label = if (isSaved) "УБРАТЬ ИЗ СОХРАНЁННОГО" else "СОХРАНИТЬ В ОТЗВУКИ",
                onClick = onToggleSave,
                modifier = Modifier.fillMaxWidth(),
                primary = !isSaved,
                enabled = track.isKnown,
            )

            Spacer(modifier = Modifier.height(12.dp))
            EchoQuote("Альбом и обложка приходят из медиа-сессии плеера. Если плеер их не отдаёт, остаётся цвет архетипа.")
        }
    }
}
