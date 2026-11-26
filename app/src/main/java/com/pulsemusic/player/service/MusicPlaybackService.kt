package com.pulsemusic.player.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.pulsemusic.player.MainActivity
import com.pulsemusic.player.PulseMusicApp
import com.pulsemusic.player.island.DynamicIslandOverlayController
import com.pulsemusic.player.util.AlbumArtLoader
import com.pulsemusic.player.util.ShareAndRingtoneHelper
import com.pulsemusic.player.widget.MusicAppWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class MusicPlaybackService : MediaSessionService(), SensorEventListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null
    private lateinit var islandOverlay: DynamicIslandOverlayController

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastShakeTimeMs = 0L

    private var tickerJob: Job? = null
    private var currentArtBitmap: Bitmap? = null
    private var lastArtSongId: Long = Long.MIN_VALUE

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val app = applicationContext as PulseMusicApp

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(app.preferencesManager.pauseOnHeadsetDisconnect.value)
            .build()

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_NOW_PLAYING, true)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        islandOverlay = DynamicIslandOverlayController(
            context = this,
            preferences = app.preferencesManager,
            onPlayPause = { app.playerController.togglePlayPause() },
            onNext = { app.playerController.skipNext() },
            onPrev = { app.playerController.skipPrevious() },
            onSeekTo = { pos -> app.playerController.seekTo(pos) },
            onToggleFavorite = { songId -> app.playerController.toggleFavorite(songId) }
        )

        setupShakeSensor()
        attachPlayerListeners(app)
        observeIslandAndEffects(app)
        startTickerLoop(app)
    }

    @OptIn(UnstableApi::class)
    private fun attachPlayerListeners(app: PulseMusicApp) {
        app.audioEffectsManager.attachToAudioSession(player.audioSessionId)

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                app.audioEffectsManager.attachToAudioSession(audioSessionId)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                app.audioEffectsManager.onPlaybackStateChanged(isPlaying)
                app.playerController.syncFromPlayer(player)
                updateOverlayAndWidget(app)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                app.playerController.onActiveTrackChanged(player)
                val currentSong = app.playerController.uiState.value.currentSong
                if (currentSong != null) {
                    app.databaseHelper.recordPlay(currentSong.id)
                    app.preferencesManager.saveLastSongId(currentSong.id)
                    loadArtAndQualityForSong(app, currentSong.id)
                }
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                    app.playerController.uiState.value.sleepTimer.endOfTrack
                ) {
                    player.pause()
                    app.playerController.cancelSleepTimer()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                app.playerController.syncFromPlayer(player)
                updateOverlayAndWidget(app)
            }
        })
    }

    private fun loadArtAndQualityForSong(app: PulseMusicApp, songId: Long) {
        val song = app.playerController.uiState.value.currentSong ?: return
        if (song.id != songId) return

        serviceScope.launch {
            val bitmap = AlbumArtLoader.loadBitmap(
                context = this@MusicPlaybackService,
                songId = song.id,
                albumId = song.albumId,
                contentUri = song.contentUri,
                targetSizePx = 420
            )
            val colors = AlbumArtLoader.extractDominantColors(bitmap)
            val quality = ShareAndRingtoneHelper.extractAudioQuality(
                context = this@MusicPlaybackService,
                uri = song.contentUri,
                mimeType = song.mimeType
            )
            currentArtBitmap = bitmap
            lastArtSongId = song.id
            app.playerController.updateRichTrackDetails(colors.first, colors.second, quality)
            updateOverlayAndWidget(app)
        }
    }

    private fun observeIslandAndEffects(app: PulseMusicApp) {
        serviceScope.launch {
            app.preferencesManager.islandConfig.collectLatest { config ->
                islandOverlay.applyConfig(config)
                updateOverlayAndWidget(app)
            }
        }
        serviceScope.launch {
            app.preferencesManager.pauseOnHeadsetDisconnect.collectLatest { enabled ->
                player.setHandleAudioBecomingNoisy(enabled)
            }
        }
        serviceScope.launch {
            app.preferencesManager.shakeToSkip.collectLatest { enabled ->
                if (enabled) {
                    accelerometer?.let {
                        sensorManager?.registerListener(
                            this@MusicPlaybackService,
                            it,
                            SensorManager.SENSOR_DELAY_UI
                        )
                    }
                } else {
                    sensorManager?.unregisterListener(this@MusicPlaybackService)
                }
            }
        }
        serviceScope.launch {
            app.audioEffectsManager.spectrumLevels.collectLatest { levels ->
                app.playerController.updateSpectrum(levels)
            }
        }
        serviceScope.launch {
            app.playerController.uiState.collectLatest { state ->
                val song = state.currentSong
                if (song != null && song.id != lastArtSongId) {
                    loadArtAndQualityForSong(app, song.id)
                }
                islandOverlay.updateState(state, currentArtBitmap)
            }
        }
    }

    private fun startTickerLoop(app: PulseMusicApp) {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                val isPlaying = player.isPlaying
                val pos = player.currentPosition
                val dur = player.duration.coerceAtLeast(0L)

                if (isPlaying) {
                    app.audioEffectsManager.tickSimulatedSpectrum(pos)
                    handleAbLoop(app, pos)
                    handleCrossfade(app, pos, dur)
                }

                app.playerController.tickPositionAndTimers(
                    positionMs = pos,
                    durationMs = dur,
                    onSleepFadeVolume = { vol -> player.volume = vol },
                    onSleepExpire = {
                        player.pause()
                        player.volume = 1.0f
                    }
                )
                delay(200L)
            }
        }
    }

    private fun handleAbLoop(app: PulseMusicApp, positionMs: Long) {
        val ab = app.playerController.uiState.value.abLoop
        if (ab.active && ab.pointAMs != null && ab.pointBMs != null && ab.pointBMs > ab.pointAMs) {
            if (positionMs >= ab.pointBMs) {
                player.seekTo(ab.pointAMs)
            }
        }
    }

    private fun handleCrossfade(app: PulseMusicApp, positionMs: Long, durationMs: Long) {
        val timer = app.playerController.uiState.value.sleepTimer
        if (timer.active && !timer.endOfTrack && timer.remainingMs <= 10_000L) {
            return
        }
        val cfSec = app.preferencesManager.crossfadeSeconds.value
        if (cfSec <= 0 || durationMs <= cfSec * 2200L) {
            if (player.volume < 0.99f) player.volume = 1.0f
            return
        }
        val cfMs = cfSec * 1000L
        val remaining = durationMs - positionMs
        val targetVol = when {
            positionMs < cfMs -> (positionMs.toFloat() / cfMs.toFloat()).coerceIn(0.15f, 1.0f)
            remaining in 1L until cfMs -> (remaining.toFloat() / cfMs.toFloat()).coerceIn(0.15f, 1.0f)
            else -> 1.0f
        }
        player.volume = targetVol
    }

    private fun updateOverlayAndWidget(app: PulseMusicApp) {
        val state = app.playerController.uiState.value
        islandOverlay.updateState(state, currentArtBitmap)
        MusicAppWidgetProvider.pushUpdate(this, currentArtBitmap)
    }

    private fun setupShakeSensor() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val gForce = sqrt((x * x + y * y + z * z) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH))
        val now = System.currentTimeMillis()
        if (gForce > 2.35f && now - lastShakeTimeMs > 1200L) {
            lastShakeTimeMs = now
            val app = applicationContext as PulseMusicApp
            if (player.isPlaying) {
                app.playerController.skipNext()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val playerRef = mediaSession?.player
        if (playerRef == null || !playerRef.playWhenReady || playerRef.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        islandOverlay.hide()
        sensorManager?.unregisterListener(this)
        val app = applicationContext as PulseMusicApp
        app.audioEffectsManager.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
