package com.example.autoplay

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class ProxyNotificationListenerService : NotificationListenerService() {

    private val tag = "ProxyNotificationListener"
    private var isConnected = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val activeSessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        Log.d(tag, "Active media sessions changed: ${controllers?.size ?: 0} sessions found")
        updateActiveController(controllers)
    }

    override fun onCreate() {
        super.onCreate()
        MediaProxyManager.initSharedPrefs(this)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        Log.d(tag, "Notification Listener connected")

        val sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = ComponentName(this, ProxyNotificationListenerService::class.java)

        try {
            // Retrieve initially active sessions
            val controllers = sessionManager.getActiveSessions(componentName)
            updateActiveController(controllers)
            
            // Set up a listener for changes in active sessions
            sessionManager.addOnActiveSessionsChangedListener(
                activeSessionsListener,
                componentName,
                mainHandler
            )
        } catch (e: SecurityException) {
            Log.e(tag, "Failed to register active sessions listener. Missing permission?", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        Log.d(tag, "Notification Listener disconnected")

        val sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        try {
            sessionManager.removeOnActiveSessionsChangedListener(activeSessionsListener)
        } catch (e: Exception) {
            Log.e(tag, "Failed to remove active sessions listener", e)
        }
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        MediaProxyManager.setAvailableControllers(controllers ?: emptyList())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        
        val activeController = MediaProxyManager.activeController.value ?: return
        
        // Only inspect notifications matching the active media app package
        if (sbn.packageName == activeController.packageName) {
            val extras = sbn.notification.extras
            
            // Extract text fields
            val title = activeController.metadata?.description?.title?.toString()
            val artist = activeController.metadata?.description?.subtitle?.toString()
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

            // Heuristic to extract lyric:
            // Check if subText or text contains a lyric line. Lyrics change rapidly and do not match 
            // the static song title or artist name.
            val lyricCandidate = when {
                !subText.isNullOrEmpty() && subText != title && subText != artist -> subText
                !text.isNullOrEmpty() && text != title && text != artist -> text
                !bigText.isNullOrEmpty() && bigText != title && bigText != artist && !bigText.contains("\n") -> bigText
                else -> null
            }

            if (!lyricCandidate.isNullOrEmpty()) {
                Log.d(tag, "Extracted lyric line from ${sbn.packageName}: $lyricCandidate")
                MediaProxyManager.updateLyric(lyricCandidate)
            }
        }
    }
}
