package com.ibramusic.mobile.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Mobile Navigation Routes matching ibramusic-one.vercel.app:
 * 6 Mobile Tabs: Home, Search, Liked, Library, Stats, Settings
 */
sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Home : Screen("home", "Home", Icons.Rounded.Home)
    data object Search : Screen("search", "Search", Icons.Rounded.Search)
    data object Liked : Screen("favorites", "Liked", Icons.Rounded.FavoriteBorder)
    data object Playlists : Screen("playlists", "Library", Icons.Rounded.QueueMusic)
    data object Stats : Screen("stats", "Stats", Icons.Rounded.Leaderboard)
    data object Settings : Screen("settings", "Settings", Icons.Rounded.Settings)
    data object NowPlaying : Screen("now_playing", "Now Playing")
    data object Queue : Screen("queue", "Queue")
    data object Library : Screen("library", "Library", Icons.Rounded.QueueMusic)
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Search,
    Screen.Liked,
    Screen.Playlists,
    Screen.Stats,
    Screen.Settings
)
