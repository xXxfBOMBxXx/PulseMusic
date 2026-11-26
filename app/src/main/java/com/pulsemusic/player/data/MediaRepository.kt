package com.pulsemusic.player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.pulsemusic.player.model.Album
import com.pulsemusic.player.model.Artist
import com.pulsemusic.player.model.FolderItem
import com.pulsemusic.player.model.GenreItem
import com.pulsemusic.player.model.Playlist
import com.pulsemusic.player.model.Song
import com.pulsemusic.player.model.SortOrder
import com.pulsemusic.player.model.SystemPlaylistType
import com.pulsemusic.player.util.DemoAudioGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepository(
    private val context: Context,
    private val database: PulseDatabaseHelper,
    private val preferences: PreferencesManager
) {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _folders = MutableStateFlow<List<FolderItem>>(emptyList())
    val folders: StateFlow<List<FolderItem>> = _folders.asStateFlow()

    private val _genres = MutableStateFlow<List<GenreItem>>(emptyList())
    val genres: StateFlow<List<GenreItem>> = _genres.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    suspend fun scanLibrary(includeDemoIfEmpty: Boolean = true) = withContext(Dispatchers.IO) {
        _isScanning.value = true
        try {
            val minDurationMs = preferences.minDurationSec.value * 1000L
            val genreMap = queryGenreMapping()
            val scanned = queryMediaStoreAudio(minDurationMs, genreMap).toMutableList()

            val existingDemos = DemoAudioGenerator.getExistingDemoTracks(context)
            if (existingDemos.isNotEmpty()) {
                scanned.addAll(existingDemos)
            } else if (scanned.isEmpty() && includeDemoIfEmpty) {
                scanned.addAll(DemoAudioGenerator.ensureDemoTracks(context))
            }

            val sorted = sortSongs(scanned, preferences.sortOrder.value)
            publishCollections(sorted)
        } finally {
            _isScanning.value = false
        }
    }

    suspend fun generateDemoTracks() {
        DemoAudioGenerator.ensureDemoTracks(context)
        scanLibrary(includeDemoIfEmpty = true)
    }

    fun applySortOrder(order: SortOrder) {
        preferences.setSortOrder(order)
        publishCollections(sortSongs(_songs.value, order))
    }

    fun buildAllPlaylists(
        allSongs: List<Song>,
        favoriteIds: Set<Long>,
        recentIds: List<Long>,
        mostPlayedIds: List<Long>,
        userPlaylists: List<Playlist>
    ): List<Playlist> {
        val validIds = allSongs.mapTo(HashSet()) { it.id }
        val systemPlaylists = listOf(
            Playlist(
                id = -1L,
                name = "Favorites",
                songIds = favoriteIds.filter { it in validIds },
                isSystem = true,
                systemType = SystemPlaylistType.FAVORITES
            ),
            Playlist(
                id = -2L,
                name = "Recently Played",
                songIds = recentIds.filter { it in validIds },
                isSystem = true,
                systemType = SystemPlaylistType.RECENTLY_PLAYED
            ),
            Playlist(
                id = -3L,
                name = "Most Played",
                songIds = mostPlayedIds.filter { it in validIds },
                isSystem = true,
                systemType = SystemPlaylistType.MOST_PLAYED
            ),
            Playlist(
                id = -4L,
                name = "Recently Added",
                songIds = allSongs.sortedByDescending { it.dateAddedSec }.take(30).map { it.id },
                isSystem = true,
                systemType = SystemPlaylistType.RECENTLY_ADDED
            )
        )
        return systemPlaylists + userPlaylists
    }

    private fun publishCollections(sortedSongs: List<Song>) {
        _songs.value = sortedSongs

        _albums.value = sortedSongs
            .groupBy { it.albumId to it.album }
            .map { (key, albumSongs) ->
                val first = albumSongs.first()
                Album(
                    id = key.first,
                    name = key.second,
                    artist = first.artist,
                    songCount = albumSongs.size,
                    year = albumSongs.maxOfOrNull { it.year } ?: 0,
                    coverUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        key.first
                    ),
                    songs = albumSongs.sortedBy { it.trackNumber }
                )
            }
            .sortedBy { it.name.lowercase() }

        _artists.value = sortedSongs
            .groupBy { it.artist }
            .map { (artistName, artistSongs) ->
                Artist(
                    name = artistName,
                    albumCount = artistSongs.map { it.album }.distinct().size,
                    songCount = artistSongs.size,
                    songs = artistSongs
                )
            }
            .sortedBy { it.name.lowercase() }

        _folders.value = sortedSongs
            .groupBy { it.folderPath }
            .map { (path, folderSongs) ->
                FolderItem(
                    path = path,
                    name = folderSongs.first().folderName,
                    songCount = folderSongs.size,
                    songs = folderSongs
                )
            }
            .sortedBy { it.name.lowercase() }

        _genres.value = sortedSongs
            .groupBy { it.genre }
            .map { (genreName, genreSongs) ->
                GenreItem(
                    name = genreName,
                    songCount = genreSongs.size,
                    songs = genreSongs
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    private fun sortSongs(list: List<Song>, order: SortOrder): List<Song> {
        return when (order) {
            SortOrder.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST_ASC -> list.sortedBy { it.artist.lowercase() }
            SortOrder.DATE_ADDED_DESC -> list.sortedByDescending { it.dateAddedSec }
            SortOrder.DURATION_DESC -> list.sortedByDescending { it.durationMs }
            SortOrder.SIZE_DESC -> list.sortedByDescending { it.sizeBytes }
        }
    }

    private fun queryMediaStoreAudio(minDurationMs: Long, genreMap: Map<Long, String>): List<Song> {
        val result = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.MIME_TYPE
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(minDurationMs.toString())

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Unknown Track"
                    val rawArtist = cursor.getString(artistCol)
                    val artist = if (rawArtist.isNullOrBlank() || rawArtist == "<unknown>") "Unknown Artist" else rawArtist
                    val rawAlbum = cursor.getString(albumCol)
                    val album = if (rawAlbum.isNullOrBlank() || rawAlbum == "<unknown>") "Unknown Album" else rawAlbum
                    val albumId = cursor.getLong(albumIdCol)
                    val duration = cursor.getLong(durationCol)
                    val filePath = cursor.getString(dataCol) ?: ""
                    val folderFile = if (filePath.isNotBlank()) File(filePath).parentFile else null
                    val folderPath = folderFile?.absolutePath ?: "/Music"
                    val folderName = folderFile?.name ?: "Music"
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    result.add(
                        Song(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = albumId,
                            durationMs = duration,
                            contentUri = contentUri,
                            filePath = filePath,
                            folderPath = folderPath,
                            folderName = folderName,
                            sizeBytes = cursor.getLong(sizeCol),
                            dateAddedSec = cursor.getLong(dateCol),
                            trackNumber = cursor.getInt(trackCol),
                            year = cursor.getInt(yearCol),
                            mimeType = cursor.getString(mimeCol) ?: "audio/mpeg",
                            genre = genreMap[id] ?: "Other"
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission not yet granted
        } catch (_: Exception) {
        }
        return result
    }

    private fun queryGenreMapping(): Map<Long, String> {
        val map = HashMap<Long, String>()
        try {
            val genresUri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
            context.contentResolver.query(
                genresUri,
                arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
                null,
                null,
                null
            )?.use { gCursor ->
                val gIdCol = gCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                val gNameCol = gCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
                while (gCursor.moveToNext()) {
                    val genreId = gCursor.getLong(gIdCol)
                    val genreName = gCursor.getString(gNameCol)?.takeIf { it.isNotBlank() } ?: continue
                    val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
                    context.contentResolver.query(
                        membersUri,
                        arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                        null,
                        null,
                        null
                    )?.use { mCursor ->
                        val audioIdCol = mCursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.AUDIO_ID)
                        while (mCursor.moveToNext()) {
                            map[mCursor.getLong(audioIdCol)] = genreName
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return map
    }
}
