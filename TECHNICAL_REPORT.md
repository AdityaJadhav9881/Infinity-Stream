# MusicFlow — Full Technical Report

**Version:** 1.0.0  
**Package:** `com.musicflow.app`  
**Platform:** Android (minSdk 26, targetSdk 35)  
**Language:** Kotlin 100%  
**UI Framework:** Jetpack Compose (Material 3)  
**Date:** July 2025

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Project Structure](#2-project-structure)
3. [Dependency Injection (Hilt)](#3-dependency-injection-hilt)
4. [Data Layer — Room Database](#4-data-layer--room-database)
5. [Data Layer — Network / Innertube API](#5-data-layer--network--innertube-api)
6. [Player System (Media3 / ExoPlayer)](#6-player-system-media3--exoplayer)
7. [Download & Offline System](#7-download--offline-system)
8. [Backup & Restore](#8-backup--restore)
9. [Background Workers](#9-background-workers)
10. [Settings & Preferences](#10-settings--preferences)
11. [UI Layer — Screens & Navigation](#11-ui-layer--screens--navigation)
12. [UI Layer — Components](#12-ui-layer--components)
13. [Theme System](#13-theme-system)
14. [Audio Engine (yt-dlp)](#14-audio-engine-yt-dlp)
15. [Permissions & Manifest](#15-permissions--manifest)
16. [Build Configuration](#16-build-configuration)
17. [Known Issues & Limitations](#17-known-issues--limitations)

---

## 1. Architecture Overview

MusicFlow follows an **MVVM (Model-View-ViewModel)** architecture with a service layer for background playback.

```
┌──────────────────────────────────────────────────────────┐
│                     UI Layer (Compose)                   │
│  Screens ─── ViewModels ─── StateFlows ─── UI State     │
├──────────────────────────────────────────────────────────┤
│                   Service Layer                         │
│  MusicPlaybackService (Media3 MediaSessionService)      │
│  OfflineDownloadManager                                 │
│  QueuePersistenceManager                                │
├──────────────────────────────────────────────────────────┤
│                    Data Layer                           │
│  Room DB ─── DAOs ─── Entities                         │
│  InnertubeClient ─── SearchRepository                   │
│  MediaExtractionRepository                              │
├──────────────────────────────────────────────────────────┤
│                  Utility Layer                          │
│  Preferences (DataStore) │ Workers │ Managers           │
├──────────────────────────────────────────────────────────┤
│                   Platform Layer                        │
│  ExoPlayer/Media3 │ yt-dlp │ OkHttp │ Coil              │
└──────────────────────────────────────────────────────────┘
```

**Key patterns:**
- **Unidirectional data flow** — UI observes `StateFlow<UiState>` from ViewModels
- **Hilt DI** — all dependencies injected via constructor injection
- **Service isolation** — playback runs in a foreground `MediaSessionService`, not in the Activity
- **No `runBlocking`** — all network/DB operations are async via coroutines

---

## 2. Project Structure

```
app/src/main/kotlin/com/musicflow/app/
├── MusicFlowApplication.kt          # Hilt Application, auto-restore, yt-dlp init
├── MainActivity.kt                  # Single Activity, Compose host, navigation
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt           # Room DB (version 5)
│   │   ├── entity/                  # 8 entities
│   │   └── dao/                     # 7 DAOs
│   ├── remote/
│   │   ├── InnertubeClient.kt       # YouTube Music API spoofing (~950 lines)
│   │   ├── SearchRepository.kt      # Search + suggestions
│   │   ├── MediaExtractionRepository.kt  # Audio URL extraction via yt-dlp
│   │   ├── AudioHeaderStore.kt      # In-memory HTTP header cache
│   │   ├── LyricsProvider.kt        # Lyrics fetching
│   │   └── TrackMetadata.kt         # Metadata sidecar model
│   └── local/
│       └── LocalBackupManager.kt    # DB backup/restore
│
├── player/
│   ├── MusicPlaybackService.kt      # Media3 MediaSessionService
│   ├── OfflineDownloadManager.kt    # Track download + file management
│   ├── QueuePersistenceManager.kt   # Queue save/restore
│   └── AudioCacheManager.kt         # ExoPlayer disk cache
│
├── worker/
│   ├── MediaStoreReconciliationWorker.kt  # Sync filesystem ↔ DB
│   └── DatabaseBackupWorker.kt            # Periodic backup
│
├── ui/
│   ├── screens/                     # 9 screens
│   │   ├── HomeScreen.kt
│   │   ├── SearchScreen.kt
│   │   ├── LibraryScreen.kt
│   │   ├── SettingsScreen.kt
│   │   ├── DownloadsScreen.kt
│   │   ├── OnboardingScreen.kt
│   │   ├── PlaylistDetailScreen.kt
│   │   ├── ArtistScreen.kt
│   │   └── AlbumScreen.kt
│   ├── components/                  # 8 reusable components
│   │   ├── MiniPlayer.kt
│   │   ├── MainPlayerScreen.kt
│   │   ├── QueueSheet.kt
│   │   ├── SleepTimerDialog.kt
│   │   ├── AddToPlaylistDialog.kt
│   │   ├── SongContextMenu.kt
│   │   ├── LyricsOverlay.kt
│   │   └── ShimmerLoading.kt
│   ├── navigation/
│   │   └── Screen.kt               # Bottom nav enum + routes
│   └── theme/                       # Material 3 theme
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
│
├── utils/
│   ├── ThemePreferences.kt          # DataStore: theme mode
│   ├── LanguagePreferences.kt       # DataStore: search languages
│   ├── PlayerSettingsManager.kt     # DataStore: skip silence, volume norm, EQ
│   ├── DownloadSettingsManager.kt   # DataStore: download quality, wifi-only
│   ├── SleepTimerManager.kt         # CountDownTimer wrapper
│   ├── EqualizerManager.kt          # Hardware AudioEffects
│   ├── SoftwareEqualizerProcessor.kt # Software DSP (experimental)
│   └── NetworkMonitor.kt            # Connectivity observer
│
├── di/
│   ├── DatabaseModule.kt            # Room DB + DAO providers
│   └── PlayerModule.kt              # ExoPlayer SimpleCache provider
│
└── build.gradle.kts                 # Dependencies & build config
```

**Total source files:** 68 Kotlin files

---

## 3. Dependency Injection (Hilt)

### Application

`MusicFlowApplication` is annotated with `@HiltAndroidApp`. Hilt generates the dependency graph at compile time.

### Modules

| Module | Scope | Provides |
|--------|-------|----------|
| `DatabaseModule` | `SingletonComponent` | `AppDatabase`, all 7 DAOs |
| `PlayerModule` | `SingletonComponent` | `SimpleCache` (2 GB LRU) |

### Injected Singletons

| Class | Injected Into |
|-------|--------------|
| `EqualizerManager` | `MusicPlaybackService`, `MainActivity` |
| `PlayerSettingsManager` | `MusicPlaybackService`, `MainActivity` |
| `DownloadSettingsManager` | `MainActivity` |
| `ThemePreferences` | `MainActivity` |
| `LanguagePreferences` | `MainActivity` |
| `LocalBackupManager` | `MainActivity`, `MusicFlowApplication` |
| `SimpleCache` | `MusicPlaybackService` |
| `AppDatabase` | All repositories |
| `InnertubeClient` | `SearchRepository`, `MediaExtractionRepository` |

### ViewModels (7 total)

All ViewModels use `@HiltViewModel` and are created via `hiltViewModel()` in Compose:

- `PlayerViewModel` — playback state, queue, like, download
- `SearchViewModel` — search query, results, suggestions, history
- `LibraryViewModel` — library items, offline tracks, playlists
- `PlaylistViewModel` — playlist CRUD, add/remove tracks
- `EngineInfoViewModel` — yt-dlp version, update status
- `ArtistViewModel` — artist page loading
- `AlbumViewModel` — album page loading

---

## 4. Data Layer — Room Database

**Database:** `AppDatabase` (Room, version 5, `fallbackToDestructiveMigration()`)

### Entities (8)

| Entity | Table | Key Fields |
|--------|-------|------------|
| `TrackEntity` | `tracks` | `songId` (PK), title, artist, artworkUrl, isFavorite, playCount, lastPlayedAt |
| `FavoriteEntity` | `favorites` | `songId` (PK), addedAt |
| `SearchHistoryEntity` | `search_history` | `id` (auto PK), query, timestamp |
| `PlaylistEntity` | `playlists` | `id` (auto PK), name, createdAt |
| `PlaylistTrackMap` | `playlist_tracks` | `id` (auto PK), playlistId, songId, addedAt |
| `LyricsEntity` | `lyrics` | `songId` (PK), lyrics, provider |
| `QueueEntity` | `queue` | `id` (auto PK), songId, position, addedAt |
| `OfflineTrackEntity` | `offline_tracks` | `songId` (PK), title, artist, artworkUrl, localFilePath, fileSize, downloadedAt |

### DAOs (7)

| DAO | Key Queries |
|-----|------------|
| `TrackDao` | `getAll()`, `getById()`, `upsert()`, `deleteById()`, `incrementPlayCount()`, `clearAll()` |
| `FavoriteDao` | `getAll()`, `isFavorite()`, `toggle()`, `deleteAll()` |
| `SearchHistoryDao` | `getAll()`, `insert()`, `delete()`, `clearAll()` |
| `PlaylistDao` | `getAll()`, `create()`, `delete()`, `addTrack()`, `removeTrack()`, `getTracks()` |
| `LyricsDao` | `get()`, `insert()`, `clearAll()` |
| `QueueDao` | `getAll()`, `saveQueue()`, `clearAll()` |
| `OfflineTrackDao` | `getAll()`, `getById()`, `getByFilePath()`, `insert()`, `delete()`, `clearAll()` |

### Data Flow

```
User Action → ViewModel → DAO (suspend) → Room DB → StateFlow → UI
```

All DAO methods are `suspend` functions or return `Flow<List<>>` for reactive updates.

---

## 5. Data Layer — Network / Innertube API

### InnertubeClient (~950 lines)

The core network component. Spoofs the **YouTube Music Android client** by building raw JSON payloads and sending them to YouTube's internal `youtubei/v1` API.

**Endpoints used:**

| Endpoint | Purpose |
|----------|---------|
| `youtubei/v1/player` | Get audio stream URL for a video |
| `youtubei/v1/next` | Get "Up Next" / radio queue |
| `youtubei/v1/browse` | Artist page, album page |
| `music/search` | Search songs/artists/albums |

**Data classes:**

```kotlin
data class SearchResult(videoId: String, title: String, artist: String, thumbnailUrl: String)
data class ArtistPage(name: String, thumbnailUrl: String, albums: List<AlbumInfo>, songs: List<SearchResult>)
data class AlbumInfo(browseId: String, title: String, thumbnailUrl: String)
data class AlbumPage(title: String, artist: String, thumbnailUrl: String, year: String, songs: List<SearchResult>)
```

### AudioHeaderStore

In-memory cache for HTTP headers (cookies, PoToken, etc.) extracted by yt-dlp. Keys are video IDs, values are `Map<String, String>` header maps. ExoPlayer reads from this store when creating data sources.

### SearchRepository

Wraps `InnertubeClient` for search operations. Manages search history via `SearchHistoryDao`.

### MediaExtractionRepository

Uses yt-dlp (via `youtubedl-android` library) to extract audio URLs. Returns `AudioExtractionResult` containing the streaming URL and HTTP headers.

### LyricsProvider

Fetches lyrics from a third-party API. Returns plain-text lyrics.

### TrackMetadata

Sidecar model for offline tracks:
```kotlin
data class TrackMetadata(
    val songId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val resolvedStreamingUrl: String?,
)
```

---

## 6. Player System (Media3 / ExoPlayer)

### MusicPlaybackService

A `MediaSessionService` — the core of background playback.

**Lifecycle:**
1. `onCreate()` → builds ExoPlayer, MediaSession, audio sink, renderers
2. ExoPlayer is configured with custom `DefaultDataSource` → OkHttp → yt-dlp headers
3. `MediaSession` handles notification, Bluetooth, lockscreen controls
4. `onIsPlayingChanged()` → initializes audio effects when AudioTrack is active
5. `onDestroy()` → releases all resources

**ExoPlayer Configuration:**

```
ExoPlayer.Builder
├── RenderersFactory (audio-only, no video)
│   └── MediaCodecAudioRenderer → DefaultAudioSink
│       └── DefaultAudioProcessorChain(SilenceSkippingAudioProcessor)
├── DefaultMediaSourceFactory
│   └── CacheDataSource → SimpleCache (2 GB disk)
│       └── OkHttpDataSource (with yt-dlp headers)
├── AudioAttributes: USAGE_MEDIA + CONTENT_TYPE_MUSIC
├── handleAudioFocus = true
├── handleAudioBecomingNoisy = true
├── wakeMode = WAKE_MODE_NETWORK
└── seek increments: ±10 seconds
```

**Audio Pipeline:**

```
yt-dlp extracted URL + headers
    ↓
OkHttpDataSource (HTTP streaming)
    ↓
CacheDataSource (2 GB LRU disk cache)
    ↓
MatroskaExtractor / FragmentedMp4Extractor
    ↓
MediaCodecAudioRenderer (hardware decode)
    ↓
DefaultAudioSink
    └── SilenceSkippingAudioProcessor
    ↓
AudioTrack (system audio output)
```

### Queue Management

- **QueuePersistenceManager** — saves/restores queue to Room DB (`QueueEntity`)
- **PlayerViewModel** — manages play/pause, skip, seek, shuffle, loop, queue manipulation
- Queue is a `MutableList<SearchResult>` with current index tracking

### Sleep Timer

- **SleepTimerManager** — `CountDownTimer` wrapper
- Pauses playback when timer expires
- UI via `SleepTimerDialog` (15/30/45/60 min options)

### Player Event Listener

Handles:
- `onPlaybackStateChanged` — IDLE/BUFFERING/READY/ENDED logging
- `onPlayerError` — error logging
- `onMediaItemTransition` — updates `currentSongId` for header injection
- `onIsPlayingChanged` — initializes equalizer on first play

---

## 7. Download & Offline System

### OfflineDownloadManager

Downloads tracks to `/sdcard/Music/MusicFlow/`.

**Download flow:**
1. Extract audio URL via `MediaExtractionRepository` (yt-dlp)
2. Download `.m4a` file via OkHttp
3. Download `.jpg` artwork
4. Create `.meta.json` sidecar with metadata
5. Insert into `OfflineTrackEntity` in Room DB
6. Create `.nomedia` file to prevent gallery indexing

**File structure per track:**
```
/sdcard/Music/MusicFlow/
├── {videoId}.m4a          # Audio file
├── {videoId}.jpg          # Artwork
├── {videoId}.meta.json    # Metadata sidecar
└── .nomedia               # Prevents gallery display
```

**Features:**
- Wifi-only mode (configurable)
- Duplicate detection (by songId and filePath)
- Bulk delete all downloads
- Cache size calculation

### MediaStoreReconciliationWorker

Background worker that runs once per app startup. Scans `/sdcard/Music/MusicFlow/` for `.m4a` files that may exist on disk but not in the database.

**Process:**
1. List all `.m4a` files in MusicFlow directory
2. For each file, check if `songId` exists in `OfflineTrackDao`
3. If not, read `.meta.json` sidecar for metadata
4. Upsert into `TrackEntity` and `OfflineTrackEntity`

### Download Settings

| Setting | Default | Description |
|---------|---------|-------------|
| `downloadQuality` | "High" | Audio quality selection |
| `wifiOnly` | true | Only download on WiFi |
| `autoDownloadLiked` | false | Auto-download favorited tracks |
| `smartDownloads` | false | Smart download suggestions |

---

## 8. Backup & Restore

### LocalBackupManager

**Backup location:** `/sdcard/Music/MusicFlow/backups/musicflow.db`

**Backup process:**
1. WAL checkpoint (`PRAGMA wal_checkpoint`)
2. Copy database file to backup location
3. Store metadata (timestamp, track count)

**Restore process:**
1. Validate backup file exists and has reasonable size
2. Stop Room DB (trigger `fallbackToDestructiveMigration`)
3. Overwrite database file from backup
4. Delete WAL/SHM files
5. Set `pending_restore_dialog` flag in SharedPreferences
6. On next launch, `MusicFlowApplication.onCreate()` restores BEFORE Room initializes

**Safety:**
- Restore validates DB size (> 0 bytes)
- Restore checks track count (> 0 tracks)
- User confirms keep/restore via dialog in `MainActivity`

### DatabaseBackupWorker

Periodic WorkManager worker for automatic backups. Runs in the background.

---

## 9. Background Workers

### MediaStoreReconciliationWorker

| Property | Value |
|----------|-------|
| Trigger | Once per app startup |
| Policy | `ExistingWorkPolicy.KEEP` |
| Constraints | None (runs immediately) |
| Output | None |

**Purpose:** Ensures any `.m4a` files added to the MusicFlow directory externally (e.g., file manager) are registered in the database.

### DatabaseBackupWorker

| Property | Value |
|----------|-------|
| Trigger | Periodic (WorkManager) |
| Constraints | None |

**Purpose:** Automatic backup of the database.

---

## 10. Settings & Preferences

All preferences use **Jetpack DataStore** (not SharedPreferences).

### ThemePreferences

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `theme_mode` | Enum | `DARK` | Light/Dark/System |

### LanguagePreferences

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `search_languages` | List | `[ENGLISH]` | Search filter languages |
| `is_onboarding_done` | Boolean | `false` | First-run completed |

### PlayerSettingsManager

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `skip_silence` | Boolean | `false` | Skip silent portions |
| `volume_normalization` | Boolean | `false` | Loudness normalization |
| `equalizer_preset` | String | `"NORMAL"` | EQ preset name |

### DownloadSettingsManager

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `download_quality` | String | `"High"` | Audio quality |
| `wifi_only` | Boolean | `true` | WiFi-only downloads |
| `auto_download_liked` | Boolean | `false` | Auto-download favorites |
| `smart_downloads` | Boolean | `false` | Smart suggestions |

---

## 11. UI Layer — Screens & Navigation

### Navigation

Bottom navigation with 4 tabs + deep-link screens:

```
Bottom Nav:
├── Home        (Screen.Home)
├── Search      (Screen.Search)
├── Library     (Screen.Library)
└── Settings    (Screen.Settings)

Deep Links:
├── Downloads         (/downloads)
├── Playlist Detail   (/playlist_detail/{id}/{name})
├── Artist            (/artist/{browseId})
└── Album             (/album/{browseId})
```

### Screen Details

| Screen | ViewModel | Description |
|--------|-----------|-------------|
| `HomeScreen` | `LibraryViewModel` | Recently played, playlists, quick actions (Mood Mix, Daily Mix), notifications |
| `SearchScreen` | `SearchViewModel` | Search bar, suggestions, results grid, search history, filter tabs |
| `LibraryScreen` | `LibraryViewModel` | Favorites/All tabs, offline tracks, playlist management |
| `SettingsScreen` | — | Theme, language, skip silence, volume norm, equalizer, engine info, backup/restore, about |
| `DownloadsScreen` | — | Offline tracks list, download settings, storage management (cache/delete) |
| `OnboardingScreen` | — | Language selection on first launch |
| `PlaylistDetailScreen` | `PlaylistViewModel` | Playlist tracks, add/remove, play all |
| `ArtistScreen` | `ArtistViewModel` | Artist info, albums, top songs |
| `AlbumScreen` | `AlbumViewModel` | Album info, track list |

---

## 12. UI Layer — Components

| Component | Used In | Description |
|-----------|---------|-------------|
| `MiniPlayer` | `MainActivity` | Persistent bottom bar showing current track, play/pause, progress |
| `MainPlayerScreen` | `MainActivity` | Full-screen player with seek, like, shuffle, loop, lyrics, sleep timer, queue |
| `QueueSheet` | `MainActivity` | Bottom sheet showing upcoming tracks, reorderable |
| `SleepTimerDialog` | `MainActivity` | Timer duration picker |
| `AddToPlaylistDialog` | `MainActivity` | Playlist selection + create new |
| `SongContextMenu` | `MainActivity` | Long-press menu: share, add to playlist, play next, enqueue, go to artist/album, download |
| `LyricsOverlay` | `MainPlayerScreen` | Scrolling lyrics display |
| `ShimmerLoading` | Various | Skeleton loading animation |

---

## 13. Theme System

### ThemePreferences

Supports three modes via `ThemeMode` enum:
- `LIGHT` — always light theme
- `DARK` — always dark theme (default)
- `SYSTEM` — follows system dark mode

### Color Palette

| Token | Light | Dark |
|-------|-------|------|
| `AccentGreen` | `#1DB954` | `#1DB954` |
| `DarkSurface` | `#121212` | `#121212` |
| `OnBackground` | `#FFFFFF` | `#FFFFFF` |
| `OnBackgroundVariant` | `#B3B3B3` | `#B3B3B3` |
| `ErrorRed` | `#E53935` | `#E53935` |

Material 3 dynamic color is NOT used — fixed Spotify-inspired dark theme.

---

## 14. Audio Engine (yt-dlp)

### YoutubeDL Integration

Uses `youtubedl-android` library (v0.18.1) for on-device audio extraction.

**Initialization:**
- Runs in `MusicFlowApplication.onCreate()`
- Checks for nightly updates on each launch
- Falls back to stable channel if nightly fails

**Audio Extraction Flow:**
1. User selects a track
2. `MediaExtractionRepository` calls yt-dlp to extract audio URL
3. yt-dlp returns streaming URL + HTTP headers (cookies, PoToken)
4. Headers are cached in `AudioHeaderStore`
5. ExoPlayer uses headers when creating the HTTP data source

### Why Headers Matter

YouTube's CDN requires specific cookies and PoToken headers. Without them, requests return 403 errors. yt-dlp extracts these dynamically, and they are injected per-song into ExoPlayer's OkHttpDataSource.

---

## 15. Permissions & Manifest

### Permissions

| Permission | Purpose |
|------------|---------|
| `INTERNET` | Network access for streaming |
| `ACCESS_NETWORK_STATE` | Check connectivity |
| `FOREGROUND_SERVICE` | Background playback |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Android 14+ foreground service type |
| `WAKE_LOCK` | Keep CPU alive during playback |
| `POST_NOTIFICATIONS` | Playback notification |
| `READ_MEDIA_AUDIO` | Read downloaded tracks |
| `MANAGE_EXTERNAL_STORAGE` | Write to `/sdcard/Music/` |
| `WRITE_EXTERNAL_STORAGE` | Legacy storage (maxSdk 28) |

### Components

| Component | Type | Notes |
|-----------|------|-------|
| `MainActivity` | Activity | Single Activity, launcher |
| `MusicPlaybackService` | Service | `MediaSessionService`, foreground media playback |
| `InitializationProvider` | Provider | Disables default WorkManager init |
| `MusicFlowApplication` | Application | Hilt entry point, auto-restore |

---

## 16. Build Configuration

### Dependencies Summary

| Category | Library | Version |
|----------|---------|---------|
| **UI** | Compose BOM | 2024.09.00 |
| **UI** | Material 3 | (via BOM) |
| **UI** | Material Icons Extended | (via BOM) |
| **Navigation** | Navigation Compose | 2.7.7 |
| **Lifecycle** | ViewModel Compose | 2.7.0 |
| **Image** | Coil Compose | 2.5.0 |
| **Media** | Media3 ExoPlayer | 1.2.1 |
| **Media** | Media3 Session | 1.2.1 |
| **Media** | Media3 DataSource OkHttp | 1.2.1 |
| **Network** | Ktor Client OkHttp | 2.3.12 |
| **Serialization** | Kotlinx Serialization JSON | 1.6.3 |
| **Coroutines** | Kotlinx Coroutines Android | 1.7.3 |
| **DI** | Hilt Android | 2.53.1 |
| **DI** | Hilt Navigation Compose | 1.1.0 |
| **DB** | Room Runtime | 2.6.1 |
| **Prefs** | DataStore Preferences | 1.1.1 |
| **Background** | WorkManager | 2.9.0 |
| **Engine** | youtubedl-android | 0.18.1 |
| **Engine** | youtubedl-android FFmpeg | 0.18.1 |

### Build Variants

- `debug` — no minification, debuggable
- `release` — no minification (proguard configured but not enabled)

### NDK ABI Filters

`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`

---

## 17. Known Issues & Limitations

### Equalizer

- **Android AudioEffect API broken on vivo devices** — `BassBoost`, `Virtualizer`, `Equalizer`, `LoudnessEnhancer` all return error -3 (`ERROR_NO_INIT`)
- Software-based `AudioProcessor` approach causes audio corruption in Media3 1.2.1
- Equalizer works on Samsung, Pixel, and other devices with functional AudioEffect HAL
- The equalizer UI and code remain in the app — it functions on supported devices

### Media3 Version

- Using Media3 1.2.1 (older). Newer versions (1.5.x+) have improved `BaseAudioProcessor` support
- `DefaultAudioSink.Builder` does not expose `setAudioSessionId()` in this version
- `ExoPlayer.Builder` does not support `setAudioSessionId()` in this version

### Database

- Uses `fallbackToDestructiveMigration()` — schema changes wipe data (development mode)
- Should use proper Room migrations for production

### Storage

- Requires `MANAGE_EXTERNAL_STORAGE` for writing to `/sdcard/Music/`
- `.nomedia` file prevents downloaded artwork from appearing in gallery
- Downloaded tracks are NOT indexed by MediaStore (intentional)

### Network

- Audio URLs expire — tracks may need re-extraction on long sessions
- yt-dlp headers (cookies, PoToken) expire and are refreshed per-session

---

*Report generated from MusicFlow codebase analysis — 68 Kotlin source files, full coverage.*
