package com.musicflow.app.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicflow.app.data.remote.AlbumPage
import com.musicflow.app.data.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    fun loadAlbum(browseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val albumPage = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    searchRepository.getAlbumPage(browseId)
                }
                _uiState.update {
                    it.copy(
                        albumPage = albumPage,
                        isLoading = false,
                        error = albumPage?.let { null } ?: "Failed to load album",
                    )
                }
            } catch (e: Exception) {
                Log.e("AlbumViewModel", "Failed to load album: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load album: ${e.message}",
                    )
                }
            }
        }
    }
}

data class AlbumUiState(
    val albumPage: AlbumPage? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)
