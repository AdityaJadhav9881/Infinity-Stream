package com.musicflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicflow.app.data.local.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [FavoriteEntity] operations.
 *
 * Provides reactive observation of the user's favorite tracks
 * and simple toggle/add/remove operations.
 */
@Dao
interface FavoriteDao {

    /**
     * Observes all favorite song IDs, ordered by most recently favorited.
     *
     * @return Flow emitting the list of favorite entities.
     */
    @Query("SELECT * FROM favorites ORDER BY favorited_at DESC")
    fun observeAllFavorites(): Flow<List<FavoriteEntity>>

    /**
     * One-shot query for all favorite song IDs.
     */
    @Query("SELECT * FROM favorites ORDER BY favorited_at DESC")
    suspend fun getAllFavorites(): List<FavoriteEntity>

    /**
     * Checks whether a track is favorited.
     *
     * @param songId The YouTube video ID to check.
     * @return True if the track is in the favorites table.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE song_id = :songId)")
    suspend fun isFavorite(songId: String): Boolean

    /**
     * Observes whether a specific track is favorited.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE song_id = :songId)")
    fun observeIsFavorite(songId: String): Flow<Boolean>

    /**
     * Adds a track to favorites.
     *
     * @param entity The favorite entity to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteEntity)

    /**
     * Removes a track from favorites.
     *
     * @param songId The YouTube video ID to remove.
     */
    @Query("DELETE FROM favorites WHERE song_id = :songId")
    suspend fun removeFavorite(songId: String)

    /**
     * Returns the total number of favorites.
     */
    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun getFavoriteCount(): Int
}
