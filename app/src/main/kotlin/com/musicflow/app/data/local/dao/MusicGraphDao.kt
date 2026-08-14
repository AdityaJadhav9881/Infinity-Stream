package com.musicflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.musicflow.app.data.local.entity.MusicGraphEntity

/**
 * Data Access Object for [MusicGraphEntity] operations.
 *
 * Provides methods to query and mutate the music relationship graph.
 * All methods are suspend functions or return reactive types to ensure
 * non-blocking database access.
 */
@Dao
interface MusicGraphDao {

    /**
     * Inserts or replaces a relationship edge.
     *
     * If an edge with the same [MusicGraphEntity.sourceId],
     * [MusicGraphEntity.targetId], and [MusicGraphEntity.relationshipType]
     * already exists it will be replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationship(relationship: MusicGraphEntity): Long

    /**
     * Batch-inserts multiple relationship edges in a single transaction.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationships(relationships: List<MusicGraphEntity>): List<Long>

    /**
     * Retrieves all relationships where the given [sourceId] is the source node.
     *
     * @param sourceId The source entity identifier to look up.
     * @return List of edges originating from [sourceId].
     */
    @Query(
        """
        SELECT * FROM music_graph
        WHERE source_id = :sourceId
        ORDER BY weight DESC
        """
    )
    suspend fun getRelationships(sourceId: String): List<MusicGraphEntity>

    /**
     * Observes relationships for real-time UI updates.
     */
    @Query(
        """
        SELECT * FROM music_graph
        WHERE source_id = :sourceId
        ORDER BY weight DESC
        """
    )
    fun observeRelationships(sourceId: String): List<MusicGraphEntity>

    /**
     * Returns the top-[limit] most strongly connected nodes to [sourceId].
     *
     * Used by the recommendation engine and galaxy view to find related
     * tracks or artists.
     *
     * @param sourceId The node to find similar items for.
     * @param limit Maximum number of results (default 10).
     * @return Edges sorted by weight descending, capped at [limit].
     */
    @Query(
        """
        SELECT * FROM music_graph
        WHERE source_id = :sourceId
        ORDER BY weight DESC
        LIMIT :limit
        """
    )
    suspend fun getSimilar(sourceId: String, limit: Int = 10): List<MusicGraphEntity>

    /**
     * Returns all relationships of a specific type.
     *
     * @param type The [GraphRelationshipType.storageValue] to filter on.
     * @param limit Maximum number of results.
     */
    @Query(
        """
        SELECT * FROM music_graph
        WHERE relationship_type = :type
        ORDER BY weight DESC
        LIMIT :limit
        """
    )
    suspend fun getRelationshipsByType(type: String, limit: Int = 100): List<MusicGraphEntity>

    /**
     * Retrieves the full graph (all edges) as a one-shot query.
     *
     * Expensive for large graphs — prefer targeted queries in production.
     * Primarily used by the graph engine during rebuild.
     */
    @Query("SELECT * FROM music_graph ORDER BY weight DESC")
    suspend fun getGraph(): List<MusicGraphEntity>

    /**
     * Deletes all edges originating from [sourceId].
     */
    @Query("DELETE FROM music_graph WHERE source_id = :sourceId")
    suspend fun deleteBySource(sourceId: String)

    /**
     * Deletes all edges targeting [targetId].
     */
    @Query("DELETE FROM music_graph WHERE target_id = :targetId")
    suspend fun deleteByTarget(targetId: String)

    /**
     * Deletes the entire graph. Used during a full rebuild.
     */
    @Query("DELETE FROM music_graph")
    suspend fun deleteAll()

    /**
     * Returns the total number of edges in the graph.
     */
    @Query("SELECT COUNT(*) FROM music_graph")
    suspend fun getEdgeCount(): Int
}
