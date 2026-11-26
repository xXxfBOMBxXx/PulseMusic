package com.pulsemusic.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulsemusic.player.model.Song
import com.pulsemusic.player.ui.components.AddToPlaylistDialog
import com.pulsemusic.player.ui.components.AlbumArtwork
import com.pulsemusic.player.ui.components.SongInfoDialog
import com.pulsemusic.player.ui.components.SongOptionsBottomSheet
import com.pulsemusic.player.ui.components.SongRow
import com.pulsemusic.player.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionDetailScreen(
    title: String,
    subtitle: String,
    songs: List<Song>,
    viewModel: MusicViewModel,
    playlistId: Long? = null,
    isUserPlaylist: Boolean = false,
    onBack: () -> Unit
) {
    val playerState by viewModel.playerState.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    var activeOptionsSong by remember { mutableStateOf<Song?>(null) }
    var addToPlaylistSongIds by remember { mutableStateOf<List<Long>?>(null) }
    var infoSong by remember { mutableStateOf<Song?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Text(
                        title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isUserPlaylist && playlistId != null) {
                        IconButton(onClick = { showRenameDialog = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Rename playlist")
                        }
                        IconButton(onClick = {
                            viewModel.deletePlaylist(playlistId)
                            onBack()
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete playlist")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            item {
                val lead = songs.firstOrNull()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumArtwork(
                        songId = lead?.id ?: -1L,
                        albumId = lead?.albumId ?: -1L,
                        contentUri = lead?.contentUri,
                        size = 86.dp,
                        cornerRadius = 18.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "$subtitle • ${songs.size} tracks",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { if (songs.isNotEmpty()) viewModel.playerController.playSongs(songs, 0) },
                        enabled = songs.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play All")
                    }
                    FilledTonalButton(
                        onClick = {
                            if (songs.isNotEmpty()) {
                                if (!playerState.shuffleEnabled) viewModel.playerController.toggleShuffle()
                                viewModel.playerController.playSongs(songs, songs.indices.random())
                            }
                        },
                        enabled = songs.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle")
                    }
                }
            }

            itemsIndexed(songs, key = { _, s -> s.id }) { idx, song ->
                val isCurrent = playerState.currentSong?.id == song.id
                SongRow(
                    song = song,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && playerState.isPlaying,
                    isFavorite = song.id in playerState.favoriteIds,
                    onClick = { viewModel.playerController.playSongs(songs, idx) },
                    onToggleFavorite = { viewModel.playerController.toggleFavorite(song.id) },
                    onMoreClick = { activeOptionsSong = song }
                )
            }
        }
    }

    activeOptionsSong?.let { song ->
        SongOptionsBottomSheet(
            song = song,
            isFavorite = song.id in playerState.favoriteIds,
            onDismiss = { activeOptionsSong = null },
            onPlayNext = { viewModel.playerController.playNext(song) },
            onAddToQueue = { viewModel.playerController.addToQueue(song) },
            onAddToPlaylist = { addToPlaylistSongIds = listOf(song.id) },
            onToggleFavorite = { viewModel.playerController.toggleFavorite(song.id) },
            onRemoveFromPlaylist = if (isUserPlaylist && playlistId != null) {
                { viewModel.removeSongFromPlaylist(playlistId, song.id) }
            } else {
                null
            },
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

    if (showRenameDialog && playlistId != null) {
        var newName by remember { mutableStateOf(title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Playlist") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renamePlaylist(playlistId, newName)
                    showRenameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }
}
