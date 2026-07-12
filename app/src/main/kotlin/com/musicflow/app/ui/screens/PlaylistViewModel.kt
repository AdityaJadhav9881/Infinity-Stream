package com.musicflow.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicflow.app.data.local.dao.PlaylistDao
import com.musicflow.app.data.local.entity.PlaylistEntity
import com.musicflow.app.data.local.entity.PlaylistTrackMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistDao: PlaylistDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        observePlaylists()
    }

    private fun observePlaylists() {
        playlistDao.observeAllPlaylists().onEach { playlists ->
            _uiState.update { state ->
                state.copy(
                    playlists = playlists,
                    isLoading = false,
                )
            }
        }.launchIn(viewModelScope)
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            playlistDao.createPlaylist(PlaylistEntity(name = name.trim()))
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            playlistDao.renamePlaylist(playlistId, newName.trim())
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistDao.deletePlaylist(playlistId)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch {
            val count = playlistDao.getPlaylistTrackCount(playlistId)
            playlistDao.addTrackToPlaylist(
                PlaylistTrackMap(
                    playlistId = playlistId,
                    songId = songId,
                    position = count,
                )
            )
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch {
            playlistDao.removeTrackFromPlaylist(playlistId, songId)
        }
    }

    fun isTrackInPlaylist(playlistId: Long, songId: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = playlistDao.isTrackInPlaylist(playlistId, songId)
            callback(result)
        }
    }

    fun getPlaylistTracks(playlistId: Long, callback: (List<com.musicflow.app.data.local.entity.TrackEntity>) -> Unit) {
        viewModelScope.launch {
            val tracks = playlistDao.getPlaylistTracks(playlistId)
            callback(tracks)
        }
    }

    fun getPlaylistTracksFlow(playlistId: Long): Flow<List<com.musicflow.app.data.local.entity.TrackEntity>> {
        return playlistDao.observePlaylistTracks(playlistId)
    }

    fun onDismissDialog() {
        _uiState.update { it.copy(showCreateDialog = false, showAddToPlaylistDialog = null) }
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true) }
    }

    fun showAddToPlaylistDialog(songId: String) {
        _uiState.update { it.copy(showAddToPlaylistDialog = songId) }
    }
}

data class PlaylistUiState(
    val playlists: List<PlaylistEntity> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateDialog: Boolean = false,
    val showAddToPlaylistDialog: String? = null, // songId or null
)
