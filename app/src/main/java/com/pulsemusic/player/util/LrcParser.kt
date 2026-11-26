package com.pulsemusic.player.util

import com.pulsemusic.player.model.LyricLine
import com.pulsemusic.player.model.LyricsContent
import java.io.File

object LrcParser {

    private val TIME_TAG_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val META_TAG_REGEX = Regex("""^\[[a-zA-Z]+:.*]$""")

    fun parse(rawText: String): LyricsContent {
        if (rawText.isBlank()) {
            return LyricsContent(emptyList(), "", false)
        }

        val parsedLines = mutableListOf<LyricLine>()
        val rawLines = rawText.lines()

        for (rawLine in rawLines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue

            val matches = TIME_TAG_REGEX.findAll(trimmed).toList()
            if (matches.isEmpty()) {
                if (META_TAG_REGEX.matches(trimmed)) continue
                continue
            }

            val lyricText = TIME_TAG_REGEX.replace(trimmed, "").trim()
            if (lyricText.isEmpty()) continue

            for (match in matches) {
                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                val fractionStr = match.groupValues[3]
                val millis = when (fractionStr.length) {
                    0 -> 0L
                    1 -> (fractionStr.toLongOrNull() ?: 0L) * 100L
                    2 -> (fractionStr.toLongOrNull() ?: 0L) * 10L
                    else -> fractionStr.take(3).toLongOrNull() ?: 0L
                }
                val timestampMs = (minutes * 60_000L) + (seconds * 1_000L) + millis
                parsedLines.add(LyricLine(timestampMs = timestampMs, text = lyricText))
            }
        }

        if (parsedLines.isNotEmpty()) {
            val sorted = parsedLines.sortedBy { it.timestampMs }
            val plain = sorted.joinToString("\n") { it.text }
            return LyricsContent(lines = sorted, plainText = plain, isSynced = true)
        }

        val plainLines = rawLines
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapIndexed { index, text -> LyricLine(index * 4000L, text) }

        return LyricsContent(
            lines = plainLines,
            plainText = plainLines.joinToString("\n") { it.text },
            isSynced = false
        )
    }

    fun findActiveLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var low = 0
        var high = lines.lastIndex
        var result = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].timestampMs <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }

    fun findSidecarLrcFile(songFilePath: String?): String? {
        if (songFilePath.isNullOrBlank()) return null
        return try {
            val audioFile = File(songFilePath)
            val baseName = audioFile.nameWithoutExtension
            val parent = audioFile.parentFile ?: return null
            val lrcFile = File(parent, "$baseName.lrc")
            if (lrcFile.exists() && lrcFile.canRead()) {
                lrcFile.readText(Charsets.UTF_8)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
