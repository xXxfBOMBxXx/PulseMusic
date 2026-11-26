package com.pulsemusic.player.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pulsemusic.player.viewmodel.MusicViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val eqState by viewModel.equalizerState.collectAsState()
    val crossfadeSec by viewModel.crossfadeSeconds.collectAsState()
    val presets = viewModel.equalizerPresets
    val reverbLabels = listOf("Off", "Small Room", "Medium Room", "Large Hall", "Plate")

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = { Text("Audio Studio & EQ", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.effects.selectPreset(0) }) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset Flat")
                    }
                    Switch(
                        checked = eqState.enabled,
                        onCheckedChange = { viewModel.effects.setEqualizerEnabled(it) },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 110.dp)
        ) {
            // Presets Carousel
            Text(
                "Sound Presets",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(presets) { idx, preset ->
                    FilterChip(
                        selected = eqState.presetIndex == idx,
                        onClick = { viewModel.effects.selectPreset(idx) },
                        label = { Text(preset.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Frequency Curve + 5 Vertical EQ Sliders
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    EqCurvePreview(
                        bands = eqState.bandLevelsMb,
                        minMb = eqState.minBandLevelMb,
                        maxMb = eqState.maxBandLevelMb,
                        enabled = eqState.enabled,
                        primary = MaterialTheme.colorScheme.primary,
                        secondary = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(78.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val bandCount = eqState.bandFrequenciesHz.size
                        for (i in 0 until bandCount) {
                            val freqHz = eqState.bandFrequenciesHz[i]
                            val freqLabel = if (freqHz >= 1000) "${freqHz / 1000}k" else "${freqHz}Hz"
                            val levelMb = eqState.bandLevelsMb.getOrElse(i) { 0 }
                            val dbText = "${(levelMb / 100f).roundToInt()}dB"

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = dbText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (eqState.enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                VerticalEqFader(
                                    value = levelMb,
                                    min = eqState.minBandLevelMb,
                                    max = eqState.maxBandLevelMb,
                                    enabled = eqState.enabled,
                                    onChange = { newVal -> viewModel.effects.setBandLevel(i, newVal) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .width(34.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = freqLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bass Boost & 3D Virtualizer & Crossfade
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Bass Boost (${eqState.bassBoostStrength / 10}%)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = eqState.bassBoostStrength.toFloat(),
                        onValueChange = { viewModel.effects.setBassBoost(it.toInt()) },
                        valueRange = 0f..1000f
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "3D Virtualizer / Surround (${eqState.virtualizerStrength / 10}%)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = eqState.virtualizerStrength.toFloat(),
                        onValueChange = { viewModel.effects.setVirtualizer(it.toInt()) },
                        valueRange = 0f..1000f
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Track Crossfade (${crossfadeSec}s)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = crossfadeSec.toFloat(),
                        onValueChange = { viewModel.preferences.setCrossfadeSeconds(it.roundToInt()) },
                        valueRange = 0f..12f,
                        steps = 11
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Reverb Selector
            Text(
                "Room Reverb",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(reverbLabels) { idx, label ->
                    FilterChip(
                        selected = eqState.reverbPreset == idx,
                        onClick = { viewModel.effects.setReverbPreset(idx) },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EqCurvePreview(
    bands: List<Int>,
    minMb: Int,
    maxMb: Int,
    enabled: Boolean,
    primary: Color,
    secondary: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val midY = size.height / 2f
        drawLine(
            color = Color.White.copy(alpha = 0.14f),
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = 2f
        )
        if (bands.isEmpty()) return@Canvas

        val range = (maxMb - minMb).coerceAtLeast(1).toFloat()
        val points = bands.mapIndexed { idx, value ->
            val x = if (bands.size == 1) size.width / 2f else (idx.toFloat() / (bands.size - 1)) * size.width
            val norm = ((value - minMb).toFloat() / range).coerceIn(0f, 1f)
            val y = size.height - (norm * size.height * 0.8f + size.height * 0.1f)
            Offset(x, y)
        }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 0 until points.lastIndex) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val cx = (p0.x + p1.x) / 2f
                cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }
        }

        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                if (enabled) listOf(primary, secondary) else listOf(Color.Gray, Color.DarkGray)
            ),
            style = Stroke(width = 6f, cap = StrokeCap.Round)
        )

        points.forEach { pt ->
            drawCircle(
                color = if (enabled) Color.White else Color.Gray,
                radius = 7f,
                center = pt
            )
        }
    }
}

@Composable
private fun VerticalEqFader(
    value: Int,
    min: Int,
    max: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val range = (max - min).coerceAtLeast(1).toFloat()
    val norm = ((value - min).toFloat() / range).coerceIn(0f, 1f)
    val trackBg = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = if (enabled) MaterialTheme.colorScheme.secondary else Color.Gray

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(trackBg)
            .pointerInput(min, max) {
                detectTapGestures { offset ->
                    val frac = (1f - (offset.y / size.height.toFloat())).coerceIn(0f, 1f)
                    onChange((min + frac * range).roundToInt())
                }
            }
            .pointerInput(min, max) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val frac = (1f - (change.position.y / size.height.toFloat())).coerceIn(0f, 1f)
                    onChange((min + frac * range).roundToInt())
                }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(norm.coerceAtLeast(0.06f))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(activeColor, MaterialTheme.colorScheme.primary)
                    )
                )
        )
    }
}
