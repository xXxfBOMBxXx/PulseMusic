package com.pulsemusic.player.util

import android.content.Context
import android.content.Intent
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.pulsemusic.player.model.AudioQualityInfo
import com.pulsemusic.player.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ShareAndRingtoneHelper {

    suspend fun extractAudioQuality(
        context: Context,
        uri: Uri,
        mimeType: String?
    ): AudioQualityInfo = withContext(Dispatchers.IO) {
        var bitrateKbps = 0
        var sampleRateHz = 44100
        var channels = 2
        var formatLabel = formatFromMime(mimeType)

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    formatLabel = formatFromMime(mime)
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRateHz = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        bitrateKbps = format.getInteger(MediaFormat.KEY_BIT_RATE) / 1000
                    }
                    break
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                extractor.release()
            } catch (_: Exception) {
            }
        }

        if (bitrateKbps <= 0) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val br = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
                if (br != null && br > 0) {
                    bitrateKbps = br / 1000
                }
            } catch (_: Exception) {
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {
                }
            }
        }

        AudioQualityInfo(
            formatLabel = formatLabel,
            bitrateKbps = bitrateKbps,
            sampleRateHz = sampleRateHz,
            channelCount = channels
        )
    }

    private fun formatFromMime(mime: String?): String {
        val m = mime.orEmpty().lowercase()
        return when {
            "flac" in m -> "FLAC"
            "mpeg" in m || "mp3" in m -> "MP3"
            "mp4" in m || "aac" in m || "m4a" in m -> "AAC"
            "opus" in m -> "OPUS"
            "vorbis" in m || "ogg" in m -> "OGG"
            "wav" in m || "raw" in m -> "WAV"
            "alac" in m -> "ALAC"
            else -> "AUDIO"
        }
    }

    fun shareSong(context: Context, song: Song) {
        val shareUri = try {
            val file = File(song.filePath)
            if (file.exists() && song.contentUri.scheme == "file") {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                song.contentUri
            }
        } catch (_: Exception) {
            song.contentUri
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = song.mimeType.ifBlank { "audio/*" }
            putExtra(Intent.EXTRA_STREAM, shareUri)
            putExtra(Intent.EXTRA_SUBJECT, "${song.title} - ${song.artist}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share \"${song.title}\"").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun canWriteSystemSettings(context: Context): Boolean {
        return Settings.System.canWrite(context)
    }

    fun openWriteSettingsScreen(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun setAsRingtone(context: Context, song: Song): Result<Unit> {
        return try {
            if (!Settings.System.canWrite(context)) {
                return Result.failure(SecurityException("Write settings permission required"))
            }
            if (song.id < 0L) {
                return Result.failure(IllegalArgumentException("Demo tracks cannot be set as system ringtone"))
            }
            RingtoneManager.setActualDefaultRingtoneUri(
                context,
                RingtoneManager.TYPE_RINGTONE,
                song.contentUri
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
