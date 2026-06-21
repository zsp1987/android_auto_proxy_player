package com.example.autoplay

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProxyAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen()
                }
            }
        }
    }
}

// Sleek Dark Theme Color Palette
private val DarkBlue = Color(0xFF0F172A)
private val CardBackground = Color(0xFF1E293B)
private val LightBlue = Color(0xFF38BDF8)
private val ActiveGreen = Color(0xFF10B981)
private val AlertRed = Color(0xFFEF4444)
private val AccentGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF38BDF8), Color(0xFF818CF8))
)

@Composable
fun ProxyAppTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = LightBlue,
        background = DarkBlue,
        surface = CardBackground,
        onBackground = Color.White,
        onSurface = Color.White
    )
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    var isPermissionGranted by remember { mutableStateOf(false) }

    // Periodically check if notification access has been granted
    LaunchedEffect(Unit) {
        while (true) {
            isPermissionGranted = isNotificationServiceEnabled(context)
            delay(1000)
        }
    }

    val activeController by MediaProxyManager.activeController.collectAsState()
    val metadata by MediaProxyManager.metadata.collectAsState()
    val playbackState by MediaProxyManager.playbackState.collectAsState()
    val lyric by MediaProxyManager.currentLyric.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBlue)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Auto Proxy Player",
            style = androidx.compose.ui.text.TextStyle(
                brush = AccentGradient,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Bridges phone media sessions and lyrics to Android Auto",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Status Card
        StatusCard(isPermissionGranted = isPermissionGranted, onGrantClick = {
            openNotificationSettings(context)
        })

        Spacer(modifier = Modifier.height(20.dp))

        // Now Playing / Proxy Status Card
        AnimatedVisibility(
            visible = isPermissionGranted,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column {
                if (activeController != null) {
                    ActiveProxyCard(
                        packageName = activeController?.packageName ?: "Unknown",
                        title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "No Title",
                        artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist",
                        isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING,
                        lyric = lyric
                    )
                } else {
                    IdleProxyCard()
                }
            }
        }
    }
}

@Composable
fun StatusCard(isPermissionGranted: Boolean, onGrantClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isPermissionGranted) ActiveGreen else AlertRed)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isPermissionGranted) "Service Connected" else "Action Required",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isPermissionGranted) {
                    "The listener service is active. Open any media app on your phone, and start playback to stream to Android Auto."
                } else {
                    "This app requires 'Notification Access' to capture playback state, cover art, and lyrics from other media apps."
                },
                fontSize = 14.sp,
                color = Color.LightGray,
                modifier = Modifier.fillMaxWidth()
            )
            if (!isPermissionGranted) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onGrantClick,
                    colors = ButtonDefaults.buttonColors(containerColor = LightBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Grant Notification Access",
                        color = DarkBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveProxyCard(
    packageName: String,
    title: String,
    artist: String,
    isPlaying: Boolean,
    lyric: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Currently Proxying:",
                fontSize = 12.sp,
                color = LightBlue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = packageName,
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = artist,
                fontSize = 16.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Test Controls Card
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { MediaProxyManager.skipToPrevious() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("Prev")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = {
                        if (isPlaying) MediaProxyManager.pause() else MediaProxyManager.play()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LightBlue)
                ) {
                    Text(if (isPlaying) "Pause" else "Play", color = DarkBlue)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { MediaProxyManager.skipToNext() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("Next")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lyrics Preview Box
            Text(
                text = "Extracted Live Lyrics:",
                fontSize = 12.sp,
                color = LightBlue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .padding(16.dp)
            ) {
                Text(
                    text = lyric.ifEmpty { "(No lyric line captured yet)" },
                    fontSize = 16.sp,
                    color = if (lyric.isEmpty()) Color.DarkGray else Color.White,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun IdleProxyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No Active Session",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Open QQ Music (or another music app) on your phone and start playing audio. The proxy will pick it up automatically.",
                fontSize = 14.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val cn = ComponentName(context, ProxyNotificationListenerService::class.java)
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(cn.flattenToString())
}

private fun openNotificationSettings(context: Context) {
    try {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback for systems where setting action is unavailable
        val intent = Intent(Settings.ACTION_SETTINGS)
        context.startActivity(intent)
    }
}
