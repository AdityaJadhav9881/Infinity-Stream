package com.musicflow.app.player

import android.util.Log
import com.musicflow.app.data.TrackMetadata
import com.musicflow.app.data.local.dao.QueueDao
import com.musicflow.app.data.local.entity.QueueEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the full restored queue state including playback position.
 */
data class RestoredQueueState(
    val tracks: List<TrackMetadata>,
    val currentItemIndex: Int,
    val playbackPositionMs: Long,
    val isShuffleOn: Boolean,
    val repeatMode: Int,
)

@Singleton
class QueuePersistenceManager @Inject constructor(
    private val queueDao: QueueDao,
) {
    companion object {
        private const val TAG = "QueuePersistenceManager"
    }

    suspend fun saveQueue(
        items: List<TrackMetadata>,
        currentItemIndex: Int = 0,
        playbackPositionMs: Long = 0L,
        isShuffleOn: Boolean = false,
        repeatMode: Int = 0,
    ) = withContext(Dispatchers.IO) {
        try {
            queueDao.clearQueue()
            val entities = items.mapIndexed { index, metadata ->
                QueueEntity(
                    position = index,
                    songId = metadata.songId,
                    title = metadata.title,
                    artist = metadata.artist,
                    artworkUrl = metadata.artworkUrl,
                    streamingUrl = metadata.resolvedStreamingUrl,
                    currentItemIndex = if (index == 0) currentItemIndex else 0,
                    playbackPositionMs = if (index == 0) playbackPositionMs else 0L,
                    isShuffleOn = if (index == 0) isShuffleOn else false,
                    repeatMode = if (index == 0) repeatMode else 0,
                )
            }
            queueDao.insertAll(entities)
            Log.d(TAG, "Saved ${entities.size} tracks to queue (index=$currentItemIndex, pos=${playbackPositionMs}ms)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save queue: ${e.message}", e)
        }
    }

    suspend fun restoreQueue(): RestoredQueueState = withContext(Dispatchers.IO) {
        try {
            val entities = queueDao.getQueue()
            val tracks = entities.map { entity ->
                TrackMetadata(
                    songId = entity.songId,
                    title = entity.title,
                    artist = entity.artist,
                    artworkUrl = entity.artworkUrl,
                    resolvedStreamingUrl = entity.streamingUrl,
                )
            }
            val first = entities.firstOrNull()
            RestoredQueueState(
                tracks = tracks,
                currentItemIndex = first?.currentItemIndex ?: 0,
                playbackPositionMs = first?.playbackPositionMs ?: 0L,
                isShuffleOn = first?.isShuffleOn ?: false,
                repeatMode = first?.repeatMode ?: 0,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore queue: ${e.message}", e)
            RestoredQueueState(emptyList(), 0, 0L, false, 0)
        }
    }

    suspend fun clearQueue() = withContext(Dispatchers.IO) {
        try {
            queueDao.clearQueue()
            Log.d(TAG, "Cleared queue")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear queue: ${e.message}", e)
        }
    }
}
