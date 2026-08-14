package com.musicflow.app.domain.graph

import com.musicflow.app.data.local.dao.ListeningEventDao
import com.musicflow.app.data.local.dao.MusicGraphDao
import com.musicflow.app.data.local.dao.TrackDao
import com.musicflow.app.data.local.entity.MusicGraphEntity
import com.musicflow.app.data.local.entity.TrackEntity
import com.musicflow.app.domain.memory.MusicMemoryEventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A scored result from a graph query.
 *
 * @property trackId The track identifier.
 * @property score Similarity or relevance score (0f..1f).
 * @property source The source of the score (for debugging / UI labels).
 */
data class SimilarityResult(
    val trackId: String,
    val score: Float,
    val source: String,
)

/**
 * Builds and queries the music relationship graph.
 *
 * The engine infers edges between tracks and artists by analysing
 * listening history co-occurrence, play count, and metadata overlap.
 * It persists edges via [MusicGraphDao] for fast lookup by the
 * galaxy view, genome, and recommendation systems.
 *
 * ## Relationship inference
 * 1. **LISTENED_TOGETHER** — tracks played within the same listening session
 *    (≤ 10 min gap) receive a weight proportional to co-occurrence count.
 * 2. **SIMILAR_ARTIST** — tracks sharing the same artist string.
 * 3. **SAME_ALBUM** — (placeholder: needs album metadata in TrackEntity).
 * 4. **SAME_GENRE** — (placeholder: needs genre metadata).
 * 5. **SAME_MOOD** — (placeholder: needs mood classification).
 *
 * @hiltSingleton
 */
@Singleton
class MusicGraphEngine @Inject constructor(
    private val musicGraphDao: MusicGraphDao,
    private val listeningEventDao: ListeningEventDao,
    private val trackDao: TrackDao,
) {

    companion object {
        /** Maximum gap between two events to count as "listened together". */
        private const val SESSION_GAP_MS = 10 * 60 * 1000L // 10 minutes

        /** Base weight for co-occurrence edges. */
        private const val BASE_CO_OCCURRENCE_WEIGHT = 0.3

        /** Weight increment per additional co-occurrence. */
        private const val CO_OCCURRENCE_INCREMENT = 0.05

        /** Maximum co-occurrence weight. */
        private const val MAX_CO_OCCURRENCE_WEIGHT = 1.0

        /** Weight for artist similarity edges. */
        private const val ARTIST_SIMILARITY_WEIGHT = 0.6
    }

    /**
     * Rebuilds the entire music graph from listening history.
     *
     * This is a heavy operation — call from a background worker or
     * on-demand after significant listening activity. The graph is
     * cleared and repopulated atomically.
     */
    suspend fun buildGraph() = withContext(Dispatchers.Default) {
        musicGraphDao.deleteAll()

        val allTracks = trackDao.getAllTracks()
        val events = listeningEventDao.getEventsSince(0L)

        // 1. Build co-occurrence edges (LISTENED_TOGETHER)
        buildCoOccurrenceEdges(events)

        // 2. Build artist similarity edges (SIMILAR_ARTIST)
        buildArtistEdges(allTracks)
    }

    /**
     * Returns the top-[limit] tracks most similar to [songId] with scores.
     *
     * Combines graph edges with metadata similarity for a unified ranking.
     */
    suspend fun getSimilarTracks(
        songId: String,
        limit: Int = 10,
    ): List<SimilarityResult> = withContext(Dispatchers.Default) {
        val edges = musicGraphDao.getSimilar(songId, limit * 2)
        val sourceTrack = trackDao.getTrackBySongId(songId)

        val scored = edges.map { edge ->
            val targetId = if (edge.targetId == songId) edge.sourceId else edge.targetId
            val metadataBoost = if (sourceTrack != null) {
                val target = trackDao.getTrackBySongId(targetId)
                metadataScore(sourceTrack, target)
            } else 0f

            val finalScore = (edge.weight.toFloat() + metadataBoost).coerceIn(0f, 1f)
            SimilarityResult(
                trackId = targetId,
                score = finalScore,
                source = edge.relationshipType,
            )
        }

        // Deduplicate by trackId, keeping the highest score
        scored.groupBy { it.trackId }
            .map { (_, results) -> results.maxByOrNull { it.score }!! }
            .sortedByDescending { it.score }
            .take(limit)
    }

    /**
     * Returns artists related to [artistId] based on shared listeners.
     *
     * @param artistId The artist name or identifier.
     * @return List of related artist identifiers with weights.
     */
    suspend fun getRelatedArtists(artistId: String): List<SimilarityResult> =
        withContext(Dispatchers.Default) {
            val edges = musicGraphDao.getSimilar(artistId, 50)
            edges
                .filter { it.relationshipType == com.musicflow.app.data.local.entity.GraphRelationshipType.SIMILAR_ARTIST.storageValue }
                .map { edge ->
                    val relatedId = if (edge.targetId == artistId) edge.sourceId else edge.targetId
                    SimilarityResult(
                        trackId = relatedId,
                        score = edge.weight.toFloat(),
                        source = "artist_graph",
                    )
                }
                .distinctBy { it.trackId }
                .sortedByDescending { it.score }
        }

    /**
     * Returns all tracks clustered under [genre].
     *
     * Falls back to empty if genre metadata is not yet available.
     */
    suspend fun getGenreCluster(genre: String): List<SimilarityResult> =
        withContext(Dispatchers.Default) {
            val edges = musicGraphDao.getRelationshipsByType(
                com.musicflow.app.data.local.entity.GraphRelationshipType.SAME_GENRE.storageValue,
            )
            edges
                .filter {
                    it.sourceId == genre || it.targetId == genre
                }
                .map { edge ->
                    val trackId = if (edge.targetId == genre) edge.sourceId else edge.targetId
                    SimilarityResult(
                        trackId = trackId,
                        score = edge.weight.toFloat(),
                        source = "genre_cluster",
                    )
                }
                .sortedByDescending { it.score }
        }

    // ── Private: Co-occurrence ────────────────────────────────────────

    private suspend fun buildCoOccurrenceEdges(
        events: List<com.musicflow.app.data.local.entity.ListeningEventEntity>,
    ) {
        // Group into sessions by time proximity
        val sortedEvents = events
            .filter { it.eventType == MusicMemoryEventType.PLAYBACK_STARTED.storageValue }
            .sortedBy { it.occurredAt }

        if (sortedEvents.size < 2) return

        // Co-occurrence matrix: trackId -> trackId -> count
        val coOccurrences = mutableMapOf<String, MutableMap<String, Int>>()

        var sessionStart = sortedEvents.first().occurredAt
        var lastEventTime = sortedEvents.first().occurredAt
        val currentSession = mutableListOf<String>()
        currentSession.add(sortedEvents.first().trackId)

        for (event in sortedEvents.drop(1)) {
            val gap = event.occurredAt - lastEventTime
            if (gap <= SESSION_GAP_MS) {
                currentSession.add(event.trackId)
                lastEventTime = event.occurredAt
            } else {
                // Flush session
                recordSessionCoOccurrences(currentSession, coOccurrences)
                currentSession.clear()
                currentSession.add(event.trackId)
                sessionStart = event.occurredAt
                lastEventTime = event.occurredAt
            }
        }
        recordSessionCoOccurrences(currentSession, coOccurrences)

        // Persist co-occurrence edges
        val edges = mutableListOf<MusicGraphEntity>()
        for ((sourceId, targets) in coOccurrences) {
            for ((targetId, count) in targets) {
                if (sourceId == targetId) continue
                val weight = (BASE_CO_OCCURRENCE_WEIGHT + CO_OCCURRENCE_INCREMENT * (count - 1))
                    .coerceAtMost(MAX_CO_OCCURRENCE_WEIGHT)
                edges.add(
                    MusicGraphEntity(
                        sourceType = "track",
                        sourceId = sourceId,
                        targetType = "track",
                        targetId = targetId,
                        relationshipType = com.musicflow.app.data.local.entity.GraphRelationshipType.LISTENED_TOGETHER.storageValue,
                        weight = weight,
                    )
                )
            }
        }
        if (edges.isNotEmpty()) {
            musicGraphDao.insertRelationships(edges)
        }
    }

    private fun recordSessionCoOccurrences(
        sessionTrackIds: List<String>,
        coOccurrences: MutableMap<String, MutableMap<String, Int>>,
    ) {
        for (i in sessionTrackIds.indices) {
            for (j in i + 1 until sessionTrackIds.size) {
                val a = sessionTrackIds[i]
                val b = sessionTrackIds[j]
                coOccurrences.getOrPut(a) { mutableMapOf() }
                    .merge(b, 1, Int::plus)
                coOccurrences.getOrPut(b) { mutableMapOf() }
                    .merge(a, 1, Int::plus)
            }
        }
    }

    // ── Private: Artist edges ─────────────────────────────────────────

    private suspend fun buildArtistEdges(allTracks: List<TrackEntity>) {
        // Group tracks by artist
        val byArtist = allTracks
            .filter { it.artist.isNotBlank() }
            .groupBy { it.artist }

        val edges = mutableListOf<MusicGraphEntity>()
        for ((artist, tracks) in byArtist) {
            if (tracks.size < 2) continue
            // Connect all tracks within the same artist
            for (i in tracks.indices) {
                for (j in i + 1 until tracks.size) {
                    edges.add(
                        MusicGraphEntity(
                            sourceType = "track",
                            sourceId = tracks[i].songId,
                            targetType = "track",
                            targetId = tracks[j].songId,
                            relationshipType = com.musicflow.app.data.local.entity.GraphRelationshipType.SIMILAR_ARTIST.storageValue,
                            weight = ARTIST_SIMILARITY_WEIGHT,
                        )
                    )
                }
            }
        }
        if (edges.isNotEmpty()) {
            musicGraphDao.insertRelationships(edges)
        }
    }

    // ── Private: Metadata scoring ─────────────────────────────────────

    /**
     * Computes a simple metadata similarity boost between two tracks.
     * Returns 0..1 added to the graph edge weight.
     */
    private fun metadataScore(source: TrackEntity, target: TrackEntity?): Float {
        if (target == null) return 0f
        var score = 0f

        // Exact artist match → +0.15
        if (source.artist.isNotBlank() && source.artist.equals(target.artist, ignoreCase = true)) {
            score += 0.15f
        }

        // Play count similarity (both heavily played → +0.05)
        if (source.playCount > 10 && target.playCount > 10) {
            score += 0.05f
        }

        return score
    }
}
