package com.musicflow.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicflow.app.data.SharedMusicState
import com.musicflow.app.data.local.dao.PlaylistDao
import com.musicflow.app.data.local.entity.PlaylistEntity
import com.musicflow.app.data.local.entity.PlaylistTrackMap
import com.musicflow.app.data.local.entity.TrackEntity
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
    private val sharedMusicState: SharedMusicState,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        observePlaylists()
    }

    private fun observePlaylists() {
        sharedMusicState.playlists.onEach { playlists ->
            _uiState.update { state ->
                state.copy(
                    playlists = playlists,
                    isLoading = false,
                )
            }
        }.launchIn(viewModelScope)
    }

    fun createPlaylist(name: String) {
        sharedMusicState.createPlaylist(name)
    }

    fun renamePlaylist(playlistId: Long, newName: String) {
        sharedMusicState.renamePlaylist(playlistId, newName)
    }

    fun deletePlaylist(playlistId: Long) {
        sharedMusicState.deletePlaylist(playlistId)
    }

    fun addTrackToPlaylist(playlistId: Long, songId: String) {
        sharedMusicState.addTrackToPlaylist(playlistId, songId)
    }

    fun removeTrackFromPlaylist(playlistId: Long, songId: String) {
        sharedMusicState.removeTrackFromPlaylist(playlistId, songId)
    }

    fun isTrackInPlaylist(playlistId: Long, songId: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = playlistDao.isTrackInPlaylist(playlistId, songId)
            callback(result)
        }
    }

    fun getPlaylistTracks(playlistId: Long, callback: (List<TrackEntity>) -> Unit) {
        viewModelScope.launch {
            val tracks = playlistDao.getPlaylistTracks(playlistId)
            callback(tracks)
        }
    }

    fun getPlaylistTracksFlow(playlistId: Long): Flow<List<TrackEntity>> {
        return playlistDao.observePlaylistTracks(playlistId)
    }

    /**
     * Duplicates a playlist with all its tracks.
     */
    fun duplicatePlaylist(playlistId: Long) {
        viewModelScope.launch {
            val original = playlistDao.getPlaylistById(playlistId) ?: return@launch
            val newId = playlistDao.createPlaylist(
                PlaylistEntity(name = "${original.name} (Copy)")
            )
            val trackIds = playlistDao.getTrackIdsInPlaylist(playlistId)
            trackIds.forEachIndexed { index, songId ->
                playlistDao.addTrackToPlaylist(
                    PlaylistTrackMap(
                        playlistId = newId,
                        songId = songId,
                        position = index,
                    )
                )
            }
        }
    }

    /**
     * Reorders a track within a playlist.
     */
    fun reorderTrack(playlistId: Long, fromPosition: Int, toPosition: Int) {
        viewModelScope.launch {
            val trackIds = playlistDao.getTrackIdsInPlaylist(playlistId).toMutableList()
            if (fromPosition in trackIds.indices && toPosition in trackIds.indices) {
                val item = trackIds.removeAt(fromPosition)
                trackIds.add(toPosition, item)
                // Update all positions
                trackIds.forEachIndexed { index, songId ->
                    playlistDao.addTrackToPlaylist(
                        PlaylistTrackMap(
                            playlistId = playlistId,
                            songId = songId,
                            position = index,
                        )
                    )
                }
            }
        }
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
    val showAddToPlaylistDialog: String? = null,
)
