package com.pulsemusic.player

import com.pulsemusic.player.model.AbLoopState
import com.pulsemusic.player.model.AppThemeStyle
import com.pulsemusic.player.model.DynamicIslandConfig
import com.pulsemusic.player.model.IslandStyle
import com.pulsemusic.player.model.PlayerUiState
import com.pulsemusic.player.model.RepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicModelsTest {

    @Test
    fun defaultDynamicIslandConfig_hasExpectedDefaults() {
        val config = DynamicIslandConfig()
        assertTrue(config.enabled)
        assertTrue(config.showWhenPaused)
        assertTrue(config.showLiveSpectrum)
        assertEquals(IslandStyle.ADAPTIVE_GLOW, config.style)
        assertEquals(5, config.autoCollapseSeconds)
    }

    @Test
    fun defaultPlayerState_startsIdle() {
        val state = PlayerUiState()
        assertFalse(state.isPlaying)
        assertEquals(RepeatMode.OFF, state.repeatMode)
        assertEquals(1.0f, state.playbackSpeed, 0.001f)
        assertFalse(state.abLoop.active)
        assertEquals(AbLoopState(), state.abLoop)
        assertEquals(4, AppThemeStyle.entries.size)
    }
}
