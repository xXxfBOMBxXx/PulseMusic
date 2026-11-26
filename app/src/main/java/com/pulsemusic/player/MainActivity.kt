package com.pulsemusic.player

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.pulsemusic.player.service.MusicPlaybackService
import com.pulsemusic.player.ui.PulseMusicAppHost
import com.pulsemusic.player.ui.theme.PulseMusicTheme
import com.pulsemusic.player.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    private var hasAudioPermission by mutableStateOf(false)
    private var hasMicPermission by mutableStateOf(false)
    private var openNowPlayingSignal by mutableIntStateOf(0)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshPermissions()
        if (hasAudioPermission) {
            viewModel.scanLibrary()
        }
        if (hasMicPermission) {
            viewModel.effects.retryAttachVisualizer()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startService(Intent(this, MusicPlaybackService::class.java))

        refreshPermissions()
        requestStartupPermissionsIfNeeded()
        viewModel.scanLibrary()
        handleIncomingIntent(intent)

        setContent {
            val themeStyle by viewModel.themeStyle.collectAsState()
            val dynamicArt by viewModel.dynamicArtTheme.collectAsState()
            val playerState by viewModel.playerState.collectAsState()

            PulseMusicTheme(
                themeStyle = themeStyle,
                dynamicArtTheme = dynamicArt,
                dominantPrimaryArgb = playerState.dominantPrimaryArgb,
                dominantSecondaryArgb = playerState.dominantSecondaryArgb
            ) {
                PulseMusicAppHost(
                    viewModel = viewModel,
                    hasAudioPermission = hasAudioPermission,
                    hasMicPermission = hasMicPermission,
                    openNowPlayingSignal = openNowPlayingSignal,
                    onRequestAudioPermission = { requestAudioPermissions() },
                    onRequestMicPermission = {
                        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                    },
                    onOpenOverlayPermission = { openOverlayPermissionSettings() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    private fun handleIncomingIntent(intent: Intent?) {
        intent ?: return
        if (intent.getBooleanExtra(EXTRA_OPEN_NOW_PLAYING, false)) {
            openNowPlayingSignal++
        }
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                viewModel.playExternalUri(uri)
                openNowPlayingSignal++
            }
        }
    }

    private fun refreshPermissions() {
        val audioPerm = requiredAudioPermission()
        hasAudioPermission = ContextCompat.checkSelfPermission(this, audioPerm) == PackageManager.PERMISSION_GRANTED
        hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStartupPermissionsIfNeeded() {
        val needed = mutableListOf<String>()
        val audioPerm = requiredAudioPermission()
        if (ContextCompat.checkSelfPermission(this, audioPerm) != PackageManager.PERMISSION_GRANTED) {
            needed.add(audioPerm)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun requestAudioPermissions() {
        permissionLauncher.launch(arrayOf(requiredAudioPermission()))
    }

    private fun requiredAudioPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun openOverlayPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    companion object {
        const val EXTRA_OPEN_NOW_PLAYING = "extra_open_now_playing"
    }
}
