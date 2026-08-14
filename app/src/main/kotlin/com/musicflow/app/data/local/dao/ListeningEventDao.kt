package com.musicflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicflow.app.data.local.entity.ListeningEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * Persistence boundary for the Music Memory timeline.
 *
 * Queries return [Flow] so Time Machine, recommendations, and future graph
 * projections can share one offline-first source of truth.
 */
@Dao
interface ListeningEventDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: ListeningEventEntity): Long

    @Query(
        """
        SELECT * FROM listening_events
        WHERE occurred_at BETWEEN :startInclusive AND :endInclusive
        ORDER BY occurred_at DESC, id DESC
        """,
    )
    fun observeTimeline(
        startInclusive: Long,
        endInclusive: Long,
    ): Flow<List<ListeningEventEntity>>

    @Query(
        """
        SELECT * FROM listening_events
        WHERE track_id = :trackId
        ORDER BY occurred_at DESC, id DESC
        """,
    )
    fun observeTrackEvents(trackId: String): Flow<List<ListeningEventEntity>>

    @Query(
        """
        SELECT * FROM listening_events
        WHERE occurred_at >= :startInclusive
        ORDER BY occurred_at DESC, id DESC
        """,
    )
    suspend fun getEventsSince(startInclusive: Long): List<ListeningEventEntity>

    @Query("SELECT COUNT(*) FROM listening_events")
    suspend fun getEventCount(): Int
}
