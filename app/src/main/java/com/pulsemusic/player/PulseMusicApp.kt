package com.pulsemusic.player

import android.app.Application
import com.pulsemusic.player.audio.AudioEffectsManager
import com.pulsemusic.player.data.MediaRepository
import com.pulsemusic.player.data.PreferencesManager
import com.pulsemusic.player.data.PulseDatabaseHelper
import com.pulsemusic.player.service.PlayerController

class PulseMusicApp : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set
    lateinit var databaseHelper: PulseDatabaseHelper
        private set
    lateinit var mediaRepository: MediaRepository
        private set
    lateinit var audioEffectsManager: AudioEffectsManager
        private set
    lateinit var playerController: PlayerController
        private set

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        databaseHelper = PulseDatabaseHelper(this)
        mediaRepository = MediaRepository(this, databaseHelper, preferencesManager)
        audioEffectsManager = AudioEffectsManager(this, preferencesManager)
        playerController = PlayerController(this, databaseHelper, preferencesManager)
        playerController.connect()
    }
}
