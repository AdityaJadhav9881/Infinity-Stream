package com.musicflow.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        observeAll()
    }

    private fun observeAll() {
        val tracksFlow = trackDao.observeAllTracks()
        val favoritesFlow = favoriteDao.observeAllFavorites()
        val playlistsFlow = playlistDao.observeAllPlaylists()
        val offlineFlow = offlineDownloadManager.getOfflineTracks()

        combine(tracksFlow, favoritesFlow, playlistsFlow, offlineFlow) { tracks, favorites, playlists, offline ->
            val favoriteIds = favorites.map { it.songId }.toSet()
            val libraryItems = tracks.map { track ->
                LibraryItem(track = track, isFavorite = track.songId in favoriteIds)
            }

            // Most played = all tracks sorted by recency (most recently added first)
            val mostPlayed = libraryItems.sortedByDescending { it.track.title }

            LibraryUiState(
                allItems = libraryItems,
                items = libraryItems,
                playlists = playlists,
                offlineTracks = offline,
                offlineStorageUsedBytes = offline.sumOf { it.fileSize },
                isLoading = false,
            )
        }.onEach { state ->
            _uiState.update { current ->
                current.copy(
                    allItems = state.allItems,
                    items = applyFilteredList(state.items, current.searchQuery, current.selectedFilter),
                    playlists = state.playlists,
                    offlineTracks = state.offlineTracks,
                    offlineStorageUsedBytes = state.offlineStorageUsedBytes,
                    isLoading = false,
                )
            }
        }.launchIn(viewModelScope)
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
        viewModelScope.launch {
            val isFav = favoriteDao.isFavorite(songId)
            if (isFav) favoriteDao.removeFavorite(songId)
            else favoriteDao.addFavorite(FavoriteEntity(songId = songId))
        }
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
            LibraryFilter.RECENT -> items.takeLast(20).reversed()
            LibraryFilter.DOWNLOADS -> items // Downloads filtered by offline state
            LibraryFilter.PLAYLISTS -> items // Playlists shown in own section
            LibraryFilter.ALBUMS -> items
            LibraryFilter.ARTISTS -> items
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
