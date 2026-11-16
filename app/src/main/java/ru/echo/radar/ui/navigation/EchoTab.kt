package ru.echo.radar.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector

enum class EchoTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Air("air", "ЭФИР", Icons.Filled.Home),
    Deck("deck", "ДЕКА", Icons.Filled.PlayArrow),
    Echoes("echoes", "ОТЗВУКИ", Icons.Filled.Notifications),
    Caches("caches", "ТАЙНИКИ", Icons.Filled.Place),
}
