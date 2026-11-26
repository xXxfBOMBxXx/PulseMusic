package com.pulsemusic.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulsemusic.player.model.Song
import com.pulsemusic.player.ui.components.AddToPlaylistDialog
import com.pulsemusic.player.ui.components.SongInfoDialog
import com.pulsemusic.player.ui.components.SongOptionsBottomSheet
import com.pulsemusic.player.ui.components.SongRow
import com.pulsemusic.player.viewmodel.MusicViewModel

@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onOpenAlbum: (Long) -> Unit,
    onOpenArtist: (String) -> Unit
) {
    val query by viewModel.searchQuery.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val playerState by viewModel.playerState.collectAsState()

    var activeOptionsSong by remember { mutableStateOf<Song?>(null) }
    var addToPlaylistSongIds by remember { mutableStateOf<List<Long>?>(null) }
    var infoSong by remember { mutableStateOf<Song?>(null) }

    val q = query.trim().lowercase()
    val matchedSongs = if (q.isEmpty()) songs.take(25) else songs.filter {
        it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.album.lowercase().contains(q) ||
            it.folderName.lowercase().contains(q)
    }
    val matchedAlbums = if (q.isEmpty()) emptyList() else albums.filter {
        it.name.lowercase().contains(q) || it.artist.lowercase().contains(q)
    }
    val matchedArtists = if (q.isEmpty()) emptyList() else artists.filter {
        it.name.lowercase().contains(q)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search songs, albums, artists, folders...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 110.dp)) {
            if (matchedArtists.isNotEmpty()) {
                item {
                    Text(
                        "Artists",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                items(matchedArtists) { artist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenArtist(artist.name) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("${artist.name} (${artist.songCount} songs)")
                    }
                }
            }

            if (matchedAlbums.isNotEmpty()) {
                item {
                    Text(
                        "Albums",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                items(matchedAlbums) { album ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenAlbum(album.id) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Album, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("${album.name} — ${album.artist}")
                    }
                }
            }

            item {
                Text(
                    if (q.isEmpty()) "Quick Picks" else "Tracks (${matchedSongs.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            itemsIndexed(matchedSongs, key = { _, s -> s.id }) { idx, song ->
                val isCurrent = playerState.currentSong?.id == song.id
                SongRow(
                    song = song,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && playerState.isPlaying,
                    isFavorite = song.id in playerState.favoriteIds,
                    onClick = { viewModel.playerController.playSongs(matchedSongs, idx) },
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
}
