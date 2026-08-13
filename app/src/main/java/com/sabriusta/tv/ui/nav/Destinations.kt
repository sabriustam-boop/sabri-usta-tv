package com.sabriusta.tv.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val HOME = "ana_sayfa"
    const val LIVE_TV = "canli_tv"
    const val RADIO = "radyo"
    const val MOVIES = "filmler"
    const val FAVORITES = "favoriler"
    const val SETTINGS = "ayarlar"
    const val PLAYLISTS = "listeler"
    const val PLAYER = "oynatici"
    const val LEGAL = "hukuki"

    fun player(itemId: String) = "$PLAYER/$itemId"
}

data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomDestinations = listOf(
    BottomDestination(Routes.HOME, "Ana Sayfa", Icons.Filled.Home),
    BottomDestination(Routes.LIVE_TV, "Canli TV", Icons.Filled.LiveTv),
    BottomDestination(Routes.RADIO, "Radyo", Icons.Filled.Radio),
    BottomDestination(Routes.MOVIES, "Filmler", Icons.Filled.Movie),
    BottomDestination(Routes.FAVORITES, "Favoriler", Icons.Filled.Favorite),
    BottomDestination(Routes.SETTINGS, "Ayarlar", Icons.Filled.Settings)
)
