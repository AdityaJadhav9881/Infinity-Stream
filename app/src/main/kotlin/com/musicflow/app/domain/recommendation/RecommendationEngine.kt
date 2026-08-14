package com.musicflow.app.domain.recommendation

import com.musicflow.app.data.local.dao.FavoriteDao
import com.musicflow.app.data.local.dao.ListeningEventDao
import com.musicflow.app.data.local.dao.TrackDao
import com.musicflow.app.data.local.entity.TrackEntity
import com.musicflow.app.data.repository.MusicMemoryRepository
import com.musicflow.app.domain.memory.MusicMemoryEventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Full recommendation engine with multiple discovery surfaces.
 *
 * Initially rule-based with architecture ready for ML upgrade.
 * Uses listening history, favorites, play counts, and skip data
 * to generate personalized recommendations across different contexts.
 */
@Singleton
class RecommendationEngine @Inject constructor(
    private val trackDao: TrackDao,
    private val favoriteDao: FavoriteDao,
    private val listeningEventDao: ListeningEventDao,
    private val memoryRepository: MusicMemoryRepository,
) {

    companion object {
        private val THIRTY_DAYS_MS = TimeUnit.DAYS.toMillis(30)
        private val SEVEN_DAYS_MS = TimeUnit.DAYS.toMillis(7)
        private val NINETY_DAYS_MS = TimeUnit.DAYS.toMillis(90)
    }

    /**
     * Personalized "For You" feed based on listening frequency + favorites.
     * Combines play count, recency, and favorite status into a relevance score.
     */
    suspend fun getForYou(limit: Int = 20): List<TrackEntity> = withContext(Dispatchers.IO) {
        val allTracks = trackDao.getAllTracks()
        val favoriteIds = favoriteDao.getAllFavorites().map { it.songId }.toSet()
        val now = System.currentTimeMillis()
        val recentEvents = listeningEventDao.getEventsSince(now - NINETY_DAYS_MS)

        val scores = mutableMapOf<String, Float>()

        // Score based on listening events
        for (event in recentEvents) {
            val weight = when (event.eventType) {
                MusicMemoryEventType.PLAYBACK_COMPLETED.storageValue -> 5f
                MusicMemoryEventType.PLAYBACK_STARTED.storageValue -> 1f
                MusicMemoryEventType.PLAYBACK_SKIPPED.storageValue -> -3f
                MusicMemoryEventType.FAVORITED.storageValue -> 8f
                else -> 0f
            }
            scores[event.trackId] = (scores[event.trackId] ?: 0f) + weight
        }

        // Boost favorites
        for (track in allTracks) {
            val favBoost = if (track.songId in favoriteIds) 10f else 0f
            val playBoost = track.playCount.toFloat() * 2f
            val recencyBoost = if (track.lastPlayedAt > now - SEVEN_DAYS_MS) 5f else 0f
            scores[track.songId] = (scores[track.songId] ?: 0f) + favBoost + playBoost + recencyBoost
        }

        // Deduplicate by songId, sort by score, return top N
        allTracks
            .distinctBy { it.songId }
            .sortedByDescending { scores[it.songId] ?: 0f }
            .take(limit)
    }

    /**
     * Daily Mix — uses the most-played track as seed to find similar tracks
     * by querying the database for tracks by the same artist or with
     * overlapping play patterns.
     */
    suspend fun getDailyMix(limit: Int = 15): List<TrackEntity> = withContext(Dispatchers.IO) {
        val mostPlayed = trackDao.getRecentlyPlayed(limit = 1)
        if (mostPlayed.isEmpty()) return@withContext emptyList()

        val seedTrack = mostPlayed.first()
        val allTracks = trackDao.getAllTracks()
        val favoriteIds = favoriteDao.getAllFavorites().map { it.songId }.toSet()
        val now = System.currentTimeMillis()

        // Find tracks with similar artist or high engagement
        val candidates = allTracks
            .filter { it.songId != seedTrack.songId }
            .map { track ->
                val artistMatch = if (track.artist == seedTrack.artist) 20f else 0f
                val playScore = track.playCount.toFloat() * 3f
                val favScore = if (track.songId in favoriteIds) 8f else 0f
                val recencyScore = if (track.lastPlayedAt > now - SEVEN_DAYS_MS) 4f else 0f
                track to (artistMatch + playScore + favScore + recencyScore)
            }
            .sortedByDescending { it.second }
            .map { it.first }

        // Ensure seed artist is represented
        val sameArtist = candidates.filter { it.artist == seedTrack.artist }.take(5)
        val diverse = candidates.filter { it.artist != seedTrack.artist }.take(limit - sameArtist.size)

        (sameArtist + diverse).take(limit)
    }

    /**
     * Forgotten Favorites — tracks the user favorited but hasn't played
     * in 30+ days. Sorted by original play count to surface beloved tracks.
     */
    suspend fun getForgottenFavorites(limit: Int = 15): List<TrackEntity> = withContext(Dispatchers.IO) {
        val allTracks = trackDao.getAllTracks()
        val favoriteIds = favoriteDao.getAllFavorites().map { it.songId }.toSet()
        val cutoff = System.currentTimeMillis() - THIRTY_DAYS_MS

        allTracks
            .filter { track ->
                track.songId in favoriteIds &&
                    track.lastPlayedAt > 0L &&
                    track.lastPlayedAt < cutoff
            }
            .sortedByDescending { it.playCount }
            .take(limit)
    }

    /**
     * Mood Mix — finds tracks based on mood keywords.
     * Matches mood against track titles and artist names as a simple heuristic.
     * Architecture ready for metadata-based mood classification.
     */
    suspend fun getMoodMix(mood: String, limit: Int = 15): List<TrackEntity> = withContext(Dispatchers.IO) {
        val moodKeywords = mapMoodToKeywords(mood)
        val allTracks = trackDao.getAllTracks()

        val scored = allTracks.map { track ->
            val searchText = "${track.title} ${track.artist}".lowercase()
            val matchCount = moodKeywords.count { keyword -> searchText.contains(keyword) }
            track to matchCount
        }

        scored
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
            .ifEmpty {
                // Fallback: return random popular tracks if no mood match
                trackDao.getRecentlyPlayed(limit)
            }
    }

    /**
     * Discovery — tracks outside the user's normal listening habits.
     * Surfaces low-play-count tracks that haven't been skipped.
     */
    suspend fun getDiscovery(limit: Int = 20): List<TrackEntity> = withContext(Dispatchers.IO) {
        val allTracks = trackDao.getAllTracks()
        val favoriteIds = favoriteDao.getAllFavorites().map { it.songId }.toSet()
        val now = System.currentTimeMillis()
        val cutoff = now - NINETY_DAYS_MS

        // Get event data to find skipped tracks
        val events = listeningEventDao.getEventsSince(cutoff)
        val skippedIds = events
            .filter { it.eventType == MusicMemoryEventType.PLAYBACK_SKIPPED.storageValue }
            .map { it.trackId }
            .toSet()

        val playedIds = events
            .filter { it.eventType == MusicMemoryEventType.PLAYBACK_STARTED.storageValue }
            .map { it.trackId }
            .toSet()

        allTracks
            .filter { track ->
                // Not in favorites (we already know those), not skipped, not recently played
                track.songId !in favoriteIds &&
                    track.songId !in skippedIds &&
                    track.songId !in playedIds
            }
            .sortedByDescending { it.addedAt }
            .take(limit)
    }

    /**
     * Finds tracks similar to a given track by matching artist and play patterns.
     */
    suspend fun getSimilarTracks(songId: String, limit: Int = 10): List<TrackEntity> = withContext(Dispatchers.IO) {
        val targetTrack = trackDao.getTrackBySongId(songId) ?: return@withContext emptyList()
        val allTracks = trackDao.getAllTracks()
        val favoriteIds = favoriteDao.getAllFavorites().map { it.songId }.toSet()

        allTracks
            .filter { it.songId != songId }
            .map { track ->
                val artistScore = if (track.artist == targetTrack.artist) 30f else 0f
                val favScore = if (track.songId in favoriteIds) 5f else 0f
                val playScore = (track.playCount.toFloat()).coerceAtMost(20f)
                track to (artistScore + favScore + playScore)
            }
            .sortedByDescending { it.second }
            .filter { it.second > 0 }
            .take(limit)
            .map { it.first }
    }

    private fun mapMoodToKeywords(mood: String): List<String> {
        return when (mood.lowercase()) {
            "calm", "relax", "chill", "peaceful", "serene" ->
                listOf("calm", "relax", "chill", "peaceful", "serene", "gentle", "soft", "quiet", "acoustic")
            "energetic", "energy", "pump", "power" ->
                listOf("energy", "pump", "power", "fire", "lit", "blast", "hype", "adrenaline")
            "happy", "joy", "cheerful", "upbeat" ->
                listOf("happy", "joy", "smile", "sunshine", "bright", "good", "feel", "love")
            "sad", "melancholy", "grief", "heartbreak" ->
                listOf("sad", "cry", "tears", "heartbreak", "lonely", "blue", "dark", "gone", "miss")
            "focus", "concentration", "work", "study" ->
                listOf("focus", "deep", "flow", "zone", "concentrate", "think", "mind", "brain")
            "workout", "gym", "exercise", "fitness" ->
                listOf("workout", "gym", "beast", "strong", "run", "lift", "grind", "push")
            else -> listOf(mood.lowercase())
        }
    }
}
