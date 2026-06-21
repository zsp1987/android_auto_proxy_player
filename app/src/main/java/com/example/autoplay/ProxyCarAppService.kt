package com.example.autoplay

import android.content.Intent
import android.media.MediaMetadata
import android.media.session.PlaybackState
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ProxyCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        // Allows connection to all Android Auto hosts
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return ProxyCarAppSession()
    }
}

class ProxyCarAppSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return LyricScreen(carContext)
    }
}

class LyricScreen(carContext: CarContext) : Screen(carContext) {

    init {
        // Automatically request redraw of the template when metadata or lyrics update
        lifecycleScope.launch {
            MediaProxyManager.metadata.collect {
                invalidate()
            }
        }
        lifecycleScope.launch {
            MediaProxyManager.playbackState.collect {
                invalidate()
            }
        }
        lifecycleScope.launch {
            MediaProxyManager.currentLyric.collect {
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): Template {
        val activeMetadata = MediaProxyManager.metadata.value
        val activePlaybackState = MediaProxyManager.playbackState.value

        val title = activeMetadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Not Playing"
        val artist = activeMetadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Start playback on your phone"
        val album = activeMetadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""

        val isPlaying = activePlaybackState?.state == PlaybackState.STATE_PLAYING
        val currentLyric = MediaProxyManager.currentLyric.value

        val infoRow = Row.Builder()
            .setTitle(title)
            .addText(if (album.isNotEmpty()) "$artist — $album" else artist)
            .build()

        val lyricRow = Row.Builder()
            .setTitle("Lyrics:")
            .addText(currentLyric.ifEmpty { "Waiting for lyrics notifications..." })
            .build()

        // Build playback controller actions inside the Pane (max 2 actions)
        val playPauseAction = Action.Builder()
            .setTitle(if (isPlaying) "Pause" else "Play")
            .setOnClickListener {
                if (isPlaying) {
                    MediaProxyManager.pause()
                } else {
                    MediaProxyManager.play()
                }
            }
            .build()

        val nextAction = Action.Builder()
            .setTitle("Next")
            .setOnClickListener {
                MediaProxyManager.skipToNext()
            }
            .build()

        val pane = Pane.Builder()
            .addRow(infoRow)
            .addRow(lyricRow)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .build()

        return PaneTemplate.Builder(pane)
            .setHeaderAction(Action.APP_ICON)
            .setTitle("Auto Proxy Lyrics")
            .build()
    }
}
