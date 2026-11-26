package com.pulsemusic.player.service

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.pulsemusic.player.data.PreferencesManager
import com.pulsemusic.player.data.PulseDatabaseHelper
import com.pulsemusic.player.model.AbLoopState
import com.pulsemusic.player.model.AudioQualityInfo
import com.pulsemusic.player.model.PlayerUiState
import com.pulsemusic.player.model.RepeatMode
import com.pulsemusic.player.model.SleepTimerState
import com.pulsemusic.player.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlayerController(
    private val context: Context,
    private val database: PulseDatabaseHelper,
    private val preferences: PreferencesManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            shuffleEnabled = preferences.getLastShuffle(),
            repeatMode = preferences.getLastRepeatMode(),
            favoriteIds = database.favoritesFlow.value
        )
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var pendingAction: ((MediaController) -> Unit)? = null
    private var sleepEndEpochMs: Long = 0L

    init {
        scope.launch {
            database.favoritesFlow.collect { favs ->
                _uiState.update { it.copy(favoriteIds = favs) }
            }
        }
    }

    fun connect() {
        if (controllerFuture != null) return
        val sessionToken = SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                try {
                    val controller = future.get()
                    mediaController = controller
                    applySavedPlaybackPreferences(controller)
                    syncFromPlayer(controller)
                    pendingAction?.invoke(controller)
                    pendingAction = null
                } catch (_: Exception) {
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun withController(action: (MediaController) -> Unit) {
        val current = mediaController
        if (current != null && current.isConnected) {
            action(current)
        } else {
            pendingAction = action
            connect()
        }
    }

    private fun applySavedPlaybackPreferences(controller: MediaController) {
        controller.shuffleModeEnabled = _uiState.value.shuffleEnabled
        controller.repeatMode = toMedia3Repeat(_uiState.value.repeatMode)
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, songs.lastIndex)
        val mediaItems = songs.map { it.toMediaItem() }
        _uiState.update {
            it.copy(
                queue = songs,
                currentIndex = safeIndex,
                currentSong = songs[safeIndex],
                abLoop = AbLoopState()
            )
        }
        withController { controller ->
            controller.setMediaItems(mediaItems, safeIndex, 0L)
            controller.prepare()
            controller.play()
        }
    }

    fun playNext(song: Song) {
        val currentQueue = _uiState.value.queue.toMutableList()
        if (currentQueue.isEmpty()) {
            playSongs(listOf(song), 0)
            return
        }
        val insertIdx = (_uiState.value.currentIndex + 1).coerceIn(0, currentQueue.size)
        currentQueue.add(insertIdx, song)
        _uiState.update { it.copy(queue = currentQueue) }
        withController { controller ->
            val targetIdx = (controller.currentMediaItemIndex + 1).coerceAtMost(controller.mediaItemCount)
            controller.addMediaItem(targetIdx, song.toMediaItem())
        }
    }

    fun addToQueue(song: Song) {
        val currentQueue = _uiState.value.queue.toMutableList()
        if (currentQueue.isEmpty()) {
            playSongs(listOf(song), 0)
            return
        }
        currentQueue.add(song)
        _uiState.update { it.copy(queue = currentQueue) }
        withController { controller ->
            controller.addMediaItem(song.toMediaItem())
        }
    }

    fun jumpToQueueIndex(index: Int) {
        val queue = _uiState.value.queue
        if (index !in queue.indices) return
        _uiState.update {
            it.copy(currentIndex = index, currentSong = queue[index], abLoop = AbLoopState())
        }
        withController { controller ->
            controller.seekToDefaultPosition(index)
            controller.play()
        }
    }

    fun removeFromQueue(index: Int) {
        val queue = _uiState.value.queue.toMutableList()
        if (index !in queue.indices || queue.size <= 1) return
        queue.removeAt(index)
        val curIdx = _uiState.value.currentIndex
        val newCurIdx = when {
            index < curIdx -> curIdx - 1
            index == curIdx -> curIdx.coerceAtMost(queue.lastIndex)
            else -> curIdx
        }
        _uiState.update {
            it.copy(queue = queue, currentIndex = newCurIdx, currentSong = queue.getOrNull(newCurIdx))
        }
        withController { controller ->
            controller.removeMediaItem(index)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val queue = _uiState.value.queue.toMutableList()
        if (fromIndex !in queue.indices || toIndex !in queue.indices || fromIndex == toIndex) return
        val item = queue.removeAt(fromIndex)
        queue.add(toIndex, item)
        val currentSong = _uiState.value.currentSong
        val updatedIdx = queue.indexOfFirst { it.id == currentSong?.id }
        _uiState.update { it.copy(queue = queue, currentIndex = updatedIdx) }
        withController { controller ->
            controller.moveMediaItem(fromIndex, toIndex)
        }
    }

    fun clearQueueExceptCurrent() {
        val current = _uiState.value.currentSong ?: return
        _uiState.update { it.copy(queue = listOf(current), currentIndex = 0) }
        withController { controller ->
            val keep = controller.currentMediaItemIndex
            if (keep > 0) controller.removeMediaItems(0, keep)
            if (controller.mediaItemCount > 1) controller.removeMediaItems(1, controller.mediaItemCount)
        }
    }

    fun togglePlayPause() {
        withController { controller ->
            if (controller.isPlaying) controller.pause() else controller.play()
        }
    }

    fun skipNext() {
        withController { controller ->
            if (controller.hasNextMediaItem()) {
                controller.seekToNextMediaItem()
            } else if (controller.mediaItemCount > 0) {
                controller.seekToDefaultPosition(0)
            }
            controller.play()
        }
    }

    fun skipPrevious() {
        withController { controller ->
            if (controller.currentPosition > 3500L) {
                controller.seekTo(0L)
            } else if (controller.hasPreviousMediaItem()) {
                controller.seekToPreviousMediaItem()
            } else if (controller.mediaItemCount > 0) {
                controller.seekToDefaultPosition(controller.mediaItemCount - 1)
            }
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        _uiState.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
        withController { controller ->
            controller.seekTo(positionMs.coerceAtLeast(0L))
        }
    }

    fun toggleShuffle() {
        val next = !_uiState.value.shuffleEnabled
        preferences.saveShuffle(next)
        _uiState.update { it.copy(shuffleEnabled = next) }
        withController { controller ->
            controller.shuffleModeEnabled = next
        }
    }

    fun cycleRepeatMode() {
        val next = when (_uiState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        preferences.saveRepeatMode(next)
        _uiState.update { it.copy(repeatMode = next) }
        withController { controller ->
            controller.repeatMode = toMedia3Repeat(next)
        }
    }

    fun setPlaybackParameters(speed: Float, pitch: Float) {
        val safeSpeed = speed.coerceIn(0.5f, 2.0f)
        val safePitch = pitch.coerceIn(0.5f, 2.0f)
        _uiState.update { it.copy(playbackSpeed = safeSpeed, playbackPitch = safePitch) }
        withController { controller ->
            controller.playbackParameters = PlaybackParameters(safeSpeed, safePitch)
        }
    }

    fun setSleepTimerMinutes(minutes: Int) {
        if (minutes <= 0) {
            cancelSleepTimer()
            return
        }
        val durationMs = minutes * 60_000L
        sleepEndEpochMs = System.currentTimeMillis() + durationMs
        _uiState.update {
            it.copy(sleepTimer = SleepTimerState(active = true, remainingMs = durationMs, endOfTrack = false))
        }
    }

    fun setSleepTimerEndOfTrack() {
        sleepEndEpochMs = 0L
        _uiState.update {
            it.copy(sleepTimer = SleepTimerState(active = true, remainingMs = 0L, endOfTrack = true))
        }
    }

    fun cancelSleepTimer() {
        sleepEndEpochMs = 0L
        _uiState.update { it.copy(sleepTimer = SleepTimerState()) }
    }

    fun setAbLoopPointA() {
        val pos = _uiState.value.positionMs
        _uiState.update {
            val currentB = it.abLoop.pointBMs
            val validB = if (currentB != null && currentB > pos) currentB else null
            it.copy(abLoop = AbLoopState(pointAMs = pos, pointBMs = validB, active = validB != null))
        }
    }

    fun setAbLoopPointB() {
        val pos = _uiState.value.positionMs
        _uiState.update {
            val pointA = it.abLoop.pointAMs ?: 0L
            if (pos > pointA + 500L) {
                it.copy(abLoop = AbLoopState(pointAMs = pointA, pointBMs = pos, active = true))
            } else {
                it
            }
        }
    }

    fun clearAbLoop() {
        _uiState.update { it.copy(abLoop = AbLoopState()) }
    }

    fun toggleFavorite(songId: Long) {
        database.toggleFavorite(songId)
    }

    fun restoreLastQueueIfEmpty(songs: List<Song>) {
        if (_uiState.value.currentSong != null || songs.isEmpty()) return
        val lastId = preferences.getLastSongId()
        val idx = songs.indexOfFirst { it.id == lastId }.takeIf { it >= 0 } ?: 0
        val mediaItems = songs.map { it.toMediaItem() }
        _uiState.update {
            it.copy(
                queue = songs,
                currentIndex = idx,
                currentSong = songs[idx],
                durationMs = songs[idx].durationMs
            )
        }
        withController { controller ->
            if (controller.mediaItemCount == 0) {
                controller.setMediaItems(mediaItems, idx, 0L)
                controller.prepare()
            }
        }
    }

    internal fun syncFromPlayer(player: Player) {
        val idx = player.currentMediaItemIndex
        val queue = _uiState.value.queue
        val matchedSong = queue.getOrNull(idx)
        _uiState.update {
            it.copy(
                isPlaying = player.isPlaying,
                currentIndex = if (matchedSong != null) idx else it.currentIndex,
                currentSong = matchedSong ?: it.currentSong,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = player.duration.takeIf { d -> d > 0L } ?: (matchedSong?.durationMs ?: it.durationMs)
            )
        }
    }

    internal fun onActiveTrackChanged(player: Player) {
        val idx = player.currentMediaItemIndex
        val matchedSong = _uiState.value.queue.getOrNull(idx)
        _uiState.update {
            it.copy(
                currentIndex = idx,
                currentSong = matchedSong ?: it.currentSong,
                positionMs = 0L,
                durationMs = matchedSong?.durationMs ?: it.durationMs,
                abLoop = AbLoopState()
            )
        }
    }

    internal fun updateRichTrackDetails(primaryArgb: Int, secondaryArgb: Int, quality: AudioQualityInfo) {
        _uiState.update {
            it.copy(
                dominantPrimaryArgb = primaryArgb,
                dominantSecondaryArgb = secondaryArgb,
                audioQuality = quality
            )
        }
    }

    internal fun updateSpectrum(levels: List<Float>) {
        _uiState.update { it.copy(spectrumLevels = levels) }
    }

    internal fun tickPositionAndTimers(
        positionMs: Long,
        durationMs: Long,
        onSleepFadeVolume: (Float) -> Unit,
        onSleepExpire: () -> Unit
    ) {
        var nextSleepState = _uiState.value.sleepTimer
        if (nextSleepState.active && !nextSleepState.endOfTrack && sleepEndEpochMs > 0L) {
            val remaining = sleepEndEpochMs - System.currentTimeMillis()
            if (remaining <= 0L) {
                sleepEndEpochMs = 0L
                nextSleepState = SleepTimerState()
                onSleepExpire()
            } else {
                nextSleepState = nextSleepState.copy(remainingMs = remaining)
                if (remaining <= 10_000L) {
                    onSleepFadeVolume((remaining / 10_000f).coerceIn(0.05f, 1.0f))
                }
            }
        }

        _uiState.update {
            it.copy(
                positionMs = positionMs.coerceAtLeast(0L),
                durationMs = if (durationMs > 0L) durationMs else it.durationMs,
                sleepTimer = nextSleepState
            )
        }
    }

    private fun toMedia3Repeat(mode: RepeatMode): Int {
        return when (mode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    private fun Song.toMediaItem(): MediaItem {
        val extras = Bundle().apply {
            putLong("song_id", id)
            putLong("album_id", albumId)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setExtras(extras)
            .build()

        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(contentUri)
            .setMediaMetadata(metadata)
            .build()
    }
}
