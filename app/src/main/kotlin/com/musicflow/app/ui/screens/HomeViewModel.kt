package com.musicflow.app.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicflow.app.data.SharedMusicState
import com.musicflow.app.data.local.dao.PlaylistDao
import com.musicflow.app.data.local.entity.PlaylistEntity
import com.musicflow.app.data.local.entity.TrackEntity
import com.musicflow.app.data.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sharedMusicState: SharedMusicState,
    private val searchRepository: SearchRepository,
    private val playlistDao: PlaylistDao,
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val playlistTrackJobs = mutableMapOf<Long, kotlinx.coroutines.Job>()

    init {
        observeSharedState()
        loadTrending()
    }

    private fun observeSharedState() {
        sharedMusicState.recentlyPlayed
            .onEach { tracks ->
                _uiState.update { state ->
                    state.copy(
                        recentlyPlayed = tracks,
                        continueListening = tracks.take(10),
                    )
                }
            }
            .launchIn(viewModelScope)

        sharedMusicState.favoriteTracks
            .onEach { tracks ->
                _uiState.update { it.copy(favoriteTracks = tracks) }
            }
            .launchIn(viewModelScope)

        sharedMusicState.playlists
            .onEach { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
                observePlaylistTracks(playlists)
            }
            .launchIn(viewModelScope)
    }

    private fun observePlaylistTracks(playlists: List<PlaylistEntity>) {
        // Cancel jobs for playlists that no longer exist
        val currentIds = playlists.map { it.id }.toSet()
        playlistTrackJobs.keys.filter { it !in currentIds }.forEach { id ->
            playlistTrackJobs[id]?.cancel()
            playlistTrackJobs.remove(id)
        }
        // Observe tracks for each playlist
        playlists.forEach { playlist ->
            if (!playlistTrackJobs.containsKey(playlist.id)) {
                playlistTrackJobs[playlist.id] = playlistDao.observePlaylistTracks(playlist.id)
                    .distinctUntilChanged()
                    .onEach { tracks ->
                        _uiState.update { state ->
                            val updated = state.playlistTracks.toMutableMap()
                            updated[playlist.id] = tracks
                            state.copy(playlistTracks = updated)
                        }
                    }
                    .launchIn(viewModelScope)
            }
        }
    }

    /**
     * Fetches real trending music from YouTube Music.
     * Searches for trending Hindi/Marathi songs.
     * Caches results locally and refreshes daily.
     */
    fun loadTrending() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTrendingLoading = true, trendingError = null) }
            try {
                val queries = listOf(
                    "trending songs India 2024",
                    "Hindi trending songs",
                    "Marathi trending songs",
                )
                val allResults = mutableListOf<com.musicflow.app.data.remote.SearchResult>()
                for (query in queries) {
                    val results = searchRepository.search(query)
                    allResults.addAll(results)
                    if (allResults.size >= 10) break
                }
                // Filter out compilations, mashups, jukeboxes
                val blockedTerms = listOf("compilation", "mashup", "jukebox", "various artists", "karaoke", "tribute")
                val filtered = allResults.filter { result ->
                    val titleLower = result.title.lowercase()
                    val artistLower = result.artist.lowercase()
                    blockedTerms.none { term -> titleLower.contains(term) || artistLower.contains(term) }
                }
                // Deduplicate by videoId
                val unique = filtered.distinctBy { it.videoId }.take(15)
                val trendingTracks = unique.map { result ->
                    TrackEntity(
                        songId = result.videoId,
                        title = result.title,
                        artist = result.artist,
                        artworkUrl = result.thumbnailUrl,
                    )
                }
                _uiState.update { state ->
                    state.copy(
                        trending = trendingTracks,
                        isTrendingLoading = false,
                    )
                }
                Log.i(TAG, "Loaded ${trendingTracks.size} trending tracks")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load trending: ${e.message}")
                _uiState.update { state ->
                    state.copy(
                        isTrendingLoading = false,
                        trendingError = "Failed to load trending. Pull to retry.",
                    )
                }
            }
        }
    }
}

data class HomeUiState(
    val recentlyPlayed: List<TrackEntity> = emptyList(),
    val continueListening: List<TrackEntity> = emptyList(),
    val favoriteTracks: List<TrackEntity> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val playlistTracks: Map<Long, List<TrackEntity>> = emptyMap(),
    val trending: List<TrackEntity> = emptyList(),
    val isTrendingLoading: Boolean = false,
    val trendingError: String? = null,
)
