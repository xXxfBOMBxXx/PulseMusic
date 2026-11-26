package com.pulsemusic.player.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.PlaylistRemove
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pulsemusic.player.model.PlayerUiState
import com.pulsemusic.player.model.Playlist
import com.pulsemusic.player.model.Song
import com.pulsemusic.player.util.AlbumArtLoader
import com.pulsemusic.player.util.TimeFormatter

@Composable
fun AlbumArtwork(
    songId: Long,
    albumId: Long,
    contentUri: Uri?,
    size: Dp,
    cornerRadius: Dp = 12.dp,
    targetPx: Int = 300,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, songId, albumId, contentUri) {
        value = if (contentUri != null) {
            AlbumArtLoader.loadBitmap(context, songId, albumId, contentUri, targetPx)
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Album artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(size * 0.45f)
            )
        }
    }
}

@Composable
fun LiveSpectrumBars(
    levels: List<Float>,
    isPlaying: Boolean,
    primaryColor: Color = MaterialTheme.colorScheme.secondary,
    secondaryColor: Color = MaterialTheme.colorScheme.primary,
    barCount: Int = 5,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val gap = size.width * 0.08f
        val barWidth = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(2f)
        val radius = barWidth / 2f

        for (i in 0 until barCount) {
            val idx = if (levels.isEmpty()) 0 else (i * levels.size / barCount).coerceIn(0, levels.lastIndex)
            val amp = if (isPlaying && levels.isNotEmpty()) {
                levels[idx].coerceIn(0.18f, 1.0f)
            } else {
                0.2f
            }
            val barHeight = (size.height * amp).coerceAtLeast(barWidth)
            val left = i * (barWidth + gap)
            val top = (size.height - barHeight) / 2f
            drawRoundRect(
                color = if (i % 2 == 0) primaryColor else secondaryColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
    }
}

@Composable
fun InAppDynamicIslandBar(
    playerState: PlayerUiState,
    onOpenFullPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val song = playerState.currentSong ?: return
    var expanded by remember { mutableStateOf(false) }
    var dragAccumulated by remember { mutableFloatStateOf(0f) }

    val primaryGlow = Color(playerState.dominantPrimaryArgb)
    val secondaryGlow = Color(playerState.dominantSecondaryArgb)
    val isFav = song.id in playerState.favoriteIds

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        primaryGlow.copy(alpha = 0.6f),
                        secondaryGlow.copy(alpha = 0.55f)
                    )
                ),
                shape = RoundedCornerShape(if (expanded) 28.dp else 32.dp)
            )
            .pointerInput(song.id) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulated = 0f },
                    onHorizontalDrag = { _, dragAmount -> dragAccumulated += dragAmount },
                    onDragEnd = {
                        if (dragAccumulated < -90f) onNext()
                        else if (dragAccumulated > 90f) onPrev()
                    }
                )
            },
        shape = RoundedCornerShape(if (expanded) 28.dp else 32.dp),
        color = Color(0xF50B0D14),
        shadowElevation = 14.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Compact capsule row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenFullPlayer() }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtwork(
                    songId = song.id,
                    albumId = song.albumId,
                    contentUri = song.contentUri,
                    size = 40.dp,
                    cornerRadius = 20.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB5BDD8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (playerState.sleepTimer.active) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• Sleep",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                LiveSpectrumBars(
                    levels = playerState.spectrumLevels,
                    isPlaying = playerState.isPlaying,
                    primaryColor = secondaryGlow,
                    secondaryColor = primaryGlow,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(width = 24.dp, height = 16.dp)
                )

                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play or Pause",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                        contentDescription = "Expand Island",
                        tint = Color(0xFFB5BDD8)
                    )
                }
            }

            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                ) {
                    val duration = playerState.durationMs.coerceAtLeast(1L)
                    var seeking by remember { mutableStateOf(false) }
                    var sliderValue by remember { mutableFloatStateOf(0f) }
                    val displayProgress = if (seeking) {
                        sliderValue
                    } else {
                        (playerState.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    }

                    Slider(
                        value = displayProgress,
                        onValueChange = {
                            seeking = true
                            sliderValue = it
                        },
                        onValueChangeFinished = {
                            onSeekTo((sliderValue * duration).toLong())
                            seeking = false
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = secondaryGlow,
                            inactiveTrackColor = Color.White.copy(alpha = 0.16f)
                        ),
                        modifier = Modifier.height(28.dp)
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
                            color = Color(0xFFA9B2CC)
                        )
                        Text(
                            text = TimeFormatter.formatDuration(playerState.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA9B2CC)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onToggleFavorite(song.id) }) {
                            Icon(
                                imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) Color(0xFFFF4D8D) else Color.White.copy(alpha = 0.78f)
                            )
                        }
                        IconButton(onClick = onPrev) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = primaryGlow,
                            modifier = Modifier
                                .size(44.dp)
                                .clickable { onPlayPause() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Play or Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        IconButton(onClick = onNext) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        IconButton(onClick = onOpenFullPlayer) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInFull,
                                contentDescription = "Open Now Playing",
                                tint = Color.White.copy(alpha = 0.78f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onToggleFavorite: () -> Unit,
    onMoreClick: () -> Unit
) {
    val bgAlpha by animateFloatAsState(
        targetValue = if (isCurrent || isSelected) 0.16f else 0f,
        label = "rowBg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = bgAlpha))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 10.dp)
            )
        }

        Box(contentAlignment = Alignment.Center) {
            AlbumArtwork(
                songId = song.id,
                albumId = song.albumId,
                contentUri = song.contentUri,
                size = 52.dp,
                cornerRadius = 12.dp
            )
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    LiveSpectrumBars(
                        levels = listOf(0.4f, 0.9f, 0.6f, 0.8f),
                        isPlaying = isPlaying,
                        barCount = 4,
                        modifier = Modifier.size(width = 20.dp, height = 16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isCurrent) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${song.artist} • ${TimeFormatter.formatDuration(song.durationMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) Color(0xFFFF4D8D) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(onClick = onMoreClick, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Song Options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    song: Song,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onSetRingtone: () -> Unit,
    onShowDetails: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtwork(
                    songId = song.id,
                    albumId = song.albumId,
                    contentUri = song.contentUri,
                    size = 48.dp,
                    cornerRadius = 10.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${song.artist} • ${song.album}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SheetOptionRow(Icons.Rounded.PlaylistPlay, "Play Next") { onPlayNext(); onDismiss() }
            SheetOptionRow(Icons.AutoMirrored.Rounded.QueueMusic, "Add to Queue") { onAddToQueue(); onDismiss() }
            SheetOptionRow(Icons.AutoMirrored.Rounded.PlaylistAdd, "Add to Playlist") { onAddToPlaylist(); onDismiss() }
            if (onRemoveFromPlaylist != null) {
                SheetOptionRow(Icons.Rounded.PlaylistRemove, "Remove from This Playlist") {
                    onRemoveFromPlaylist()
                    onDismiss()
                }
            }
            SheetOptionRow(
                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (isFavorite) "Remove from Favorites" else "Add to Favorites"
            ) { onToggleFavorite(); onDismiss() }
            SheetOptionRow(Icons.Rounded.Share, "Share Audio") { onShare(); onDismiss() }
            SheetOptionRow(Icons.Rounded.NotificationsActive, "Set as Ringtone") { onSetRingtone(); onDismiss() }
            SheetOptionRow(Icons.Rounded.Info, "Track Details & Path") { onShowDetails(); onDismiss() }
        }
    }
}

@Composable
private fun SheetOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim())
                        onDismiss()
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (Long) -> Unit,
    onCreateNewPlaylist: (String) -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    val userLists = playlists.filter { !it.isSystem }

    if (showCreate) {
        CreatePlaylistDialog(
            onDismiss = { showCreate = false },
            onCreate = {
                onCreateNewPlaylist(it)
                showCreate = false
                onDismiss()
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showCreate = true }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Create New Playlist", fontWeight = FontWeight.SemiBold)
                    }
                }
                items(userLists) { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onSelectPlaylist(pl.id)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("${pl.name} (${pl.songIds.size})", modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun SongInfoDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Track Information") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoPair("Title", song.title)
                InfoPair("Artist", song.artist)
                InfoPair("Album", song.album)
                InfoPair("Genre", song.genre)
                InfoPair("Duration", TimeFormatter.formatDuration(song.durationMs))
                InfoPair("Size", TimeFormatter.formatFileSize(song.sizeBytes))
                InfoPair("Format", song.mimeType)
                InfoPair("Folder", song.folderName)
                InfoPair("Path", song.filePath.ifBlank { song.contentUri.toString() })
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun InfoPair(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
