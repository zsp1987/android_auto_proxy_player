package com.example.autoplay

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ProxyMediaLibraryService : MediaLibraryService() {

    private val tag = "ProxyMediaLibraryService"
    
    private lateinit var player: ProxyPlayer
    private lateinit var session: MediaLibrarySession
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "Service onCreate")
        
        player = ProxyPlayer(mainLooper)
        session = MediaLibrarySession.Builder(this, player, LibrarySessionCallback()).build()

        // Observe changes in playback state and metadata to trigger player updates in Media3
        serviceScope.launch {
            MediaProxyManager.playbackState.collect { state ->
                Log.d(tag, "Playback state changed, invalidating player state: $state")
                player.notifyStateChanged()
            }
        }

        serviceScope.launch {
            MediaProxyManager.metadata.collect { metadata ->
                Log.d(tag, "Metadata changed, invalidating player state")
                player.notifyStateChanged()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        // Allow all controllers (including Android Auto) to connect
        return session
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Clean up service if swiped away from recent tasks
        stopSelf()
    }

    override fun onDestroy() {
        Log.d(tag, "Service onDestroy")
        session.release()
        player.release()
        serviceScope.cancel()
        super.onDestroy()
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            Log.d(tag, "onGetLibraryRoot called by ${browser.packageName}")
            
            val rootItem = MediaItem.Builder()
                .setMediaId("root_id")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setFolderType(MediaMetadata.FOLDER_TYPE_MIXED)
                        .setIsPlayable(false)
                        .setTitle("Proxy Player Root")
                        .build()
                )
                .build()
            
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            Log.d(tag, "onGetChildren called with parentId: $parentId")
            
            val children = mutableListOf<MediaItem>()
            
            if (parentId == "root_id") {
                // Return a list item pointing to the current active playing track
                val activeMetadata = MediaProxyManager.metadata.value
                val title = activeMetadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "Not Playing"
                val artist = activeMetadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "Start playback on phone"
                
                val item = MediaItem.Builder()
                    .setMediaId("now_playing_item")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setFolderType(MediaMetadata.FOLDER_TYPE_NONE)
                            .setIsPlayable(true)
                            .setTitle(title)
                            .setArtist(artist)
                            .build()
                    )
                    .build()
                
                children.add(item)
            }
            
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(children), params))
        }
    }
}
