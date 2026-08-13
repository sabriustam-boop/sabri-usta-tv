package com.sabriusta.tv.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sabriusta.tv.data.prefs.AppSettings
import com.sabriusta.tv.ui.favorites.FavoritesScreen
import com.sabriusta.tv.ui.home.HomeScreen
import com.sabriusta.tv.ui.legal.LegalScreen
import com.sabriusta.tv.ui.livetv.LiveTvScreen
import com.sabriusta.tv.ui.movies.MoviesScreen
import com.sabriusta.tv.ui.nav.Routes
import com.sabriusta.tv.ui.nav.bottomDestinations
import com.sabriusta.tv.ui.player.PlayerScreen
import com.sabriusta.tv.ui.playlists.PlaylistsScreen
import com.sabriusta.tv.ui.radio.RadioScreen
import com.sabriusta.tv.ui.settings.SettingsScreen
import com.sabriusta.tv.ui.theme.Altin

@Composable
fun SabriUstaTvRoot(
    settings: AppSettings,
    isInPipMode: Boolean
) {
    if (!settings.legalAccepted) {
        LegalScreen()
        return
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val hideChrome = isInPipMode || currentRoute?.startsWith(Routes.PLAYER) == true

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!hideChrome) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    bottomDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                if (currentRoute != destination.route) {
                                    navController.navigate(destination.route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label, style = MaterialTheme.typography.bodyMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Altin,
                                selectedTextColor = Altin,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (hideChrome) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
        ) {
            NavHost(navController = navController, startDestination = Routes.HOME) {
                composable(Routes.HOME) {
                    HomeScreen(
                        onOpenPlayer = { id -> navController.navigate(Routes.player(id)) },
                        onNavigate = { route -> navController.navigate(route) }
                    )
                }
                composable(Routes.LIVE_TV) {
                    LiveTvScreen(onOpenPlayer = { id -> navController.navigate(Routes.player(id)) })
                }
                composable(Routes.RADIO) { RadioScreen() }
                composable(Routes.MOVIES) {
                    MoviesScreen(onOpenPlayer = { id -> navController.navigate(Routes.player(id)) })
                }
                composable(Routes.FAVORITES) {
                    FavoritesScreen(onOpenPlayer = { id -> navController.navigate(Routes.player(id)) })
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(onOpenPlaylists = { navController.navigate(Routes.PLAYLISTS) })
                }
                composable(Routes.PLAYLISTS) {
                    PlaylistsScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route = "${Routes.PLAYER}/{itemId}",
                    arguments = listOf(navArgument("itemId") { type = NavType.StringType })
                ) { entry ->
                    PlayerScreen(
                        itemId = entry.arguments?.getString("itemId").orEmpty(),
                        autoFullscreen = settings.autoFullscreen,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
