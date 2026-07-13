package com.musicflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicflow.app.data.local.entity.DownloadQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadQueueDao {

    @Query("SELECT * FROM download_queue ORDER BY created_at ASC")
    fun observeAll(): Flow<List<DownloadQueueEntity>>

    @Query("SELECT * FROM download_queue WHERE status IN ('QUEUED', 'DOWNLOADING', 'PAUSED') ORDER BY created_at ASC")
    fun observeActive(): Flow<List<DownloadQueueEntity>>

    @Query("SELECT * FROM download_queue WHERE status = 'DOWNLOADING'")
    suspend fun getDownloading(): List<DownloadQueueEntity>

    @Query("SELECT * FROM download_queue WHERE status = 'QUEUED' ORDER BY created_at ASC LIMIT :limit")
    suspend fun getNextQueued(limit: Int = 1): List<DownloadQueueEntity>

    @Query("SELECT * FROM download_queue WHERE song_id = :songId")
    suspend fun getBySongId(songId: String): DownloadQueueEntity?

    @Query("SELECT * FROM download_queue WHERE song_id = :songId")
    fun observeBySongId(songId: String): Flow<DownloadQueueEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM download_queue WHERE song_id = :songId AND status IN ('QUEUED', 'DOWNLOADING', 'PAUSED'))")
    suspend fun isAlreadyQueued(songId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DownloadQueueEntity)

    @Query("UPDATE download_queue SET status = :status, progress = :progress, speed_bytes_per_sec = :speed WHERE song_id = :songId")
    suspend fun updateProgress(songId: String, status: String, progress: Float, speed: Long = 0L)

    @Query("UPDATE download_queue SET status = 'FAILED', error_reason = :reason WHERE song_id = :songId")
    suspend fun markFailed(songId: String, reason: String)

    @Query("UPDATE download_queue SET status = 'COMPLETED', progress = 1.0, completed_at = :completedAt, local_file_path = :filePath WHERE song_id = :songId")
    suspend fun markCompleted(songId: String, completedAt: Long = System.currentTimeMillis(), filePath: String = "")

    @Query("UPDATE download_queue SET status = 'QUEUED', retry_count = retry_count + 1, error_reason = '' WHERE song_id = :songId")
    suspend fun retry(songId: String)

    @Query("UPDATE download_queue SET status = 'CANCELLED' WHERE song_id = :songId")
    suspend fun cancel(songId: String)

    @Query("DELETE FROM download_queue WHERE status IN ('COMPLETED', 'CANCELLED')")
    suspend fun cleanupFinished()

    @Query("DELETE FROM download_queue")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM download_queue WHERE status = 'DOWNLOADING'")
    suspend fun getActiveDownloadCount(): Int
}
