package com.musicflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicflow.app.data.local.entity.OfflineTrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineTrackDao {
    @Query("SELECT * FROM offline_tracks ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<OfflineTrackEntity>>
    
    @Query("SELECT * FROM offline_tracks ORDER BY downloadedAt DESC")
    suspend fun getAll(): List<OfflineTrackEntity>
    
    @Query("SELECT * FROM offline_tracks WHERE songId = :songId")
    suspend fun getBySongId(songId: String): OfflineTrackEntity?
    
    @Query("SELECT * FROM offline_tracks WHERE localFilePath = :filePath LIMIT 1")
    suspend fun getByFilePath(filePath: String): OfflineTrackEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OfflineTrackEntity)
    
    @Query("DELETE FROM offline_tracks WHERE songId = :songId")
    suspend fun deleteBySongId(songId: String)
    
    @Query("DELETE FROM offline_tracks")
    suspend fun deleteAll()
    
    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM offline_tracks")
    suspend fun getTotalSize(): Long
    
    @Query("SELECT COUNT(*) FROM offline_tracks")
    suspend fun getCount(): Int
}
