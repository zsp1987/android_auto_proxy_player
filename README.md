# Auto Proxy Player 🚗🎵

An Android & Android Auto proxy application designed to bridge music players (such as QQ Music, NetEase Cloud Music, Spotify, etc.) that do not natively support Android Auto but implement standard Android Media Sessions. 

It mirrors song metadata, active playback states, handles physical steering wheel controls, and dynamically extracts live scrolling lyrics from the phone's notifications to show on a custom dashboard screen in your car.

---

## 🌟 Key Features

*   **Dynamic Media App Proxy**: Automatically binds to whichever media app is currently playing audio on your phone.
*   **Custom Android Auto Interface**: Displays a dedicated screen with song details, a playback controls toggle, and live scrolling lyrics.
*   **Jetpack Media3 Integration**: Implements `MediaLibraryService` so the proxy behaves like a native music player card in the Android Auto bottom bar and system tray.
*   **Notification Lyric Extraction**: Automatically parses notification texts (like lockscreen notification sub-texts) to extract active lyric lines on the fly.
*   **Sleek Phone Dashboard**: Built with Jetpack Compose, featuring a premium dark-mode dashboard to check connection states, preview lyrics, and toggle permissions.

---

## 📂 Project Structure

```
android_auto_player/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/autoplay/
│   │   │   ├── MainActivity.kt                  # Compose Phone UI Dashboard
│   │   │   ├── MediaProxyManager.kt             # Shared State Sync Singleton
│   │   │   ├── ProxyNotificationListener.kt     # App tracking & lyric extractor
│   │   │   ├── ProxyPlayer.kt                   # Custom Media3 SimpleBasePlayer
│   │   │   ├── ProxyMediaLibraryService.kt      # Native system media connection
│   │   │   └── ProxyCarAppService.kt            # Custom Car Screen (Lyrics)
│   │   └── AndroidManifest.xml                  # Permissions and service declarations
│   └── build.gradle.kts                         # App module dependencies
├── build.gradle.kts                             # Root build configuration
├── settings.gradle.kts                          # Project modules configuration
└── local.properties                             # SDK configurations
```

---

## 🛠️ Prerequisites

*   **Android OS**: Android 8.0 (API Level 26) or higher.
*   **Development Tools**: Android Studio (Koala/Ladybug or newer recommended), JDK 17+.

---

## 🚀 Setup & Installation

### Step 1: Clone and Compile
1. Open the project folder in Android Studio.
2. Let Gradle sync and resolve dependencies.
3. Connect your phone via USB and run/install the `app` module.

### Step 2: Grant Notification Access
1. Open the **Auto Proxy Player** app on your phone.
2. Click **Grant Notification Access** (it will open Android system settings).
3. Toggle the permission switch for **Auto Proxy Player** to `Enabled`.

### Step 3: Enable "Unknown Sources" on Android Auto
*Since this is a locally built/sideloaded application, Android Auto will hide it on your car screen by default unless developer settings are enabled.*

1. On your phone, go to **Settings** -> Search for **Android Auto** -> Open Android Auto Settings.
2. Scroll to the bottom and tap **Version** **10 times** consecutively to activate Developer Mode.
3. Tap **OK** on the dialog prompt.
4. Tap the **three-dot menu** in the top-right corner, select **Developer settings**, and check **Unknown sources**.
5. Check the **Customize launcher** setting to verify **Auto Proxy Player** is checked.

---

## 🎵 How to Use

1. Open **QQ Music** (or Spotify/NetEase Cloud Music) on your phone and start playing a song.
2. Verify that the **Auto Proxy Player** dashboard on your phone displays the active metadata and live lyrics.
3. Connect your phone to your car.
4. Tap the **Auto Proxy Lyrics** icon in the Android Auto launcher to view song details and live lyrics on your car's dashboard!
