# PulseMusic — Native Android Music Player & Dynamic Island

**PulseMusic** is a full-featured native Android music player built with **Kotlin**, **Jetpack Compose (Material 3)**, and **AndroidX Media3 (ExoPlayer + MediaSessionService)**.

## Key Features

- **System-Wide Floating Dynamic Island (`DynamicIslandOverlayController`)**
  - Interactive pill floating around the status bar / camera cutout across apps (`SYSTEM_ALERT_WINDOW`)
  - Morphs smoothly between **Compact Capsule** (circular album art, marquee track title, live 5-bar audio spectrum) and **Expanded Card** (artwork, quality/sleep badge, seekbar, transport controls, favorite button, open-app button)
  - Supports tap to expand/collapse, horizontal swipe to skip Next/Previous, and drag-to-reposition with saved X/Y offsets
  - Visual styles: *Classic Capsule*, *Adaptive Album Glow*, and *Neon Border*
  - Also includes a persistent **In-App Dynamic Island Capsule** inside the app UI
- **Custom Adaptive App Icon**
  - Layered vector adaptive icon (`ic_launcher`, `ic_launcher_round`) with cosmic gradient background, pulse waveform + play emblem foreground, and Android 13+ themed monochrome icon
- **Media3 Audio Engine & Background Playback (`MusicPlaybackService`)**
  - `MediaSessionService` + `ExoPlayer` with lock-screen media artwork, notification controls, Bluetooth AVRCP metadata, audio focus, and automatic pause on headphone disconnect
- **Full Library Browser & Smart Playlists**
  - Tabs for **Songs**, **Playlists**, **Albums**, **Artists**, **Folders**, and **Genres**
  - Smart auto-playlists (*Favorites*, *Recently Played*, *Most Played*, *Recently Added*) + custom SQLite playlists
  - Multi-select batch actions, instant unified search, and **Built-in Demo Synth Track Generator** (for testing immediately on empty emulators)
- **Immersive Now Playing Screen**
  - Dynamic gradient extracted from album art, swipeable artwork, circular live audio spectrum ring
  - Audio quality badge (`FLAC / MP3 / AAC / WAV`, sample rate, bitrate via `MediaExtractor`)
  - **Synced `.lrc` Lyrics** (sidecar `.lrc` lookup + built-in LRC editor, tap lyric lines to seek)
  - **A-B Loop**, **Speed & Pitch (`0.5x–2.0x`)**, **Sleep Timer** (with 10-second gentle volume fade-out), and interactive **Queue Manager**
- **5-Band Studio Equalizer & Audio Effects (`AudioEffectsManager`)**
  - 5-band Equalizer (`Equalizer` API) with 11 presets + interactive spline curve & vertical faders
  - **Bass Boost**, **3D Virtualizer**, **Room Reverb**, and **Crossfade**
- **Home Screen Widget & Extras**
  - 4x2 interactive Home Screen Widget (`MusicAppWidgetProvider`)
  - Shake-to-skip accelerometer gesture, audio sharing via `FileProvider`, set-as-ringtone, and 4 Material 3 themes (*Midnight Pulse*, *AMOLED Obsidian*, *Aurora Violet*, *Ocean Cyan*)

## Opening & Building

1. Open `C:\Users\Mahan\.gemini\antigravity\scratch\PulseMusic` in **Android Studio** (Ladybug or newer recommended).
2. Let Android Studio sync Gradle (targets Android SDK 35 / minSdk 26, Java 17, Kotlin 2.0.21, AGP 8.7.3).
3. Run on a physical Android device or Android Emulator (`API 26+`).
4. To enable the system-wide floating Dynamic Island, tap **Setup Island** in the top bar (or open **Settings → Dynamic Island Player → Grant**) and allow *Display over other apps*.
