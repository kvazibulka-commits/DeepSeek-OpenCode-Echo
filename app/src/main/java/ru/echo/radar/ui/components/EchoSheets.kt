package ru.echo.radar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.echo.radar.ui.theme.EchoColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EchoSheetHandle() {
    Box(modifier = Modifier.padding(vertical = 10.dp)) {
        Box(
            modifier = Modifier
                .size(width = 42.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(EchoColors.LineStrong),
        )
    }
}

@Composable
fun EchoSheetTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = EchoColors.TextDim,
    )
}

fun formatTime(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return ""
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochSeconds * 1000L))
}

fun formatAgo(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return ""
    val diff = System.currentTimeMillis() / 1000L - epochSeconds
    return when {
        diff < 60L -> "только что"
        diff < 3600L -> "${diff / 60L} мин назад"
        diff < 86_400L -> "${diff / 3600L} ч назад"
        else -> formatTime(epochSeconds)
    }
}

/** Русское склонение: 1 квартал, 2 квартала, 5 кварталов. */
fun pluralRu(count: Int, one: String, few: String, many: String): String {
    val mod100 = count % 100
    val mod10 = count % 10
    return when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
}
