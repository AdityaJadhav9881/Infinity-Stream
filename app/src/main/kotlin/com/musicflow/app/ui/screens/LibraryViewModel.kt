package com.musicflow.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicflow.app.data.SharedMusicState
import com.musicflow.app.data.local.dao.FavoriteDao
import com.musicflow.app.data.local.dao.PlaylistDao
import com.musicflow.app.data.local.dao.TrackDao
import com.musicflow.app.data.local.entity.FavoriteEntity
import com.musicflow.app.data.local.entity.OfflineTrackEntity
import com.musicflow.app.data.local.entity.PlaylistEntity
import com.musicflow.app.data.local.entity.TrackEntity
import com.musicflow.app.player.OfflineDownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val trackDao: TrackDao,
    private val favoriteDao: FavoriteDao,
    private val playlistDao: PlaylistDao,
    private val offlineDownloadManager: OfflineDownloadManager,
    private val sharedMusicState: SharedMusicState,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        observeAll()
    }

    private fun observeAll() {
        // Observe all tracks
        sharedMusicState.allTracks
            .onEach { tracks ->
                _uiState.update { state ->
                    val items = tracks.map { track ->
                        LibraryItem(
                            track = track,
                            isFavorite = track.songId in sharedMusicState.favoriteIds.value,
                        )
                    }
                    state.copy(
                        allItems = items,
                        items = applyFilteredList(items, state.searchQuery, state.selectedFilter),
                        isLoading = false,
                    )
                }
            }
            .launchIn(viewModelScope)

        // Observe favorites
        sharedMusicState.favoriteIds
            .onEach { favIds ->
                _uiState.update { state ->
                    val items = state.allItems.map { item ->
                        item.copy(isFavorite = item.track.songId in favIds)
                    }
                    state.copy(
                        allItems = items,
                        items = applyFilteredList(items, state.searchQuery, state.selectedFilter),
                    )
                }
            }
            .launchIn(viewModelScope)

        // Observe playlists
        sharedMusicState.playlists
            .onEach { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
            .launchIn(viewModelScope)

        // Observe offline tracks
        offlineDownloadManager.getOfflineTracks()
            .distinctUntilChanged()
            .onEach { offlineTracks ->
                _uiState.update { state ->
                    state.copy(
                        offlineTracks = offlineTracks,
                        offlineStorageUsedBytes = offlineTracks.sumOf { it.fileSize },
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                items = applyFilteredList(state.allItems, query, state.selectedFilter),
            )
        }
    }

    fun onFilterChange(filter: LibraryFilter) {
        _uiState.update { state ->
            state.copy(
                selectedFilter = filter,
                items = applyFilteredList(state.allItems, state.searchQuery, filter),
            )
        }
    }

    fun onToggleFavorite(songId: String) {
        sharedMusicState.toggleFavorite(songId)
    }

    fun onDeleteTrack(songId: String) {
        viewModelScope.launch { trackDao.deleteTrackBySongId(songId) }
    }

    fun deleteOfflineTrack(songId: String) {
        viewModelScope.launch { offlineDownloadManager.deleteTrack(songId) }
    }

    fun clearAllOfflineTracks() {
        viewModelScope.launch { offlineDownloadManager.clearAllOfflineTracks() }
    }

    private fun applyFilteredList(items: List<LibraryItem>, query: String, filter: LibraryFilter): List<LibraryItem> {
        var result = when (filter) {
            LibraryFilter.ALL -> items
            LibraryFilter.FAVORITES -> items.filter { it.isFavorite }
            LibraryFilter.RECENT -> {
                items.filter { it.track.lastPlayedAt > 0 }
                    .sortedByDescending { it.track.lastPlayedAt }
            }
            LibraryFilter.DOWNLOADS -> items
            LibraryFilter.PLAYLISTS -> items
            LibraryFilter.ALBUMS -> items
            LibraryFilter.ARTISTS -> items
            LibraryFilter.TITLE_ASC -> items.sortedBy { it.track.title.lowercase() }
            LibraryFilter.ARTIST_ASC -> items.sortedBy { it.track.artist.lowercase() }
        }
        if (query.isNotBlank()) {
            val lower = query.lowercase()
            result = result.filter {
                it.track.title.lowercase().contains(lower) ||
                    it.track.artist.lowercase().contains(lower)
            }
        }
        return result
    }
}

enum class LibraryFilter(val label: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    RECENT("Recent"),
    DOWNLOADS("Downloads"),
    PLAYLISTS("Playlists"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    TITLE_ASC("Title A-Z"),
    ARTIST_ASC("Artist A-Z"),
}

data class LibraryItem(
    val track: TrackEntity,
    val isFavorite: Boolean,
)

data class LibraryUiState(
    val allItems: List<LibraryItem> = emptyList(),
    val items: List<LibraryItem> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedFilter: LibraryFilter = LibraryFilter.ALL,
    val playlists: List<PlaylistEntity> = emptyList(),
    val offlineTracks: List<OfflineTrackEntity> = emptyList(),
    val offlineStorageUsedBytes: Long = 0L,
)
