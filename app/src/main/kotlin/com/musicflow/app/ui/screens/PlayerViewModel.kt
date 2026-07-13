package com.musicflow.app.ui.screens

import android.app.Application
import android.content.ComponentName
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.musicflow.app.data.TrackMetadata
import com.musicflow.app.data.SharedMusicState
import com.musicflow.app.data.local.dao.FavoriteDao
import com.musicflow.app.data.local.dao.PlaylistDao
import com.musicflow.app.data.local.dao.TrackDao
import com.musicflow.app.data.local.entity.FavoriteEntity
import com.musicflow.app.data.local.entity.LyricsEntity
import com.musicflow.app.data.local.entity.TrackEntity
import com.musicflow.app.data.lyrics.LyricsProvider
import com.musicflow.app.data.remote.AudioHeaderStore
import com.musicflow.app.data.remote.SearchResult
import com.musicflow.app.data.repository.SearchRepository
import com.musicflow.app.player.MusicPlaybackService
import com.musicflow.app.player.OfflineDownloadManager
import com.musicflow.app.player.QueuePersistenceManager
import com.musicflow.app.utils.NetworkMonitor
import com.musicflow.app.utils.SleepTimerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val searchRepository: SearchRepository,
    private val trackDao: TrackDao,
    private val favoriteDao: FavoriteDao,
    private val playlistDao: PlaylistDao,
    private val lyricsProvider: LyricsProvider,
    private val sleepTimerManager: SleepTimerManager,
    private val queuePersistenceManager: QueuePersistenceManager,
    private val offlineDownloadManager: OfflineDownloadManager,
    private val networkMonitor: NetworkMonitor,
    private val sharedMusicState: SharedMusicState,
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "PlayerViewModel"
        private const val POSITION_POLL_INTERVAL_MS = 200L
        private const val MAX_UP_NEXT = 15
        private const val REFILL_THRESHOLD = 3
        private const val PARALLEL_EXTRACTORS = 3
    }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressJob: kotlinx.coroutines.Job? = null
    private var upNextJob: kotlinx.coroutines.Job? = null

    /** True when we are actively filling the queue for the current seed track. */
    private var isFillingQueue = false

    init {
        connectToService()
        sleepTimerManager.onTimerExpired = {
            mediaController?.pause()
            _uiState.update { it.copy(isSleepTimerRunning = false, sleepTimerRemaining = null) }
        }
        // Observe network state
        viewModelScope.launch {
            networkMonitor.observe.collect { online ->
                _uiState.update { it.copy(isNetworkAvailable = online) }
                Log.i(TAG, "Network state: ${if (online) "ONLINE" else "OFFLINE"}")
            }
        }
        // Observe favorites from SharedMusicState to keep isCurrentTrackLiked in sync
        viewModelScope.launch {
            sharedMusicState.favoriteIds.collect { favIds ->
                val track = _uiState.value.currentTrack
                if (track != null) {
                    _uiState.update { it.copy(isCurrentTrackLiked = track.songId in favIds) }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        upNextJob?.cancel()
        disconnectFromService()
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerManager.startMinutes(minutes)
        _uiState.update { it.copy(isSleepTimerRunning = true) }
        viewModelScope.launch {
            sleepTimerManager.remainingTimeMs.collect { remaining ->
                _uiState.update { it.copy(sleepTimerRemaining = remaining) }
                if (remaining == null || remaining <= 0L) {
                    _uiState.update { it.copy(isSleepTimerRunning = false) }
                }
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerManager.cancel()
        _uiState.update { it.copy(isSleepTimerRunning = false, sleepTimerRemaining = null) }
    }

    fun getSleepTimerFormatted(): String {
        return sleepTimerManager.formatRemainingTime(
            sleepTimerManager.remainingTimeMs.value ?: 0L
        )
    }

    /**
     * Downloads a track for offline playback.
     */
    fun downloadTrack(track: SearchResult) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isDownloading = true, downloadingTrackId = track.title) }
                val result = searchRepository.extractAudioWithHeaders(track.videoId)
                AudioHeaderStore.put(track.videoId, result.headers)
                val metadata = TrackMetadata(
                    songId = track.videoId,
                    title = track.title,
                    artist = track.artist,
                    artworkUrl = track.thumbnailUrl,
                    resolvedStreamingUrl = result.url,
                )
                offlineDownloadManager.downloadTrack(metadata, result.headers)
                    .onSuccess {
                        _uiState.update { it.copy(isDownloading = false, downloadingTrackId = null, downloadSuccess = track.title) }
                        Log.i(TAG, "Downloaded for offline: ${track.title}")
                    }
                    .onFailure { e ->
                        _uiState.update { it.copy(isDownloading = false, downloadingTrackId = null, downloadError = e.message) }
                        Log.e(TAG, "Download failed: ${e.message}", e)
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDownloading = false, downloadingTrackId = null, downloadError = e.message) }
                Log.e(TAG, "downloadTrack failed: ${e.message}", e)
            }
        }
    }

    /**
     * Checks if a track is already downloaded for offline.
     */
    suspend fun isTrackOffline(songId: String): Boolean {
        return offlineDownloadManager.isTrackOffline(songId)
    }

    /**
     * Deletes an offline track.
     */
    fun deleteOfflineTrack(songId: String) {
        viewModelScope.launch {
            offlineDownloadManager.deleteTrack(songId)
        }
    }

    /**
     * Gets the local file path for an offline track.
     * Checks both public MusicFlow dir and entity's localFilePath.
     */
    private suspend fun getLocalPathForTrack(songId: String): String? {
        // Check entity's stored path first (matches download filename)
        val entity = offlineDownloadManager.getOfflineTracks().first()
            .find { it.songId == songId }
        if (entity != null) {
            val file = java.io.File(entity.localFilePath)
            if (file.exists()) return file.absolutePath
        }
        // Fallback: check old format (songId.m4a)
        val legacyFile = java.io.File(
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC),
            "MusicFlow/$songId.m4a"
        )
        if (legacyFile.exists()) return legacyFile.absolutePath
        return null
    }

    /**
     * Gets total offline storage used.
     */
    suspend fun getOfflineStorageUsed(): Long {
        return offlineDownloadManager.getTotalStorageUsed()
    }

    fun clearDownloadMessages() {
        _uiState.update { it.copy(downloadSuccess = null, downloadError = null) }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
        updateStateFromController()
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    fun skipToPrevious() {
        mediaController?.let { controller ->
            if (controller.currentPosition > 3_000) controller.seekTo(0)
            else controller.seekToPrevious()
        }
        updateStateFromController()
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNext()
            updateStateFromController()
        }
    }

    /**
     * Toggles the lyrics overlay visibility.
     */
    fun toggleLyricsVisibility() {
        _uiState.update { it.copy(isLyricsVisible = !it.isLyricsVisible) }
    }

    /**
     * Toggles the like/favorite status of the current track.
     * Uses SharedMusicState as single source of truth.
     */
    fun toggleLikeCurrentTrack() {
        val track = _uiState.value.currentTrack ?: return
        viewModelScope.launch {
            val isFav = sharedMusicState.isFavorite(track.songId)
            sharedMusicState.toggleFavorite(track.songId)
            _uiState.update { it.copy(isCurrentTrackLiked = !isFav) }
        }
    }

    /**
     * Checks if the current track is liked and updates UI state.
     */
    private suspend fun checkCurrentTrackLiked() {
        val track = _uiState.value.currentTrack ?: return
        val isLiked = sharedMusicState.isFavorite(track.songId)
        _uiState.update { it.copy(isCurrentTrackLiked = isLiked) }
    }

    /**
     * Sets the lyrics overlay visibility.
     */
    fun setLyricsVisibility(visible: Boolean) {
        _uiState.update { it.copy(isLyricsVisible = visible) }
    }

    /**
     * Fetches lyrics for the current track.
     */
    private fun fetchLyricsForCurrentTrack() {
        val track = _uiState.value.currentTrack ?: return
        viewModelScope.launch {
            try {
                val lyrics = lyricsProvider.fetchLyrics(
                    songId = track.songId,
                    title = track.title,
                    artist = track.artist,
                )
                _uiState.update { it.copy(lyrics = lyrics) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch lyrics: ${e.message}")
            }
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
        _uiState.update { it.copy(isShuffleOn = controller.shuffleModeEnabled) }
    }

    /**
     * Cycles loop modes: Off (0) -> All (1) -> One (2) -> Off (0)
     */
    fun toggleLoop() {
        val controller = mediaController ?: return
        val newMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = newMode
        _uiState.update { it.copy(loopMode = newMode) }
    }

    /**
     * Sets loop mode explicitly.
     * @param mode One of Player.REPEAT_MODE_OFF/ALL/ONE
     */
    fun setLoopMode(mode: Int) {
        val controller = mediaController ?: return
        controller.repeatMode = mode
        _uiState.update { it.copy(loopMode = mode) }
    }

    /**
     * Shuffles the upcoming queue items while keeping the current
     * playing item in place. This rearranges media items in the
     * MediaController so the UI and playback order match.
     */
    fun shuffleQueue() {
        val controller = mediaController ?: return
        val currentIndex = controller.currentMediaItemIndex
        val total = controller.mediaItemCount
        if (total <= currentIndex + 1) return // nothing to shuffle

        // Collect upcoming mediaIds
        val upcoming = (currentIndex + 1 until total).map { idx ->
            controller.getMediaItemAt(idx)
        }.toMutableList()

        // Shuffle list
        upcoming.shuffle()

        // Reorder controller to match shuffled list
        for (targetPos in upcoming.indices) {
            val desired = upcoming[targetPos]
            // find current position of desired item
            val currentPos = (0 until controller.mediaItemCount).firstOrNull { i ->
                controller.getMediaItemAt(i).mediaId == desired.mediaId
            } ?: continue

            val destIndex = currentIndex + 1 + targetPos
            if (currentPos != destIndex) {
                try {
                    controller.moveMediaItem(currentPos, destIndex)
                } catch (e: Exception) {
                    // ignore move errors
                }
            }
        }

        // Reflect shuffle mode flag
        controller.shuffleModeEnabled = true
        _uiState.update { it.copy(isShuffleOn = controller.shuffleModeEnabled) }
    }

    /**
     * Move an item within the upcoming queue.
     * @param from Index in upcoming list (0 = next item)
     * @param to Target index in upcoming list
     */
    fun moveQueueItem(from: Int, to: Int) {
        val controller = mediaController ?: return
        val currentIndex = controller.currentMediaItemIndex
        val absFrom = currentIndex + 1 + from
        val absTo = currentIndex + 1 + to
        if (absFrom < 0 || absTo < 0 || absFrom >= controller.mediaItemCount || absTo >= controller.mediaItemCount) return
        try {
            controller.moveMediaItem(absFrom, absTo)
            updateStateFromController()
        } catch (e: Exception) {
            // ignore
        }
    }

    fun skipToQueueItem(index: Int) {
        val controller = mediaController ?: return
        val targetIndex = controller.currentMediaItemIndex + 1 + index
        if (targetIndex < controller.mediaItemCount) {
            controller.seekTo(targetIndex, 0)
            updateStateFromController()
        }
    }

    /**
     * Removes a track from the queue by its position in the upcoming list.
     * @param index Index in the upcoming tracks list (0 = next track)
     */
    fun removeFromQueue(index: Int) {
        val controller = mediaController ?: return
        val targetIndex = controller.currentMediaItemIndex + 1 + index
        if (targetIndex < controller.mediaItemCount) {
            controller.removeMediaItem(targetIndex)
            updateStateFromController()
        }
    }

    /**
     * Called from search — plays a brand-new track and fills the queue.
     */
    fun playTrack(track: SearchResult, audioUrl: String) {
        val controller = mediaController ?: run {
            Log.e(TAG, "mediaController is NULL")
            return
        }

        Log.i(TAG, "playTrack: ${track.title}")
        MusicPlaybackService.currentSongId = track.videoId

        val metadata = TrackMetadata(
            songId = track.videoId,
            title = track.title,
            artist = track.artist,
            artworkUrl = track.thumbnailUrl,
            resolvedStreamingUrl = audioUrl,
        )

        controller.setMediaItems(listOf(metadata.toMediaItem()))
        controller.prepare()
        controller.play()

        // Save to library
        saveTrackToLibrary(metadata)

        // Fill the queue with similar tracks
        fillQueue(track.videoId)

        // Save queue to database
        saveCurrentQueue()
    }

    /**
     * Adds a track to play next (after the current track).
     * Requires network unless track is downloaded.
     */
    fun playNext(track: SearchResult) {
        viewModelScope.launch {
            if (!networkMonitor.isOnline.value) {
                Log.w(TAG, "playNext: offline — cannot add to queue")
                return@launch
            }
            try {
                val result = searchRepository.extractAudioWithHeaders(track.videoId)
                AudioHeaderStore.put(track.videoId, result.headers)

                val metadata = TrackMetadata(
                    songId = track.videoId,
                    title = track.title,
                    artist = track.artist,
                    artworkUrl = track.thumbnailUrl,
                    resolvedStreamingUrl = result.url,
                )

                val controller = mediaController ?: return@launch
                val insertIndex = controller.currentMediaItemIndex + 1
                controller.addMediaItem(insertIndex, metadata.toMediaItem())
                saveTrackToLibrary(metadata)
                Log.i(TAG, "Added to play next: ${track.title}")
            } catch (e: Exception) {
                Log.e(TAG, "playNext failed: ${e.message}", e)
            }
        }
    }

    /**
     * Adds a track to the end of the queue.
     * Requires network unless track is downloaded.
     */
    fun enqueue(track: SearchResult) {
        viewModelScope.launch {
            if (!networkMonitor.isOnline.value) {
                Log.w(TAG, "enqueue: offline — cannot add to queue")
                return@launch
            }
            try {
                val result = searchRepository.extractAudioWithHeaders(track.videoId)
                AudioHeaderStore.put(track.videoId, result.headers)

                val metadata = TrackMetadata(
                    songId = track.videoId,
                    title = track.title,
                    artist = track.artist,
                    artworkUrl = track.thumbnailUrl,
                    resolvedStreamingUrl = result.url,
                )

                val controller = mediaController ?: return@launch
                controller.addMediaItem(controller.mediaItemCount, metadata.toMediaItem())
                saveTrackToLibrary(metadata)
                Log.i(TAG, "Enqueued: ${track.title}")
            } catch (e: Exception) {
                Log.e(TAG, "enqueue failed: ${e.message}", e)
            }
        }
    }

    /**
     * Plays a track from the library.
     * Offline-first: if the track is downloaded, plays from local file
     * (saves data when online, works when offline).
     * If not downloaded, extracts audio URL from network.
     */
    fun playFromLibrary(songId: String, title: String, artist: String, artworkUrl: String) {
        viewModelScope.launch {
            // Offline-first: check if track is downloaded
            val isOffline = offlineDownloadManager.isTrackOffline(songId)
            if (isOffline) {
                val localPath = getLocalPathForTrack(songId)
                if (localPath != null) {
                    Log.i(TAG, "playFromLibrary: playing offline '$title' from $localPath")
                    playOfflineTrack(songId, title, artist, artworkUrl, localPath)
                    return@launch
                }
            }

            // Not downloaded — try network
            val isOnline = networkMonitor.isOnline.value
            if (!isOnline) {
                Log.w(TAG, "playFromLibrary: offline and track not downloaded: $title")
                _uiState.update { it.copy(downloadError = "No internet connection. Download the track to play offline.") }
                return@launch
            }

            try {
                val result = searchRepository.extractAudioWithHeaders(songId)
                AudioHeaderStore.put(songId, result.headers)

                val metadata = TrackMetadata(
                    songId = songId,
                    title = title,
                    artist = artist,
                    artworkUrl = artworkUrl,
                    resolvedStreamingUrl = result.url,
                )

                playTrack(
                    com.musicflow.app.data.remote.SearchResult(
                        videoId = songId,
                        title = title,
                        artist = artist,
                        thumbnailUrl = artworkUrl,
                    ),
                    result.url,
                )
            } catch (e: Exception) {
                Log.e(TAG, "playFromLibrary failed: ${e.message}")
            }
        }
    }

    /**
     * Plays a track from a local offline file.
     */
    fun playOfflineTrack(songId: String, title: String, artist: String, artworkUrl: String, localFilePath: String) {
        val controller = mediaController ?: run {
            Log.e(TAG, "mediaController is NULL")
            return
        }

        Log.i(TAG, "playOfflineTrack: $title from $localFilePath")
        MusicPlaybackService.currentSongId = songId

        val fileUri = android.net.Uri.fromFile(java.io.File(localFilePath))
        val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(android.net.Uri.parse(artworkUrl))
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()

        val requestMetadata = androidx.media3.common.MediaItem.RequestMetadata.Builder()
            .setMediaUri(fileUri)
            .build()

        val mediaItem = androidx.media3.common.MediaItem.Builder()
            .setMediaId(songId)
            .setUri(fileUri)
            .setCustomCacheKey(songId)
            .setMediaMetadata(mediaMetadata)
            .setRequestMetadata(requestMetadata)
            .build()

        controller.setMediaItems(listOf(mediaItem))
        controller.prepare()
        controller.play()
    }

    /**
     * Plays a track in the context of a playlist — sets the tapped track
     * as current and queues up all remaining playlist tracks after it.
     * Offline-first: downloaded tracks play from local file.
     */
    fun playFromPlaylistContext(
        songId: String, title: String, artist: String, artworkUrl: String,
        allPlaylistTracks: List<com.musicflow.app.data.local.entity.TrackEntity>,
        startIndex: Int,
    ) {
        viewModelScope.launch {
            val controller = mediaController ?: run {
                Log.e(TAG, "mediaController is NULL")
                return@launch
            }

            MusicPlaybackService.currentSongId = songId
            val mediaItems = mutableListOf<MediaItem>()

            // Add selected track first
            val selectedTrack = allPlaylistTracks[startIndex]
            val isOffline = offlineDownloadManager.isTrackOffline(songId)
            if (isOffline) {
                val localPath = getLocalPathForTrack(songId)
                if (localPath != null) {
                    mediaItems.add(buildOfflineMediaItem(songId, title, artist, artworkUrl, localPath))
                }
            } else {
                val isOnline = networkMonitor.isOnline.value
                if (!isOnline) {
                    _uiState.update { it.copy(downloadError = "No internet. Download the track to play offline.") }
                    return@launch
                }
                try {
                    val result = searchRepository.extractAudioWithHeaders(songId)
                    AudioHeaderStore.put(songId, result.headers)
                    val metadata = TrackMetadata(songId, title, artist, artworkUrl, result.url)
                    mediaItems.add(metadata.toMediaItem())
                } catch (e: Exception) {
                    Log.e(TAG, "playFromPlaylistContext: extract failed for $songId: ${e.message}")
                    return@launch
                }
            }

            // Add remaining tracks from the playlist (after the selected one)
            val remainingTracks = allPlaylistTracks.drop(startIndex + 1)
            for (track in remainingTracks) {
                val isTrackOffline = offlineDownloadManager.isTrackOffline(track.songId)
                if (isTrackOffline) {
                    val localPath = getLocalPathForTrack(track.songId)
                    if (localPath != null) {
                        mediaItems.add(buildOfflineMediaItem(track.songId, track.title, track.artist, track.artworkUrl, localPath))
                    }
                } else {
                    try {
                        val result = searchRepository.extractAudioWithHeaders(track.songId)
                        AudioHeaderStore.put(track.songId, result.headers)
                        mediaItems.add(TrackMetadata(track.songId, track.title, track.artist, track.artworkUrl, result.url).toMediaItem())
                    } catch (e: Exception) {
                        Log.w(TAG, "playFromPlaylistContext: skip ${track.songId}: ${e.message}")
                    }
                }
            }

            // Add tracks from before the selected one (wrap around)
            val beforeTracks = allPlaylistTracks.take(startIndex)
            for (track in beforeTracks) {
                val isTrackOffline = offlineDownloadManager.isTrackOffline(track.songId)
                if (isTrackOffline) {
                    val localPath = getLocalPathForTrack(track.songId)
                    if (localPath != null) {
                        mediaItems.add(buildOfflineMediaItem(track.songId, track.title, track.artist, track.artworkUrl, localPath))
                    }
                } else {
                    try {
                        val result = searchRepository.extractAudioWithHeaders(track.songId)
                        AudioHeaderStore.put(track.songId, result.headers)
                        mediaItems.add(TrackMetadata(track.songId, track.title, track.artist, track.artworkUrl, result.url).toMediaItem())
                    } catch (e: Exception) {
                        Log.w(TAG, "playFromPlaylistContext: skip ${track.songId}: ${e.message}")
                    }
                }
            }

            if (mediaItems.isEmpty()) {
                Log.w(TAG, "playFromPlaylistContext: no playable tracks")
                return@launch
            }

            controller.setMediaItems(mediaItems)
            controller.prepare()
            controller.play()
            saveTrackToLibrary(TrackMetadata(songId, title, artist, artworkUrl, ""))
            updateStateFromController()
            saveCurrentQueue()
            Log.i(TAG, "playFromPlaylistContext: playing $title, queue=${mediaItems.size} tracks")
        }
    }

    /**
     * Builds a MediaItem for offline playback from a local file.
     */
    private fun buildOfflineMediaItem(
        songId: String, title: String, artist: String, artworkUrl: String, localFilePath: String,
    ): MediaItem {
        val fileUri = android.net.Uri.fromFile(java.io.File(localFilePath))
        // Use local artwork if available (saved alongside audio as .jpg)
        val audioFile = java.io.File(localFilePath)
        val localArtworkFile = java.io.File(audioFile.parent, audioFile.nameWithoutExtension + ".jpg")
        val artworkUri = if (localArtworkFile.exists()) {
            android.net.Uri.fromFile(localArtworkFile)
        } else {
            android.net.Uri.parse(artworkUrl)
        }
        val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(artworkUri)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()
        val requestMetadata = androidx.media3.common.MediaItem.RequestMetadata.Builder()
            .setMediaUri(fileUri)
            .build()
        return androidx.media3.common.MediaItem.Builder()
            .setMediaId(songId)
            .setUri(fileUri)
            .setCustomCacheKey(songId)
            .setMediaMetadata(mediaMetadata)
            .setRequestMetadata(requestMetadata)
            .build()
    }

    // ── Mood Mix / Playlist / Daily Mix Playback ───────────────────────

    /**
     * Searches for tracks by query, plays the first one immediately,
     * then fills the rest of the queue in background (parallel extraction).
     * Requires network connection.
     */
    fun playMoodMix(query: String) {
        viewModelScope.launch {
            if (!networkMonitor.isOnline.value) {
                Log.w(TAG, "playMoodMix: offline — cannot search for mixes")
                _uiState.update { it.copy(downloadError = "No internet connection. Connect to play mixes.") }
                return@launch
            }
            try {
                Log.i(TAG, "playMoodMix: searching '$query'")
                val results = searchRepository.search(query)
                if (results.isEmpty()) {
                    Log.w(TAG, "playMoodMix: no results for '$query'")
                    return@launch
                }

                val tracks = results.take(10)
                Log.i(TAG, "playMoodMix: got ${tracks.size} tracks")

                // Extract first track and play immediately
                val firstTrack = tracks.first()
                val firstExtraction = searchRepository.extractAudioWithHeaders(firstTrack.videoId)
                AudioHeaderStore.put(firstTrack.videoId, firstExtraction.headers)

                val firstMetadata = TrackMetadata(
                    songId = firstTrack.videoId,
                    title = firstTrack.title,
                    artist = firstTrack.artist,
                    artworkUrl = firstTrack.thumbnailUrl,
                    resolvedStreamingUrl = firstExtraction.url,
                )

                val controller = mediaController ?: return@launch
                MusicPlaybackService.currentSongId = firstTrack.videoId
                controller.setMediaItems(listOf(firstMetadata.toMediaItem()))
                controller.prepare()
                controller.play()
                saveTrackToLibrary(firstMetadata)

                // Fill remaining tracks in background (parallel, 3 at a time)
                val remaining = tracks.drop(1)
                val semaphore = Semaphore(PARALLEL_EXTRACTORS)
                val deferreds = remaining.map { track ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            try {
                                val extraction = searchRepository.extractAudioWithHeaders(track.videoId)
                                AudioHeaderStore.put(track.videoId, extraction.headers)
                                TrackMetadata(
                                    songId = track.videoId,
                                    title = track.title,
                                    artist = track.artist,
                                    artworkUrl = track.thumbnailUrl,
                                    resolvedStreamingUrl = extraction.url,
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "playMoodMix: failed ${track.videoId}: ${e.message}")
                                null
                            }
                        }
                    }
                }

                for (deferred in deferreds) {
                    val metadata = deferred.await() ?: continue
                    try {
                        val ctrl = mediaController ?: break
                        ctrl.addMediaItem(ctrl.mediaItemCount, metadata.toMediaItem())
                        saveTrackToLibrary(metadata)
                    } catch (e: Exception) {
                        Log.e(TAG, "playMoodMix: addMediaItem failed: ${e.message}")
                    }
                }

                updateStateFromController()
                saveCurrentQueue()
                Log.i(TAG, "playMoodMix: done, queue size=${mediaController?.mediaItemCount}")
            } catch (e: Exception) {
                Log.e(TAG, "playMoodMix failed: ${e.message}", e)
            }
        }
    }

    /**
     * Loads tracks from a playlist and plays them as a queue.
     * If playlist is empty, does nothing (caller should show toast).
     */
    fun playPlaylistQueue(playlistId: Long) {
        viewModelScope.launch {
            try {
                val playlistTracks = playlistDao.getPlaylistTracks(playlistId)
                if (playlistTracks.isEmpty()) {
                    Log.w(TAG, "playPlaylistQueue: playlist $playlistId is empty")
                    return@launch
                }

                Log.i(TAG, "playPlaylistQueue: loading ${playlistTracks.size} tracks from playlist $playlistId")

                val controller = mediaController ?: return@launch

                val mediaItems = playlistTracks.map { track ->
                    TrackMetadata(
                        songId = track.songId,
                        title = track.title,
                        artist = track.artist,
                        artworkUrl = track.artworkUrl,
                        resolvedStreamingUrl = "",
                    ).toMediaItem()
                }

                controller.setMediaItems(mediaItems)
                controller.prepare()
                controller.play()

                saveCurrentQueue()
                Log.i(TAG, "playPlaylistQueue: playing ${playlistTracks.size} tracks")
            } catch (e: Exception) {
                Log.e(TAG, "playPlaylistQueue failed: ${e.message}", e)
            }
        }
    }

    /**
     * Plays a Daily Mix: uses the most-played track to fetch similar tracks via getUpNext.
     * Falls back to a general query if no history exists.
     * Requires network connection.
     */
    fun playDailyMix() {
        viewModelScope.launch {
            if (!networkMonitor.isOnline.value) {
                Log.w(TAG, "playDailyMix: offline — cannot play mixes")
                _uiState.update { it.copy(downloadError = "No internet connection. Connect to play mixes.") }
                return@launch
            }
            try {
                val controller = mediaController ?: return@launch

                val allTracks = trackDao.getAllTracks()
                if (allTracks.isNotEmpty()) {
                    val mostPlayed = allTracks.first()
                    Log.i(TAG, "playDailyMix: using seed track '${mostPlayed.title}' (${mostPlayed.songId})")

                    val similarTracks = searchRepository.getUpNext(mostPlayed.songId)
                    val tracks = similarTracks.take(10)

                    if (tracks.isNotEmpty()) {
                        // Extract first track and play immediately
                        val firstTrack = tracks.first()
                        val firstExtraction = searchRepository.extractAudioWithHeaders(firstTrack.videoId)
                        AudioHeaderStore.put(firstTrack.videoId, firstExtraction.headers)

                        val firstMetadata = TrackMetadata(
                            songId = firstTrack.videoId,
                            title = firstTrack.title,
                            artist = firstTrack.artist,
                            artworkUrl = firstTrack.thumbnailUrl,
                            resolvedStreamingUrl = firstExtraction.url,
                        )

                        MusicPlaybackService.currentSongId = firstTrack.videoId
                        controller.setMediaItems(listOf(firstMetadata.toMediaItem()))
                        controller.prepare()
                        controller.play()
                        saveTrackToLibrary(firstMetadata)

                        // Fill rest in background
                        val remaining = tracks.drop(1)
                        val semaphore = Semaphore(PARALLEL_EXTRACTORS)
                        val deferreds = remaining.map { track ->
                            async(Dispatchers.IO) {
                                semaphore.withPermit {
                                    try {
                                        val extraction = searchRepository.extractAudioWithHeaders(track.videoId)
                                        AudioHeaderStore.put(track.videoId, extraction.headers)
                                        TrackMetadata(
                                            songId = track.videoId,
                                            title = track.title,
                                            artist = track.artist,
                                            artworkUrl = track.thumbnailUrl,
                                            resolvedStreamingUrl = extraction.url,
                                        )
                                    } catch (e: Exception) {
                                        Log.e(TAG, "playDailyMix: failed ${track.videoId}: ${e.message}")
                                        null
                                    }
                                }
                            }
                        }

                        for (deferred in deferreds) {
                            val metadata = deferred.await() ?: continue
                            try {
                                val ctrl = mediaController ?: break
                                ctrl.addMediaItem(ctrl.mediaItemCount, metadata.toMediaItem())
                                saveTrackToLibrary(metadata)
                            } catch (e: Exception) {
                                Log.e(TAG, "playDailyMix: addMediaItem failed: ${e.message}")
                            }
                        }

                        updateStateFromController()
                        saveCurrentQueue()
                        Log.i(TAG, "playDailyMix: done, queue size=${mediaController?.mediaItemCount}")
                        return@launch
                    }
                }

                Log.i(TAG, "playDailyMix: no history, falling back to general search")
                playMoodMix("popular music hits 2024 trending")
            } catch (e: Exception) {
                Log.e(TAG, "playDailyMix failed: ${e.message}", e)
            }
        }
    }

    /**
     * Saves a track to the Room library database AND marks it as recently played.
     * Called automatically when a track starts playing.
     * Uses SharedMusicState for single-source-of-truth updates.
     */
    private fun saveTrackToLibrary(metadata: TrackMetadata) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existingTrack = trackDao.getTrackBySongId(metadata.songId)
                val track = TrackEntity(
                    songId = metadata.songId,
                    title = metadata.title,
                    artist = metadata.artist,
                    artworkUrl = metadata.artworkUrl,
                    addedAt = existingTrack?.addedAt ?: System.currentTimeMillis(),
                    playCount = (existingTrack?.playCount ?: 0) + 1,
                    lastPlayedAt = System.currentTimeMillis(),
                )
                trackDao.upsertTrack(track)
                Log.d(TAG, "Saved to library: ${metadata.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save to library: ${e.message}")
            }
        }
    }

    /**
     * Saves the current playback position for the current track.
     * Enables "Continue Listening" to show accurate progress bar.
     */
    private fun saveCurrentPlaybackPosition() {
        val controller = mediaController ?: return
        val track = _uiState.value.currentTrack ?: return
        val position = controller.currentPosition
        val duration = controller.duration
        if (position > 0 && duration > 0) {
            sharedMusicState.savePlaybackPosition(track.songId, position, duration)
        }
    }

    // ── Queue Engine ────────────────────────────────────────────────────

    /**
     * Fetches Up Next tracks and fills the ExoPlayer queue.
     * Only runs when online. Skipped when offline.
     */
    private fun fillQueue(seedVideoId: String) {
        upNextJob?.cancel()
        isFillingQueue = true

        if (!networkMonitor.isOnline.value) {
            Log.i(TAG, "fillQueue: offline — skipping queue fill")
            isFillingQueue = false
            return
        }

        upNextJob = viewModelScope.launch {
            try {
                Log.i(TAG, "fillQueue: fetching Up Next for $seedVideoId")
                val similarTracks = searchRepository.getUpNext(seedVideoId)

                if (similarTracks.isEmpty()) {
                    Log.w(TAG, "fillQueue: no tracks returned")
                    isFillingQueue = false
                    return@launch
                }

                val tracks = similarTracks
                    .filter { it.videoId != seedVideoId }
                    .take(MAX_UP_NEXT)

                Log.i(TAG, "fillQueue: resolving ${tracks.size} tracks in parallel")

                // Resolve audio URLs in parallel with a semaphore
                val semaphore = Semaphore(PARALLEL_EXTRACTORS)
                val controller = mediaController ?: return@launch

                val deferreds = tracks.map { track ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            try {
                                val result = searchRepository.extractAudioWithHeaders(track.videoId)
                                AudioHeaderStore.put(track.videoId, result.headers)

                                TrackMetadata(
                                    songId = track.videoId,
                                    title = track.title,
                                    artist = track.artist,
                                    artworkUrl = track.thumbnailUrl,
                                    resolvedStreamingUrl = result.url,
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "fillQueue: failed ${track.videoId}: ${e.message}")
                                null
                            }
                        }
                    }
                }

                // Collect resolved tracks and add to queue one by one
                var added = 0
                for (deferred in deferreds) {
                    val metadata = deferred.await() ?: continue
                    try {
                        // Check controller is still valid
                        val ctrl = mediaController ?: break
                        ctrl.addMediaItem(ctrl.mediaItemCount, metadata.toMediaItem())
                        added++
                        Log.d(TAG, "fillQueue: added [$added] ${metadata.title} (queue: ${ctrl.mediaItemCount})")
                    } catch (e: Exception) {
                        Log.e(TAG, "fillQueue: addMediaItem failed: ${e.message}")
                    }
                }

                Log.i(TAG, "fillQueue: done — added $added tracks (queue size: ${mediaController?.mediaItemCount})")
                updateStateFromController()
                saveCurrentQueue()
            } catch (e: Exception) {
                Log.e(TAG, "fillQueue failed: ${e.message}", e)
            } finally {
                isFillingQueue = false
            }
        }
    }

    /**
     * Refills the queue when it's running low.
     * Called from onMediaItemTransition — only triggers if queue
     * is nearly empty and we're not already filling.
     */
    private fun maybeRefillQueue(currentVideoId: String) {
        val controller = mediaController ?: return
        val remaining = controller.mediaItemCount - controller.currentMediaItemIndex - 1

        if (remaining <= REFILL_THRESHOLD && !isFillingQueue) {
            Log.i(TAG, "maybeRefillQueue: $remaining tracks remaining, refilling")
            fillQueue(currentVideoId)
        }
    }

    // ── Service Connection ──────────────────────────────────────────────

    private fun connectToService() {
        val context = getApplication<Application>()
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlaybackService::class.java),
        )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get() ?: return@addListener
                mediaController = controller
                controller.addListener(PlayerListener())
                updateStateFromController()
                startProgressPolling()
                restoreQueueFromDatabase()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect to service", e)
            }
        }, { it.run() })
    }

    private fun disconnectFromService() {
        progressJob?.cancel()
        mediaController?.removeListener(PlayerListener())
        controllerFuture?.cancel(true)
        mediaController?.release()
        mediaController = null
        controllerFuture = null
    }

    private fun restoreQueueFromDatabase() {
        viewModelScope.launch {
            try {
                val restoredQueue = queuePersistenceManager.restoreQueue()
                if (restoredQueue.isNotEmpty()) {
                    val controller = mediaController ?: return@launch
                    val mediaItems = restoredQueue.map { it.toMediaItem() }
                    controller.setMediaItems(mediaItems)
                    controller.prepare()
                    Log.i(TAG, "Restored ${restoredQueue.size} tracks from database")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore queue: ${e.message}")
            }
        }
    }

    private fun saveCurrentQueue() {
        viewModelScope.launch {
            try {
                val controller = mediaController ?: return@launch
                val tracks = mutableListOf<TrackMetadata>()
                for (i in 0 until controller.mediaItemCount) {
                    val item = controller.getMediaItemAt(i)
                    TrackMetadata.fromMediaItem(item)?.let { tracks.add(it) }
                }
                queuePersistenceManager.saveQueue(tracks)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save queue: ${e.message}")
            }
        }
    }

    // ── Progress Polling ────────────────────────────────────────────────

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    _uiState.update { state ->
                        state.copy(
                            currentPosition = controller.currentPosition,
                            duration = controller.duration.coerceAtLeast(0),
                        )
                    }
                }
                delay(POSITION_POLL_INTERVAL_MS)
            }
        }
    }

    // ── State Sync ──────────────────────────────────────────────────────

    private fun updateStateFromController() {
        val controller = mediaController ?: return

        val currentMediaItem = controller.currentMediaItem
        val trackMetadata = currentMediaItem?.let { TrackMetadata.fromMediaItem(it) }

        // Build upcoming tracks list (everything after the current item)
        val upcoming = mutableListOf<TrackMetadata>()
        val currentIndex = controller.currentMediaItemIndex
        for (i in (currentIndex + 1) until controller.mediaItemCount) {
            val item = controller.getMediaItemAt(i)
            TrackMetadata.fromMediaItem(item)?.let { upcoming.add(it) }
        }

        _uiState.update { state ->
            state.copy(
                isPlaying = controller.isPlaying,
                currentPosition = controller.currentPosition,
                duration = controller.duration.coerceAtLeast(0),
                currentTrack = trackMetadata,
                playbackState = controller.playbackState,
                queueSize = controller.mediaItemCount,
                upcomingTracks = upcoming,
                isShuffleOn = controller.shuffleModeEnabled,
                loopMode = controller.repeatMode,
            )
        }
    }

    private inner class PlayerListener : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateStateFromController()
            // Save position when paused
            if (!isPlaying) {
                saveCurrentPlaybackPosition()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updateStateFromController()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Save the previous track's playback position before switching
            saveCurrentPlaybackPosition()

            val id = mediaItem?.mediaId ?: ""
            if (id.isNotEmpty()) {
                MusicPlaybackService.currentSongId = id
            }
            updateStateFromController()

            // Save queue tracks to library
            mediaItem?.let { item ->
                TrackMetadata.fromMediaItem(item)?.let { saveTrackToLibrary(it) }
            }

            // Check if new track is liked
            viewModelScope.launch {
                checkCurrentTrackLiked()
            }

            // Fetch lyrics for new track
            if (id.isNotEmpty()) {
                fetchLyricsForCurrentTrack()
            }

            // Auto-refill queue when running low
            if (id.isNotEmpty()) {
                maybeRefillQueue(id)
            }
        }

        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            updateStateFromController()
        }
    }
}

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val currentTrack: TrackMetadata? = null,
    val playbackState: Int = Player.STATE_IDLE,
    val queueSize: Int = 0,
    val upcomingTracks: List<TrackMetadata> = emptyList(),
    val isShuffleOn: Boolean = false,
    val loopMode: Int = Player.REPEAT_MODE_OFF,
    val lyrics: LyricsEntity? = null,
    val isLyricsVisible: Boolean = false,
    val isSleepTimerRunning: Boolean = false,
    val sleepTimerRemaining: Long? = null,
    val isDownloading: Boolean = false,
    val downloadingTrackId: String? = null,
    val downloadSuccess: String? = null,
    val downloadError: String? = null,
    val isCurrentTrackLiked: Boolean = false,
    val isNetworkAvailable: Boolean = true,
    val downloadQueueSize: Int = 0,
    val activeDownloads: Int = 0,
)
