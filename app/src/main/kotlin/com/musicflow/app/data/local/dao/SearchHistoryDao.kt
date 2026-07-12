package com.musicflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicflow.app.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [SearchHistoryEntity] operations.
 *
 * Stores recent search queries for quick re-access.
 * On conflict (duplicate query), the timestamp is updated.
 */
@Dao
interface SearchHistoryDao {

    /**
     * Observes all search history entries, most recent first.
     *
     * @return Flow emitting the list of search history entries.
     */
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SearchHistoryEntity>>

    /**
     * One-shot query for all search history entries.
     */
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    suspend fun getAll(): List<SearchHistoryEntity>

    /**
     * Inserts or updates a search query in history.
     *
     * If the query already exists, its timestamp is updated to now.
     *
     * @param entity The search history entry to upsert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SearchHistoryEntity)

    /**
     * Deletes a specific query from history.
     *
     * @param query The query text to remove.
     */
    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun delete(query: String)

    /**
     * Clears all search history.
     */
    @Query("DELETE FROM search_history")
    suspend fun clearAll()
}
