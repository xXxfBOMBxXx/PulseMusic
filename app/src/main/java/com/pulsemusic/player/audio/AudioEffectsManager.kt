package com.pulsemusic.player.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.media.audiofx.Visualizer
import androidx.core.content.ContextCompat
import com.pulsemusic.player.data.PreferencesManager
import com.pulsemusic.player.model.EqualizerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.hypot
import kotlin.math.sin

class AudioEffectsManager(
    private val context: Context,
    private val preferences: PreferencesManager
) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var visualizer: Visualizer? = null
    private var currentSessionId: Int = 0

    private val _spectrumLevels = MutableStateFlow(List(16) { 0.15f })
    val spectrumLevels: StateFlow<List<Float>> = _spectrumLevels.asStateFlow()

    private val _isPlayingState = MutableStateFlow(false)

    fun attachToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        releaseEffectsOnly()
        currentSessionId = audioSessionId

        val state = preferences.equalizerState.value
        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                val bandCount = numberOfBands.toInt().coerceAtLeast(1)
                val range = bandLevelRange
                val minMb = range[0].toInt()
                val maxMb = range[1].toInt()
                val freqs = (0 until bandCount).map { band ->
                    getCenterFreq(band.toShort()) / 1000
                }
                val normalizedBands = (0 until bandCount).map { idx ->
                    state.bandLevelsMb.getOrElse(idx) { 0 }.coerceIn(minMb, maxMb)
                }
                preferences.updateEqualizerState {
                    it.copy(
                        bandFrequenciesHz = freqs,
                        minBandLevelMb = minMb,
                        maxBandLevelMb = maxMb,
                        bandLevelsMb = normalizedBands
                    )
                }
            }
        } catch (_: Exception) {
            equalizer = null
        }

        try {
            bassBoost = BassBoost(0, audioSessionId)
        } catch (_: Exception) {
            bassBoost = null
        }

        try {
            virtualizer = Virtualizer(0, audioSessionId)
        } catch (_: Exception) {
            virtualizer = null
        }

        try {
            presetReverb = PresetReverb(0, audioSessionId)
        } catch (_: Exception) {
            presetReverb = null
        }

        applyState(preferences.equalizerState.value)
        setupVisualizerIfPermitted(audioSessionId)
    }

    fun applyState(state: EqualizerState) {
        try {
            equalizer?.let { eq ->
                eq.enabled = state.enabled
                val bandCount = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                for (band in 0 until bandCount) {
                    val level = state.bandLevelsMb.getOrElse(band) { 0 }
                        .coerceIn(range[0].toInt(), range[1].toInt())
                    eq.setBandLevel(band.toShort(), level.toShort())
                }
            }
        } catch (_: Exception) {
        }

        try {
            bassBoost?.let { bb ->
                if (bb.strengthSupported) {
                    bb.enabled = state.enabled && state.bassBoostStrength > 0
                    bb.setStrength(state.bassBoostStrength.coerceIn(0, 1000).toShort())
                }
            }
        } catch (_: Exception) {
        }

        try {
            virtualizer?.let { virt ->
                if (virt.strengthSupported) {
                    virt.enabled = state.enabled && state.virtualizerStrength > 0
                    virt.setStrength(state.virtualizerStrength.coerceIn(0, 1000).toShort())
                }
            }
        } catch (_: Exception) {
        }

        try {
            presetReverb?.let { rev ->
                rev.enabled = state.enabled && state.reverbPreset > 0
                val preset = when (state.reverbPreset) {
                    1 -> PresetReverb.PRESET_SMALLROOM
                    2 -> PresetReverb.PRESET_MEDIUMROOM
                    3 -> PresetReverb.PRESET_LARGEHALL
                    4 -> PresetReverb.PRESET_PLATE
                    else -> PresetReverb.PRESET_NONE
                }
                rev.preset = preset
            }
        } catch (_: Exception) {
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        preferences.updateEqualizerState { it.copy(enabled = enabled) }
        applyState(preferences.equalizerState.value)
    }

    fun selectPreset(presetIndex: Int) {
        val preset = preferences.builtInPresets.getOrNull(presetIndex) ?: return
        preferences.updateEqualizerState { current ->
            current.copy(
                enabled = true,
                presetIndex = presetIndex,
                bandLevelsMb = if (preset.name == "Custom") current.bandLevelsMb else preset.bandLevelsMb
            )
        }
        applyState(preferences.equalizerState.value)
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        preferences.updateEqualizerState { current ->
            val updated = current.bandLevelsMb.toMutableList()
            while (updated.size <= bandIndex) updated.add(0)
            updated[bandIndex] = levelMb.coerceIn(current.minBandLevelMb, current.maxBandLevelMb)
            current.copy(
                enabled = true,
                presetIndex = preferences.builtInPresets.lastIndex,
                bandLevelsMb = updated
            )
        }
        applyState(preferences.equalizerState.value)
    }

    fun setBassBoost(strength: Int) {
        preferences.updateEqualizerState {
            it.copy(enabled = true, bassBoostStrength = strength.coerceIn(0, 1000))
        }
        applyState(preferences.equalizerState.value)
    }

    fun setVirtualizer(strength: Int) {
        preferences.updateEqualizerState {
            it.copy(enabled = true, virtualizerStrength = strength.coerceIn(0, 1000))
        }
        applyState(preferences.equalizerState.value)
    }

    fun setReverbPreset(preset: Int) {
        preferences.updateEqualizerState {
            it.copy(enabled = true, reverbPreset = preset.coerceIn(0, 4))
        }
        applyState(preferences.equalizerState.value)
    }

    fun onPlaybackStateChanged(isPlaying: Boolean) {
        _isPlayingState.value = isPlaying
        try {
            visualizer?.enabled = isPlaying
        } catch (_: Exception) {
        }
        if (!isPlaying) {
            _spectrumLevels.value = List(16) { 0.08f }
        }
    }

    fun tickSimulatedSpectrum(positionMs: Long) {
        if (!_isPlayingState.value || visualizer != null) return
        val t = positionMs / 140.0
        val levels = List(16) { idx ->
            val wave1 = (sin(t + idx * 0.55) + 1.0) * 0.5
            val wave2 = (sin(t * 1.7 - idx * 0.32) + 1.0) * 0.5
            val centerBias = 1f - kotlin.math.abs(idx - 7.5f) / 11f
            (0.16f + (wave1 * 0.52 + wave2 * 0.32).toFloat() * centerBias).coerceIn(0.1f, 0.98f)
        }
        _spectrumLevels.value = levels
    }

    fun retryAttachVisualizer() {
        if (currentSessionId > 0 && visualizer == null) {
            setupVisualizerIfPermitted(currentSessionId)
        }
    }

    private fun setupVisualizerIfPermitted(audioSessionId: Int) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return

        try {
            val captureSize = Visualizer.getCaptureSizeRange()[1].coerceAtMost(256)
            visualizer = Visualizer(audioSessionId).apply {
                this.captureSize = captureSize
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) = Unit

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (fft == null || fft.size < 4 || !_isPlayingState.value) return
                            val bins = 16
                            val usable = (fft.size / 2 - 1).coerceAtLeast(1)
                            val step = (usable / bins).coerceAtLeast(1)
                            val result = MutableList(bins) { 0.12f }
                            for (b in 0 until bins) {
                                val idx = (1 + b * step) * 2
                                if (idx + 1 < fft.size) {
                                    val re = fft[idx].toFloat()
                                    val im = fft[idx + 1].toFloat()
                                    val mag = hypot(re, im)
                                    result[b] = (mag / 48f).coerceIn(0.12f, 1.0f)
                                }
                            }
                            _spectrumLevels.value = result
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true
                )
                enabled = _isPlayingState.value
            }
        } catch (_: Exception) {
            visualizer = null
        }
    }

    private fun releaseEffectsOnly() {
        try {
            visualizer?.release()
        } catch (_: Exception) {
        }
        try {
            equalizer?.release()
        } catch (_: Exception) {
        }
        try {
            bassBoost?.release()
        } catch (_: Exception) {
        }
        try {
            virtualizer?.release()
        } catch (_: Exception) {
        }
        try {
            presetReverb?.release()
        } catch (_: Exception) {
        }
        visualizer = null
        equalizer = null
        bassBoost = null
        virtualizer = null
        presetReverb = null
    }

    fun release() {
        releaseEffectsOnly()
        currentSessionId = 0
    }
}
