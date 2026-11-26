package com.pulsemusic.player.data

import android.content.Context
import com.pulsemusic.player.model.AppThemeStyle
import com.pulsemusic.player.model.DynamicIslandConfig
import com.pulsemusic.player.model.EqualizerPreset
import com.pulsemusic.player.model.EqualizerState
import com.pulsemusic.player.model.IslandStyle
import com.pulsemusic.player.model.RepeatMode
import com.pulsemusic.player.model.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {

    private val prefs = context.getSharedPreferences("pulsemusic_prefs", Context.MODE_PRIVATE)

    private val _themeStyle = MutableStateFlow(loadThemeStyle())
    val themeStyle: StateFlow<AppThemeStyle> = _themeStyle.asStateFlow()

    private val _dynamicArtTheme = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC_ART_THEME, true))
    val dynamicArtTheme: StateFlow<Boolean> = _dynamicArtTheme.asStateFlow()

    private val _islandConfig = MutableStateFlow(loadIslandConfig())
    val islandConfig: StateFlow<DynamicIslandConfig> = _islandConfig.asStateFlow()

    private val _sortOrder = MutableStateFlow(loadSortOrder())
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _minDurationSec = MutableStateFlow(prefs.getInt(KEY_MIN_DURATION_SEC, 15))
    val minDurationSec: StateFlow<Int> = _minDurationSec.asStateFlow()

    private val _crossfadeSeconds = MutableStateFlow(prefs.getInt(KEY_CROSSFADE_SEC, 0))
    val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds.asStateFlow()

    private val _shakeToSkip = MutableStateFlow(prefs.getBoolean(KEY_SHAKE_TO_SKIP, false))
    val shakeToSkip: StateFlow<Boolean> = _shakeToSkip.asStateFlow()

    private val _pauseOnHeadsetDisconnect = MutableStateFlow(prefs.getBoolean(KEY_PAUSE_NOISY, true))
    val pauseOnHeadsetDisconnect: StateFlow<Boolean> = _pauseOnHeadsetDisconnect.asStateFlow()

    private val _equalizerState = MutableStateFlow(loadEqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    val builtInPresets: List<EqualizerPreset> = listOf(
        EqualizerPreset(0, "Flat", listOf(0, 0, 0, 0, 0)),
        EqualizerPreset(1, "Bass Boost", listOf(750, 500, 100, 0, 150)),
        EqualizerPreset(2, "Pop", listOf(-150, 350, 550, 300, -100)),
        EqualizerPreset(3, "Rock", listOf(600, 300, -200, 350, 650)),
        EqualizerPreset(4, "Jazz", listOf(450, 250, -150, 300, 450)),
        EqualizerPreset(5, "Classical", listOf(500, 200, -100, 400, 550)),
        EqualizerPreset(6, "Electronic", listOf(650, 450, -100, 300, 700)),
        EqualizerPreset(7, "Vocal Boost", listOf(-300, 100, 650, 550, 150)),
        EqualizerPreset(8, "Hip-Hop", listOf(800, 550, 0, 200, 400)),
        EqualizerPreset(9, "Late Night", listOf(350, 200, 300, 100, -200)),
        EqualizerPreset(10, "Custom", listOf(0, 0, 0, 0, 0))
    )

    fun setThemeStyle(style: AppThemeStyle) {
        prefs.edit().putString(KEY_THEME_STYLE, style.name).apply()
        _themeStyle.value = style
    }

    fun setDynamicArtTheme(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_ART_THEME, enabled).apply()
        _dynamicArtTheme.value = enabled
    }

    fun updateIslandConfig(transform: (DynamicIslandConfig) -> DynamicIslandConfig) {
        val updated = transform(_islandConfig.value)
        prefs.edit()
            .putBoolean(KEY_ISLAND_ENABLED, updated.enabled)
            .putBoolean(KEY_ISLAND_WHEN_PAUSED, updated.showWhenPaused)
            .putInt(KEY_ISLAND_COLLAPSE_SEC, updated.autoCollapseSeconds)
            .putInt(KEY_ISLAND_OFFSET_X, updated.offsetX)
            .putInt(KEY_ISLAND_OFFSET_Y, updated.offsetY)
            .putString(KEY_ISLAND_STYLE, updated.style.name)
            .putBoolean(KEY_ISLAND_SPECTRUM, updated.showLiveSpectrum)
            .apply()
        _islandConfig.value = updated
    }

    fun setSortOrder(order: SortOrder) {
        prefs.edit().putString(KEY_SORT_ORDER, order.name).apply()
        _sortOrder.value = order
    }

    fun setMinDurationSec(seconds: Int) {
        prefs.edit().putInt(KEY_MIN_DURATION_SEC, seconds).apply()
        _minDurationSec.value = seconds
    }

    fun setCrossfadeSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_CROSSFADE_SEC, seconds.coerceIn(0, 12)).apply()
        _crossfadeSeconds.value = seconds.coerceIn(0, 12)
    }

    fun setShakeToSkip(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHAKE_TO_SKIP, enabled).apply()
        _shakeToSkip.value = enabled
    }

    fun setPauseOnHeadsetDisconnect(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PAUSE_NOISY, enabled).apply()
        _pauseOnHeadsetDisconnect.value = enabled
    }

    fun updateEqualizerState(transform: (EqualizerState) -> EqualizerState) {
        val updated = transform(_equalizerState.value)
        prefs.edit()
            .putBoolean(KEY_EQ_ENABLED, updated.enabled)
            .putInt(KEY_EQ_PRESET, updated.presetIndex)
            .putString(KEY_EQ_BANDS, updated.bandLevelsMb.joinToString(","))
            .putInt(KEY_EQ_BASS, updated.bassBoostStrength)
            .putInt(KEY_EQ_VIRTUALIZER, updated.virtualizerStrength)
            .putInt(KEY_EQ_REVERB, updated.reverbPreset)
            .apply()
        _equalizerState.value = updated
    }

    fun getLastRepeatMode(): RepeatMode {
        val name = prefs.getString(KEY_REPEAT_MODE, RepeatMode.OFF.name)
        return runCatching { RepeatMode.valueOf(name ?: RepeatMode.OFF.name) }.getOrDefault(RepeatMode.OFF)
    }

    fun saveRepeatMode(mode: RepeatMode) {
        prefs.edit().putString(KEY_REPEAT_MODE, mode.name).apply()
    }

    fun getLastShuffle(): Boolean = prefs.getBoolean(KEY_SHUFFLE, false)

    fun saveShuffle(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHUFFLE, enabled).apply()
    }

    fun getLastSongId(): Long = prefs.getLong(KEY_LAST_SONG_ID, -1L)

    fun saveLastSongId(songId: Long) {
        prefs.edit().putLong(KEY_LAST_SONG_ID, songId).apply()
    }

    private fun loadThemeStyle(): AppThemeStyle {
        val name = prefs.getString(KEY_THEME_STYLE, AppThemeStyle.MIDNIGHT_PULSE.name)
        return runCatching { AppThemeStyle.valueOf(name ?: AppThemeStyle.MIDNIGHT_PULSE.name) }
            .getOrDefault(AppThemeStyle.MIDNIGHT_PULSE)
    }

    private fun loadSortOrder(): SortOrder {
        val name = prefs.getString(KEY_SORT_ORDER, SortOrder.TITLE_ASC.name)
        return runCatching { SortOrder.valueOf(name ?: SortOrder.TITLE_ASC.name) }
            .getOrDefault(SortOrder.TITLE_ASC)
    }

    private fun loadIslandConfig(): DynamicIslandConfig {
        val styleName = prefs.getString(KEY_ISLAND_STYLE, IslandStyle.ADAPTIVE_GLOW.name)
        val style = runCatching { IslandStyle.valueOf(styleName ?: IslandStyle.ADAPTIVE_GLOW.name) }
            .getOrDefault(IslandStyle.ADAPTIVE_GLOW)
        return DynamicIslandConfig(
            enabled = prefs.getBoolean(KEY_ISLAND_ENABLED, true),
            showWhenPaused = prefs.getBoolean(KEY_ISLAND_WHEN_PAUSED, true),
            autoCollapseSeconds = prefs.getInt(KEY_ISLAND_COLLAPSE_SEC, 5),
            offsetX = prefs.getInt(KEY_ISLAND_OFFSET_X, 0),
            offsetY = prefs.getInt(KEY_ISLAND_OFFSET_Y, 18),
            style = style,
            showLiveSpectrum = prefs.getBoolean(KEY_ISLAND_SPECTRUM, true)
        )
    }

    private fun loadEqualizerState(): EqualizerState {
        val rawBands = prefs.getString(KEY_EQ_BANDS, "0,0,0,0,0") ?: "0,0,0,0,0"
        val bands = rawBands.split(",").mapNotNull { it.toIntOrNull() }
            .ifEmpty { listOf(0, 0, 0, 0, 0) }
        return EqualizerState(
            enabled = prefs.getBoolean(KEY_EQ_ENABLED, false),
            presetIndex = prefs.getInt(KEY_EQ_PRESET, 0),
            bandLevelsMb = bands,
            bassBoostStrength = prefs.getInt(KEY_EQ_BASS, 0),
            virtualizerStrength = prefs.getInt(KEY_EQ_VIRTUALIZER, 0),
            reverbPreset = prefs.getInt(KEY_EQ_REVERB, 0)
        )
    }

    companion object {
        private const val KEY_THEME_STYLE = "theme_style"
        private const val KEY_DYNAMIC_ART_THEME = "dynamic_art_theme"
        private const val KEY_ISLAND_ENABLED = "island_enabled"
        private const val KEY_ISLAND_WHEN_PAUSED = "island_when_paused"
        private const val KEY_ISLAND_COLLAPSE_SEC = "island_collapse_sec"
        private const val KEY_ISLAND_OFFSET_X = "island_offset_x"
        private const val KEY_ISLAND_OFFSET_Y = "island_offset_y"
        private const val KEY_ISLAND_STYLE = "island_style"
        private const val KEY_ISLAND_SPECTRUM = "island_spectrum"
        private const val KEY_SORT_ORDER = "sort_order"
        private const val KEY_MIN_DURATION_SEC = "min_duration_sec"
        private const val KEY_CROSSFADE_SEC = "crossfade_sec"
        private const val KEY_SHAKE_TO_SKIP = "shake_to_skip"
        private const val KEY_PAUSE_NOISY = "pause_noisy"
        private const val KEY_EQ_ENABLED = "eq_enabled"
        private const val KEY_EQ_PRESET = "eq_preset"
        private const val KEY_EQ_BANDS = "eq_bands"
        private const val KEY_EQ_BASS = "eq_bass"
        private const val KEY_EQ_VIRTUALIZER = "eq_virtualizer"
        private const val KEY_EQ_REVERB = "eq_reverb"
        private const val KEY_REPEAT_MODE = "repeat_mode"
        private const val KEY_SHUFFLE = "shuffle"
        private const val KEY_LAST_SONG_ID = "last_song_id"
    }
}
