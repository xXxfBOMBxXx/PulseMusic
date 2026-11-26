package com.pulsemusic.player.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pulsemusic.player.PulseMusicApp
import com.pulsemusic.player.model.Album
import com.pulsemusic.player.model.AppThemeStyle
import com.pulsemusic.player.model.Artist
import com.pulsemusic.player.model.DynamicIslandConfig
import com.pulsemusic.player.model.EqualizerPreset
import com.pulsemusic.player.model.EqualizerState
import com.pulsemusic.player.model.FolderItem
import com.pulsemusic.player.model.GenreItem
import com.pulsemusic.player.model.LibraryTab
import com.pulsemusic.player.model.LyricsContent
import com.pulsemusic.player.model.PlayerUiState
import com.pulsemusic.player.model.Playlist
import com.pulsemusic.player.model.Song
import com.pulsemusic.player.model.SortOrder
import com.pulsemusic.player.util.LrcParser
import com.pulsemusic.player.util.ShareAndRingtoneHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PulseMusicApp
    val preferences = app.preferencesManager
    val database = app.databaseHelper
    val repository = app.mediaRepository
    val effects = app.audioEffectsManager
    val playerController = app.playerController

    val playerState: StateFlow<PlayerUiState> = playerController.uiState
    val songs: StateFlow<List<Song>> = repository.songs
    val albums: StateFlow<List<Album>> = repository.albums
    val artists: StateFlow<List<Artist>> = repository.artists
    val folders: StateFlow<List<FolderItem>> = repository.folders
    val genres: StateFlow<List<GenreItem>> = repository.genres
    val isScanning: StateFlow<Boolean> = repository.isScanning

    val themeStyle: StateFlow<AppThemeStyle> = preferences.themeStyle
    val dynamicArtTheme: StateFlow<Boolean> = preferences.dynamicArtTheme
    val islandConfig: StateFlow<DynamicIslandConfig> = preferences.islandConfig
    val sortOrder: StateFlow<SortOrder> = preferences.sortOrder
    val minDurationSec: StateFlow<Int> = preferences.minDurationSec
    val crossfadeSeconds: StateFlow<Int> = preferences.crossfadeSeconds
    val shakeToSkip: StateFlow<Boolean> = preferences.shakeToSkip
    val pauseOnHeadsetDisconnect: StateFlow<Boolean> = preferences.pauseOnHeadsetDisconnect
    val equalizerState: StateFlow<EqualizerState> = preferences.equalizerState
    val equalizerPresets: List<EqualizerPreset> = preferences.builtInPresets

    val playlists: StateFlow<List<Playlist>> = combine(
        repository.songs,
        database.favoritesFlow,
        database.recentSongIdsFlow,
        database.mostPlayedSongIdsFlow,
        database.playlistsFlow
    ) { allSongs, favs, recent, mostPlayed, userLists ->
        repository.buildAllPlaylists(allSongs, favs, recent, mostPlayed, userLists)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTab = MutableStateFlow(LibraryTab.SONGS)
    val selectedTab: StateFlow<LibraryTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentLyrics = MutableStateFlow<LyricsContent?>(null)
    val currentLyrics: StateFlow<LyricsContent?> = _currentLyrics.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var lastLoadedLyricsSongId: Long = Long.MIN_VALUE

    init {
        viewModelScope.launch {
            playerState.collect { state ->
                val song = state.currentSong
                if (song != null && song.id != lastLoadedLyricsSongId) {
                    loadLyricsForSong(song)
                }
            }
        }
    }

    fun selectTab(tab: LibraryTab) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun scanLibrary() {
        viewModelScope.launch {
            repository.scanLibrary(includeDemoIfEmpty = true)
            playerController.restoreLastQueueIfEmpty(repository.songs.value)
        }
    }

    fun generateDemoTracks() {
        viewModelScope.launch {
            repository.generateDemoTracks()
            playerController.restoreLastQueueIfEmpty(repository.songs.value)
            showMessage("Demo synth tracks generated in studio library")
        }
    }

    fun playExternalUri(uri: Uri) {
        val matching = songs.value.indexOfFirst { it.contentUri == uri }
        if (matching >= 0) {
            playerController.playSongs(songs.value, matching)
            return
        }
        val externalSong = Song(
            id = System.currentTimeMillis(),
            title = uri.lastPathSegment?.substringAfterLast('/') ?: "External Audio",
            artist = "External File",
            album = "Opened Audio",
            albumId = -1L,
            durationMs = 0L,
            contentUri = uri,
            filePath = uri.path ?: "",
            folderPath = "",
            folderName = "External",
            sizeBytes = 0L,
            dateAddedSec = System.currentTimeMillis() / 1000L
        )
        playerController.playSongs(listOf(externalSong), 0)
    }

    fun shuffleAll() {
        val list = songs.value
        if (list.isEmpty()) return
        if (!playerState.value.shuffleEnabled) {
            playerController.toggleShuffle()
        }
        val start = list.indices.random()
        playerController.playSongs(list, start)
    }

    fun createPlaylist(name: String, songIds: List<Long> = emptyList()) {
        if (name.isBlank()) return
        database.createPlaylist(name, songIds)
        showMessage("Created playlist \"$name\"")
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        if (newName.isBlank()) return
        database.renamePlaylist(playlistId, newName)
        showMessage("Playlist renamed")
    }

    fun deletePlaylist(playlistId: Long) {
        database.deletePlaylist(playlistId)
        showMessage("Playlist deleted")
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        database.addSongsToPlaylist(playlistId, listOf(songId))
        showMessage("Added to playlist")
    }

    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        database.addSongsToPlaylist(playlistId, songIds)
        showMessage("Added ${songIds.size} tracks to playlist")
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        database.removeSongFromPlaylist(playlistId, songId)
    }

    fun saveQueueAsPlaylist(name: String) {
        val ids = playerState.value.queue.map { it.id }
        if (ids.isEmpty() || name.isBlank()) return
        createPlaylist(name, ids)
    }

    fun loadLyricsForSong(song: Song) {
        lastLoadedLyricsSongId = song.id
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val custom = database.getCustomLyrics(song.id)
                val sidecar = if (custom.isNullOrBlank()) LrcParser.findSidecarLrcFile(song.filePath) else null
                val raw = custom ?: sidecar
                if (!raw.isNullOrBlank()) {
                    LrcParser.parse(raw)
                } else {
                    null
                }
            }
            _currentLyrics.value = loaded
        }
    }

    fun saveLyricsForCurrentSong(rawLyrics: String) {
        val song = playerState.value.currentSong ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                database.saveCustomLyrics(song.id, rawLyrics)
            }
            _currentLyrics.value = if (rawLyrics.isBlank()) null else LrcParser.parse(rawLyrics)
            showMessage("Lyrics updated")
        }
    }

    fun shareSong(song: Song) {
        ShareAndRingtoneHelper.shareSong(app, song)
    }

    fun setAsRingtone(song: Song) {
        if (!ShareAndRingtoneHelper.canWriteSystemSettings(app)) {
            ShareAndRingtoneHelper.openWriteSettingsScreen(app)
            showMessage("Grant 'Modify system settings' permission to set ringtones")
            return
        }
        val res = ShareAndRingtoneHelper.setAsRingtone(app, song)
        res.onSuccess { showMessage("\"${song.title}\" set as ringtone") }
            .onFailure { showMessage(it.message ?: "Unable to set ringtone") }
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun consumeMessage() {
        _snackbarMessage.value = null
    }
}
