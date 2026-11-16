package ru.echo.radar.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import ru.echo.radar.data.repository.EchoRepository
import ru.echo.radar.data.settings.EchoSettingsStore
import ru.echo.radar.ui.components.EchoIncomingBanner
import ru.echo.radar.ui.screens.AirScreen
import ru.echo.radar.ui.screens.CachesScreen
import ru.echo.radar.ui.screens.DeckScreen
import ru.echo.radar.ui.screens.EchoesScreen
import ru.echo.radar.ui.screens.SettingsScreen
import ru.echo.radar.ui.theme.EchoColors

private const val SettingsRoute = "settings"

@Composable
fun EchoApp(
    repository: EchoRepository,
    settingsStore: EchoSettingsStore,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val snapshot by repository.snapshot.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(repository) {
        repository.events.collect { event ->
            snackbarHostState.showSnackbar(
                message = event.message,
                withDismissAction = true,
                duration = SnackbarDuration.Short,
            )
        }
    }

    LaunchedEffect(snapshot.incoming?.id) {
        if (snapshot.incoming != null) {
            delay(5000)
            repository.dismissIncoming()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EchoColors.Background),
    ) {
        Scaffold(
            containerColor = EchoColors.Background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (currentRoute != SettingsRoute) {
                    EchoBottomBar(
                        currentRoute = currentRoute,
                        onSelect = { tab ->
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = EchoTab.Air.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(EchoTab.Air.route) {
                    AirScreen(
                        repository = repository,
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                    )
                }
                composable(EchoTab.Deck.route) {
                    DeckScreen(
                        repository = repository,
                        settingsStore = settingsStore,
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                    )
                }
                composable(EchoTab.Echoes.route) {
                    EchoesScreen(
                        repository = repository,
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                    )
                }
                composable(EchoTab.Caches.route) {
                    CachesScreen(
                        repository = repository,
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                    )
                }
                composable(SettingsRoute) {
                    SettingsScreen(
                        repository = repository,
                        settingsStore = settingsStore,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }

        EchoIncomingBanner(
            incoming = snapshot.incoming,
            onDismiss = { repository.dismissIncoming() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
        )
    }
}

@Composable
private fun EchoBottomBar(
    currentRoute: String?,
    onSelect: (EchoTab) -> Unit,
) {
    NavigationBar(
        containerColor = EchoColors.Surface,
        tonalElevation = 0.dp,
    ) {
        EchoTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(21.dp),
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = EchoColors.SignalCyan,
                    selectedTextColor = EchoColors.SignalCyan,
                    indicatorColor = EchoColors.SignalCyan.copy(alpha = 0.12f),
                    unselectedIconColor = EchoColors.TextDim,
                    unselectedTextColor = EchoColors.TextDim,
                ),
            )
        }
    }
}
