package com.pulsemusic.player.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pulsemusic.player.ui.components.InAppDynamicIslandBar
import com.pulsemusic.player.ui.screens.CollectionDetailScreen
import com.pulsemusic.player.ui.screens.EqualizerScreen
import com.pulsemusic.player.ui.screens.HomeLibraryScreen
import com.pulsemusic.player.ui.screens.NowPlayingScreen
import com.pulsemusic.player.ui.screens.SearchScreen
import com.pulsemusic.player.ui.screens.SettingsScreen
import com.pulsemusic.player.viewmodel.MusicViewModel

object Routes {
    const val HOME = "home"
    const val NOW_PLAYING = "now_playing"
    const val SEARCH = "search"
    const val EQUALIZER = "equalizer"
    const val SETTINGS = "settings"
    const val ALBUM = "album/{albumId}"
    const val ARTIST = "artist/{artistName}"
    const val PLAYLIST = "playlist/{playlistId}"
    const val FOLDER = "folder/{folderPath}"
    const val GENRE = "genre/{genreName}"
}

@Composable
fun PulseMusicAppHost(
    viewModel: MusicViewModel,
    hasAudioPermission: Boolean,
    hasMicPermission: Boolean,
    openNowPlayingSignal: Int,
    onRequestAudioPermission: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onOpenOverlayPermission: () -> Unit
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val playerState by viewModel.playerState.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val genres by viewModel.genres.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(openNowPlayingSignal) {
        if (openNowPlayingSignal > 0 && currentRoute != Routes.NOW_PLAYING) {
            navController.navigate(Routes.NOW_PLAYING)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME
            ) {
                composable(Routes.HOME) {
                    HomeLibraryScreen(
                        viewModel = viewModel,
                        hasAudioPermission = hasAudioPermission,
                        onRequestAudioPermission = onRequestAudioPermission,
                        onOpenOverlayPermission = onOpenOverlayPermission,
                        onOpenSearch = { navController.navigate(Routes.SEARCH) },
                        onOpenEqualizer = { navController.navigate(Routes.EQUALIZER) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                        onOpenAlbum = { albumId -> navController.navigate("album/$albumId") },
                        onOpenArtist = { artist -> navController.navigate("artist/${Uri.encode(artist)}") },
                        onOpenPlaylist = { playlistId -> navController.navigate("playlist/$playlistId") },
                        onOpenFolder = { path -> navController.navigate("folder/${Uri.encode(path)}") },
                        onOpenGenre = { genre -> navController.navigate("genre/${Uri.encode(genre)}") }
                    )
                }

                composable(Routes.NOW_PLAYING) {
                    NowPlayingScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOpenEqualizer = { navController.navigate(Routes.EQUALIZER) }
                    )
                }

                composable(Routes.SEARCH) {
                    SearchScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOpenAlbum = { albumId -> navController.navigate("album/$albumId") },
                        onOpenArtist = { artist -> navController.navigate("artist/${Uri.encode(artist)}") }
                    )
                }

                composable(Routes.EQUALIZER) {
                    EqualizerScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        viewModel = viewModel,
                        hasMicPermission = hasMicPermission,
                        onRequestMicPermission = onRequestMicPermission,
                        onOpenOverlayPermission = onOpenOverlayPermission,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Routes.ALBUM,
                    arguments = listOf(navArgument("albumId") { type = NavType.LongType })
                ) { entry ->
                    val albumId = entry.arguments?.getLong("albumId") ?: -1L
                    val album = albums.firstOrNull { it.id == albumId }
                    CollectionDetailScreen(
                        title = album?.name ?: "Album",
                        subtitle = album?.artist ?: "Unknown Artist",
                        songs = album?.songs ?: emptyList(),
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Routes.ARTIST,
                    arguments = listOf(navArgument("artistName") { type = NavType.StringType })
                ) { entry ->
                    val artistName = Uri.decode(entry.arguments?.getString("artistName") ?: "")
                    val artist = artists.firstOrNull { it.name == artistName }
                    CollectionDetailScreen(
                        title = artist?.name ?: artistName,
                        subtitle = "${artist?.albumCount ?: 0} albums",
                        songs = artist?.songs ?: emptyList(),
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Routes.PLAYLIST,
                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                ) { entry ->
                    val playlistId = entry.arguments?.getLong("playlistId") ?: -1L
                    val pl = playlists.firstOrNull { it.id == playlistId }
                    val songMap = songs.associateBy { it.id }
                    val plSongs = pl?.songIds?.mapNotNull { songMap[it] } ?: emptyList()
                    CollectionDetailScreen(
                        title = pl?.name ?: "Playlist",
                        subtitle = if (pl?.isSystem == true) "Smart Playlist" else "Custom Playlist",
                        songs = plSongs,
                        viewModel = viewModel,
                        playlistId = playlistId,
                        isUserPlaylist = pl?.isSystem == false,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Routes.FOLDER,
                    arguments = listOf(navArgument("folderPath") { type = NavType.StringType })
                ) { entry ->
                    val folderPath = Uri.decode(entry.arguments?.getString("folderPath") ?: "")
                    val folder = folders.firstOrNull { it.path == folderPath }
                    CollectionDetailScreen(
                        title = folder?.name ?: "Folder",
                        subtitle = folderPath,
                        songs = folder?.songs ?: emptyList(),
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Routes.GENRE,
                    arguments = listOf(navArgument("genreName") { type = NavType.StringType })
                ) { entry ->
                    val genreName = Uri.decode(entry.arguments?.getString("genreName") ?: "")
                    val genre = genres.firstOrNull { it.name == genreName }
                    CollectionDetailScreen(
                        title = genre?.name ?: genreName,
                        subtitle = "Genre",
                        songs = genre?.songs ?: emptyList(),
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // Persistent In-App Dynamic Island Player Capsule
            val showInAppIsland = playerState.currentSong != null && currentRoute != Routes.NOW_PLAYING
            AnimatedVisibility(
                visible = showInAppIsland,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                InAppDynamicIslandBar(
                    playerState = playerState,
                    onOpenFullPlayer = { navController.navigate(Routes.NOW_PLAYING) },
                    onPlayPause = { viewModel.playerController.togglePlayPause() },
                    onNext = { viewModel.playerController.skipNext() },
                    onPrev = { viewModel.playerController.skipPrevious() },
                    onSeekTo = { pos -> viewModel.playerController.seekTo(pos) },
                    onToggleFavorite = { id -> viewModel.playerController.toggleFavorite(id) }
                )
            }
        }
    }
}
