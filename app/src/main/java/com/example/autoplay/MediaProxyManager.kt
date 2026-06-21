package com.example.autoplay

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton manager that coordinates the communication between the monitored external media sessions
 * (QQ Music, etc.) and our own Android Auto media service and Car App screens.
 */
object MediaProxyManager {
    private val _activeController = MutableStateFlow<MediaController?>(null)
    val activeController = _activeController.asStateFlow()

    private val _playbackState = MutableStateFlow<PlaybackState?>(null)
    val playbackState = _playbackState.asStateFlow()

    private val _metadata = MutableStateFlow<MediaMetadata?>(null)
    val metadata = _metadata.asStateFlow()

    private val _currentLyric = MutableStateFlow<String>("")
    val currentLyric = _currentLyric.asStateFlow()

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            _playbackState.value = state
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            _metadata.value = metadata
        }
    }

    fun setActiveController(controller: MediaController?) {
        val old = _activeController.value
        if (old?.packageName == controller?.packageName) {
            // If it is the same package, make sure we have active callbacks but don't reset unnecessarily
            if (controller != null && old != controller) {
                old?.unregisterCallback(controllerCallback)
                _activeController.value = controller
                controller.registerCallback(controllerCallback)
            }
            return
        }

        old?.unregisterCallback(controllerCallback)
        _activeController.value = controller
        
        if (controller != null) {
            controller.registerCallback(controllerCallback)
            _playbackState.value = controller.playbackState
            _metadata.value = controller.metadata
            _currentLyric.value = "" // Reset lyrics for new song
        } else {
            _playbackState.value = null
            _metadata.value = null
            _currentLyric.value = ""
        }
    }

    fun updateLyric(lyric: String) {
        if (_currentLyric.value != lyric) {
            _currentLyric.value = lyric
        }
    }

    // Direct controls forwarded to the proxied app's media session
    fun play() {
        _activeController.value?.transportControls?.play()
    }

    fun pause() {
        _activeController.value?.transportControls?.pause()
    }

    fun skipToNext() {
        _activeController.value?.transportControls?.skipToNext()
    }

    fun skipToPrevious() {
        _activeController.value?.transportControls?.skipToPrevious()
    }

    fun seekTo(posMs: Long) {
        _activeController.value?.transportControls?.seekTo(posMs)
    }
}
