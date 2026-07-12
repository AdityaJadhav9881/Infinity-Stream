package com.musicflow.app.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicflow.app.data.local.dao.SearchHistoryDao
import com.musicflow.app.data.local.entity.SearchHistoryEntity
import com.musicflow.app.data.remote.AudioHeaderStore
import com.musicflow.app.data.remote.SearchResult
import com.musicflow.app.data.repository.SearchRepository
import com.musicflow.app.utils.LanguagePreferences
import com.musicflow.app.utils.SearchLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel managing the search UI state and interactions.
 *
 * Features:
 * - Search query debouncing (300ms)
 * - Autocomplete suggestions from YouTube Music
 * - Search history persistence
 * - Result type filters (Songs/All)
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val searchHistoryDao: SearchHistoryDao,
    private val languagePreferences: LanguagePreferences,
) : ViewModel() {

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val SUGGESTIONS_DEBOUNCE_MS = 150L
    }

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var suggestionsJob: Job? = null

    init {
        observeSearchHistory()
    }

    val searchLanguage: Flow<SearchLanguage> = languagePreferences.searchLanguage

    private fun observeSearchHistory() {
        searchHistoryDao.observeAll().onEach { history ->
            _uiState.update { state ->
                state.copy(searchHistory = history.map { it.query })
            }
        }.launchIn(viewModelScope)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        debounceSearch(query)
        debounceSuggestions(query)
    }

    fun onClearSearch() {
        searchJob?.cancel()
        suggestionsJob?.cancel()
        _uiState.update {
            it.copy(
                query = "",
                results = emptyList(),
                suggestions = emptyList(),
                isLoading = false,
                error = null,
            )
        }
    }

    fun onSuggestionSelected(suggestion: String) {
        _uiState.update { it.copy(query = suggestion, suggestions = emptyList()) }
        saveToHistory(suggestion)
        debounceSearch(suggestion)
    }

    fun onFilterChange(filter: SearchFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        if (_uiState.value.query.isNotBlank()) {
            debounceSearch(_uiState.value.query)
        }
    }

    fun onClearHistory() {
        viewModelScope.launch {
            searchHistoryDao.clearAll()
        }
    }

    fun onDeleteHistoryItem(query: String) {
        viewModelScope.launch {
            searchHistoryDao.delete(query)
        }
    }

    fun onSuggestionsDismissed() {
        _uiState.update { it.copy(suggestions = emptyList()) }
    }

    suspend fun onTrackSelected(result: SearchResult): String? {
        _uiState.update { it.copy(isLoading = true, selectedTrack = result) }
        saveToHistory(_uiState.value.query)

        return try {
            val extractionResult = searchRepository.extractAudioWithHeaders(result.videoId)
            AudioHeaderStore.put(result.videoId, extractionResult.headers)
            Log.i("SearchViewModel", "Stored ${extractionResult.headers.size} headers for ${result.videoId}")

            _uiState.update {
                it.copy(
                    isLoading = false,
                    resolvedUrl = extractionResult.url,
                )
            }

            extractionResult.url
        } catch (e: Exception) {
            Log.e("SearchViewModel", "Failed to extract audio: ${e.message}", e)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Failed to load track: ${e.message}",
                )
            }
            null
        }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(error = null) }
    }

    private fun saveToHistory(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            searchHistoryDao.insert(
                SearchHistoryEntity(query = query.trim())
            )
        }
    }

    private fun debounceSearch(query: String) {
        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(results = emptyList(), isLoading = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            performSearch(query)
        }
    }

    private fun debounceSuggestions(query: String) {
        suggestionsJob?.cancel()

        if (query.isBlank()) {
            _uiState.update { it.copy(suggestions = emptyList()) }
            return
        }

        suggestionsJob = viewModelScope.launch {
            delay(SUGGESTIONS_DEBOUNCE_MS)
            fetchSuggestions(query)
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }

        try {
            val language = languagePreferences.searchLanguage.first()
            val filter = _uiState.value.selectedFilter
            val results = withContext(Dispatchers.IO) {
                searchRepository.searchWithFilter(query, filter.name, language.param)
            }

            _uiState.update {
                it.copy(
                    results = results,
                    isLoading = false,
                    suggestions = emptyList(),
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Search failed: ${e.message}",
                )
            }
        }
    }

    private suspend fun fetchSuggestions(query: String) {
        try {
            val suggestions = withContext(Dispatchers.IO) {
                searchRepository.getSuggestions(query)
            }
            _uiState.update { it.copy(suggestions = suggestions.take(8)) }
        } catch (e: Exception) {
            Log.e("SearchViewModel", "Failed to fetch suggestions: ${e.message}")
        }
    }

    /**
     * Searches for an artist and returns the browseId of the first result.
     */
    suspend fun findArtistBrowseId(artistName: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val results = searchRepository.searchWithFilter(
                    artistName,
                    "EgWKAQIYAWoKEAMQBRAJEAoQBQ%3D%3D"
                )
                results.firstOrNull()?.videoId
            } catch (e: Exception) {
                Log.e("SearchViewModel", "findArtistBrowseId failed: ${e.message}")
                null
            }
        }
    }

    /**
     * Searches for an album and returns the browseId of the first result.
     */
    suspend fun findAlbumBrowseId(albumTitle: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val results = searchRepository.searchWithFilter(
                    albumTitle,
                    "EgWKAQIQAWoKEAMQBRAJEAoQBQ%3D%3D"
                )
                results.firstOrNull()?.videoId
            } catch (e: Exception) {
                Log.e("SearchViewModel", "findAlbumBrowseId failed: ${e.message}")
                null
            }
        }
    }
}

/**
 * Search result type filter.
 */
enum class SearchFilter(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists"),
}

/**
 * Immutable data class representing the search UI state.
 */
data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTrack: SearchResult? = null,
    val resolvedUrl: String? = null,
    val suggestions: List<String> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val selectedFilter: SearchFilter = SearchFilter.ALL,
)