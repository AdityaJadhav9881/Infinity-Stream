package com.musicflow.app.domain.mix

import com.musicflow.app.data.TrackMetadata
import com.musicflow.app.data.remote.SearchResult
import com.musicflow.app.data.repository.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Infinite Mix Engine — continuous, auto-mixing playback mode.
 *
 * Builds a chain of compatible tracks starting from a seed track.
 * Compatibility is determined by metadata similarity: same artist,
 * genre keywords in title, and the YouTube "Up Next" algorithm via
 * the SearchRepository.
 *
 * The engine is stateless — it builds a chain on demand and lets the
 * playback layer manage the actual queue.
 */
@Singleton
class InfiniteMixEngine @Inject constructor(
    private val searchRepository: SearchRepository,
) {

    companion object {
        private const val MAX_CHAIN_LENGTH = 50
        private const val PARALLEL_FETCH_LIMIT = 10
    }

    /**
     * Builds a mix chain starting from a seed track.
     *
     * Uses the "Up Next" endpoint to discover related tracks, then
     * chains them together by iteratively fetching more related tracks
     * for the last item in the chain.
     *
     * @param seedTrack The starting track metadata.
     * @param maxLength Maximum number of tracks in the chain (capped at 50).
     * @return Ordered list of compatible track metadata.
     */
    suspend fun buildMixChain(
        seedTrack: TrackMetadata,
        maxLength: Int = 20,
    ): List<TrackMetadata> = withContext(Dispatchers.IO) {
        val targetLength = maxLength.coerceAtMost(MAX_CHAIN_LENGTH)
        val chain = mutableListOf<TrackMetadata>()
        val seenIds = mutableSetOf<String>()

        chain.add(seedTrack)
        seenIds.add(seedTrack.songId)

        var currentId = seedTrack.songId

        while (chain.size < targetLength) {
            val related = searchRepository.getUpNext(currentId)
                .filter { it.videoId !in seenIds }

            if (related.isEmpty()) break

            // Take the best match (first result from Up Next is most related)
            val next = related.first()
            seenIds.add(next.videoId)

            val metadata = TrackMetadata(
                songId = next.videoId,
                title = next.title,
                artist = next.artist,
                artworkUrl = next.thumbnailUrl,
                resolvedStreamingUrl = "", // Will be resolved at play time
            )
            chain.add(metadata)
            currentId = next.videoId
        }

        chain
    }

    /**
     * Finds the next compatible track given the last played track
     * and the current playback history (to avoid repeats).
     *
     * @param lastTrack The most recently played track.
     * @param history Set of song IDs already played in the current session.
     * @return The next compatible track, or null if none found.
     */
    suspend fun getNextCompatible(
        lastTrack: TrackMetadata,
        history: Set<String>,
    ): TrackMetadata? = withContext(Dispatchers.IO) {
        try {
            val related = searchRepository.getUpNext(lastTrack.songId)
            val candidates = related.filter { it.videoId !in history }

            candidates.firstOrNull()?.let { result ->
                TrackMetadata(
                    songId = result.videoId,
                    title = result.title,
                    artist = result.artist,
                    artworkUrl = result.thumbnailUrl,
                    resolvedStreamingUrl = "",
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts a SearchResult from the API to a lightweight TrackMetadata
     * for mix chain building. The resolved streaming URL is left empty
     * and must be resolved before playback.
     */
    fun SearchResult.toMixMetadata(): TrackMetadata {
        return TrackMetadata(
            songId = videoId,
            title = title,
            artist = artist,
            artworkUrl = thumbnailUrl,
            resolvedStreamingUrl = "",
        )
    }
}
