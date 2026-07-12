package com.musicflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicflow.app.data.local.entity.LyricsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [LyricsEntity] operations.
 *
 * Provides methods for fetching and caching lyrics for tracks.
 * Supports both plain text and synchronized LRC formats.
 */
@Dao
interface LyricsDao {

    /**
     * Observes lyrics for a specific song.
     *
     * @param songId The YouTube video ID to look up.
     * @return Flow emitting the lyrics entity, or null if not found.
     */
    @Query("SELECT * FROM lyrics WHERE song_id = :songId")
    fun observeLyrics(songId: String): Flow<LyricsEntity?>

    /**
     * One-shot query for lyrics.
     *
     * @param songId The YouTube video ID to look up.
     * @return The lyrics entity, or null if not found.
     */
    @Query("SELECT * FROM lyrics WHERE song_id = :songId")
    suspend fun get(songId: String): LyricsEntity?

    /**
     * Inserts or replaces lyrics for a track.
     *
     * @param entity The lyrics entity to insert or update.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LyricsEntity)

    /**
     * Deletes lyrics for a specific song.
     *
     * @param songId The YouTube video ID to remove lyrics for.
     */
    @Query("DELETE FROM lyrics WHERE song_id = :songId")
    suspend fun delete(songId: String)

    /**
     * Deletes all cached lyrics.
     *
     * Useful for cache clearing operations.
     */
    @Query("DELETE FROM lyrics")
    suspend fun deleteAll()
}