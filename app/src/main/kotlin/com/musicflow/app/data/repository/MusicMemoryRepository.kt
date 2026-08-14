package com.musicflow.app.data.repository

import androidx.room.withTransaction
import com.musicflow.app.data.TrackMetadata
import com.musicflow.app.data.local.AppDatabase
import com.musicflow.app.data.local.dao.ListeningEventDao
import com.musicflow.app.data.local.dao.TrackDao
import com.musicflow.app.data.local.entity.ListeningEventEntity
import com.musicflow.app.data.local.entity.TrackEntity
import com.musicflow.app.domain.memory.MusicMemoryEventType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline-first repository for the listener's immutable Music Memory.
 *
 * Track metadata caching and play-count updates occur in the same Room
 * transaction as the event insert.  This prevents a timeline item being
 * written without its corresponding library state (or the reverse).
 */
@Singleton
class MusicMemoryRepository @Inject constructor(
    private val database: AppDatabase,
    private val trackDao: TrackDao,
    private val listeningEventDao: ListeningEventDao,
) {

    fun observeTimeline(
        startInclusive: Long,
        endInclusive: Long,
    ): Flow<List<ListeningEventEntity>> =
        listeningEventDao.observeTimeline(startInclusive, endInclusive)

    fun observeTrackEvents(trackId: String): Flow<List<ListeningEventEntity>> =
        listeningEventDao.observeTrackEvents(trackId)

    suspend fun getEventsSince(startInclusive: Long): List<ListeningEventEntity> =
        listeningEventDao.getEventsSince(startInclusive)

    suspend fun record(
        type: MusicMemoryEventType,
        track: TrackMetadata,
        positionMs: Long = 0L,
        durationMs: Long = 0L,
        listenedMs: Long = 0L,
        occurredAt: Long = System.currentTimeMillis(),
    ) {
        database.withTransaction {
            upsertTrackMetadata(track)
            if (type == MusicMemoryEventType.PLAYBACK_STARTED) {
                trackDao.markAsPlayed(track.songId, occurredAt)
            }
            listeningEventDao.insert(
                ListeningEventEntity(
                    trackId = track.songId,
                    eventType = type.storageValue,
                    occurredAt = occurredAt,
                    positionMs = positionMs.coerceAtLeast(0L),
                    durationMs = durationMs.coerceAtLeast(0L),
                    listenedMs = listenedMs.coerceAtLeast(0L),
                    title = track.title,
                    artist = track.artist,
                    artworkUrl = track.artworkUrl,
                ),
            )
        }
    }

    /**
     * Stores queue/search metadata without treating it as a listen.  Queue
     * construction must never inflate Recently Played or Most Played.
     */
    suspend fun cacheTrack(track: TrackMetadata) {
        database.withTransaction { upsertTrackMetadata(track) }
    }

    /** Records a library action without requiring a playback URL. */
    suspend fun recordLibraryAction(type: MusicMemoryEventType, songId: String) {
        database.withTransaction {
            val track = trackDao.getTrackBySongId(songId) ?: return@withTransaction
            listeningEventDao.insert(
                ListeningEventEntity(
                    trackId = track.songId,
                    eventType = type.storageValue,
                    title = track.title,
                    artist = track.artist,
                    artworkUrl = track.artworkUrl,
                ),
            )
        }
    }

    private suspend fun upsertTrackMetadata(track: TrackMetadata) {
        val existing = trackDao.getTrackBySongId(track.songId)
        trackDao.upsertTrack(
            TrackEntity(
                songId = track.songId,
                title = track.title,
                artist = track.artist,
                artworkUrl = track.artworkUrl,
                durationMs = existing?.durationMs ?: 0L,
                lastPlayedAt = existing?.lastPlayedAt ?: 0L,
                addedAt = existing?.addedAt ?: System.currentTimeMillis(),
                playCount = existing?.playCount ?: 0,
                lastPlayedPositionMs = existing?.lastPlayedPositionMs ?: 0L,
                lastPlayedDurationMs = existing?.lastPlayedDurationMs ?: 0L,
            ),
        )
    }
}
