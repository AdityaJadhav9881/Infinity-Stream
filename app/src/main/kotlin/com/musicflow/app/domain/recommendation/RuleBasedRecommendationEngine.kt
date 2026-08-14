package com.musicflow.app.domain.recommendation

import com.musicflow.app.data.local.dao.FavoriteDao
import com.musicflow.app.data.local.dao.TrackDao
import com.musicflow.app.data.local.entity.TrackEntity
import com.musicflow.app.data.repository.MusicMemoryRepository
import com.musicflow.app.domain.memory.MusicMemoryEventType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Explainable offline recommendation policy.
 *
 * It intentionally uses only evidence the app owns: the listener's history,
 * favorites, skips, completions, and cached library. Network discovery can be
 * added as a separate source later without changing this ranking contract.
 */
@Singleton
class RuleBasedRecommendationEngine @Inject constructor(
    private val trackDao: TrackDao,
    private val favoriteDao: FavoriteDao,
    private val memoryRepository: MusicMemoryRepository,
) {
    suspend fun build(limit: Int = 12): RecommendationSet {
        val now = System.currentTimeMillis()
        val tracks = trackDao.getAllTracks()
        val favoriteIds = favoriteDao.getAllFavorites().map { it.songId }.toSet()
        val events = memoryRepository.getEventsSince(now - NINETY_DAYS_MS)
        val scoreByTrack = mutableMapOf<String, Int>()

        events.forEach { event ->
            val signal = when (event.eventType) {
                MusicMemoryEventType.PLAYBACK_COMPLETED.storageValue -> 5
                MusicMemoryEventType.FAVORITED.storageValue -> 6
                MusicMemoryEventType.PLAYBACK_STARTED.storageValue -> 1
                MusicMemoryEventType.PLAYBACK_SKIPPED.storageValue -> -4
                MusicMemoryEventType.PLAYBACK_ERROR.storageValue -> -1
                else -> 0
            }
            scoreByTrack[event.trackId] = (scoreByTrack[event.trackId] ?: 0) + signal
        }

        val recentCutoff = now - SEVEN_DAYS_MS
        val scored = tracks.sortedWith(
            compareByDescending<TrackEntity> { track ->
                (scoreByTrack[track.songId] ?: 0) +
                    (track.playCount * 2) +
                    if (track.songId in favoriteIds) 8 else 0
            }.thenByDescending { it.lastPlayedAt },
        )

        val forgottenFavorites = tracks
            .filter { track ->
                track.songId in favoriteIds &&
                    track.lastPlayedAt > 0L &&
                    track.lastPlayedAt < now - TWENTY_ONE_DAYS_MS
            }
            .sortedByDescending { it.playCount }
            .take(limit)

        val rediscoveries = scored
            .filter { track ->
                track.playCount > 0 && track.lastPlayedAt < recentCutoff
            }
            .take(limit)

        val surprise = tracks
            .filter { track ->
                track.songId !in favoriteIds &&
                    track.lastPlayedAt < recentCutoff &&
                    (scoreByTrack[track.songId] ?: 0) >= -2
            }
            // A stable daily ordering gives variety without pretending to know
            // genre or mood that the app has not actually analysed.
            .sortedBy { track -> dailyRank(track.songId, now) }
            .take(limit)

        return RecommendationSet(
            forYou = scored.take(limit),
            forgottenFavorites = forgottenFavorites,
            rediscoveries = rediscoveries,
            surprise = surprise,
        )
    }

    private fun dailyRank(songId: String, nowMs: Long): Int {
        val day = nowMs / DAY_MS
        return (songId.hashCode() xor day.toInt()) and Int.MAX_VALUE
    }

    private companion object {
        const val DAY_MS = 24L * 60L * 60L * 1_000L
        const val SEVEN_DAYS_MS = 7L * DAY_MS
        const val TWENTY_ONE_DAYS_MS = 21L * DAY_MS
        const val NINETY_DAYS_MS = 90L * DAY_MS
    }
}

data class RecommendationSet(
    val forYou: List<TrackEntity> = emptyList(),
    val forgottenFavorites: List<TrackEntity> = emptyList(),
    val rediscoveries: List<TrackEntity> = emptyList(),
    val surprise: List<TrackEntity> = emptyList(),
)
