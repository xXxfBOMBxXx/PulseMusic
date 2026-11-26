package com.pulsemusic.player.util

import android.content.Context
import android.net.Uri
import com.pulsemusic.player.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object DemoAudioGenerator {

    private data class DemoSpec(
        val fileName: String,
        val title: String,
        val artist: String,
        val album: String,
        val genre: String,
        val rootHz: Double,
        val chordRatios: List<Double>,
        val durationSec: Int
    )

    private val specs = listOf(
        DemoSpec(
            fileName = "pulse_neon_horizon.wav",
            title = "Neon Horizon",
            artist = "Pulse Synth Lab",
            album = "Midnight Frequency",
            genre = "Synthwave",
            rootHz = 220.0,
            chordRatios = listOf(1.0, 1.25, 1.5, 1.875),
            durationSec = 22
        ),
        DemoSpec(
            fileName = "pulse_lunar_drift.wav",
            title = "Lunar Drift",
            artist = "Aura Collective",
            album = "Midnight Frequency",
            genre = "Ambient",
            rootHz = 196.0,
            chordRatios = listOf(1.0, 1.189, 1.498, 1.782),
            durationSec = 24
        ),
        DemoSpec(
            fileName = "pulse_velvet_echo.wav",
            title = "Velvet Echo",
            artist = "Pulse Synth Lab",
            album = "Studio Sessions",
            genre = "Lo-Fi",
            rootHz = 261.63,
            chordRatios = listOf(1.0, 1.2, 1.5, 1.8),
            durationSec = 20
        )
    )

    suspend fun ensureDemoTracks(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val demoDir = File(context.filesDir, "demo_music").apply { mkdirs() }
        val result = mutableListOf<Song>()
        specs.forEachIndexed { index, spec ->
            val outFile = File(demoDir, spec.fileName)
            if (!outFile.exists() || outFile.length() < 1024L) {
                writeSynthesizedWav(outFile, spec)
            }
            result.add(
                Song(
                    id = -1000L - index,
                    title = spec.title,
                    artist = spec.artist,
                    album = spec.album,
                    albumId = -200L - (index % 2),
                    durationMs = spec.durationSec * 1000L,
                    contentUri = Uri.fromFile(outFile),
                    filePath = outFile.absolutePath,
                    folderPath = demoDir.absolutePath,
                    folderName = "Demo Studio",
                    sizeBytes = outFile.length(),
                    dateAddedSec = System.currentTimeMillis() / 1000L - index,
                    trackNumber = index + 1,
                    year = 2026,
                    mimeType = "audio/wav",
                    genre = spec.genre
                )
            )
        }
        result
    }

    fun getExistingDemoTracks(context: Context): List<Song> {
        val demoDir = File(context.filesDir, "demo_music")
        if (!demoDir.exists()) return emptyList()
        return specs.mapIndexedNotNull { index, spec ->
            val file = File(demoDir, spec.fileName)
            if (file.exists() && file.length() > 1024L) {
                Song(
                    id = -1000L - index,
                    title = spec.title,
                    artist = spec.artist,
                    album = spec.album,
                    albumId = -200L - (index % 2),
                    durationMs = spec.durationSec * 1000L,
                    contentUri = Uri.fromFile(file),
                    filePath = file.absolutePath,
                    folderPath = demoDir.absolutePath,
                    folderName = "Demo Studio",
                    sizeBytes = file.length(),
                    dateAddedSec = file.lastModified() / 1000L,
                    trackNumber = index + 1,
                    year = 2026,
                    mimeType = "audio/wav",
                    genre = spec.genre
                )
            } else {
                null
            }
        }
    }

    private fun writeSynthesizedWav(file: File, spec: DemoSpec) {
        val sampleRate = 22050
        val totalSamples = sampleRate * spec.durationSec
        val dataSize = totalSamples * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + dataSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1.toShort())
            putShort(1.toShort())
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2.toShort())
            putShort(16.toShort())
            put("data".toByteArray())
            putInt(dataSize)
        }.array()

        val pcmBuffer = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
        val arpBeatSamples = sampleRate / 2
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = when {
                t < 0.8 -> t / 0.8
                t > spec.durationSec - 1.2 -> ((spec.durationSec - t) / 1.2).coerceAtLeast(0.0)
                else -> 1.0
            }
            val chordIndex = (i / (sampleRate * 4)) % 2
            val rootShift = if (chordIndex == 0) 1.0 else 0.8909
            val arpNote = spec.chordRatios[(i / arpBeatSamples) % spec.chordRatios.size]

            val pad = spec.chordRatios.sumOf { r ->
                sin(2.0 * PI * spec.rootHz * rootShift * r * 0.5 * t)
            } / spec.chordRatios.size

            val arpPhase = (i % arpBeatSamples).toDouble() / arpBeatSamples
            val arpDecay = kotlin.math.exp(-arpPhase * 4.5)
            val lead = sin(2.0 * PI * spec.rootHz * rootShift * arpNote * t) * arpDecay * 0.55

            val bassPulse = sin(2.0 * PI * (spec.rootHz * 0.25 * rootShift) * t) * 0.45
            val mixed = ((pad * 0.45 + lead + bassPulse) * 0.48 * envelope).coerceIn(-0.98, 0.98)
            pcmBuffer.putShort((mixed * Short.MAX_VALUE).toInt().toShort())
        }

        FileOutputStream(file).use { out ->
            out.write(header)
            out.write(pcmBuffer.array())
        }
    }
}
