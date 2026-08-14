# MusicFlow — Technical Report

**Version:** 1.4.0  
**Package:** `com.musicflow.app`  
**Platform:** Android (minSdk 26, targetSdk 35)  
**Language:** Kotlin 100%  
**UI Framework:** Jetpack Compose (Material 3)  
**Date:** August 2026

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Project Structure](#2-project-structure)
3. [Dependency Injection (Hilt)](#3-dependency-injection-hilt)
4. [Data Layer — Room Database](#4-data-layer--room-database)
5. [Data Layer — Network / Innertube API](#5-data-layer--network--innertube-api)
6. [Player System (Media3 / ExoPlayer)](#6-player-system-media3--exoplayer)
7. [State Management — SharedMusicState](#7-state-management--sharedmusicstate)
8. [Download & Offline System](#8-download--offline-system)
9. [Backup & Restore](#9-backup--restore)
10. [Background Workers](#10-background-workers)
11. [Settings & Preferences](#11-settings--preferences)
12. [UI Layer — Screens & Navigation](#12-ui-layer--screens--navigation)
13. [UI Layer — Components](#13-ui-layer--components)
14. [Design System — Infinity Stream V2](#14-design-system--infinity-stream-v2)
15. [Theme System](#15-theme-system)
16. [Audio Engine (yt-dlp)](#16-audio-engine-yt-dlp)
17. [Permissions & Manifest](#17-permissions--manifest)
18. [Build Configuration](#18-build-configuration)
19. [Known Issues & Limitations](#19-known-issues--limitations)
20. [Bug Fixes Applied](#20-bug-fixes-applied)

---

## 1. Architecture Overview

MusicFlow follows an **MVVM (Model-View-ViewModel)** architecture with a singleton state management layer and a service layer for background playback.

```
┌──────────────────────────────────────────────────────────┐
│                     UI Layer (Compose)                   │
│  Screens ─── ViewModels ─── StateFlows ─── UI State     │
│  MainPlayerScreen ←── PlaybackStatus ←── ExoPlayer      │
├──────────────────────────────────────────────────────────┤
│                  State Layer (Singleton)                  │
│  SharedMusicState (@Singleton) — Single Source of Truth  │
│  Manages: tracks, favorites, playlists, playback state   │
├──────────────────────────────────────────────────────────┤
│                   Service Layer                          │
│  MusicPlaybackService (Media3 MediaSessionService)      │
│  QueuePersistenceManager (Room queue persistence)       │
├──────────────────────────────────────────────────────────┤
│                    Data Layer                            │
│  Room DB (v12) ─── 11 DAOs ─── 10 Entities              │
│  InnertubeClient ─── SearchRepository                   │
│  MediaExtractionRepository (yt-dlp)                     │
├──────────────────────────────────────────────────────────┤
│                  Utility Layer                           │
│  Preferences (DataStore) │ Workers │ Managers           │
│  ApkInstaller (OkHttp) │ UpdateChecker │ CrashReporter  │
├──────────────────────────────────────────────────────────┤
│                   Platform Layer                         │
│  ExoPlayer/Media3 │ yt-dlp │ OkHttp │ Coil              │
└──────────────────────────────────────────────────────────┘
```

**Key patterns:**
- **Unidirectional data flow** — UI observes `StateFlow<UiState>` from ViewModels
- **Singleton state** — `SharedMusicState` is the single source of truth for all shared state (tracks, favorites, playlists)
- **Hilt DI** — all dependencies injected via constructor injection
- **Service isolation** — playback runs in a foreground `MediaSessionService`, not in the Activity
- **No `runBlocking`** — all network/DB operations are async via coroutines
- **Reactive database** — DAO methods return `Flow<List<>>` for real-time UI updates

---

## 2. Project Structure

```
app/src/main/kotlin/com/musicflow/app/
├── MusicFlowApplication.kt          # Hilt Application, auto-restore, yt-dlp init
├── MainActivity.kt                  # Single Activity, Compose host, navigation
│
├── data/
│   ├── SharedMusicState.kt          # @Singleton single source of truth
│   ├── local/
│   │   ├── AppDatabase.kt           # Room DB (version 7)
│   │   ├── entity/                  # 9 entities
│   │   └── dao/                     # 8 DAOs
│   ├── remote/
│   │   ├── InnertubeClient.kt       # YouTube Music API spoofing (~950 lines)
│   │   ├── SearchRepository.kt      # Search + suggestions
│   │   ├── MediaExtractionRepository.kt  # Audio URL extraction via yt-dlp
│   │   ├── AudioHeaderStore.kt      # In-memory HTTP header cache
│   │   └── LyricsProvider.kt        # Lyrics fetching
│   └── local/
│       └── LocalBackupManager.kt    # DB backup/restore
│
├── player/
│   ├── MusicPlaybackService.kt      # Media3 MediaSessionService
│   ├── DownloadManager.kt           # Queue-based download system
│   ├── QueuePersistenceManager.kt   # Queue save/restore with position
│   ├── AudioEffectsController.kt    # Equalizer/volume effects
│   ├── MusicMemoryEngine.kt         # Listening pattern analysis
│   ├── PlaybackController.kt        # Playback state management
│   └── QueueController.kt           # Queue manipulation

├── audio/
│   └── AudioAnalysisEngine.kt       # Real-time audio analysis

├── artwork/
│   └── ArtworkIntelligenceEngine.kt # Artwork analysis + palette

├── domain/
│   └── (use cases)

├── worker/
│   ├── MediaStoreReconciliationWorker.kt  # Sync filesystem ↔ DB
│   └── DatabaseBackupWorker.kt            # Periodic backup
│
├── ui/
│   ├── screens/                     # 12 screens
│   │   ├── HomeScreen.kt
│   │   ├── SearchScreen.kt
│   │   ├── LibraryScreen.kt
│   │   ├── SettingsScreen.kt
│   │   ├── DownloadsScreen.kt
│   │   ├── OnboardingScreen.kt
│   │   ├── PlaylistDetailScreen.kt
│   │   ├── ArtistScreen.kt
│   │   ├── AlbumScreen.kt
│   │   ├── UpdateScreen.kt          # App update UI
│   │   ├── UnauthorizedScreen.kt    # Auth required screen
│   │   ├── SplashScreen.kt
│   │   └── (ViewModels colocated)
│   ├── components/                  # 8 reusable components
│   │   ├── MiniPlayer.kt
│   │   ├── MainPlayerScreen.kt
│   │   ├── BottomNavBar.kt
│   │   ├── QueueSheet.kt
│   │   ├── SleepTimerDialog.kt      # ModalBottomSheet
│   │   ├── AddToPlaylistDialog.kt   # ModalBottomSheet
│   │   ├── SongContextMenu.kt
│   │   ├── LyricsOverlay.kt
│   │   └── ShimmerLoading.kt
│   ├── engine/                      # Motion/animation engines
│   │   └── MotionEngine.kt
│   ├── visual/                      # Visual effects engine
│   │   └── VisualEngine.kt
│   ├── zero/                        # Zero-UI overlay system
│   ├── gesture/                     # Gesture engine
│   ├── galaxy/                      # Music Galaxy visualization
│   ├── genome/                      # Music Genome system
│   ├── timemachine/                 # Time Machine feature
│   ├── navigation/
│   │   └── Screen.kt               # Bottom nav enum + routes
│   └── theme/                       # Material 3 theme + design system
│       ├── Color.kt                 # MFColors (layered surfaces, backward-compat aliases)
│       ├── Theme.kt                 # Dark-only Material 3 scheme
│       ├── Type.kt                  # MFTypography (ExtraBold hero titles)
│       ├── DesignTokens.kt          # MFTokens (spacing, radius, elevation, animation)
│       ├── GlassEffect.kt           # MFGlass (glassmorphism composables)
│       ├── AnimatedComponents.kt    # MFAnimations (pressScale, glow, mfRipple)
│       ├── DynamicColors.kt         # MFDynamicColors (Palette extraction)
│       └── DynamicThemeEngine.kt    # Runtime artwork-based theme
│
├── utils/
│   ├── ThemePreferences.kt
│   ├── LanguagePreferences.kt
│   ├── PlayerSettingsManager.kt
│   ├── DownloadSettingsManager.kt
│   ├── SleepTimerManager.kt
│   ├── EqualizerManager.kt
│   ├── SoftwareEqualizerProcessor.kt
│   ├── NetworkMonitor.kt
│   ├── ApkInstaller.kt              # APK download + install (OkHttp-based)
│   ├── UpdateChecker.kt             # Checks for app updates
│   ├── CrashReporter.kt             # Crash reporting
│   ├── DeviceIdProvider.kt          # Unique device ID
│   ├── RemoteConfigManager.kt       # Remote config
│   └── TelemetryTracker.kt          # Usage telemetry
│
├── di/
│   ├── DatabaseModule.kt
│   ├── PlayerModule.kt
│   ├── NetworkModule.kt
│   └── DataStoreModule.kt
│
└── build.gradle.kts
```

**Total source files:** ~130 Kotlin files

---

## 3. Dependency Injection (Hilt)

### Application

`MusicFlowApplication` is annotated with `@HiltAndroidApp`. Hilt generates the dependency graph at compile time.

### Modules

| Module | Scope | Provides |
|--------|-------|----------|
| `DatabaseModule` | `SingletonComponent` | `AppDatabase`, all 8 DAOs |
| `PlayerModule` | `SingletonComponent` | `SimpleCache` (2 GB LRU) |
| `NetworkModule` | `SingletonComponent` | `HttpClient` (Ktor OkHttp) |
| `DataStoreModule` | `SingletonComponent` | `DataStore<Preferences>` |

### Injected Singletons

| Class | Injected Into |
|-------|--------------|
| `SharedMusicState` | All ViewModels that need shared state |
| `EqualizerManager` | `MusicPlaybackService`, `MainActivity` |
| `PlayerSettingsManager` | `MusicPlaybackService`, `MainActivity` |
| `DownloadSettingsManager` | `MainActivity` |
| `ThemePreferences` | `MainActivity` |
| `LanguagePreferences` | `MainActivity` |
| `LocalBackupManager` | `MainActivity`, `MusicFlowApplication` |
| `SimpleCache` | `MusicPlaybackService` |
| `AppDatabase` | All repositories |

### ViewModels (10 total)

| ViewModel | Key Dependencies |
|-----------|-----------------|
| `PlayerViewModel` | `SharedMusicState`, `SearchRepository`, `QueuePersistenceManager` |
| `HomeViewModel` | `SharedMusicState`, `SearchRepository`, `PlaylistDao` |
| `LibraryViewModel` | `SharedMusicState` |
| `SearchViewModel` | `SearchRepository` |
| `PlaylistViewModel` | `SharedMusicState` |
| `ArtistViewModel` | `SearchRepository` |
| `AlbumViewModel` | `SearchRepository` |
| `UpdateViewModel` | `ApkInstaller`, `UpdateChecker` |
| `EngineInfoViewModel` | — |

---

## 4. Data Layer — Room Database

**Database:** `AppDatabase` (Room, **version 12**, `fallbackToDestructiveMigration()`)

### Entities (10)

| Entity | Table | Key Fields |
|--------|-------|------------|
| `TrackEntity` | `tracks` | `songId` (PK), title, artist, artworkUrl, playCount, lastPlayedAt, addedAt, lastPlayedPositionMs, lastPlayedDurationMs |
| `FavoriteEntity` | `favorites` | `songId` (PK), addedAt |
| `SearchHistoryEntity` | `search_history` | `id` (auto PK), query, timestamp |
| `PlaylistEntity` | `playlists` | `id` (auto PK), name, createdAt, updatedAt |
| `PlaylistTrackMap` | `playlist_track_map` | `id` (auto PK), playlistId, songId, position |
| `LyricsEntity` | `lyrics` | `songId` (PK), lyrics, provider |
| `QueueEntity` | `queue` | `id` (auto PK), songId, position, addedAt, title, artist, artworkUrl, resolvedStreamingUrl, currentItemIndex, playbackPositionMs, isShuffleOn, repeatMode |
| `OfflineTrackEntity` | `offline_tracks` | `songId` (PK), title, artist, artworkUrl, localFilePath, fileSize, downloadedAt |
| `DownloadQueueEntity` | `download_queue` | `songId` (PK), status, progress, speed, totalBytes, downloadedBytes, retryCount |
| `MusicGraphEntity` | `music_graph` | `songId` (PK), embedding (JSON), relationships (JSON), lastAnalyzed |

### DAOs (11)

| DAO | Key Queries |
|-----|------------|
| `TrackDao` | `observeAllTracks()`, `observeRecentlyPlayed()`, `observeMostPlayed()`, `markAsPlayed()`, `savePlaybackPosition()`, `upsertTrack()` |
| `FavoriteDao` | `observeAllFavorites()`, `observeIsFavorite()`, `isFavorite()`, `toggle()` |
| `SearchHistoryDao` | `observeAll()`, `insert()`, `delete()`, `clearAll()` |
| `PlaylistDao` | `observeAllPlaylists()`, `observePlaylistTracks()`, `addTrackToPlaylistAtomic()`, `getPlaylistTrackCount()` |
| `LyricsDao` | `observeLyrics()`, `get()`, `insert()`, `delete()` |
| `QueueDao` | `getAll()`, `saveQueue()`, `clearAll()` |
| `OfflineTrackDao` | `getAll()`, `getById()`, `insert()`, `delete()` |
| `DownloadQueueDao` | `observeAll()`, `observeActive()`, `getNextQueued()`, `updateProgress()`, `markFailed()`, `markCompleted()`, `retry()`, `cancel()` |
| `MusicGraphDao` | `observeAll()`, `getById()`, `upsert()`, `delete()`, `getRecent()` |
| `ListeningEventDao` | `insert()`, `observeRecent()`, `getBySongId()` |

### Data Flow

```
User Action → ViewModel → SharedMusicState → DAO (suspend) → Room DB → StateFlow → UI
```

---

## 5. Data Layer — Network / Innertube API

### InnertubeClient (~950 lines)

Spoofs the **YouTube Music Android client** by building raw JSON payloads and sending them to YouTube's internal `youtubei/v1` API.

**Context spoofing:** All payloads include `gl: "US"` and `hl: "en"` for US English content.

**Endpoints used:**

| Endpoint | Purpose |
|----------|---------|
| `youtubei/v1/player` | Get audio stream URL for a video |
| `youtubei/v1/next` | Get "Up Next" / radio queue |
| `youtubei/v1/browse` | Artist page, album page |
| `youtubei/v1/search` | Search songs/artists/albums |
| `youtubei/v1/music/get_search_suggestions` | Autocomplete suggestions |

**Data classes:**

```kotlin
data class SearchResult(videoId: String, title: String, artist: String, thumbnailUrl: String)
data class ArtistPage(name: String, thumbnailUrl: String, description: String?, songs: List<SearchResult>, albums: List<AlbumInfo>)
data class AlbumInfo(browseId: String, title: String, thumbnailUrl: String, artist: String)
data class AlbumPage(title: String, artist: String, thumbnailUrl: String, year: String?, tracks: List<SearchResult>)
```

### SearchRepository

Wraps `InnertubeClient` for all network operations. Provides `search()`, `searchWithFilter()`, `getUpNext()`, `getArtistPage()`, `getAlbumPage()`, `getSuggestions()`.

### LyricsProvider

Fetches lyrics from YouTube Music's internal API. Returns plain-text lyrics.

---

## 6. Player System (Media3 / ExoPlayer)

### MusicPlaybackService

A `MediaSessionService` — the core of background playback.

**Lifecycle:**
1. `onCreate()` → builds ExoPlayer, MediaSession, audio sink, renderers
2. ExoPlayer configured with custom `DefaultDataSource` → OkHttp → yt-dlp headers
3. `MediaSession` handles notification, Bluetooth, lockscreen controls
4. `onIsPlayingChanged()` → initializes audio effects when AudioTrack is active
5. `onDestroy()` → releases all resources

**ExoPlayer Configuration:**

```
ExoPlayer.Builder
├── RenderersFactory (audio-only, no video)
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

- `PlayerViewModel` manages play/pause, skip, seek, shuffle, loop, queue manipulation
- Queue persisted via `QueuePersistenceManager` → Room DB with position, index, shuffle/repeat state
- Stale URLs re-extracted on app restart via parallel semaphore (3 concurrent)
- Queue restored on startup with saved playback position and track index

### Smart Playback Status Engine

`PlaybackStatus` enum provides real-time status updates to the UI:

| Status | Trigger | Visual |
|--------|---------|--------|
| `IDLE` | No track loaded | Dots hidden |
| `EXTRACTING` | Audio URL extraction starts | Fast staggered dots + "Extracting audio..." |
| `PREPARING` | ExoPlayer preparing | Medium staggered dots + "Preparing..." |
| `BUFFERING` | ExoPlayer buffering | Slow uniform pulse + "Buffering..." |
| `PLAYING` | ExoPlayer ready & playing | Gentle uniform pulse, no text |
| `PAUSED` | Playback paused | Dots dim to 0.2, "Paused" |
| `RESTORING` | Queue restoring from DB | Fast uniform pulse + "Restoring queue..." |
| `FILLING_QUEUE` | Filling queue with similar tracks | Medium staggered + "Loading similar tracks..." |
| `ERROR` | Playback error occurred | Red dots, "Playback error" |

**Status update points in PlayerViewModel:**
1. `playTrack()` → PREPARING
2. `playFromLibrary()` → EXTRACTING
3. `playNext()` → EXTRACTING
4. `enqueue()` → EXTRACTING
5. `playFromPlaylistContext()` → EXTRACTING
6. `playPlaylistQueue()` → EXTRACTING
7. `restoreQueueFromDatabase()` → RESTORING
8. `fillQueue()` → FILLING_QUEUE
9. `onPlaybackStateChanged(STATE_BUFFERING)` → BUFFERING
10. `onPlaybackStateChanged(STATE_READY)` → PLAYING
11. `onIsPlayingChanged(false)` → PAUSED
12. `onPlayerError()` → ERROR

**MainPlayerScreen indicator:**
- Three dots with independent per-dot animations (0ms, 200ms, 400ms offset)
- Each state has unique animation timing (300ms–1500ms)
- Status text shown below dots when loading/paused/error, hidden when playing
- Red dots for errors, dim dots for paused

### Sleep Timer

- `SleepTimerManager` — `CountDownTimer` wrapper
- Pauses playback when timer expires
- UI via `SleepTimerDialog` (15/30/45/60 min options)

---

## 7. State Management — SharedMusicState

`SharedMusicState` is the **single source of truth** for all shared application state. It is a `@Singleton` injected via Hilt.

### Reactive StateFlows

| Flow | Type | Description |
|------|------|-------------|
| `recentlyPlayed` | `Flow<List<TrackEntity>>` | Tracks sorted by `lastPlayedAt DESC` |
| `mostPlayed` | `Flow<List<TrackEntity>>` | Tracks sorted by `playCount DESC` |
| `allTracks` | `Flow<List<TrackEntity>>` | All tracks in library |
| `favoriteIds` | `Flow<Set<String>>` | Set of favorited songIds |
| `favoriteTracks` | `Flow<List<TrackEntity>>` | Full TrackEntity objects for favorites |
| `playlists` | `Flow<List<PlaylistEntity>>` | All playlists |

### Actions

| Method | Description |
|--------|-------------|
| `markAsPlayed(songId)` | Updates `lastPlayedAt` + increments `playCount` |
| `savePlaybackPosition(songId, posMs, durMs)` | Saves playback resume position |
| `saveTrack(track)` | Upserts a track into the library |
| `toggleFavorite(songId)` | Adds/removes from favorites |
| `isFavorite(songId)` | Suspend check for favorite status |
| `createPlaylist(name)` | Creates a new empty playlist |
| `renamePlaylist(playlistId, newName)` | Renames a playlist |
| `deletePlaylist(playlistId)` | Deletes playlist and its track mappings |
| `addTrackToPlaylist(playlistId, songId)` | Atomically adds track (prevents race condition) |
| `removeTrackFromPlaylist(playlistId, songId)` | Removes track from playlist |

### Design Principle

All screens observe `SharedMusicState` flows instead of maintaining their own copies. This eliminates state synchronization bugs (e.g., favorites not updating across screens, duplicate play count increments).

---

## 8. Download & Offline System

### DownloadManager

Queue-based download system with support for concurrent downloads.

**Download flow:**
1. Enqueue track (deduplication by songId)
2. Extract audio URL via `MediaExtractionRepository` (yt-dlp)
3. Download `.m4a` file via OkHttp
4. Download `.jpg` artwork
5. Create `.meta.json` sidecar with metadata
6. Update `DownloadQueueEntity` status
7. Process next in queue

**Queue features:**
- Max concurrent downloads: configurable
- Status tracking: QUEUED, DOWNLOADING, COMPLETED, FAILED, PAUSED, CANCELLED
- Progress tracking with speed calculation
- Auto-retry failed downloads (MAX_RETRIES = 3)
- Persists state via `DownloadQueueDao`

### MediaStoreReconciliationWorker

Background worker that runs once per app startup. Scans `/sdcard/Music/MusicFlow/` for `.m4a` files that may exist on disk but not in the database.

---

## 9. Backup & Restore

### LocalBackupManager

**Backup location:** `/sdcard/Music/MusicFlow/backups/musicflow.db`

**Backup process:**
1. WAL checkpoint (`PRAGMA wal_checkpoint`)
2. Copy database file to backup location
3. Store metadata (timestamp, track count)

**Restore process:**
1. Validate backup file exists and has reasonable size
2. Close Room DB connection (prevents stale singleton reference)
3. Overwrite database file from backup
4. Delete WAL/SHM files
5. App restarts after restore to reinitialize Room with the restored database

**Safety:**
- Restore validates DB size (> 0 bytes)
- User confirms keep/restore via dialog in `SettingsScreen`
- App restarts to ensure clean state

---

## 10. Background Workers

### MediaStoreReconciliationWorker

| Property | Value |
|----------|-------|
| Trigger | Once per app startup |
| Policy | `ExistingWorkPolicy.KEEP` |

**Purpose:** Ensures any `.m4a` files added externally are registered in the database.

### DatabaseBackupWorker

| Property | Value |
|----------|-------|
| Trigger | Periodic (WorkManager) |

**Purpose:** Automatic backup of the database.

---

## 11. Settings & Preferences

All preferences use **Jetpack DataStore**.

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

---

## 12. UI Layer — Screens & Navigation

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
| `HomeScreen` | `HomeViewModel` | Greeting, quick actions, continue listening, recently played, mixes, per-playlist carousels, favorites, trending |
| `SearchScreen` | `SearchViewModel` | Search bar, suggestions, results with filter tabs (Songs, Artists, Albums, Playlists) |
| `LibraryScreen` | `LibraryViewModel` | Library tracks with sort (Title A-Z, Artist A-Z, Recently Played, Most Played), playlists |
| `SettingsScreen` | — | Theme, language, skip silence, volume norm, equalizer, engine info, backup/restore, about |
| `DownloadsScreen` | — | Offline tracks, download settings |
| `PlaylistDetailScreen` | `PlaylistViewModel` | Playlist tracks, rename, delete, duplicate, play all |
| `ArtistScreen` | `ArtistViewModel` | Artist info, songs, albums with error retry |
| `AlbumScreen` | `AlbumViewModel` | Album info, tracks with error retry |

---

## 13. UI Layer — Components

| Component | Used In | Description |
|-----------|---------|-------------|
| `MiniPlayer` | `MainActivity` | Floating compact card (20dp radius), 44dp artwork, 2dp progress bar, 42dp play button, press feedback, equalizer bars |
| `MainPlayerScreen` | `MainActivity` | Full-screen player: dynamic accent colors throughout, symmetric transport controls, custom progress track with animated thumb, artwork-derived palette |
| `QueueSheet` | `MainActivity` | Bottom sheet showing upcoming tracks |
| `SleepTimerDialog` | `MainActivity` | Compact `ModalBottomSheet` — 3-column preset grid, custom input, drag handle, content-based height |
| `AddToPlaylistDialog` | `MainActivity` | Compact `ModalBottomSheet` — create button, scrollable playlist list, empty state, drag handle, content-based height |
| `SongContextMenu` | `MainActivity` | Long-press menu with all track actions |
| `LyricsOverlay` | `MainPlayerScreen` | Scrolling lyrics display |
| `ShimmerLoading` | Various | Skeleton loading animation |

---

## 14. Design System — Infinity Stream V2

A comprehensive design system for premium dark UI with layered surfaces, glassmorphism, dynamic colors, and micro-animations.

### Design Tokens (`MFTokens`)

| Category | Token | Value |
|----------|-------|-------|
| **Spacing** | `SpacingXXS` through `Spacing6XL` | 4.dp → 64.dp |
| **Radius** | `SmallRadius` | 8.dp |
| | `MediumRadius` | 16.dp (RoundedCornerShape) |
| | `LargeRadius` | 24.dp |
| | `FullRadius` | 100.dp |
| **Elevation** | `ElevationNone/Small/Medium/Large/XL` | 0–24.dp |
| **Animation** | `FastDuration` | 150ms |
| | `NormalDuration` | 300ms |
| | `SlowDuration` | 500ms |
| **Component** | `MiniPlayerHeight` | 64.dp |
| | `BottomNavHeight` | 64.dp |
| | `PlayButtonSize` | 64.dp |
| **Screen** | `ScreenHorizontalPadding` | 20.dp |
| | `SectionSpacing` | 28.dp |
| | `SectionHeaderTextSize` | 22.sp |

### Layered Surface Colors (`MFColors`)

| Token | Hex | Purpose |
|-------|-----|---------|
| `Background` | `#0B0B0E` | App background (near-black, OLED-safe) |
| `Surface` | `#15161A` | Primary surface |
| `Card` | `#1B1C20` | Card backgrounds |
| `Elevated` | `#22242A` | Elevated elements (search bars, chips) |
| `Overlay` | `#2A2C33` | Dialogs, modals |
| `Accent` | `#1ED760` | Emerald accent (buttons, active states) |
| `TextPrimary` | `#FFFFFF` | Primary text |
| `TextSecondary` | `#B3B3B3` | Secondary text |
| `TextTertiary` | `#727272` | Disabled/hint text |
| `TextOnAccent` | `#000000` | Text on accent backgrounds |
| `Divider` | `#2A2C33` | Subtle borders |

### Backward-Compatible Aliases

Old color tokens are preserved as extension properties:
```kotlin
val AccentGreen get() = MFColors.Accent
val DarkSurface get() = MFColors.Surface
val DarkSurfaceVariant get() = MFColors.Elevated
val OnBackground get() = MFColors.TextPrimary
val OnBackgroundVariant get() = MFColors.TextSecondary
val ErrorRed get() = MFColors.Error
```

### Glassmorphism (`MFGlass`)

Three glass composables with semi-transparent backgrounds, subtle borders, and blur effects:
- `MiniPlayerGlass` — floating mini player card
- `BottomNavGlass` — floating bottom navigation
- `DialogGlass` — alert dialogs and modals

### Micro-Animations (`MFAnimations`)

| Animation | Description |
|-----------|-------------|
| `pressScale` | Spring-based scale on press (0.95 → 1.0) |
| `glow` | Animated shadow/elevation pulse |
| `accentGlow` | Emerald glow behind accent elements |
| `dynamicGlow` | Palette-derived glow from artwork |
| `mfRipple` | Accent-tinted ripple effect |

### Dynamic Colors (`MFDynamicColors`)

Extracts dominant, vibrant, and darkMuted colors from artwork URLs using Android's Palette API. Used for:
- Full-screen player gradient background
- Album art ambient glow
- Mini player waveform accent color

---

## 15. Theme System

### ThemePreferences

Supports three modes via `ThemeMode` enum:
- `LIGHT` — always light theme
- `DARK` — always dark theme (default)
- `SYSTEM` — follows system dark mode

### Color Palette

Material 3 theme uses `MFColors` layered surfaces (see Section 14). The old flat palette is replaced:

| Old Token | New Token | Value |
|-----------|-----------|-------|
| `AccentGreen` | `MFColors.Accent` | `#1ED760` |
| `DarkSurface` | `MFColors.Surface` | `#15161A` |
| `OnBackground` | `MFColors.TextPrimary` | `#FFFFFF` |
| `OnBackgroundVariant` | `MFColors.TextSecondary` | `#B3B3B3` |
| `ErrorRed` | `MFColors.Error` | `#E53935` |

### Typography

`MFTypography` provides scaled type hierarchy:
- Hero: 32.sp, ExtraBold (w800), letterSpacing -0.5
- Section Header: 22.sp, Bold (w700), letterSpacing -0.4
- Body Large: 16.sp
- Body Medium: 14.sp
- Label: 12.sp

---

## 15. Audio Engine (yt-dlp)

### YoutubeDL Integration

Uses `youtubedl-android` library for on-device audio extraction.

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

## 16. Permissions & Manifest

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

## 17. Build Configuration

### Dependencies Summary

| Category | Library | Version |
|----------|---------|---------|
| **UI** | Compose BOM | 2024.09.00 |
| **UI** | Material 3 | (via BOM) |
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

### Build Variants

- `debug` — no minification, debuggable
- `release` — no minification (proguard configured but not enabled)

### NDK ABI Filters

`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`

---

## 18. Known Issues & Limitations

### Equalizer

- **Android AudioEffect API broken on vivo devices** — `BassBoost`, `Virtualizer`, `Equalizer`, `LoudnessEnhancer` all return error -3 (`ERROR_NO_INIT`)
- Software-based `AudioProcessor` approach causes audio corruption in Media3 1.2.1
- Equalizer works on Samsung, Pixel, and other devices with functional AudioEffect HAL

### Media3 Version

- Using Media3 1.2.1 (older). Newer versions (1.5.x+) have improved `BaseAudioProcessor` support
- `DefaultAudioSink.Builder` does not expose `setAudioSessionId()` in this version

### Database

- Uses `fallbackToDestructiveMigration()` — schema changes wipe data (development mode)
- Should use proper Room migrations for production

### Storage

- Requires `MANAGE_EXTERNAL_STORAGE` for writing to `/sdcard/Music/`
- `.nomedia` file prevents downloaded artwork from appearing in gallery

### Network

- Audio URLs expire — tracks may need re-extraction on long sessions
- yt-dlp headers (cookies, PoToken) expire and are refreshed per-session

---

## 20. Bug Fixes Applied

### Critical Bugs (Session 2 — July 13, 2026)

| Bug | Root Cause | Fix |
|-----|-----------|-----|
| Favorites not persisting | Double toggle: `toggleFavoriteAndMaybeDownload()` called both `libraryViewModel` and `playerViewModel.toggleLikeCurrentTrack()`, each toggling `SharedMusicState.toggleFavorite()` | Removed `playerViewModel.toggleLikeCurrentTrack()` |
| Like button out of sync | `PlayerViewModel.isCurrentTrackLiked` not synced with `SharedMusicState` | Added observation of `sharedMusicState.favoriteIds` |
| Play count double-incrementing | `saveTrackToLibrary` called upsert (+1) AND `sharedMusicState.markAsPlayed()` (+1) | Removed redundant `markAsPlayed()` call |
| Backup/restore crash | Room DB singleton still pointed to old deleted file after restore | Close DB before file replacement + app restart |
| Playlist track race condition | Read-then-write of track position not atomic | `@Transaction` annotated `addTrackToPlaylistAtomic()` |
| Search filters not navigating | ARTISTS/ALBUMS/PLAYLISTS filters played tracks | Added `onArtistSelected`/`onAlbumSelected` callbacks |
| Sort options non-functional | "Title A-Z" and "Artist A-Z" were stubs | Added `TITLE_ASC`/`ARTIST_ASC` filter cases |
| NPE crash on startup | `playlistTrackJobs` declared after `init` block; Kotlin initializes in declaration order | Moved `playlistTrackJobs` before `init` |

### Design System Integration (Session 3 — July 14, 2026)

| Area | Change |
|------|--------|
| Color system | Rewrote `Color.kt` with layered surfaces + backward-compat aliases |
| Theme | Dark-only, no pure black, Material 3 seeded from `MFColors` |
| Typography | ExtraBold hero titles, Bold section headers |
| MiniPlayer | Floating glass card, waveform animation, generous spacing |
| BottomNav | Floating glass, no pill, glowing active icon |
| Player transition | Fade + slide, `MFColors.Background` |
| HomeScreen | Hero greeting, chevron "See All", design system throughout |
| LibraryScreen | Filter chips, playlist cards, section headers on design tokens |
| SearchScreen | Search bar, result items, thumbnails on design tokens |
| SettingsScreen | Section headers, background, spacing on design tokens |
| MainPlayerScreen | All colors/spacing on `MFColors`/`MFTokens` |

### Bug Fixes & Improvements (Session 4 — August 1, 2026)

| Bug/Issue | Root Cause | Fix |
|-----------|-----------|-----|
| Google Drive download broken | Android `DownloadManager` can't handle Google Drive's virus-scan consent page — downloads HTML instead of APK | Replaced with direct OkHttp download with redirect following |
| API key hardcoded in source | Secret key `im_sk_...` was a `const val` in `InfinityMasterClient.kt` — trivially extractable via decompilation | Moved to `BuildConfig.API_KEY` via `build.gradle.kts` |
| Hard process kill on restart | `Runtime.getRuntime().exit(0)` in `ApkInstaller.restartApp()` causes data loss | Removed — now just relaunches via `startActivity()` |
| Dead `cancelDownload()` code | `UpdateViewModel.cancelDownload()` was never called from any UI | Removed dead method |
| Silent exception swallowing | Empty `catch (e: Exception) {}` blocks in `EqualizerManager` (6 locations) and `MainPlayerScreen` | Added `Log.w()` to all catch blocks |
| Playback state not visible to user | Three dots were a static animation with no state information | Replaced with smart `PlaybackStatus` engine showing real-time state |
| Queue position lost on restart | `QueueEntity` had no position/index/shuffle/repeat columns | Added columns, bumped DB v8→v9, added `MIGRATION_8_9` |
| Stale URLs on restart | YouTube CDN URLs expire after hours | `restoreQueueFromDatabase()` re-extracts URLs in parallel on startup |
| Corrupted song stops playback | Single bad audio URL crashed ExoPlayer with no recovery | `onPlayerError()` now auto-skips to next track |
| Battery drain from animations | `rememberInfiniteTransition` ran even when player paused | Replaced with `animateFloatAsState` that only animates when playing |
| Hardcoded year in trending | `loadTrending()` used hardcoded `"2024"` | Changed to `Calendar.getInstance().get(YEAR)` |
| Downloads filter showed all tracks | `LibraryViewModel` DOWNLOADS filter didn't check offline status | Now filters by `offlineTracks` set |

### Critical Bugs (Session 6 — August 14, 2026)

| Bug | Root Cause | Fix |
|-----|-----------|-----|
| Full player buttons unresponsive | Box wrapping `MainPlayerScreen` had `.clickable(onClick = {})` that swallowed all touch events | Removed empty clickable modifier |
| Playlist tracks not showing | `addTrackToPlaylist()` only wrote to `playlist_track_map` but not `tracks` table; JOIN query returned empty | Added `trackDao.upsertTrack()` before playlist mapping insert; updated callers to pass title/artist/artwork |
| Downloads hidden by 2GB limit | Hardcoded `DEFAULT_MAX_BYTES = 2L * 1024 * 1024 * 1024` and `2048.0` in storage calculations | Removed artificial limit, progress bar, and remaining storage display |
| Swipe gestures firing multiple times | `detectDragGestures` callback fires every frame exceeding threshold | Added `swipeFired` consumed flag, resets on drag end/cancel |
| QueueSheet crash on duplicates | LazyColumn key used `songId` but queue can have duplicates | Changed key to `"${item.songId}_$index"` |
| App update check always failed | `JsonPrimitive.toString()` wraps in quotes; `toBooleanStrictOrNull()` can't parse `"\"true\""` | Uses `jsonPrimitive.content` for raw value extraction |
| MediaSession release order wrong | `player.release()` called before `mediaSession.release()` | Reversed: session first, then player |
| Progress bar seek broken | `ProgressSection` had no `pointerInput` — display-only, no touch handling | Added `detectDragGestures` + `detectTapGestures`, single gesture overlay Box |
| DownloadManager race condition | Multiple `processQueue()` calls read `activeCount` concurrently | Added `Mutex` lock to serialize queue processing |

---

*Report generated from MusicFlow codebase analysis — 82 Kotlin source files, full coverage.*
