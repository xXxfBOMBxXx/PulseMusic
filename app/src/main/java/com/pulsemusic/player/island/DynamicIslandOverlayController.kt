package com.pulsemusic.player.island

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.pulsemusic.player.MainActivity
import com.pulsemusic.player.R
import com.pulsemusic.player.data.PreferencesManager
import com.pulsemusic.player.model.DynamicIslandConfig
import com.pulsemusic.player.model.IslandStyle
import com.pulsemusic.player.model.PlayerUiState
import com.pulsemusic.player.util.TimeFormatter
import kotlin.math.abs

class DynamicIslandOverlayController(
    private val context: Context,
    private val preferences: PreferencesManager,
    private val onPlayPause: () -> Unit,
    private val onNext: () -> Unit,
    private val onPrev: () -> Unit,
    private val onSeekTo: (Long) -> Unit,
    private val onToggleFavorite: (Long) -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var rootView: View? = null
    private var rootCard: FrameLayout? = null
    private var compactContainer: LinearLayout? = null
    private var expandedContainer: LinearLayout? = null

    private var compactArt: ImageView? = null
    private var compactTitle: TextView? = null
    private var compactSpectrum: SpectrumWaveView? = null

    private var expandedArt: ImageView? = null
    private var expandedTitle: TextView? = null
    private var expandedArtist: TextView? = null
    private var expandedBadge: TextView? = null
    private var expandedSpectrum: SpectrumWaveView? = null
    private var btnFavorite: ImageButton? = null
    private var btnCollapse: ImageButton? = null
    private var btnPrev: ImageButton? = null
    private var btnPlayPause: ImageButton? = null
    private var btnNext: ImageButton? = null
    private var btnOpenApp: ImageButton? = null
    private var seekBar: SeekBar? = null
    private var timeElapsed: TextView? = null
    private var timeTotal: TextView? = null

    private var layoutParams: WindowManager.LayoutParams? = null
    private var isExpanded = false
    private var isShowing = false
    private var isUserSeeking = false
    private var lastRenderedSongId: Long = Long.MIN_VALUE
    private var currentState = PlayerUiState()
    private var currentConfig = preferences.islandConfig.value

    private val autoCollapseRunnable = Runnable {
        if (isExpanded) setExpanded(false)
    }

    fun canDrawOverlay(): Boolean = Settings.canDrawOverlays(context)

    fun updateState(state: PlayerUiState, artBitmap: Bitmap?) {
        currentState = state
        currentConfig = preferences.islandConfig.value

        val shouldShow = currentConfig.enabled &&
            canDrawOverlay() &&
            state.currentSong != null &&
            (state.isPlaying || currentConfig.showWhenPaused)

        if (!shouldShow) {
            hide()
            return
        }

        if (!isShowing) {
            show()
        }
        if (!isShowing) return

        val song = state.currentSong ?: return
        val songChanged = song.id != lastRenderedSongId
        lastRenderedSongId = song.id

        compactTitle?.text = song.title
        expandedTitle?.text = song.title
        expandedArtist?.text = song.artist

        val qualityText = state.audioQuality?.let { "${it.formatLabel} • ${TimeFormatter.formatSampleRate(it.sampleRateHz)}" }
        val timerText = if (state.sleepTimer.active) {
            if (state.sleepTimer.endOfTrack) "Sleep: Track End"
            else "Sleep: ${TimeFormatter.formatDuration(state.sleepTimer.remainingMs)}"
        } else null
        expandedBadge?.text = timerText ?: qualityText ?: "Pulse Island"

        if (artBitmap != null) {
            compactArt?.setImageBitmap(artBitmap)
            expandedArt?.setImageBitmap(artBitmap)
        } else if (songChanged) {
            compactArt?.setImageResource(R.drawable.ic_widget_album_placeholder)
            expandedArt?.setImageResource(R.drawable.ic_widget_album_placeholder)
        }

        btnPlayPause?.setImageResource(
            if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        )

        val isFav = song.id in state.favoriteIds
        btnFavorite?.setImageResource(
            if (isFav) R.drawable.ic_widget_heart_filled else R.drawable.ic_widget_heart
        )

        val duration = state.durationMs.coerceAtLeast(1L)
        if (!isUserSeeking) {
            val progress = ((state.positionMs.toFloat() / duration.toFloat()) * 1000f).toInt().coerceIn(0, 1000)
            seekBar?.progress = progress
            timeElapsed?.text = TimeFormatter.formatDuration(state.positionMs)
        }
        timeTotal?.text = TimeFormatter.formatDuration(state.durationMs)

        val showSpec = currentConfig.showLiveSpectrum
        compactSpectrum?.visibility = if (showSpec) View.VISIBLE else View.GONE
        expandedSpectrum?.visibility = if (showSpec) View.VISIBLE else View.GONE
        compactSpectrum?.setAccentColors(state.dominantSecondaryArgb, state.dominantPrimaryArgb)
        expandedSpectrum?.setAccentColors(state.dominantSecondaryArgb, state.dominantPrimaryArgb)
        compactSpectrum?.updateLevels(state.spectrumLevels, state.isPlaying)
        expandedSpectrum?.updateLevels(state.spectrumLevels, state.isPlaying)

        applyIslandStyle(currentConfig, state)

        if (songChanged && !isExpanded && state.isPlaying) {
            pulseOnTrackChange()
        }
    }

    fun applyConfig(config: DynamicIslandConfig) {
        currentConfig = config
        layoutParams?.let { lp ->
            lp.x = dpToPx(config.offsetX)
            lp.y = dpToPx(config.offsetY)
            if (isShowing && rootView != null) {
                try {
                    windowManager.updateViewLayout(rootView, lp)
                } catch (_: Exception) {
                }
            }
        }
        applyIslandStyle(config, currentState)
        if (!config.enabled) hide()
    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    private fun show() {
        if (isShowing || !canDrawOverlay()) return
        try {
            val view = LayoutInflater.from(context).inflate(R.layout.overlay_dynamic_island, null)
            rootView = view
            rootCard = view.findViewById(R.id.island_root_card)
            compactContainer = view.findViewById(R.id.island_compact_container)
            expandedContainer = view.findViewById(R.id.island_expanded_container)
            compactArt = view.findViewById(R.id.island_compact_art)
            compactTitle = view.findViewById(R.id.island_compact_title)
            compactSpectrum = view.findViewById(R.id.island_compact_spectrum)
            expandedArt = view.findViewById(R.id.island_expanded_art)
            expandedTitle = view.findViewById(R.id.island_expanded_title)
            expandedArtist = view.findViewById(R.id.island_expanded_artist)
            expandedBadge = view.findViewById(R.id.island_expanded_badge)
            expandedSpectrum = view.findViewById(R.id.island_expanded_spectrum)
            btnFavorite = view.findViewById(R.id.island_btn_favorite)
            btnCollapse = view.findViewById(R.id.island_btn_collapse)
            btnPrev = view.findViewById(R.id.island_btn_prev)
            btnPlayPause = view.findViewById(R.id.island_btn_play_pause)
            btnNext = view.findViewById(R.id.island_btn_next)
            btnOpenApp = view.findViewById(R.id.island_btn_open_app)
            seekBar = view.findViewById(R.id.island_seekbar)
            timeElapsed = view.findViewById(R.id.island_time_elapsed)
            timeTotal = view.findViewById(R.id.island_time_total)

            compactTitle?.isSelected = true
            applyCircularClip(compactArt, 999f)
            applyCircularClip(expandedArt, dpToPx(14).toFloat())

            setupInteractions()

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                x = dpToPx(currentConfig.offsetX)
                y = dpToPx(currentConfig.offsetY)
            }
            layoutParams = params
            windowManager.addView(view, params)
            isShowing = true
            setExpanded(false, animate = false)
        } catch (_: Exception) {
            rootView = null
            isShowing = false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupInteractions() {
        btnPlayPause?.setOnClickListener {
            onPlayPause()
            scheduleAutoCollapse()
        }
        btnNext?.setOnClickListener {
            onNext()
            scheduleAutoCollapse()
        }
        btnPrev?.setOnClickListener {
            onPrev()
            scheduleAutoCollapse()
        }
        btnFavorite?.setOnClickListener {
            currentState.currentSong?.let { onToggleFavorite(it.id) }
            scheduleAutoCollapse()
        }
        btnCollapse?.setOnClickListener {
            setExpanded(false)
        }
        btnOpenApp?.setOnClickListener {
            openAppToNowPlaying()
            setExpanded(false)
        }
        expandedArt?.setOnClickListener {
            openAppToNowPlaying()
            setExpanded(false)
        }

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val target = (currentState.durationMs * progress) / 1000L
                    timeElapsed?.text = TimeFormatter.formatDuration(target)
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {
                isUserSeeking = true
                mainHandler.removeCallbacks(autoCollapseRunnable)
            }

            override fun onStopTrackingTouch(sb: SeekBar?) {
                val progress = sb?.progress ?: 0
                val target = (currentState.durationMs * progress) / 1000L
                onSeekTo(target)
                isUserSeeking = false
                scheduleAutoCollapse()
            }
        })

        val gestureDetector = GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    setExpanded(!isExpanded)
                    return true
                }

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    openAppToNowPlaying()
                    return true
                }

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (e1 == null) return false
                    val dx = e2.x - e1.x
                    val dy = e2.y - e1.y
                    if (abs(dx) > abs(dy) && abs(dx) > 90f) {
                        if (dx < 0) onNext() else onPrev()
                        return true
                    }
                    if (dy < -80f && isExpanded) {
                        setExpanded(false)
                        return true
                    }
                    return false
                }
            }
        )

        var startRawX = 0f
        var startRawY = 0f
        var startParamX = 0
        var startParamY = 0
        var dragging = false

        compactContainer?.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            val lp = layoutParams ?: return@setOnTouchListener true
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startRawX = event.rawX
                    startRawY = event.rawY
                    startParamX = lp.x
                    startParamY = lp.y
                    dragging = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startRawX
                    val dy = event.rawY - startRawY
                    if (abs(dx) > 22f || abs(dy) > 22f) {
                        dragging = true
                        lp.x = (startParamX + dx.toInt()).coerceIn(-dpToPx(120), dpToPx(120))
                        lp.y = (startParamY + dy.toInt()).coerceIn(dpToPx(4), dpToPx(240))
                        try {
                            windowManager.updateViewLayout(rootView, lp)
                        } catch (_: Exception) {
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (dragging) {
                        preferences.updateIslandConfig {
                            it.copy(offsetX = pxToDp(lp.x), offsetY = pxToDp(lp.y))
                        }
                    }
                }
            }
            true
        }
    }

    private fun setExpanded(expand: Boolean, animate: Boolean = true) {
        isExpanded = expand
        mainHandler.removeCallbacks(autoCollapseRunnable)

        if (expand) {
            compactContainer?.visibility = View.GONE
            expandedContainer?.visibility = View.VISIBLE
            scheduleAutoCollapse()
        } else {
            expandedContainer?.visibility = View.GONE
            compactContainer?.visibility = View.VISIBLE
        }

        applyIslandStyle(currentConfig, currentState)

        if (animate) {
            rootCard?.apply {
                scaleX = if (expand) 0.88f else 1.06f
                scaleY = if (expand) 0.88f else 1.06f
                animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(260L)
                    .setInterpolator(OvershootInterpolator(1.1f))
                    .start()
            }
        }
    }

    private fun pulseOnTrackChange() {
        rootCard?.animate()?.cancel()
        rootCard?.apply {
            scaleX = 0.94f
            scaleY = 0.94f
            animate()
                .scaleX(1.04f)
                .scaleY(1.04f)
                .setDuration(180L)
                .withEndAction {
                    animate().scaleX(1f).scaleY(1f).setDuration(180L).start()
                }
                .start()
        }
    }

    private fun scheduleAutoCollapse() {
        mainHandler.removeCallbacks(autoCollapseRunnable)
        val seconds = currentConfig.autoCollapseSeconds
        if (seconds > 0) {
            mainHandler.postDelayed(autoCollapseRunnable, seconds * 1000L)
        }
    }

    private fun applyIslandStyle(config: DynamicIslandConfig, state: PlayerUiState) {
        val card = rootCard ?: return
        val cornerRadiusPx = dpToPx(if (isExpanded) 28 else 22).toFloat()

        val bg = GradientDrawable().apply {
            cornerRadius = cornerRadiusPx
            when (config.style) {
                IslandStyle.CLASSIC_DARK -> {
                    setColor(Color.parseColor("#F509090D"))
                    setStroke(dpToPx(1), Color.parseColor("#33FFFFFF"))
                }
                IslandStyle.ADAPTIVE_GLOW -> {
                    val tintedTop = blendColor(Color.parseColor("#FF0B0B12"), state.dominantPrimaryArgb, 0.24f)
                    val tintedBottom = Color.parseColor("#F709090F")
                    colors = intArrayOf(tintedTop, tintedBottom)
                    orientation = GradientDrawable.Orientation.TL_BR
                    setStroke(dpToPx(1), withAlpha(state.dominantSecondaryArgb, 0x77))
                }
                IslandStyle.NEON_BORDER -> {
                    setColor(Color.parseColor("#F50A0C16"))
                    setStroke(dpToPx(2), Color.parseColor("#CC00E5FF"))
                }
            }
        }
        card.background = bg
    }

    private fun openAppToNowPlaying() {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_NOW_PLAYING, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        context.startActivity(intent)
    }

    fun hide() {
        mainHandler.removeCallbacks(autoCollapseRunnable)
        if (!isShowing) return
        try {
            rootView?.let { windowManager.removeView(it) }
        } catch (_: Exception) {
        }
        rootView = null
        isShowing = false
        isExpanded = false
    }

    private fun applyCircularClip(view: View?, radiusPx: Float) {
        view ?: return
        view.clipToOutline = true
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, outline: Outline) {
                val r = if (radiusPx > 500f) minOf(v.width, v.height) / 2f else radiusPx
                outline.setRoundRect(0, 0, v.width, v.height, r)
            }
        }
    }

    private fun blendColor(base: Int, overlay: Int, ratio: Float): Int {
        val inv = 1f - ratio
        val r = (Color.red(base) * inv + Color.red(overlay) * ratio).toInt().coerceIn(0, 255)
        val g = (Color.green(base) * inv + Color.green(overlay) * ratio).toInt().coerceIn(0, 255)
        val b = (Color.blue(base) * inv + Color.blue(overlay) * ratio).toInt().coerceIn(0, 255)
        return Color.argb(245, r, g, b)
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    private fun pxToDp(px: Int): Int {
        return (px / context.resources.displayMetrics.density).toInt()
    }
}
