package com.pulsemusic.player

import com.pulsemusic.player.util.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun parseSyncedLrc_sortsTimestampsAndExpandsMultiTags() {
        val raw = """
            [ti:Neon Horizon]
            [ar:Pulse Synth Lab]
            [00:10.50][00:30.00]Chorus line
            [00:02.1]Opening verse
        """.trimIndent()

        val parsed = LrcParser.parse(raw)
        assertTrue(parsed.isSynced)
        assertEquals(3, parsed.lines.size)
        assertEquals(2100L, parsed.lines[0].timestampMs)
        assertEquals("Opening verse", parsed.lines[0].text)
        assertEquals(10500L, parsed.lines[1].timestampMs)
        assertEquals("Chorus line", parsed.lines[1].text)
        assertEquals(30000L, parsed.lines[2].timestampMs)
    }

    @Test
    fun parsePlainLyrics_returnsUnsyncedLines() {
        val raw = "First line\n\nSecond line"
        val parsed = LrcParser.parse(raw)
        assertFalse(parsed.isSynced)
        assertEquals(2, parsed.lines.size)
        assertEquals("First line", parsed.lines[0].text)
        assertEquals("Second line", parsed.lines[1].text)
    }

    @Test
    fun findActiveLineIndex_tracksCurrentPosition() {
        val parsed = LrcParser.parse(
            """
            [00:05.00]Line A
            [00:12.50]Line B
            [00:25.00]Line C
            """.trimIndent()
        )
        assertEquals(-1, LrcParser.findActiveLineIndex(parsed.lines, 2000L))
        assertEquals(0, LrcParser.findActiveLineIndex(parsed.lines, 5000L))
        assertEquals(1, LrcParser.findActiveLineIndex(parsed.lines, 18000L))
        assertEquals(2, LrcParser.findActiveLineIndex(parsed.lines, 40000L))
    }
}
