package com.musicflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicflow.app.data.local.entity.QueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue ORDER BY position ASC")
    fun observeQueue(): Flow<List<QueueEntity>>
    
    @Query("SELECT * FROM queue ORDER BY position ASC")
    suspend fun getQueue(): List<QueueEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<QueueEntity>)
    
    @Query("DELETE FROM queue")
    suspend fun clearQueue()
}
