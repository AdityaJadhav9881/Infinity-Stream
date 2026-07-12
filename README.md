# MusicFlow

Ad-free YouTube Music streaming for Android. Built with Jetpack Compose, Media3/ExoPlayer, and yt-dlp.

## Features

- **Search & Stream** — Search YouTube Music, stream any song instantly
- **Background Playback** — Full Media3 MediaSession with notification controls, Bluetooth, lockscreen
- **Offline Downloads** — Download songs to `/sdcard/Music/MusicFlow/` for offline playback
- **Smart Library** — Favorites, playlists, recently played, offline tracks
- **Auto-Download** — Automatically download liked songs
- **Lyrics** — In-player lyrics overlay
- **Queue Management** — Drag-to-reorder queue, play next, enqueue
- **Sleep Timer** — 15/30/45/60 minute auto-pause
- **Equalizer** — Bass boost, virtualizer presets (device-dependent)
- **Backup & Restore** — Export/import playlists and library
- **Multi-Language Search** — Filter search results by language
- **Dark Theme** — Spotify-inspired dark UI

## Architecture

```
MVVM + Service Layer

UI (Compose) → ViewModels → Room DB / Innertube API
                ↓
         MusicPlaybackService (Media3)
                ↓
         ExoPlayer → OkHttp → YouTube CDN
```

- **68 Kotlin source files** — 100% Kotlin
- **Hilt DI** — dependency injection throughout
- **Room DB** — 8 entities, 7 DAOs, version 5
- **DataStore** — all preferences via Jetpack DataStore
- **Coroutines + Flow** — async everywhere, no `runBlocking`

## Tech Stack

| Component | Technology |
|-----------|-----------|
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose |
| Player | Media3 ExoPlayer 1.2.1 |
| Session | Media3 Session |
| Network | Ktor + OkHttp |
| Database | Room 2.6.1 |
| DI | Hilt 2.53.1 |
| Images | Coil 2.5.0 |
| Engine | yt-dlp (youtubedl-android 0.18.1) |
| Background | WorkManager 2.9.0 |

## Screenshots

> Add screenshots here

## Requirements

- Android 8.0+ (API 26)
- Internet connection
- Storage permission for downloads

## Build

```bash
# Clone
git clone https://github.com/yourusername/MusicFlow.git
cd MusicFlow

# Build debug APK
./gradlew assembleDebug

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Permissions

| Permission | Why |
|------------|-----|
| `INTERNET` | Stream music from YouTube |
| `FOREGROUND_SERVICE` | Background playback |
| `WAKE_LOCK` | Keep CPU during playback |
| `POST_NOTIFICATIONS` | Playback notification |
| `MANAGE_EXTERNAL_STORAGE` | Save downloads to `/sdcard/Music/` |

## How It Works

1. **Search** — Queries YouTube Music via Innertube API (spoofs Android client)
2. **Extract** — yt-dlp extracts audio URL + CDN headers for the selected track
3. **Stream** — ExoPlayer streams audio via OkHttp with yt-dlp headers (bypasses 403)
4. **Cache** — 2 GB LRU disk cache for instant replay
5. **Download** — Saves `.m4a` + `.jpg` + `.meta.json` sidecar to device storage

## Project Structure

```
app/src/main/kotlin/com/musicflow/app/
├── data/           # Room entities, DAOs, API clients
├── player/         # ExoPlayer service, download manager
├── worker/         # Background tasks
├── ui/             # Compose screens, components, theme
├── utils/          # Preferences, managers, utilities
├── di/             # Hilt modules
└── MainActivity.kt
```

## Known Issues

- **Equalizer on some devices** — Android AudioEffect API may be broken on certain manufacturers (e.g., vivo). Works on Samsung, Pixel, and most other devices.
- **Media3 1.2.1** — Older version; newer versions have better AudioProcessor support.

## License

> Add your license here

## Credits

- [yt-dlp](https://github.com/yt-dlp/yt-dlp) — audio extraction engine
- [youtubedl-android](https://github.com/junkfood02/youtubedl-android) — on-device yt-dlp
- [Media3](https://developer.android.com/media/media3) — ExoPlayer successor
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — modern UI toolkit
