package com.pulsemusic.player.ui.screens

import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulsemusic.player.model.AppThemeStyle
import com.pulsemusic.player.model.IslandStyle
import com.pulsemusic.player.viewmodel.MusicViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    onOpenOverlayPermission: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val overlayGranted = Settings.canDrawOverlays(context)

    val islandConfig by viewModel.islandConfig.collectAsState()
    val themeStyle by viewModel.themeStyle.collectAsState()
    val dynamicArt by viewModel.dynamicArtTheme.collectAsState()
    val shakeToSkip by viewModel.shakeToSkip.collectAsState()
    val pauseOnNoisy by viewModel.pauseOnHeadsetDisconnect.collectAsState()
    val minDurationSec by viewModel.minDurationSec.collectAsState()
    val crossfadeSec by viewModel.crossfadeSeconds.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = { Text("PulseMusic Studio & Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Dynamic Island Studio Section
            SectionHeader(icon = Icons.Rounded.Layers, title = "Dynamic Island Player")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (overlayGranted) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = if (overlayGranted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (overlayGranted) "System Floating Overlay Ready" else "Overlay Permission Needed",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Lets the Dynamic Island float over your home screen and other apps.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!overlayGranted) {
                            Button(onClick = onOpenOverlayPermission) { Text("Grant") }
                        }
                    }

                    SettingSwitchRow(
                        title = "Enable Floating Dynamic Island",
                        subtitle = "Show interactive pill at top of screen during playback",
                        checked = islandConfig.enabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !overlayGranted) onOpenOverlayPermission()
                            viewModel.preferences.updateIslandConfig { it.copy(enabled = enabled) }
                        }
                    )

                    SettingSwitchRow(
                        title = "Keep Visible When Paused",
                        subtitle = "Stay docked as compact capsule when music is paused",
                        checked = islandConfig.showWhenPaused,
                        onCheckedChange = { show ->
                            viewModel.preferences.updateIslandConfig { it.copy(showWhenPaused = show) }
                        }
                    )

                    SettingSwitchRow(
                        title = "Live Island Spectrum Bars",
                        subtitle = "Animate audio wave bars inside the capsule",
                        checked = islandConfig.showLiveSpectrum,
                        onCheckedChange = { spec ->
                            viewModel.preferences.updateIslandConfig { it.copy(showLiveSpectrum = spec) }
                        }
                    )

                    Text("Island Visual Style", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IslandStyle.entries.forEach { style ->
                            FilterChip(
                                selected = islandConfig.style == style,
                                onClick = {
                                    viewModel.preferences.updateIslandConfig { it.copy(style = style) }
                                },
                                label = { Text(style.label) }
                            )
                        }
                    }

                    Text(
                        "Auto-Collapse Expanded Card: ${if (islandConfig.autoCollapseSeconds == 0) "Manual" else "${islandConfig.autoCollapseSeconds}s"}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = islandConfig.autoCollapseSeconds.toFloat(),
                        onValueChange = { sec ->
                            viewModel.preferences.updateIslandConfig { it.copy(autoCollapseSeconds = sec.roundToInt()) }
                        },
                        valueRange = 0f..15f,
                        steps = 14
                    )

                    Text(
                        "Top Cutout Offset Y: ${islandConfig.offsetY} dp",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = islandConfig.offsetY.toFloat(),
                        onValueChange = { y ->
                            viewModel.preferences.updateIslandConfig { it.copy(offsetY = y.roundToInt()) }
                        },
                        valueRange = 4f..100f
                    )

                    Text(
                        "Horizontal Offset X: ${islandConfig.offsetX} dp",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = islandConfig.offsetX.toFloat(),
                        onValueChange = { x ->
                            viewModel.preferences.updateIslandConfig { it.copy(offsetX = x.roundToInt()) }
                        },
                        valueRange = -90f..90f
                    )
                }
            }

            // Appearance & Theme Section
            SectionHeader(icon = Icons.Rounded.Palette, title = "Appearance & Theme")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemeStyle.entries.forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.preferences.setThemeStyle(style) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(style.label, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    style.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (themeStyle == style) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }

                    SettingSwitchRow(
                        title = "Adaptive Album Art Colors",
                        subtitle = "Tint app accents with current track artwork",
                        checked = dynamicArt,
                        onCheckedChange = { viewModel.preferences.setDynamicArtTheme(it) }
                    )
                }
            }

            // Audio & Playback Gestures
            SectionHeader(icon = Icons.Rounded.GraphicEq, title = "Audio & Smart Gestures")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SettingSwitchRow(
                        title = "Shake Phone to Skip Track",
                        subtitle = "Gently shake device during playback to advance",
                        checked = shakeToSkip,
                        onCheckedChange = { viewModel.preferences.setShakeToSkip(it) }
                    )
                    SettingSwitchRow(
                        title = "Pause When Headphones Unplug",
                        subtitle = "Automatically pause when Bluetooth/wired audio disconnects",
                        checked = pauseOnNoisy,
                        onCheckedChange = { viewModel.preferences.setPauseOnHeadsetDisconnect(it) }
                    )

                    Text("Crossfade: ${crossfadeSec}s", style = MaterialTheme.typography.titleSmall)
                    Slider(
                        value = crossfadeSec.toFloat(),
                        onValueChange = { viewModel.preferences.setCrossfadeSeconds(it.roundToInt()) },
                        valueRange = 0f..12f,
                        steps = 11
                    )

                    Text("Ignore Short Audio Under ${minDurationSec}s", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 15, 30, 60).forEach { sec ->
                            FilterChip(
                                selected = minDurationSec == sec,
                                onClick = {
                                    viewModel.preferences.setMinDurationSec(sec)
                                    viewModel.scanLibrary()
                                },
                                label = { Text(if (sec == 0) "All" else "${sec}s") }
                            )
                        }
                    }

                    if (!hasMicPermission) {
                        OutlinedButton(
                            onClick = onRequestMicPermission,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enable Real-Time Hardware Audio FFT (Mic Permission)")
                        }
                    }
                }
            }

            // Library Tools
            SectionHeader(icon = Icons.Rounded.AutoAwesome, title = "Library & Studio Tools")
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.scanLibrary() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rescan Device")
                        }
                        OutlinedButton(
                            onClick = { viewModel.generateDemoTracks() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Demo Tracks")
                        }
                    }
                    Text(
                        "PulseMusic v1.0.0 • Media3 ExoPlayer + Adaptive Dynamic Island + 5-Band Studio EQ + LRC Synced Lyrics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
