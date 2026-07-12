package com.musicflow.app.player

import android.util.Log
import com.musicflow.app.data.TrackMetadata
import com.musicflow.app.data.local.dao.QueueDao
import com.musicflow.app.data.local.entity.QueueEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueuePersistenceManager @Inject constructor(
    private val queueDao: QueueDao,
) {
    companion object {
        private const val TAG = "QueuePersistenceManager"
    }

    suspend fun saveQueue(items: List<TrackMetadata>) = withContext(Dispatchers.IO) {
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
                )
            }
            queueDao.insertAll(entities)
            Log.d(TAG, "Saved ${entities.size} tracks to queue")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save queue: ${e.message}", e)
        }
    }

    suspend fun restoreQueue(): List<TrackMetadata> = withContext(Dispatchers.IO) {
        try {
            val entities = queueDao.getQueue()
            entities.map { entity ->
                TrackMetadata(
                    songId = entity.songId,
                    title = entity.title,
                    artist = entity.artist,
                    artworkUrl = entity.artworkUrl,
                    resolvedStreamingUrl = entity.streamingUrl,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore queue: ${e.message}", e)
            emptyList()
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
