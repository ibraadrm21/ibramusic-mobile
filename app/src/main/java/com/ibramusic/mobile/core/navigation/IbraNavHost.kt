package com.ibramusic.mobile.core.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ibramusic.mobile.domain.model.Album
import com.ibramusic.mobile.domain.model.Artist
import com.ibramusic.mobile.feature.album.AlbumDetailScreen
import com.ibramusic.mobile.feature.album.AlbumDetailViewModel
import com.ibramusic.mobile.feature.artist.ArtistProfileScreen
import com.ibramusic.mobile.feature.artist.ArtistProfileViewModel
import com.ibramusic.mobile.feature.home.HomeScreen
import com.ibramusic.mobile.feature.home.HomeViewModel
import com.ibramusic.mobile.feature.library.LibraryScreen
import com.ibramusic.mobile.feature.library.LibraryViewModel
import com.ibramusic.mobile.feature.liked.LikedScreen
import com.ibramusic.mobile.feature.player.NowPlayingScreen
import com.ibramusic.mobile.feature.player.NowPlayingViewModel
import com.ibramusic.mobile.feature.playlist.PlaylistDetailScreen
import com.ibramusic.mobile.feature.playlist.PlaylistViewModel
import com.ibramusic.mobile.feature.queue.QueueScreen
import com.ibramusic.mobile.feature.queue.QueueViewModel
import com.ibramusic.mobile.feature.search.SearchScreen
import com.ibramusic.mobile.feature.search.SearchViewModel
import com.ibramusic.mobile.feature.settings.SettingsScreen
import com.ibramusic.mobile.feature.settings.SettingsViewModel
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun IbraNavHost(
    navController: NavHostController,
    nowPlayingViewModel: NowPlayingViewModel,
    modifier: Modifier = Modifier
) {
    val navigateToArtist: (String, String?) -> Unit = { name, id ->
        val encName = URLEncoder.encode(name, "UTF-8")
        val idParam = id ?: ""
        navController.navigate("artist/$encName?artistId=$idParam&artworkUrl=")
    }

    val navigateToAlbum: (String, String, String?) -> Unit = { title, artist, art ->
        val encTitle = URLEncoder.encode(title, "UTF-8")
        val encArtist = URLEncoder.encode(artist, "UTF-8")
        val encArt = if (!art.isNullOrEmpty()) URLEncoder.encode(art, "UTF-8") else ""
        navController.navigate("album/$encTitle?artistName=$encArtist&artworkUrl=$encArt")
    }

    val onArtistSelected: (Artist) -> Unit = { artist ->
        val encName = URLEncoder.encode(artist.name, "UTF-8")
        val idParam = artist.id
        val encArt = if (!artist.artworkUrl.isNullOrEmpty()) URLEncoder.encode(artist.artworkUrl, "UTF-8") else ""
        navController.navigate("artist/$encName?artistId=$idParam&artworkUrl=$encArt")
    }

    val onAlbumSelected: (Album) -> Unit = { album ->
        val encTitle = URLEncoder.encode(album.title, "UTF-8")
        val encArtist = URLEncoder.encode(album.artistName, "UTF-8")
        val encArt = if (!album.artworkUrl.isNullOrEmpty()) URLEncoder.encode(album.artworkUrl, "UTF-8") else ""
        val idParam = album.id
        navController.navigate("album/$encTitle?artistName=$encArtist&artworkUrl=$encArt&albumId=$idParam")
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier.fillMaxSize(),
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                onTrackSelected = {
                    navController.navigate(Screen.NowPlaying.route)
                },
                onPlaylistClick = { playlistId ->
                    navController.navigate("playlist/$playlistId")
                },
                onLikedClick = {
                    navController.navigate(Screen.Liked.route)
                },
                onGoToArtist = navigateToArtist,
                onGoToAlbum = navigateToAlbum
            )
        }

        composable(Screen.Search.route) {
            val viewModel: SearchViewModel = hiltViewModel()
            SearchScreen(
                viewModel = viewModel,
                onTrackSelected = {
                    navController.navigate(Screen.NowPlaying.route)
                },
                onArtistClick = onArtistSelected,
                onAlbumClick = onAlbumSelected,
                onGoToArtist = navigateToArtist,
                onGoToAlbum = navigateToAlbum
            )
        }

        composable(Screen.Liked.route) {
            val viewModel: LibraryViewModel = hiltViewModel()
            LikedScreen(
                viewModel = viewModel,
                onTrackSelected = {
                    navController.navigate(Screen.NowPlaying.route)
                },
                onGoToArtist = navigateToArtist,
                onGoToAlbum = navigateToAlbum
            )
        }

        composable(Screen.Playlists.route) {
            val viewModel: LibraryViewModel = hiltViewModel()
            LibraryScreen(
                viewModel = viewModel,
                onTrackSelected = {
                    navController.navigate(Screen.NowPlaying.route)
                },
                onPlaylistClick = { playlistId ->
                    navController.navigate("playlist/$playlistId")
                },
                onArtistClick = onArtistSelected,
                onAlbumClick = onAlbumSelected,
                onGoToArtist = navigateToArtist,
                onGoToAlbum = navigateToAlbum
            )
        }

        composable(Screen.Library.route) {
            val viewModel: LibraryViewModel = hiltViewModel()
            LibraryScreen(
                viewModel = viewModel,
                onTrackSelected = {
                    navController.navigate(Screen.NowPlaying.route)
                },
                onPlaylistClick = { playlistId ->
                    navController.navigate("playlist/$playlistId")
                },
                onArtistClick = onArtistSelected,
                onAlbumClick = onAlbumSelected,
                onGoToArtist = navigateToArtist,
                onGoToAlbum = navigateToAlbum
            )
        }

        composable(
            route = "playlist/{playlistId}",
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(260, easing = LinearOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(260))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(220, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(220))
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(260, easing = LinearOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(260))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(220, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(220))
            }
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
            val viewModel: PlaylistViewModel = hiltViewModel()
            PlaylistDetailScreen(
                playlistId = playlistId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onGoToArtist = navigateToArtist,
                onGoToAlbum = navigateToAlbum
            )
        }

        composable(
            route = "artist/{artistName}?artistId={artistId}&artworkUrl={artworkUrl}",
            arguments = listOf(
                navArgument("artistName") { type = NavType.StringType },
                navArgument("artistId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("artworkUrl") { type = NavType.StringType; nullable = true; defaultValue = null }
            ),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(260, easing = LinearOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(260))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(220, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(220))
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(260, easing = LinearOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(260))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(220, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(220))
            }
        ) { backStackEntry ->
            val rawName = backStackEntry.arguments?.getString("artistName") ?: ""
            val artistName = try { URLDecoder.decode(rawName, "UTF-8") } catch (e: Exception) { rawName }
            val artistId = backStackEntry.arguments?.getString("artistId")
            val rawArtwork = backStackEntry.arguments?.getString("artworkUrl")
            val artworkUrl = if (!rawArtwork.isNullOrEmpty()) {
                try { URLDecoder.decode(rawArtwork, "UTF-8") } catch (e: Exception) { rawArtwork }
            } else null

            val viewModel: ArtistProfileViewModel = hiltViewModel()
            ArtistProfileScreen(
                artistName = artistName,
                artistId = artistId,
                artworkUrl = artworkUrl,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onAlbumClick = onAlbumSelected,
                onTrackOptionsGoToArtist = navigateToArtist,
                onTrackOptionsGoToAlbum = navigateToAlbum
            )
        }

        composable(
            route = "album/{albumTitle}?artistName={artistName}&artworkUrl={artworkUrl}&albumId={albumId}",
            arguments = listOf(
                navArgument("albumTitle") { type = NavType.StringType },
                navArgument("artistName") { type = NavType.StringType; defaultValue = "" },
                navArgument("artworkUrl") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("albumId") { type = NavType.StringType; nullable = true; defaultValue = null }
            ),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(260, easing = LinearOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(260))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(220, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(220))
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(260, easing = LinearOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(260))
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(220, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(220))
            }
        ) { backStackEntry ->
            val rawTitle = backStackEntry.arguments?.getString("albumTitle") ?: ""
            val albumTitle = try { URLDecoder.decode(rawTitle, "UTF-8") } catch (e: Exception) { rawTitle }
            val rawArtist = backStackEntry.arguments?.getString("artistName") ?: ""
            val artistName = try { URLDecoder.decode(rawArtist, "UTF-8") } catch (e: Exception) { rawArtist }
            val rawArtwork = backStackEntry.arguments?.getString("artworkUrl")
            val artworkUrl = if (!rawArtwork.isNullOrEmpty()) {
                try { URLDecoder.decode(rawArtwork, "UTF-8") } catch (e: Exception) { rawArtwork }
            } else null
            val albumId = backStackEntry.arguments?.getString("albumId")

            val viewModel: AlbumDetailViewModel = hiltViewModel()
            AlbumDetailScreen(
                albumTitle = albumTitle,
                artistName = artistName,
                artworkUrl = artworkUrl,
                albumId = albumId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onArtistClick = { name -> navigateToArtist(name, null) },
                onPlaylistCreated = { newPlaylistId ->
                    navController.navigate("playlist/$newPlaylistId")
                },
                onTrackOptionsGoToArtist = navigateToArtist,
                onTrackOptionsGoToAlbum = navigateToAlbum
            )
        }

        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToStats = { navController.navigate(Screen.Stats.route) }
            )
        }

        composable(
            route = Screen.NowPlaying.route,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                )
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(200, easing = FastOutLinearInEasing)
                )
            },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(200, easing = FastOutLinearInEasing)
                )
            }
        ) {
            NowPlayingScreen(
                viewModel = nowPlayingViewModel,
                onDismiss = { navController.popBackStack() },
                onNavigateToQueue = { navController.navigate(Screen.Queue.route) },
                onGoToArtist = navigateToArtist,
                onGoToAlbum = navigateToAlbum
            )
        }

        composable(
            route = Screen.Queue.route,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(250, easing = FastOutSlowInEasing)
                )
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(200, easing = FastOutLinearInEasing)
                )
            },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(200, easing = FastOutLinearInEasing)
                )
            }
        ) {
            val queueViewModel: QueueViewModel = hiltViewModel()
            QueueScreen(
                viewModel = queueViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Stats.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(260, easing = LinearOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(260))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(220, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(220))
            }
        ) {
            val statsViewModel: com.ibramusic.mobile.feature.stats.StatsViewModel = hiltViewModel()
            com.ibramusic.mobile.feature.stats.StatsScreen(
                viewModel = statsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
