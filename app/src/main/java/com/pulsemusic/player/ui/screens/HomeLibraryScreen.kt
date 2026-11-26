package com.pulsemusic.player.ui.screens

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulsemusic.player.R
import com.pulsemusic.player.model.Album
import com.pulsemusic.player.model.Artist
import com.pulsemusic.player.model.FolderItem
import com.pulsemusic.player.model.GenreItem
import com.pulsemusic.player.model.LibraryTab
import com.pulsemusic.player.model.Playlist
import com.pulsemusic.player.model.Song
import com.pulsemusic.player.model.SortOrder
import com.pulsemusic.player.model.SystemPlaylistType
import com.pulsemusic.player.ui.components.AddToPlaylistDialog
import com.pulsemusic.player.ui.components.AlbumArtwork
import com.pulsemusic.player.ui.components.CreatePlaylistDialog
import com.pulsemusic.player.ui.components.SongInfoDialog
import com.pulsemusic.player.ui.components.SongOptionsBottomSheet
import com.pulsemusic.player.ui.components.SongRow
import com.pulsemusic.player.viewmodel.MusicViewModel

@Composable
fun HomeLibraryScreen(
    viewModel: MusicViewModel,
    hasAudioPermission: Boolean,
    onRequestAudioPermission: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenGenre: (String) -> Unit
) {
    val context = LocalContext.current
    val songs by viewModel.songs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val genres by viewModel.genres.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val islandConfig by viewModel.islandConfig.collectAsState()

    val overlayGranted = Settings.canDrawOverlays(context)
    val selectedSongIds = remember { mutableStateListOf<Long>() }
    val selectionMode = selectedSongIds.isNotEmpty()

    var activeOptionsSong by remember { mutableStateOf<Song?>(null) }
    var addToPlaylistSongIds by remember { mutableStateOf<List<Long>?>(null) }
    var infoSong by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_brand_logo),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PulseMusic",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${songs.size} tracks • ${albums.size} albums",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Dynamic Island status pill
            val islandActive = islandConfig.enabled && overlayGranted
            Surface(
                shape = CircleShape,
                color = if (islandActive) {
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        if (!overlayGranted) {
                            onOpenOverlayPermission()
                        } else {
                            viewModel.preferences.updateIslandConfig { it.copy(enabled = !it.enabled) }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RadioButtonChecked,
                        contentDescription = null,
                        tint = if (islandActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (islandActive) "Island" else if (!overlayGranted) "Setup Island" else "Island Off",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (islandActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Rounded.Search, contentDescription = "Search")
            }
            IconButton(onClick = onOpenEqualizer) {
                Icon(Icons.Rounded.Equalizer, contentDescription = "Equalizer")
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings")
            }
        }

        if (isScanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Batch Multi-Select Bar
        if (selectionMode) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedSongIds.size} selected",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { addToPlaylistSongIds = selectedSongIds.toList() }) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = "Add to playlist")
                    }
                    IconButton(onClick = {
                        val chosen = songs.filter { it.id in selectedSongIds }
                        if (chosen.isNotEmpty()) viewModel.playerController.playSongs(chosen, 0)
                        selectedSongIds.clear()
                    }) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = "Play selected")
                    }
                    IconButton(onClick = { selectedSongIds.clear() }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear selection")
                    }
                }
            }
        }

        // Permission Prompt Banner if needed
        if (!hasAudioPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.LibraryMusic, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Audio Access Needed", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Allow music access to scan MP3/FLAC/WAV songs on your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onRequestAudioPermission) {
                        Text("Allow")
                    }
                }
            }
        }

        // Navigation Tabs
        ScrollableTabRow(
            selectedTabIndex = LibraryTab.entries.indexOf(selectedTab),
            containerColor = Color.Transparent,
            edgePadding = 12.dp,
            divider = {}
        ) {
            LibraryTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = {
                        Text(
                            text = tab.label,
                            fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val contentBottomPadding = if (playerState.currentSong != null) 104.dp else 20.dp

        when (selectedTab) {
            LibraryTab.SONGS -> {
                if (songs.isEmpty()) {
                    EmptyLibraryView(
                        onRescan = { viewModel.scanLibrary() },
                        onGenerateDemo = { viewModel.generateDemoTracks() }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = contentBottomPadding)
                    ) {
                        // Quick Actions & Sort Header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary
                                                )
                                            )
                                        )
                                        .clickable { viewModel.shuffleAll() }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = Color.Black)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Shuffle All (${songs.size})",
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Box {
                                    AssistChip(
                                        onClick = { showSortMenu = true },
                                        label = { Text(sortOrder.label, maxLines = 1) },
                                        leadingIcon = {
                                            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null)
                                        }
                                    )
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false }
                                    ) {
                                        SortOrder.entries.forEach { order ->
                                            DropdownMenuItem(
                                                text = { Text(order.label) },
                                                onClick = {
                                                    viewModel.repository.applySortOrder(order)
                                                    showSortMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Recently Played Quick Carousel
                        val recentPl = playlists.firstOrNull { it.systemType == SystemPlaylistType.RECENTLY_PLAYED }
                        if (recentPl != null && recentPl.songIds.isNotEmpty()) {
                            val songMap = songs.associateBy { it.id }
                            val recentSongs = recentPl.songIds.mapNotNull { songMap[it] }.take(10)
                            if (recentSongs.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Jump Back In",
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(recentSongs, key = { "recent_${it.id}" }) { rSong ->
                                            Column(
                                                modifier = Modifier
                                                    .width(112.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        val idx = songs.indexOfFirst { it.id == rSong.id }.coerceAtLeast(0)
                                                        viewModel.playerController.playSongs(songs, idx)
                                                    }
                                            ) {
                                                AlbumArtwork(
                                                    songId = rSong.id,
                                                    albumId = rSong.albumId,
                                                    contentUri = rSong.contentUri,
                                                    size = 112.dp,
                                                    cornerRadius = 14.dp
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = rSong.title,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = rSong.artist,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }

                        itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                            val isCurrent = playerState.currentSong?.id == song.id
                            val isSelected = song.id in selectedSongIds
                            SongRow(
                                song = song,
                                isCurrent = isCurrent,
                                isPlaying = isCurrent && playerState.isPlaying,
                                isFavorite = song.id in playerState.favoriteIds,
                                selectionMode = selectionMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (selectionMode) {
                                        if (isSelected) selectedSongIds.remove(song.id)
                                        else selectedSongIds.add(song.id)
                                    } else {
                                        viewModel.playerController.playSongs(songs, index)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelected) selectedSongIds.add(song.id)
                                },
                                onToggleFavorite = { viewModel.playerController.toggleFavorite(song.id) },
                                onMoreClick = { activeOptionsSong = song }
                            )
                        }
                    }
                }
            }

            LibraryTab.PLAYLISTS -> PlaylistsTabContent(
                playlists = playlists,
                bottomPadding = contentBottomPadding,
                onCreateClick = { showCreatePlaylist = true },
                onOpenPlaylist = onOpenPlaylist,
                onDeletePlaylist = { viewModel.deletePlaylist(it.id) }
            )

            LibraryTab.ALBUMS -> AlbumsTabContent(
                albums = albums,
                bottomPadding = contentBottomPadding,
                onOpenAlbum = onOpenAlbum
            )

            LibraryTab.ARTISTS -> ArtistsTabContent(
                artists = artists,
                bottomPadding = contentBottomPadding,
                onOpenArtist = onOpenArtist
            )

            LibraryTab.FOLDERS -> FoldersTabContent(
                folders = folders,
                bottomPadding = contentBottomPadding,
                onOpenFolder = onOpenFolder
            )

            LibraryTab.GENRES -> GenresTabContent(
                genres = genres,
                bottomPadding = contentBottomPadding,
                onOpenGenre = onOpenGenre
            )
        }
    }

    activeOptionsSong?.let { song ->
        SongOptionsBottomSheet(
            song = song,
            isFavorite = song.id in playerState.favoriteIds,
            onDismiss = { activeOptionsSong = null },
            onPlayNext = {
                viewModel.playerController.playNext(song)
                viewModel.showMessage("\"${song.title}\" queued next")
            },
            onAddToQueue = {
                viewModel.playerController.addToQueue(song)
                viewModel.showMessage("Added to queue")
            },
            onAddToPlaylist = { addToPlaylistSongIds = listOf(song.id) },
            onToggleFavorite = { viewModel.playerController.toggleFavorite(song.id) },
            onShare = { viewModel.shareSong(song) },
            onSetRingtone = { viewModel.setAsRingtone(song) },
            onShowDetails = { infoSong = song }
        )
    }

    addToPlaylistSongIds?.let { ids ->
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { addToPlaylistSongIds = null },
            onSelectPlaylist = { plId -> viewModel.addSongsToPlaylist(plId, ids) },
            onCreateNewPlaylist = { name -> viewModel.createPlaylist(name, ids) }
        )
    }

    infoSong?.let { song ->
        SongInfoDialog(song = song, onDismiss = { infoSong = null })
    }

    if (showCreatePlaylist) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylist = false },
            onCreate = { name -> viewModel.createPlaylist(name) }
        )
    }
}

@Composable
private fun EmptyLibraryView(
    onRescan: () -> Unit,
    onGenerateDemo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text("No Music Found Yet", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Scan your device folders or generate built-in studio synth tracks to test playback, Equalizer, and Dynamic Island right away.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRescan) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Rescan")
            }
            Button(onClick = onGenerateDemo) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Demo Tracks")
            }
        }
    }
}

@Composable
private fun PlaylistsTabContent(
    playlists: List<Playlist>,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onCreateClick: () -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding)) {
        item {
            FilterChip(
                selected = true,
                onClick = onCreateClick,
                label = { Text("Create New Playlist") },
                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
        items(playlists, key = { it.id }) { pl ->
            val icon = when (pl.systemType) {
                SystemPlaylistType.FAVORITES -> Icons.Rounded.Favorite
                SystemPlaylistType.RECENTLY_PLAYED -> Icons.Rounded.History
                SystemPlaylistType.MOST_PLAYED -> Icons.Rounded.TrendingUp
                SystemPlaylistType.RECENTLY_ADDED -> Icons.Rounded.AutoAwesome
                null -> Icons.AutoMirrored.Rounded.PlaylistAdd
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable { onOpenPlaylist(pl.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pl.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${pl.songIds.size} tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!pl.isSystem) {
                        IconButton(onClick = { onDeletePlaylist(pl) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete playlist")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumsTabContent(
    albums: List<Album>,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onOpenAlbum: (Long) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(albums, key = { "${it.id}_${it.name}" }) { album ->
            val firstSong = album.songs.firstOrNull()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAlbum(album.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    AlbumArtwork(
                        songId = firstSong?.id ?: -1L,
                        albumId = album.id,
                        contentUri = firstSong?.contentUri,
                        size = 156.dp,
                        cornerRadius = 14.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        album.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${album.artist} • ${album.songCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistsTabContent(
    artists: List<Artist>,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onOpenArtist: (String) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding)) {
        items(artists, key = { it.name }) { artist ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable { onOpenArtist(artist.name) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(artist.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${artist.albumCount} albums • ${artist.songCount} songs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoldersTabContent(
    folders: List<FolderItem>,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onOpenFolder: (String) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding)) {
        items(folders, key = { it.path }) { folder ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable { onOpenFolder(folder.path) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(folder.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${folder.songCount} tracks • ${folder.path}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenresTabContent(
    genres: List<GenreItem>,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onOpenGenre: (String) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = bottomPadding)) {
        items(genres, key = { it.name }) { genre ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable { onOpenGenre(genre.name) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(genre.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${genre.songCount} tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Rounded.Album, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
