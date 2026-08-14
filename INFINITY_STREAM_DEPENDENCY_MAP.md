# Infinity Stream Dependency Map

## Baseline assessment — 2026-08-14

MusicFlow already has a capable Android playback foundation. Infinity Stream
should evolve that foundation; it should not replace it with a second player
or a visual-only prototype.

```text
Compose UI
    |
    +-- screen ViewModels ---- PlayerViewModel ------------------+
    |          |             Home/Library/Playlist/Search VMs     |
    |          v                                                   v
    |   domain use cases (new work begins here)          MediaController
    |          |                                                   |
    |          v                                                   v
    |   repositories / SharedMusicState                 MusicPlaybackService
    |          |                                                   |
    |          +---- Room / DataStore / network                   v
    |                                             Media3 + audio focus + session
    |                                                           |
    +---- observes state <---- Music Memory Engine <-------------+
                                   |
                                   v
                          Room listening_events timeline
```

## Current systems and their Infinity Stream role

| Target subsystem | Reuse now | Required next step |
| --- | --- | --- |
| Audio Engine | `MusicPlaybackService`, Media3 `MediaSession`, ExoPlayer | Keep this as the one playback owner; move remaining playback facts into a service-owned state stream. |
| Playback / Queue | `PlayerViewModel`, `QueuePersistenceManager`, `QueueDao` | Extract queue construction and Infinite Mix policy into domain use cases. |
| Downloads / offline | `OfflineDownloadManager`, download queue DAO, MediaStore worker | Expose one download state stream and add retry/storage policy tests. |
| Library / playlists / favorites | Room DAOs and `SharedMusicState` | Keep `SharedMusicState` as library SSOT while moving actions behind use cases. |
| Search / discovery / trending | `SearchRepository`, `InnertubeClient`, `HomeViewModel` | Separate source adapters from query interpretation; live-source and licensing review required before distribution. |
| Music Memory | **New:** `MusicMemoryEngine`, repository, use case, `listening_events` | Add timeline projections, favorites/playlist/download hooks, and retention/privacy settings. |
| Recommendation / Infinite Mix | Existing history, favorites, track metadata and Up Next source | Begin deterministic ranking only after the memory event quality has been validated. |
| Artwork Intelligence | `MFDynamicColors` and Palette | Promote palette output to a cached theme model with contrast checks. |
| Audio Analysis / visualizer | No dependable analysis source yet | Add a measured, opt-in analyzer off the audio render path; do not synthesize BPM or frequency data. |
| Motion / gestures / haptics | Compose animation components and existing screens | Centralize page and shared-element transitions before adding new gestures. |
| Updates / diagnostics | Update checker, APK installer, telemetry/crash utilities | Validate signing, checksum, and remote configuration policy independently of UI. |

## Implementation completed in this slice

The Music Memory foundation is real and offline-first:

- Room v10 adds immutable `listening_events` rows with indexed timeline queries.
- `MusicPlaybackService` sends actual Media3 lifecycle facts to
  `MusicMemoryEngine`; no screen invents history events.
- The engine records playback starts, pauses, completions, skips, and errors
  asynchronously. A persistence error is logged but never affects playback.
- Event rows capture the track ID, title, artist, artwork snapshot, timestamp,
  position, and duration when available.
- Queue preloading now only caches metadata. It cannot increase play counts or
  create a Recently Played entry before a track actually starts.

The event vocabulary reserves `FAVORITED`, `PLAYLIST_ADDED`, and `DOWNLOADED`,
but those library-action hooks are intentionally not marked complete until the
respective flows write them. Album metadata is also not currently available in
the core `TrackMetadata` model, so the timeline does not claim to store it.

## Refactoring boundaries

1. Do not introduce a second ExoPlayer, MediaSession, queue, or playback
   position clock. `MusicPlaybackService` remains the only audio owner.
2. New features follow `UI -> ViewModel -> use case -> repository -> data
   source`. Existing direct DAO use in legacy ViewModels will be migrated
   feature-by-feature, beginning with Music Memory.
3. `SharedMusicState` remains the source of truth for library state. Playback
   truth originates in Media3 and is observed through `PlayerViewModel`.
4. Visual work consumes state only. It must not issue network/database calls or
   run analysis on the UI thread.
5. Network search and streaming are enhancements, not prerequisites for the
   local library, downloads, settings, or memory timeline.

## Recommended delivery order

1. Finish Phase 1 hardening: real Room migrations for all production schema
   changes, a repository boundary around legacy direct DAO calls, and download
   retry/state verification.
2. Complete Music Memory event hooks and build a real Time Machine screen from
   `ListeningEventDao.observeTimeline`.
3. Add deterministic recommendations using only stored, explainable signals
   (completions, skips, favorites, recency, and listening time).
4. Add cached artwork theme intelligence with contrast validation.
5. Prototype audio analysis and visualizer behind a battery/performance toggle,
   benchmarking on lower-end target devices before enabling it by default.

## Release gates

- Validate every music-data and streaming source against its current terms and
  licensing before release; do not treat an implementation detail as a
  distribution right.
- Replace the current development-only destructive Room fallback before a
  public release.
- Keep API credentials out of shipped APKs; a `BuildConfig` value is still
  extractable from a client application and is not a secret-management system.
- Add accessibility, reduced-motion, offline, migration, background-playback,
  and battery regression coverage before enabling the advanced visual phases.
