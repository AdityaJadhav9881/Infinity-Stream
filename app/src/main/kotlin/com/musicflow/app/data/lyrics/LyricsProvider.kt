package com.musicflow.app.data.lyrics

import android.util.Log
import com.musicflow.app.data.local.dao.LyricsDao
import com.musicflow.app.data.local.entity.LyricsEntity
import com.musicflow.app.data.repository.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Provider for fetching and caching lyrics.
 *
 * Fetches lyrics from YouTube Music's /next endpoint for plain text lyrics,
 * and optionally from LRC providers for synchronized lyrics.
 */
class LyricsProvider @Inject constructor(
    private val searchRepository: SearchRepository,
    private val lyricsDao: LyricsDao,
) {

    companion object {
        private const val TAG = "LyricsProvider"
    }

    /**
     * Fetches lyrics for a track, checking cache first.
     *
     * @param songId The YouTube video ID.
     * @param title The track title.
     * @param artist The track artist.
     * @return LyricsEntity containing both plain and synced lyrics, or null if not found.
     */
    suspend fun fetchLyrics(
        songId: String,
        title: String,
        artist: String,
    ): LyricsEntity? = withContext(Dispatchers.IO) {
        // Check cache first
        lyricsDao.get(songId)?.takeIf { it.plainText != null || it.syncedLrc != null }?.let { cached ->
            Log.d(TAG, "Cache hit for $songId")
            return@withContext cached
        }

        try {
            // Fetch from YouTube Music
            val plainLyrics = fetchFromYouTubeMusic(songId)

            val entity = LyricsEntity(
                songId = songId,
                plainText = plainLyrics,
                syncedLrc = null, // LRC fetching requires additional provider
            )

            // Cache the result if found
            if (plainLyrics != null) {
                lyricsDao.insert(entity)
                Log.d(TAG, "Fetched and cached lyrics for $songId")
            }

            entity.takeIf { plainLyrics != null }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch lyrics for $songId: ${e.message}")
            null
        }
    }

    /**
     * Fetches plain lyrics from YouTube Music's /next endpoint.
     */
    private suspend fun fetchFromYouTubeMusic(songId: String): String? {
        return try {
            val json = searchRepository.getNextTrack(songId) ?: return null

            val lyrics = json.toString()
                .let { response ->
                    // Parse lyrics from the JSON response
                    parseLyricsFromJson(response)
                }

            if (!lyrics.isNullOrBlank()) {
                Log.d(TAG, "Found plain lyrics for $songId")
                lyrics
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "No lyrics from YouTube Music for $songId: ${e.message}")
            null
        }
    }

    /**
     * Parses lyrics from the JSON response.
     * This is a simplified parser - in production would use proper JSON parsing.
     */
    private fun parseLyricsFromJson(json: String): String? {
        // Look for subtitle runs in the JSON
        val subtitlePattern = Regex(""""subtitle"[^}]*"runs"[^]]*\[([^\]]+)\]""")
        val runsPattern = Regex(""""text"\s*:\s*"([^"]*)""" )

        val subtitleMatch = subtitlePattern.find(json) ?: return null
        val runsContent = subtitleMatch.groupValues[1]

        return runsPattern.findAll(runsContent)
            .joinToString("") { it.groupValues[1] }
            .takeIf { it.isNotBlank() }
    }

    /**
     * Clears all cached lyrics.
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        lyricsDao.deleteAll()
        Log.d(TAG, "Cleared lyrics cache")
    }
}