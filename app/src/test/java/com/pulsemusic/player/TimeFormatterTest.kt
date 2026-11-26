package com.pulsemusic.player

import com.pulsemusic.player.util.TimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {

    @Test
    fun formatDuration_formatsMinutesAndHours() {
        assertEquals("0:00", TimeFormatter.formatDuration(-500L))
        assertEquals("0:42", TimeFormatter.formatDuration(42_000L))
        assertEquals("3:05", TimeFormatter.formatDuration(185_000L))
        assertEquals("1:02:09", TimeFormatter.formatDuration(3_729_000L))
    }

    @Test
    fun formatFileSize_formatsUnits() {
        assertEquals("512 B", TimeFormatter.formatFileSize(512L))
        assertEquals("4 KB", TimeFormatter.formatFileSize(4096L))
        assertEquals("5.0 MB", TimeFormatter.formatFileSize(5L * 1024L * 1024L))
    }

    @Test
    fun formatBitrateAndSampleRate_formatsCleanly() {
        assertEquals("320 kbps", TimeFormatter.formatBitrate(320))
        assertEquals("Variable", TimeFormatter.formatBitrate(0))
        assertEquals("48 kHz", TimeFormatter.formatSampleRate(48000))
        assertEquals("44.1 kHz", TimeFormatter.formatSampleRate(44100))
    }
}
