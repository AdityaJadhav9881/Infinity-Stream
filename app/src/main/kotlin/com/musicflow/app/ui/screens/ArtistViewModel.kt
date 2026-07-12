package com.musicflow.app.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicflow.app.data.remote.ArtistPage
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
class ArtistViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArtistUiState())
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    fun loadArtist(browseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val artistPage = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    searchRepository.getArtistPage(browseId)
                }
                _uiState.update {
                    it.copy(
                        artistPage = artistPage,
                        isLoading = false,
                        error = artistPage?.let { null } ?: "Failed to load artist",
                    )
                }
            } catch (e: Exception) {
                Log.e("ArtistViewModel", "Failed to load artist: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load artist: ${e.message}",
                    )
                }
            }
        }
    }
}

data class ArtistUiState(
    val artistPage: ArtistPage? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)
