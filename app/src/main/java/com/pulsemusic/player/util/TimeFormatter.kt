package com.pulsemusic.player.util

import java.util.Locale

object TimeFormatter {

    fun formatDuration(durationMs: Long): String {
        val safeMs = durationMs.coerceAtLeast(0L)
        val totalSeconds = safeMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    fun formatFileSize(bytes: Long): String {
        val safeBytes = bytes.coerceAtLeast(0L)
        return when {
            safeBytes >= 1024L * 1024L * 1024L ->
                String.format(Locale.US, "%.2f GB", safeBytes / (1024.0 * 1024.0 * 1024.0))
            safeBytes >= 1024L * 1024L ->
                String.format(Locale.US, "%.1f MB", safeBytes / (1024.0 * 1024.0))
            safeBytes >= 1024L ->
                String.format(Locale.US, "%.0f KB", safeBytes / 1024.0)
            else -> "$safeBytes B"
        }
    }

    fun formatBitrate(kbps: Int): String {
        return if (kbps > 0) "$kbps kbps" else "Variable"
    }

    fun formatSampleRate(hz: Int): String {
        return if (hz > 0) {
            val khz = hz / 1000.0
            if (hz % 1000 == 0) {
                String.format(Locale.US, "%.0f kHz", khz)
            } else {
                String.format(Locale.US, "%.1f kHz", khz)
            }
        } else {
            "44.1 kHz"
        }
    }
}
