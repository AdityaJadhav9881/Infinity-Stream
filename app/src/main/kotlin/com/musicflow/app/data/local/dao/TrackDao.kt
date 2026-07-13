package com.musicflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicflow.app.data.local.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [TrackEntity] operations.
 *
 * All methods are suspend functions or return [Flow], ensuring
 * non-blocking database access from any coroutine scope.
 *
 * Concurrency:
 * - Room enforces that only one write transaction can execute at a time.
 * - Read queries are concurrent and do not block writes.
 * - No `runBlocking` is used anywhere in the call chain.
 */
@Dao
interface TrackDao {

    /**
     * Inserts or replaces a track in the cache.
     *
     * If a track with the same [TrackEntity.songId] already exists,
     * it is silently replaced. This is the primary upsert operation
     * for saving track metadata after resolution.
     *
     * @param track The track entity to insert or update.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrack(track: TrackEntity)

    /**
     * Inserts or replaces multiple tracks in a single transaction.
     *
     * Useful for batch-saving an entire queue or playlist.
     *
     * @param tracks The list of track entities to insert or update.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(tracks: List<TrackEntity>)

    /**
     * Retrieves all cached tracks, ordered by title.
     *
     * Returns a [Flow] that emits a new list whenever the underlying
     * data changes (reactive UI updates).
     *
     * @return Flow emitting the list of all cached tracks.
     */
    @Query("SELECT * FROM tracks ORDER BY title ASC")
    fun observeAllTracks(): Flow<List<TrackEntity>>

    /**
     * Observes tracks sorted by last played time (most recent first).
     * Used for "Recently Played" and "Continue Listening" sections.
     */
    @Query("SELECT * FROM tracks WHERE last_played_at > 0 ORDER BY last_played_at DESC")
    fun observeRecentlyPlayed(): Flow<List<TrackEntity>>

    /**
     * Observes tracks sorted by play count (most played first).
     * Used for "Most Played" section.
     */
    @Query("SELECT * FROM tracks WHERE play_count > 0 ORDER BY play_count DESC, last_played_at DESC")
    fun observeMostPlayed(): Flow<List<TrackEntity>>

    /**
     * Updates the last_played_at timestamp and increments play_count for a track.
     * Called every time a track starts playing.
     */
    @Query("UPDATE tracks SET last_played_at = :timestamp, play_count = play_count + 1 WHERE song_id = :songId")
    suspend fun markAsPlayed(songId: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Saves the playback position and duration for a track.
     * Enables "Continue Listening" to show accurate progress.
     */
    @Query("UPDATE tracks SET last_played_position_ms = :positionMs, last_played_duration_ms = :durationMs WHERE song_id = :songId")
    suspend fun savePlaybackPosition(songId: String, positionMs: Long, durationMs: Long)

    /**
     * Retrieves recently played tracks as a one-shot query.
     */
    @Query("SELECT * FROM tracks WHERE last_played_at > 0 ORDER BY last_played_at DESC LIMIT :limit")
    suspend fun getRecentlyPlayed(limit: Int = 20): List<TrackEntity>

    /**
     * Retrieves all cached tracks as a one-shot query.
     *
     * Use this for non-reactive operations (e.g. queue construction,
     * export). For UI observation, prefer [observeAllTracks].
     *
     * @return List of all cached tracks.
     */
    @Query("SELECT * FROM tracks ORDER BY title ASC")
    suspend fun getAllTracks(): List<TrackEntity>

    /**
     * Retrieves a single track by its song ID.
     *
     * @param songId The YouTube video ID to look up.
     * @return The track entity, or null if not cached.
     */
    @Query("SELECT * FROM tracks WHERE song_id = :songId LIMIT 1")
    suspend fun getTrackBySongId(songId: String): TrackEntity?

    /**
     * Checks whether a track exists in the cache.
     *
     * More efficient than [getTrackBySongId] when only existence
     * matters (avoids loading all columns).
     *
     * @param songId The YouTube video ID to check.
     * @return True if the track is cached, false otherwise.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM tracks WHERE song_id = :songId)")
    suspend fun isTrackCached(songId: String): Boolean

    /**
     * Deletes a single track by its song ID.
     *
     * @param songId The YouTube video ID of the track to remove.
     */
    @Query("DELETE FROM tracks WHERE song_id = :songId")
    suspend fun deleteTrackBySongId(songId: String)

    /**
     * Deletes all cached tracks.
     *
     * Use with caution — this is a destructive operation typically
     * invoked from a "Clear Cache" settings action.
     */
    @Query("DELETE FROM tracks")
    suspend fun deleteAllTracks()

    /**
     * Returns the total number of cached tracks.
     *
     * Useful for displaying cache size in the UI.
     *
     * @return The count of tracks in the cache.
     */
    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun getTrackCount(): Int
}
