package com.pulsemusic.player.island

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class SpectrumWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var levels: List<Float> = listOf(0.25f, 0.55f, 0.85f, 0.45f, 0.65f)
    private var primaryColor: Int = Color.parseColor("#00E5FF")
    private var secondaryColor: Int = Color.parseColor("#7C4DFF")
    private var active: Boolean = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val barRect = RectF()

    fun updateLevels(newLevels: List<Float>, isPlaying: Boolean) {
        active = isPlaying
        if (newLevels.isNotEmpty()) {
            levels = newLevels
        }
        invalidate()
    }

    fun setAccentColors(primary: Int, secondary: Int) {
        primaryColor = primary
        secondaryColor = secondary
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val barCount = 5
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val gap = w * 0.09f
        val barWidth = (w - gap * (barCount - 1)) / barCount
        val radius = barWidth / 2f

        for (i in 0 until barCount) {
            val sampleIdx = (i * levels.size / barCount).coerceIn(0, levels.lastIndex)
            val amp = if (active) levels[sampleIdx].coerceIn(0.18f, 1.0f) else 0.22f
            val barH = (h * amp).coerceAtLeast(barWidth)
            val left = i * (barWidth + gap)
            val top = (h - barH) / 2f
            barRect.set(left, top, left + barWidth, top + barH)

            paint.color = if (i % 2 == 0) primaryColor else secondaryColor
            canvas.drawRoundRect(barRect, radius, radius, paint)
        }
    }
}
