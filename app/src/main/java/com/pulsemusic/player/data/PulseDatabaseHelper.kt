package com.pulsemusic.player.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.pulsemusic.player.model.Playlist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PulseDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    private val _favoritesFlow = MutableStateFlow<Set<Long>>(emptySet())
    val favoritesFlow: StateFlow<Set<Long>> = _favoritesFlow.asStateFlow()

    private val _playlistsFlow = MutableStateFlow<List<Playlist>>(emptyList())
    val playlistsFlow: StateFlow<List<Playlist>> = _playlistsFlow.asStateFlow()

    private val _recentSongIdsFlow = MutableStateFlow<List<Long>>(emptyList())
    val recentSongIdsFlow: StateFlow<List<Long>> = _recentSongIdsFlow.asStateFlow()

    private val _mostPlayedSongIdsFlow = MutableStateFlow<List<Long>>(emptyList())
    val mostPlayedSongIdsFlow: StateFlow<List<Long>> = _mostPlayedSongIdsFlow.asStateFlow()

    init {
        refreshAllFlows()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE favorites (
                song_id INTEGER PRIMARY KEY,
                added_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE playlists (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE playlist_songs (
                playlist_id INTEGER NOT NULL,
                song_id INTEGER NOT NULL,
                position INTEGER NOT NULL,
                PRIMARY KEY (playlist_id, song_id)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE play_history (
                song_id INTEGER PRIMARY KEY,
                play_count INTEGER NOT NULL DEFAULT 1,
                last_played_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE custom_lyrics (
                song_id INTEGER PRIMARY KEY,
                lyrics_text TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future migrations go here
    }

    fun refreshAllFlows() {
        _favoritesFlow.value = queryFavorites()
        _playlistsFlow.value = queryUserPlaylists()
        _recentSongIdsFlow.value = queryRecentSongIds()
        _mostPlayedSongIdsFlow.value = queryMostPlayedSongIds()
    }

    fun isFavorite(songId: Long): Boolean = songId in _favoritesFlow.value

    fun toggleFavorite(songId: Long): Boolean {
        val db = writableDatabase
        val now = System.currentTimeMillis()
        val nowFavorite: Boolean
        if (isFavorite(songId)) {
            db.delete("favorites", "song_id = ?", arrayOf(songId.toString()))
            nowFavorite = false
        } else {
            val cv = ContentValues().apply {
                put("song_id", songId)
                put("added_at", now)
            }
            db.insertWithOnConflict("favorites", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            nowFavorite = true
        }
        _favoritesFlow.value = queryFavorites()
        return nowFavorite
    }

    fun recordPlay(songId: Long) {
        val db = writableDatabase
        val now = System.currentTimeMillis()
        db.execSQL(
            """
            INSERT INTO play_history (song_id, play_count, last_played_at)
            VALUES (?, 1, ?)
            ON CONFLICT(song_id) DO UPDATE SET
                play_count = play_count + 1,
                last_played_at = excluded.last_played_at
            """.trimIndent(),
            arrayOf(songId, now)
        )
        _recentSongIdsFlow.value = queryRecentSongIds()
        _mostPlayedSongIdsFlow.value = queryMostPlayedSongIds()
    }

    fun createPlaylist(name: String, initialSongIds: List<Long> = emptyList()): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("name", name.trim())
            put("created_at", System.currentTimeMillis())
        }
        val playlistId = db.insert("playlists", null, cv)
        if (playlistId > 0L && initialSongIds.isNotEmpty()) {
            addSongsToPlaylist(playlistId, initialSongIds)
        } else {
            _playlistsFlow.value = queryUserPlaylists()
        }
        return playlistId
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        val cv = ContentValues().apply { put("name", newName.trim()) }
        writableDatabase.update("playlists", cv, "id = ?", arrayOf(playlistId.toString()))
        _playlistsFlow.value = queryUserPlaylists()
    }

    fun deletePlaylist(playlistId: Long) {
        val db = writableDatabase
        db.delete("playlist_songs", "playlist_id = ?", arrayOf(playlistId.toString()))
        db.delete("playlists", "id = ?", arrayOf(playlistId.toString()))
        _playlistsFlow.value = queryUserPlaylists()
    }

    fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) {
        val db = writableDatabase
        var maxPos = 0
        db.rawQuery(
            "SELECT COALESCE(MAX(position), -1) FROM playlist_songs WHERE playlist_id = ?",
            arrayOf(playlistId.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) maxPos = cursor.getInt(0) + 1
        }
        db.beginTransaction()
        try {
            songIds.forEachIndexed { idx, songId ->
                val cv = ContentValues().apply {
                    put("playlist_id", playlistId)
                    put("song_id", songId)
                    put("position", maxPos + idx)
                }
                db.insertWithOnConflict("playlist_songs", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _playlistsFlow.value = queryUserPlaylists()
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        writableDatabase.delete(
            "playlist_songs",
            "playlist_id = ? AND song_id = ?",
            arrayOf(playlistId.toString(), songId.toString())
        )
        _playlistsFlow.value = queryUserPlaylists()
    }

    fun saveCustomLyrics(songId: Long, lyricsText: String) {
        val cv = ContentValues().apply {
            put("song_id", songId)
            put("lyrics_text", lyricsText)
        }
        writableDatabase.insertWithOnConflict("custom_lyrics", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getCustomLyrics(songId: Long): String? {
        readableDatabase.rawQuery(
            "SELECT lyrics_text FROM custom_lyrics WHERE song_id = ?",
            arrayOf(songId.toString())
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    private fun queryFavorites(): Set<Long> {
        val set = LinkedHashSet<Long>()
        readableDatabase.rawQuery(
            "SELECT song_id FROM favorites ORDER BY added_at DESC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                set.add(cursor.getLong(0))
            }
        }
        return set
    }

    private fun queryUserPlaylists(): List<Playlist> {
        val list = mutableListOf<Playlist>()
        val db = readableDatabase
        db.rawQuery(
            "SELECT id, name, created_at FROM playlists ORDER BY created_at DESC",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val name = cursor.getString(1)
                val createdAt = cursor.getLong(2)
                val songIds = mutableListOf<Long>()
                db.rawQuery(
                    "SELECT song_id FROM playlist_songs WHERE playlist_id = ? ORDER BY position ASC",
                    arrayOf(id.toString())
                ).use { songCursor ->
                    while (songCursor.moveToNext()) {
                        songIds.add(songCursor.getLong(0))
                    }
                }
                list.add(
                    Playlist(
                        id = id,
                        name = name,
                        songIds = songIds,
                        isSystem = false,
                        createdAt = createdAt
                    )
                )
            }
        }
        return list
    }

    private fun queryRecentSongIds(): List<Long> {
        val ids = mutableListOf<Long>()
        readableDatabase.rawQuery(
            "SELECT song_id FROM play_history ORDER BY last_played_at DESC LIMIT 50",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0))
            }
        }
        return ids
    }

    private fun queryMostPlayedSongIds(): List<Long> {
        val ids = mutableListOf<Long>()
        readableDatabase.rawQuery(
            "SELECT song_id FROM play_history ORDER BY play_count DESC, last_played_at DESC LIMIT 50",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0))
            }
        }
        return ids
    }

    companion object {
        private const val DB_NAME = "pulsemusic.db"
        private const val DB_VERSION = 1
    }
}
