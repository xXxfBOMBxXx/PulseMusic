package com.pulsemusic.player.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val contentUri: Uri,
    val filePath: String,
    val folderPath: String,
    val folderName: String,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val mimeType: String = "audio/mpeg",
    val genre: String = "Unknown Genre"
)

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val songCount: Int,
    val year: Int,
    val coverUri: Uri,
    val songs: List<Song>
)

data class Artist(
    val name: String,
    val albumCount: Int,
    val songCount: Int,
    val songs: List<Song>
)

data class Playlist(
    val id: Long,
    val name: String,
    val songIds: List<Long>,
    val isSystem: Boolean = false,
    val systemType: SystemPlaylistType? = null,
    val createdAt: Long = 0L
)

enum class SystemPlaylistType {
    FAVORITES,
    RECENTLY_PLAYED,
    MOST_PLAYED,
    RECENTLY_ADDED
}

data class FolderItem(
    val path: String,
    val name: String,
    val songCount: Int,
    val songs: List<Song>
)

data class GenreItem(
    val name: String,
    val songCount: Int,
    val songs: List<Song>
)

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

data class LyricsContent(
    val lines: List<LyricLine>,
    val plainText: String,
    val isSynced: Boolean
)

data class AudioQualityInfo(
    val formatLabel: String,
    val bitrateKbps: Int,
    val sampleRateHz: Int,
    val channelCount: Int
)

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class SortOrder(val label: String) {
    TITLE_ASC("Title (A-Z)"),
    ARTIST_ASC("Artist (A-Z)"),
    DATE_ADDED_DESC("Recently Added"),
    DURATION_DESC("Longest First"),
    SIZE_DESC("Largest Size")
}

enum class LibraryTab(val label: String) {
    SONGS("Songs"),
    PLAYLISTS("Playlists"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FOLDERS("Folders"),
    GENRES("Genres")
}

enum class AppThemeStyle(val label: String, val description: String) {
    MIDNIGHT_PULSE("Midnight Pulse", "Deep indigo & neon cyan accents"),
    AMOLED_BLACK("AMOLED Obsidian", "True #000000 black for OLED screens"),
    AURORA_VIOLET("Aurora Violet", "Electric violet & magenta atmosphere"),
    OCEAN_CYAN("Ocean Cyan", "Cool teal & deep slate studio look")
}

enum class IslandStyle(val label: String) {
    CLASSIC_DARK("Classic Capsule"),
    ADAPTIVE_GLOW("Adaptive Album Glow"),
    NEON_BORDER("Neon Border")
}

data class DynamicIslandConfig(
    val enabled: Boolean = true,
    val showWhenPaused: Boolean = true,
    val autoCollapseSeconds: Int = 5,
    val offsetX: Int = 0,
    val offsetY: Int = 18,
    val style: IslandStyle = IslandStyle.ADAPTIVE_GLOW,
    val showLiveSpectrum: Boolean = true
)

data class EqualizerPreset(
    val id: Int,
    val name: String,
    val bandLevelsMb: List<Int>
)

data class EqualizerState(
    val enabled: Boolean = false,
    val presetIndex: Int = 0,
    val bandLevelsMb: List<Int> = listOf(0, 0, 0, 0, 0),
    val bandFrequenciesHz: List<Int> = listOf(60, 230, 910, 3600, 14000),
    val minBandLevelMb: Int = -1500,
    val maxBandLevelMb: Int = 1500,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val reverbPreset: Int = 0
)

data class AbLoopState(
    val pointAMs: Long? = null,
    val pointBMs: Long? = null,
    val active: Boolean = false
)

data class SleepTimerState(
    val active: Boolean = false,
    val remainingMs: Long = 0L,
    val endOfTrack: Boolean = false
)

data class PlayerUiState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val favoriteIds: Set<Long> = emptySet(),
    val sleepTimer: SleepTimerState = SleepTimerState(),
    val abLoop: AbLoopState = AbLoopState(),
    val audioQuality: AudioQualityInfo? = null,
    val dominantPrimaryArgb: Int = 0xFF7C4DFF.toInt(),
    val dominantSecondaryArgb: Int = 0xFF00E5FF.toInt(),
    val spectrumLevels: List<Float> = listOf(0.2f, 0.4f, 0.7f, 0.5f, 0.8f, 0.4f, 0.6f, 0.3f)
)
