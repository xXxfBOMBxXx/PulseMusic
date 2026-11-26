package com.pulsemusic.player.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pulsemusic.player.model.RepeatMode
import com.pulsemusic.player.ui.components.AddToPlaylistDialog
import com.pulsemusic.player.ui.components.AlbumArtwork
import com.pulsemusic.player.ui.components.CreatePlaylistDialog
import com.pulsemusic.player.ui.components.SongInfoDialog
import com.pulsemusic.player.ui.components.SongOptionsBottomSheet
import com.pulsemusic.player.util.LrcParser
import com.pulsemusic.player.util.TimeFormatter
import com.pulsemusic.player.viewmodel.MusicViewModel
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    val playerState by viewModel.playerState.collectAsState()
    val lyricsContent by viewModel.currentLyrics.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    val song = playerState.currentSong
    if (song == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No active track", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBack) { Text("Return to Library") }
            }
        }
        return
    }

    var showLyrics by remember { mutableStateOf(false) }
    var showVisualizer by remember { mutableStateOf(true) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepSheet by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showLyricsEditor by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showSongInfo by remember { mutableStateOf(false) }
    var showSaveQueueDialog by remember { mutableStateOf(false) }

    val primaryGlow = Color(playerState.dominantPrimaryArgb)
    val secondaryGlow = Color(playerState.dominantSecondaryArgb)
    val isFavorite = song.id in playerState.favoriteIds

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = 0.48f),
                        secondaryGlow.copy(alpha = 0.22f),
                        Color(0xFF07080E)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Minimize",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                    val qualityBadge = playerState.audioQuality?.let {
                        val br = if (it.bitrateKbps > 0) " • ${it.bitrateKbps} kbps" else ""
                        "${it.formatLabel} • ${TimeFormatter.formatSampleRate(it.sampleRateHz)}$br"
                    } ?: song.album
                    Text(
                        text = qualityBadge,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = secondaryGlow,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = "Toggle Lyrics",
                        tint = if (showLyrics) secondaryGlow else Color.White
                    )
                }
                IconButton(onClick = onOpenEqualizer) {
                    Icon(
                        imageVector = Icons.Rounded.Equalizer,
                        contentDescription = "Equalizer",
                        tint = Color.White
                    )
                }
                IconButton(onClick = { showMoreSheet = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Center Stage: Artwork + Visualizer OR Synced Lyrics
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = showLyrics, label = "centerStage") { lyricsMode ->
                    if (!lyricsMode) {
                        var dragDelta by remember { mutableFloatStateOf(0f) }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(song.id) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { dragDelta = 0f },
                                        onHorizontalDrag = { _, d -> dragDelta += d },
                                        onDragEnd = {
                                            if (dragDelta < -100f) viewModel.playerController.skipNext()
                                            else if (dragDelta > 100f) viewModel.playerController.skipPrevious()
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (showVisualizer) {
                                CircularSpectrumRing(
                                    levels = playerState.spectrumLevels,
                                    isPlaying = playerState.isPlaying,
                                    primary = secondaryGlow,
                                    secondary = primaryGlow,
                                    modifier = Modifier.size(312.dp)
                                )
                            }

                            AlbumArtwork(
                                songId = song.id,
                                albumId = song.albumId,
                                contentUri = song.contentUri,
                                size = 252.dp,
                                cornerRadius = 28.dp,
                                targetPx = 600,
                                modifier = Modifier.clickable { showVisualizer = !showVisualizer }
                            )
                        }
                    } else {
                        LyricsPanel(
                            lyrics = lyricsContent,
                            positionMs = playerState.positionMs,
                            accentColor = secondaryGlow,
                            onSeekTo = { viewModel.playerController.seekTo(it) },
                            onEditClick = { showLyricsEditor = true }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Song Title + Artist + Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${song.artist} — ${song.album}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { viewModel.playerController.toggleFavorite(song.id) }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFFF4D8D) else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Seekbar
            val duration = playerState.durationMs.coerceAtLeast(1L)
            var seeking by remember { mutableStateOf(false) }
            var sliderValue by remember { mutableFloatStateOf(0f) }
            val progress = if (seeking) {
                sliderValue
            } else {
                (playerState.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
            }

            Slider(
                value = progress,
                onValueChange = {
                    seeking = true
                    sliderValue = it
                },
                onValueChangeFinished = {
                    viewModel.playerController.seekTo((sliderValue * duration).toLong())
                    seeking = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = secondaryGlow,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = TimeFormatter.formatDuration(
                        if (seeking) (sliderValue * duration).toLong() else playerState.positionMs
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
                if (playerState.abLoop.active) {
                    Text(
                        text = "A-B Loop: ${TimeFormatter.formatDuration(playerState.abLoop.pointAMs ?: 0L)} → ${TimeFormatter.formatDuration(playerState.abLoop.pointBMs ?: 0L)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryGlow
                    )
                }
                Text(
                    text = TimeFormatter.formatDuration(playerState.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.playerController.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playerState.shuffleEnabled) secondaryGlow else Color.White.copy(alpha = 0.65f)
                    )
                }

                IconButton(
                    onClick = { viewModel.playerController.skipPrevious() },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .size(72.dp)
                        .clickable { viewModel.playerController.togglePlayPause() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play or Pause",
                            tint = Color(0xFF0A0C14),
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.playerController.skipNext() },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                IconButton(onClick = { viewModel.playerController.cycleRepeatMode() }) {
                    val repIcon = if (playerState.repeatMode == RepeatMode.ONE) {
                        Icons.Rounded.RepeatOne
                    } else {
                        Icons.Rounded.Repeat
                    }
                    Icon(
                        imageVector = repIcon,
                        contentDescription = "Repeat",
                        tint = if (playerState.repeatMode != RepeatMode.OFF) secondaryGlow else Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Utility Pills Row (Speed, A-B Loop, Sleep Timer, Visualizer, Queue)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { showSpeedDialog = true },
                    label = { Text("${String.format("%.1f", playerState.playbackSpeed)}x") },
                    leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = AssistChipDefaults.assistChipColors(labelColor = Color.White)
                )

                val abLabel = when {
                    playerState.abLoop.active -> "A-B Off"
                    playerState.abLoop.pointAMs != null -> "Set B"
                    else -> "Loop A"
                }
                AssistChip(
                    onClick = {
                        when {
                            playerState.abLoop.active -> viewModel.playerController.clearAbLoop()
                            playerState.abLoop.pointAMs == null -> viewModel.playerController.setAbLoopPointA()
                            else -> viewModel.playerController.setAbLoopPointB()
                        }
                    },
                    label = { Text(abLabel) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = if (playerState.abLoop.active || playerState.abLoop.pointAMs != null) secondaryGlow else Color.White
                    )
                )

                val sleepLabel = if (playerState.sleepTimer.active) {
                    if (playerState.sleepTimer.endOfTrack) "End" else TimeFormatter.formatDuration(playerState.sleepTimer.remainingMs)
                } else {
                    "Sleep"
                }
                AssistChip(
                    onClick = { showSleepSheet = true },
                    label = { Text(sleepLabel) },
                    leadingIcon = { Icon(Icons.Rounded.Bedtime, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = if (playerState.sleepTimer.active) secondaryGlow else Color.White
                    )
                )

                IconButton(onClick = { showQueueSheet = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        tint = Color.White
                    )
                }
            }
        }
    }

    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Playing Queue (${playerState.queue.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showSaveQueueDialog = true }) {
                        Icon(Icons.Rounded.Save, contentDescription = "Save queue as playlist")
                    }
                    IconButton(onClick = { viewModel.playerController.clearQueueExceptCurrent() }) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear upcoming")
                    }
                }

                LazyColumn(
                    modifier = Modifier.height(380.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(playerState.queue) { index, item ->
                        val isActive = index == playerState.currentIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.playerController.jumpToQueueIndex(index) }
                                .background(if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AlbumArtwork(
                                songId = item.id,
                                albumId = item.albumId,
                                contentUri = item.contentUri,
                                size = 42.dp,
                                cornerRadius = 8.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.title,
                                    color = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    item.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (index > 0) {
                                IconButton(
                                    onClick = { viewModel.playerController.moveQueueItem(index, index - 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(18.dp))
                                }
                            }
                            if (index < playerState.queue.lastIndex) {
                                IconButton(
                                    onClick = { viewModel.playerController.moveQueueItem(index, index + 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(18.dp))
                                }
                            }
                            if (playerState.queue.size > 1) {
                                IconButton(
                                    onClick = { viewModel.playerController.removeFromQueue(index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSleepSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSleepSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Sleep Timer", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Playback gently fades out over the final 10 seconds before pausing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 15, 30, 45, 60).forEach { mins ->
                        FilledTonalButton(
                            onClick = {
                                viewModel.playerController.setSleepTimerMinutes(mins)
                                showSleepSheet = false
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("${mins}m")
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.playerController.setSleepTimerEndOfTrack()
                            showSleepSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("End of Current Track")
                    }
                    if (playerState.sleepTimer.active) {
                        TextButton(
                            onClick = {
                                viewModel.playerController.cancelSleepTimer()
                                showSleepSheet = false
                            }
                        ) {
                            Text("Cancel Timer")
                        }
                    }
                }
            }
        }
    }

    if (showSpeedDialog) {
        var speed by remember { mutableFloatStateOf(playerState.playbackSpeed) }
        var pitch by remember { mutableFloatStateOf(playerState.playbackPitch) }
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Speed & Pitch") },
            text = {
                Column {
                    Text("Playback Speed: ${String.format("%.2f", speed)}x")
                    Slider(
                        value = speed,
                        onValueChange = {
                            speed = it
                            viewModel.playerController.setPlaybackParameters(speed, pitch)
                        },
                        valueRange = 0.5f..2.0f
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Pitch Shift: ${String.format("%.2f", pitch)}x")
                    Slider(
                        value = pitch,
                        onValueChange = {
                            pitch = it
                            viewModel.playerController.setPlaybackParameters(speed, pitch)
                        },
                        valueRange = 0.5f..2.0f
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = {
                    speed = 1.0f
                    pitch = 1.0f
                    viewModel.playerController.setPlaybackParameters(1.0f, 1.0f)
                }) { Text("Reset") }
            }
        )
    }

    if (showLyricsEditor) {
        var raw by remember {
            mutableStateOf(
                lyricsContent?.lines?.joinToString("\n") {
                    if (lyricsContent?.isSynced == true) "[${TimeFormatter.formatDuration(it.timestampMs)}] ${it.text}"
                    else it.text
                } ?: ""
            )
        }
        AlertDialog(
            onDismissRequest = { showLyricsEditor = false },
            title = { Text("Edit / Paste LRC Lyrics") },
            text = {
                OutlinedTextField(
                    value = raw,
                    onValueChange = { raw = it },
                    label = { Text("Supports [mm:ss.xx] synced LRC or plain text") },
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveLyricsForCurrentSong(raw)
                    showLyricsEditor = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showLyricsEditor = false }) { Text("Cancel") }
            }
        )
    }

    if (showMoreSheet) {
        SongOptionsBottomSheet(
            song = song,
            isFavorite = isFavorite,
            onDismiss = { showMoreSheet = false },
            onPlayNext = { viewModel.playerController.playNext(song) },
            onAddToQueue = { viewModel.playerController.addToQueue(song) },
            onAddToPlaylist = { showAddToPlaylist = true },
            onToggleFavorite = { viewModel.playerController.toggleFavorite(song.id) },
            onShare = { viewModel.shareSong(song) },
            onSetRingtone = { viewModel.setAsRingtone(song) },
            onShowDetails = { showSongInfo = true }
        )
    }

    if (showAddToPlaylist) {
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { showAddToPlaylist = false },
            onSelectPlaylist = { plId -> viewModel.addSongToPlaylist(plId, song.id) },
            onCreateNewPlaylist = { name -> viewModel.createPlaylist(name, listOf(song.id)) }
        )
    }

    if (showSongInfo) {
        SongInfoDialog(song = song, onDismiss = { showSongInfo = false })
    }

    if (showSaveQueueDialog) {
        CreatePlaylistDialog(
            onDismiss = { showSaveQueueDialog = false },
            onCreate = { name -> viewModel.saveQueueAsPlaylist(name) }
        )
    }
}

@Composable
private fun CircularSpectrumRing(
    levels: List<Float>,
    isPlaying: Boolean,
    primary: Color,
    secondary: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val barCount = 40
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension * 0.42f
        val maxBarLen = size.minDimension * 0.085f

        for (i in 0 until barCount) {
            val angleRad = (i.toFloat() / barCount) * (2f * Math.PI.toFloat())
            val sampleIdx = if (levels.isEmpty()) 0 else i % levels.size
            val amp = if (isPlaying && levels.isNotEmpty()) levels[sampleIdx] else 0.14f
            val len = maxBarLen * amp.coerceIn(0.14f, 1.0f)

            val start = Offset(
                center.x + cos(angleRad) * baseRadius,
                center.y + sin(angleRad) * baseRadius
            )
            val end = Offset(
                center.x + cos(angleRad) * (baseRadius + len),
                center.y + sin(angleRad) * (baseRadius + len)
            )
            drawLine(
                color = if (i % 2 == 0) primary.copy(alpha = 0.85f) else secondary.copy(alpha = 0.8f),
                start = start,
                end = end,
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun LyricsPanel(
    lyrics: com.pulsemusic.player.model.LyricsContent?,
    positionMs: Long,
    accentColor: Color,
    onSeekTo: (Long) -> Unit,
    onEditClick: () -> Unit
) {
    if (lyrics == null || lyrics.lines.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = accentColor, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("No Lyrics Found", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Place a matching .lrc file next to the song or paste synced LRC lyrics directly.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.68f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            FilledTonalButton(onClick = onEditClick) {
                Icon(Icons.Rounded.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add / Paste Lyrics")
            }
        }
        return
    }

    val activeIndex = if (lyrics.isSynced) {
        LrcParser.findActiveLineIndex(lyrics.lines, positionMs)
    } else {
        -1
    }
    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 36.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(lyrics.lines) { idx, line ->
                val active = idx == activeIndex
                Text(
                    text = line.text,
                    style = if (active) {
                        MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    } else {
                        MaterialTheme.typography.bodyLarge
                    },
                    color = if (active) accentColor else Color.White.copy(alpha = 0.56f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = lyrics.isSynced) { onSeekTo(line.timestampMs) }
                        .padding(vertical = 4.dp)
                )
            }
        }

        IconButton(
            onClick = onEditClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
        ) {
            Icon(Icons.Rounded.Edit, contentDescription = "Edit lyrics", tint = Color.White)
        }
    }
}
