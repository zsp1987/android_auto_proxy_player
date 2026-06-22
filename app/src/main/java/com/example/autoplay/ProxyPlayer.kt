package com.example.autoplay

import android.graphics.Bitmap
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.io.ByteArrayOutputStream

/**
 * A custom Media3 player implementation using SimpleBasePlayer.
 * Instead of executing playback locally, it delegates all controls and mirrors all states
 * from the active proxied media controller (e.g. QQ Music).
 */
class ProxyPlayer(looper: Looper) : SimpleBasePlayer(looper) {

    override fun getState(): State {
        val activeMetadata = MediaProxyManager.metadata.value
        val activePlaybackState = MediaProxyManager.playbackState.value

        // Map the PlaybackState
        val activeStateVal = activePlaybackState?.state ?: android.media.session.PlaybackState.STATE_NONE
        
        val isPlaying = activeStateVal == android.media.session.PlaybackState.STATE_PLAYING
        
        val playbackState = when (activeStateVal) {
            android.media.session.PlaybackState.STATE_PLAYING,
            android.media.session.PlaybackState.STATE_PAUSED,
            android.media.session.PlaybackState.STATE_FAST_FORWARDING,
            android.media.session.PlaybackState.STATE_REWINDING,
            android.media.session.PlaybackState.STATE_SKIPPING_TO_NEXT,
            android.media.session.PlaybackState.STATE_SKIPPING_TO_PREVIOUS -> Player.STATE_READY
            
            android.media.session.PlaybackState.STATE_BUFFERING,
            android.media.session.PlaybackState.STATE_CONNECTING -> Player.STATE_BUFFERING
            
            android.media.session.PlaybackState.STATE_STOPPED,
            android.media.session.PlaybackState.STATE_ERROR -> Player.STATE_ENDED
            
            else -> Player.STATE_IDLE
        }

        // Map Metadata fields
        val title = activeMetadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "No active player"
        val artist = activeMetadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "Play music on phone"
        val album = activeMetadata?.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val durationMs = activeMetadata?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val durationUs = durationMs * 1000L

        // Extract album artwork thumbnail from active media metadata
        val bitmap = activeMetadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: activeMetadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
            ?: activeMetadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON)

        val artworkBytes = if (bitmap != null) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            stream.toByteArray()
        } else {
            null
        }

        val mediaMetadataBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)

        if (artworkBytes != null) {
            mediaMetadataBuilder.setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }

        val mediaMetadata = mediaMetadataBuilder.build()

        val mediaItem = MediaItem.Builder()
            .setMediaId("proxy_now_playing")
            .setMediaMetadata(mediaMetadata)
            .build()

        // Wrap MediaItem with MediaItemData which SimpleBasePlayer uses to manage playlists
        val mediaItemData = SimpleBasePlayer.MediaItemData.Builder("proxy_now_playing")
            .setMediaItem(mediaItem)
            .setDurationUs(durationUs)
            .build()

        // Declare commands supported by our proxy player
        val commands = Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_STOP,
                Player.COMMAND_PREPARE,
                Player.COMMAND_SEEK_TO_DEFAULT_POSITION,
                Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_METADATA
            )
            .build()

        val position = activePlaybackState?.position ?: 0L

        return State.Builder()
            .setAvailableCommands(commands)
            .setPlayWhenReady(isPlaying, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(playbackState)
            .setPlaylist(listOf(mediaItemData))
            .setCurrentMediaItemIndex(0)
            .setContentPositionMs(position)
            .build()
    }

    /**
     * Public method to allow external classes (like the service) to request a state invalidation.
     */
    fun notifyStateChanged() {
        invalidateState()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady) {
            MediaProxyManager.play()
        } else {
            MediaProxyManager.pause()
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }

    override fun handleSeek(
        mediaItemIndex: Int,
        positionMs: Long,
        seekCommand: Int
    ): ListenableFuture<*> {
        // Intercept track skips inside the seek handler since seekCommand carries information
        // about next/previous button triggers on Android Auto.
        when (seekCommand) {
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> {
                MediaProxyManager.skipToNext()
            }
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> {
                MediaProxyManager.skipToPrevious()
            }
            else -> {
                MediaProxyManager.seekTo(positionMs)
            }
        }
        invalidateState()
        return Futures.immediateVoidFuture()
    }
}
