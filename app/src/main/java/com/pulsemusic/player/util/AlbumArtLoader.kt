package com.pulsemusic.player.util

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AlbumArtLoader {

    private val maxCacheBytes = (Runtime.getRuntime().maxMemory() / 8L).toInt().coerceIn(8 * 1024 * 1024, 48 * 1024 * 1024)

    private val bitmapCache = object : LruCache<String, Bitmap>(maxCacheBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    suspend fun loadBitmap(
        context: Context,
        songId: Long,
        albumId: Long,
        contentUri: Uri,
        targetSizePx: Int = 360
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${songId}_${albumId}_${targetSizePx}"
        bitmapCache.get(cacheKey)?.let { return@withContext it }

        val loaded = tryLoadThumbnail(context, contentUri, targetSizePx)
            ?: tryLoadAlbumArtUri(context, albumId, targetSizePx)
            ?: tryLoadEmbeddedArt(context, contentUri, targetSizePx)

        if (loaded != null) {
            bitmapCache.put(cacheKey, loaded)
        }
        loaded
    }

    private fun tryLoadThumbnail(context: Context, uri: Uri, sizePx: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            context.contentResolver.loadThumbnail(uri, Size(sizePx, sizePx), null)
        } catch (_: Exception) {
            null
        }
    }

    private fun tryLoadAlbumArtUri(context: Context, albumId: Long, targetSizePx: Int): Bitmap? {
        if (albumId <= 0L) return null
        return try {
            val artUri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                albumId
            )
            context.contentResolver.openInputStream(artUri)?.use { stream ->
                val raw = BitmapFactory.decodeStream(stream) ?: return null
                scaleDown(raw, targetSizePx)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun tryLoadEmbeddedArt(context: Context, uri: Uri, targetSizePx: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val data = retriever.embeddedPicture ?: return null
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(data, 0, data.size, opts)
            opts.inSampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight, targetSizePx, targetSizePx)
            opts.inJustDecodeBounds = false
            val decoded = BitmapFactory.decodeByteArray(data, 0, data.size, opts) ?: return null
            scaleDown(decoded, targetSizePx)
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqW: Int, reqH: Int): Int {
        var inSampleSize = 1
        if (height > reqH || width > reqW) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqH && halfWidth / inSampleSize >= reqW) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun scaleDown(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / w, maxDim.toFloat() / h)
        val newW = (w * ratio).toInt().coerceAtLeast(1)
        val newH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    fun extractDominantColors(bitmap: Bitmap?): Pair<Int, Int> {
        if (bitmap == null) {
            return 0xFF7C4DFF.toInt() to 0xFF00E5FF.toInt()
        }
        return try {
            val tiny = Bitmap.createScaledBitmap(bitmap, 24, 24, true)
            var rSum = 0L
            var gSum = 0L
            var bSum = 0L
            var count = 0L
            var vibrantScoreBest = -1f
            var vibrantColor = 0xFF7C4DFF.toInt()
            val hsv = FloatArray(3)

            for (y in 0 until tiny.height) {
                for (x in 0 until tiny.width) {
                    val px = tiny.getPixel(x, y)
                    val r = Color.red(px)
                    val g = Color.green(px)
                    val b = Color.blue(px)
                    rSum += r
                    gSum += g
                    bSum += b
                    count++

                    Color.RGBToHSV(r, g, b, hsv)
                    val saturation = hsv[1]
                    val value = hsv[2]
                    if (value in 0.25f..0.95f && saturation > 0.28f) {
                        val score = saturation * 1.4f + (1f - kotlin.math.abs(value - 0.65f))
                        if (score > vibrantScoreBest) {
                            vibrantScoreBest = score
                            vibrantColor = px
                        }
                    }
                }
            }
            if (tiny !== bitmap) tiny.recycle()

            val avgColor = if (count > 0) {
                Color.rgb((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
            } else {
                0xFF281B54.toInt()
            }
            val primary = darkenOrBoost(vibrantColor, minBrightness = 0.35f)
            val secondary = darkenOrBoost(avgColor, minBrightness = 0.22f)
            primary to secondary
        } catch (_: Exception) {
            0xFF7C4DFF.toInt() to 0xFF00E5FF.toInt()
        }
    }

    private fun darkenOrBoost(color: Int, minBrightness: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = hsv[1].coerceAtLeast(0.42f)
        hsv[2] = hsv[2].coerceIn(minBrightness, 0.88f)
        return Color.HSVToColor(255, hsv)
    }
}
