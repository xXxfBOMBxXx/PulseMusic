package com.pulsemusic.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import com.pulsemusic.player.MainActivity
import com.pulsemusic.player.PulseMusicApp
import com.pulsemusic.player.R

class MusicAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, widgetId, lastArtBitmap)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val app = context.applicationContext as? PulseMusicApp ?: return
        val controller = app.playerController
        when (intent.action) {
            ACTION_PLAY_PAUSE -> controller.togglePlayPause()
            ACTION_NEXT -> controller.skipNext()
            ACTION_PREV -> controller.skipPrevious()
            ACTION_FAVORITE -> {
                controller.uiState.value.currentSong?.let { song ->
                    controller.toggleFavorite(song.id)
                }
            }
            ACTION_WIDGET_UPDATE -> {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, MusicAppWidgetProvider::class.java))
                onUpdate(context, manager, ids)
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_UPDATE = "com.pulsemusic.player.ACTION_WIDGET_UPDATE"
        const val ACTION_PLAY_PAUSE = "com.pulsemusic.player.ACTION_WIDGET_PLAY_PAUSE"
        const val ACTION_NEXT = "com.pulsemusic.player.ACTION_WIDGET_NEXT"
        const val ACTION_PREV = "com.pulsemusic.player.ACTION_WIDGET_PREV"
        const val ACTION_FAVORITE = "com.pulsemusic.player.ACTION_WIDGET_FAVORITE"

        @Volatile
        private var lastArtBitmap: Bitmap? = null

        fun pushUpdate(context: Context, artBitmap: Bitmap? = lastArtBitmap) {
            lastArtBitmap = artBitmap
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, MusicAppWidgetProvider::class.java))
            for (id in ids) {
                updateSingleWidget(context, manager, id, artBitmap)
            }
        }

        private fun updateSingleWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            artBitmap: Bitmap?
        ) {
            val app = context.applicationContext as? PulseMusicApp ?: return
            val state = app.playerController.uiState.value
            val song = state.currentSong

            val views = RemoteViews(context.packageName, R.layout.widget_music_player)
            views.setTextViewText(R.id.widget_title, song?.title ?: context.getString(R.string.app_name))
            views.setTextViewText(
                R.id.widget_artist,
                song?.artist ?: context.getString(R.string.tap_to_play)
            )
            views.setImageViewResource(
                R.id.widget_btn_play_pause,
                if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
            )
            val isFav = song != null && song.id in state.favoriteIds
            views.setImageViewResource(
                R.id.widget_btn_favorite,
                if (isFav) R.drawable.ic_widget_heart_filled else R.drawable.ic_widget_heart
            )

            if (artBitmap != null) {
                views.setImageViewBitmap(R.id.widget_album_art, artBitmap)
            } else {
                views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_widget_album_placeholder)
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_NOW_PLAYING, song != null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val openPending = PendingIntent.getActivity(
                context,
                100,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPending)
            views.setOnClickPendingIntent(R.id.widget_album_art, openPending)

            views.setOnClickPendingIntent(
                R.id.widget_btn_prev,
                broadcastPendingIntent(context, ACTION_PREV, 101)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_play_pause,
                broadcastPendingIntent(context, ACTION_PLAY_PAUSE, 102)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_next,
                broadcastPendingIntent(context, ACTION_NEXT, 103)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_favorite,
                broadcastPendingIntent(context, ACTION_FAVORITE, 104)
            )

            manager.updateAppWidget(widgetId, views)
        }

        private fun broadcastPendingIntent(context: Context, action: String, reqCode: Int): PendingIntent {
            val intent = Intent(context, MusicAppWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
