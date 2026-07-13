package com.musicflow.app.data

import android.util.Log
import com.musicflow.app.data.local.dao.FavoriteDao
import com.musicflow.app.data.local.dao.PlaylistDao
import com.musicflow.app.data.local.dao.TrackDao
import com.musicflow.app.data.local.entity.FavoriteEntity
import com.musicflow.app.data.local.entity.PlaylistEntity
import com.musicflow.app.data.local.entity.TrackEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central single-source-of-truth for shared music state.
 *
 * All screens observe this instead of maintaining their own copies.
 * Changes made through this manager propagate to ALL screens immediately.
 *
 * ## Architecture
 * ```
 * SharedMusicState (singleton)
 *   ├── TrackDao        → recently played, most played, all tracks
 *   ├── FavoriteDao     → favorites
 *   └── PlaylistDao     → playlists
 * ```
 *
 * ## Usage
 * - Favorites: call `toggleFavorite(songId)` from any screen
 * - Recently Played: call `markAsPlayed(songId)` from PlayerViewModel
 * - All screens observe `recentlyPlayed`, `favorites`, `mostPlayed`, `playlists`
 */
@Singleton
class SharedMusicState @Inject constructor(
    private val trackDao: TrackDao,
    private val favoriteDao: FavoriteDao,
    private val playlistDao: PlaylistDao,
) {
    companion object {
        private const val TAG = "SharedMusicState"
        private const val MAX_RECENTLY_PLAYED = 50
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ── Exposed State ──────────────────────────────────────────────

    private val _recentlyPlayed = MutableStateFlow<List<TrackEntity>>(emptyList())
    val recentlyPlayed: StateFlow<List<TrackEntity>> = _recentlyPlayed.asStateFlow()

    private val _mostPlayed = MutableStateFlow<List<TrackEntity>>(emptyList())
    val mostPlayed: StateFlow<List<TrackEntity>> = _mostPlayed.asStateFlow()

    private val _allTracks = MutableStateFlow<List<TrackEntity>>(emptyList())
    val allTracks: StateFlow<List<TrackEntity>> = _allTracks.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _favoriteTracks = MutableStateFlow<List<TrackEntity>>(emptyList())
    val favoriteTracks: StateFlow<List<TrackEntity>> = _favoriteTracks.asStateFlow()

    private val _playlists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    val playlists: StateFlow<List<PlaylistEntity>> = _playlists.asStateFlow()

    init {
        observeAll()
    }

    private fun observeAll() {
        // Observe recently played
        trackDao.observeRecentlyPlayed()
            .distinctUntilChanged()
            .onEach { tracks ->
                _recentlyPlayed.value = tracks.take(MAX_RECENTLY_PLAYED)
            }
            .launchIn(scope)

        // Observe most played
        trackDao.observeMostPlayed()
            .distinctUntilChanged()
            .onEach { tracks ->
                _mostPlayed.value = tracks.take(20)
            }
            .launchIn(scope)

        // Observe all tracks
        trackDao.observeAllTracks()
            .distinctUntilChanged()
            .onEach { tracks ->
                _allTracks.value = tracks
            }
            .launchIn(scope)

        // Observe favorites - combine with track data to get full track info
        combine(
            favoriteDao.observeAllFavorites(),
            trackDao.observeAllTracks(),
        ) { favorites, tracks ->
            val favIds = favorites.map { it.songId }.toSet()
            val favTracks = tracks.filter { it.songId in favIds }
            Pair(favIds, favTracks)
        }
            .distinctUntilChanged()
            .onEach { (ids, tracks) ->
                _favoriteIds.value = ids
                _favoriteTracks.value = tracks
            }
            .launchIn(scope)

        // Observe playlists
        playlistDao.observeAllPlaylists()
            .distinctUntilChanged()
            .onEach { playlists ->
                _playlists.value = playlists
            }
            .launchIn(scope)
    }

    // ── Public Actions ─────────────────────────────────────────────

    /**
     * Marks a track as played. Updates last_played_at and play_count.
     * Called by PlayerViewModel every time a song starts playing.
     */
    fun markAsPlayed(songId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                trackDao.markAsPlayed(songId)
                Log.d(TAG, "Marked as played: $songId")
            } catch (e: Exception) {
                Log.e(TAG, "markAsPlayed failed: ${e.message}")
            }
        }
    }

    /**
     * Saves the playback position for a track.
     * Enables "Continue Listening" to show accurate progress.
     */
    fun savePlaybackPosition(songId: String, positionMs: Long, durationMs: Long) {
        scope.launch(Dispatchers.IO) {
            try {
                trackDao.savePlaybackPosition(songId, positionMs, durationMs)
            } catch (e: Exception) {
                Log.e(TAG, "savePlaybackPosition failed: ${e.message}")
            }
        }
    }

    /**
     * Ensures a track exists in the library (upserts metadata).
     */
    fun saveTrack(track: TrackEntity) {
        scope.launch(Dispatchers.IO) {
            try {
                trackDao.upsertTrack(track)
            } catch (e: Exception) {
                Log.e(TAG, "saveTrack failed: ${e.message}")
            }
        }
    }

    /**
     * Toggles favorite status. Returns the new state.
     * This is the SINGLE entry point for all favorite operations.
     */
    fun toggleFavorite(songId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val isFav = favoriteDao.isFavorite(songId)
                if (isFav) {
                    favoriteDao.removeFavorite(songId)
                    Log.d(TAG, "Removed favorite: $songId")
                } else {
                    favoriteDao.addFavorite(FavoriteEntity(songId = songId))
                    Log.d(TAG, "Added favorite: $songId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "toggleFavorite failed: ${e.message}")
            }
        }
    }

    /**
     * Checks if a song is favorited (one-shot).
     */
    suspend fun isFavorite(songId: String): Boolean {
        return favoriteDao.isFavorite(songId)
    }

    /**
     * Creates a new playlist.
     */
    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        scope.launch(Dispatchers.IO) {
            try {
                playlistDao.createPlaylist(PlaylistEntity(name = name.trim()))
                Log.d(TAG, "Created playlist: $name")
            } catch (e: Exception) {
                Log.e(TAG, "createPlaylist failed: ${e.message}")
            }
        }
    }

    /**
     * Renames a playlist.
     */
    fun renamePlaylist(playlistId: Long, newName: String) {
        if (newName.isBlank()) return
        scope.launch(Dispatchers.IO) {
            try {
                playlistDao.renamePlaylist(playlistId, newName.trim())
                Log.d(TAG, "Renamed playlist $playlistId to $newName")
            } catch (e: Exception) {
                Log.e(TAG, "renamePlaylist failed: ${e.message}")
            }
        }
    }

    /**
     * Deletes a playlist and all its tracks (CASCADE).
     */
    fun deletePlaylist(playlistId: Long) {
        scope.launch(Dispatchers.IO) {
            try {
                playlistDao.deletePlaylist(playlistId)
                Log.d(TAG, "Deleted playlist $playlistId")
            } catch (e: Exception) {
                Log.e(TAG, "deletePlaylist failed: ${e.message}")
            }
        }
    }

    /**
     * Adds a track to a playlist.
     */
    fun addTrackToPlaylist(playlistId: Long, songId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                playlistDao.addTrackToPlaylistAtomic(playlistId, songId)
                Log.d(TAG, "Added track $songId to playlist $playlistId")
            } catch (e: Exception) {
                Log.e(TAG, "addTrackToPlaylist failed: ${e.message}")
            }
        }
    }

    /**
     * Removes a track from a playlist.
     */
    fun removeTrackFromPlaylist(playlistId: Long, songId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                playlistDao.removeTrackFromPlaylist(playlistId, songId)
                Log.d(TAG, "Removed track $songId from playlist $playlistId")
            } catch (e: Exception) {
                Log.e(TAG, "removeTrackFromPlaylist failed: ${e.message}")
            }
        }
    }
}
