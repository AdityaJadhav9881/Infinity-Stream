package com.musicflow.app.domain.discovery

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.musicflow.app.data.local.dao.ListeningEventDao
import com.musicflow.app.data.local.dao.TrackDao
import com.musicflow.app.data.local.entity.TrackEntity
import com.musicflow.app.domain.memory.MusicMemoryEventType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.discoveryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "discovery_settings",
)

/**
 * "Surprise Me" discovery engine.
 *
 * Learns the user's musical comfort zone from listening habits and deliberately
 * selects tracks outside their normal patterns. The adventure score (0 = conservative,
 * 1 = adventurous) persists across sessions and adapts based on whether the user
 * listens to, skips, or likes the suggested tracks.
 */
@Singleton
class DiscoveryEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackDao: TrackDao,
    private val listeningEventDao: ListeningEventDao,
) {

    companion object {
        private val ADVENTURE_SCORE_KEY = floatPreferencesKey("adventure_score")
        private const val DEFAULT_ADVENTURE_SCORE = 0.5f
        private const val ADVENTURE_LEARN_RATE = 0.05f
        private const val NINETY_DAYS_MS = 90L * 24 * 60 * 60 * 1000
    }

    private val dataStore: DataStore<Preferences>
        get() = context.discoveryDataStore

    /**
     * Returns tracks intentionally outside the user's normal habits.
     * Higher adventure scores increase the novelty of suggestions.
     */
    suspend fun getSurpriseMe(limit: Int = 15): List<TrackEntity> = withContext(Dispatchers.IO) {
        val adventureLevel = getAdventureLevelSync()
        val allTracks = trackDao.getAllTracks()
        val now = System.currentTimeMillis()
        val events = listeningEventDao.getEventsSince(now - NINETY_DAYS_MS)

        // Build habit profile
        val playCounts = mutableMapOf<String, Int>()
        val artistCounts = mutableMapOf<String, Int>()
        val skippedIds = mutableSetOf<String>()

        for (event in events) {
            when (event.eventType) {
                MusicMemoryEventType.PLAYBACK_STARTED.storageValue -> {
                    playCounts[event.trackId] = (playCounts[event.trackId] ?: 0) + 1
                }
                MusicMemoryEventType.PLAYBACK_SKIPPED.storageValue -> {
                    skippedIds.add(event.trackId)
                }
            }
        }

        // Count artist frequency
        for (track in allTracks) {
            val count = playCounts[track.songId] ?: 0
            if (count > 0) {
                artistCounts[track.artist] = (artistCounts[track.artist] ?: 0) + count
            }
        }

        val topArtists = artistCounts.entries
            .sortedByDescending { it.value }
            .take(maxOf(3, (artistCounts.size * (1f - adventureLevel)).toInt()))
            .map { it.key }
            .toSet()

        val frequentTrackIds = playCounts.entries
            .filter { it.value > 3 }
            .map { it.key }
            .toSet()

        // Score tracks by novelty
        allTracks
            .filter { it.songId !in skippedIds }
            .map { track ->
                val isFrequent = track.songId in frequentTrackIds
                val isKnownArtist = track.artist in topArtists

                val noveltyScore = when {
                    !isFrequent && !isKnownArtist -> 10f  // Completely new
                    !isFrequent && isKnownArtist -> 5f     // New track from known artist
                    isFrequent && !isKnownArtist -> 7f     // Frequent track from unknown artist
                    else -> 1f                              // Very familiar
                }

                // Adventure level amplifies novelty preference
                val adjustedScore = noveltyScore * (0.5f + adventureLevel)
                track to adjustedScore
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    /**
     * Updates the adventure score based on user interaction with suggested tracks.
     * @param listened Whether the user listened to the suggested track
     * @param skipped Whether the user skipped the suggested track
     * @param liked Whether the user liked/favorited the suggested track
     */
    suspend fun updateAdventureScore(
        listened: Boolean = false,
        skipped: Boolean = false,
        liked: Boolean = false,
    ) = withContext(Dispatchers.IO) {
        val current = getAdventureLevelSync()
        val delta = when {
            liked -> ADVENTURE_LEARN_RATE * 2f
            listened && !skipped -> ADVENTURE_LEARN_RATE
            skipped -> -ADVENTURE_LEARN_RATE
            else -> 0f
        }
        val newScore = (current + delta).coerceIn(0f, 1f)
        dataStore.edit { prefs ->
            prefs[ADVENTURE_SCORE_KEY] = newScore
        }
    }

    /**
     * Returns the current adventure level as a Flow (0 = conservative, 1 = adventurous).
     */
    fun getAdventureLevel(): Flow<Float> {
        return dataStore.data.map { prefs ->
            prefs[ADVENTURE_SCORE_KEY] ?: DEFAULT_ADVENTURE_SCORE
        }
    }

    private suspend fun getAdventureLevelSync(): Float {
        return dataStore.data.map { prefs ->
            prefs[ADVENTURE_SCORE_KEY] ?: DEFAULT_ADVENTURE_SCORE
        }.first()
    }
}
