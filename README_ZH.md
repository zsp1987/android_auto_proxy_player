# Auto Proxy Player 🚗🎵 (Android Auto 媒体会话与歌词代理桥梁)

这是一个 Android & Android Auto 代理应用程序，旨在为不支持 Android Auto 但实现了标准 Android 媒体会话（Media Session）的音乐播放器（如 QQ 音乐、网易云音乐、酷狗音乐等）提供完美的车载适配支持。

该应用能够同步歌曲信息、播放状态，响应方向盘物理切歌按键控制，并能动态解析手机通知栏内容以提取**实时滚动歌词**，在 Android Auto 专属车载界面上优雅地展示。

---

## 🌟 主要特性

*   **动态媒体应用代理**：自动适配手机上当前正在播放音频的任意音乐软件，无需为单个播放器做硬编码适配。
*   **自定义车载界面**：在 Android Auto 上渲染专属界面，直观地展示歌词内容、歌曲元数据以及播放控制按钮。
*   **原生 Media3 框架集成**：实现 `MediaLibraryService` 接口，使本代理应用能完美融入 Android Auto 底部控制栏和锁屏卡片控制器，像原生音乐软件一样工作。
*   **通知栏歌词动态提取**：通过分析锁屏通知文本（如 sub-text），在后台自动提取并实时刷新正在播放的歌词行。
*   **现代手机端仪表盘**：使用 Jetpack Compose 构建高颜值的深色模式界面，用于检测连接状态、预览实时歌词和快捷引导授权。

---

## 📂 项目结构

```
android_auto_player/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/autoplay/
│   │   │   ├── MainActivity.kt                  # 手机端 Compose 仪表盘界面
│   │   │   ├── MediaProxyManager.kt             # 数据共享与同步中心
│   │   │   ├── ProxyNotificationListener.kt     # 播放器监听与通知栏歌词解析服务
│   │   │   ├── ProxyPlayer.kt                   # 自定义 Media3 播放器实现
│   │   │   ├── ProxyMediaLibraryService.kt      # 原生系统媒体会话连接
│   │   │   └── ProxyCarAppService.kt            # 车载自定义展示界面（显示歌词）
│   │   └── AndroidManifest.xml                  # 权限与组件声明
│   └── build.gradle.kts                         # 模块依赖库配置
├── build.gradle.kts                             # 项目根构建规则
├── settings.gradle.kts                          # 项目模块映射
└── local.properties                             # 本地 SDK 路径配置
```

---

## 🛠️ 构建要求

*   **手机系统版本**：Android 8.0 (API Level 26) 或更高。
*   **编译环境**：Android Studio (推荐 Koala/Ladybug 或更高版本)，JDK 17+。

---

## 🚀 安装与配置指引

### 第一步：编译安装
1. 使用 Android Studio 打开该项目。
2. 等待 Gradle 同步并下载依赖库。
3. 连接手机并开启 USB 调试，运行并安装 `app` 模块。

### 第二步：授予通知读取权限
1. 打开手机上的 **Auto Proxy Player** 应用程序。
2. 点击 **Grant Notification Access** 按钮（会自动跳转到系统设置）。
3. 在应用列表中找到 **Auto Proxy Player** 并打开开关，允许其读取通知。

### 第三步：在手机端 Android Auto 开启“未知来源”
*因为这是本地编译的开发版应用（Sideloaded），Android Auto 默认会在车载列表里隐藏它，需手动开启开发者设置。*

1. 打开手机的**系统设置** -> 搜索 **Android Auto** 并进入设置界面。
2. 滑动到最底部，连续快速点击 **版本 (Version)** 区域 **10次** 开启开发者模式。
3. 在弹出的确认框中选择 **确定**。
4. 点击界面右上角的**三点菜单**，选择 **开发者设置 (Developer settings)**。
5. 勾选 **未知来源 (Unknown sources)** 复选框。
6. 进入 **自定义启动器 (Customize launcher)** 确保 **Auto Proxy Player** 已勾选启用。

---

## 🎵 使用说明

1. 在手机上打开 **QQ 音乐** 或其它音乐 App 开始播放一首歌曲。
2. 打开手机端的 **Auto Proxy Player** 仪表盘，确认歌曲信息和歌词是否成功刷新。
3. 将手机连接到汽车。
4. 在车机 Android Auto 应用列表里，点击 **Auto Proxy Lyrics** 即可在车载屏幕上同步显示歌词与切歌！
