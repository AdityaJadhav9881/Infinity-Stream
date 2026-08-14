package com.musicflow.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Relationship edge in the music graph.
 *
 * Each row represents a weighted, typed link between two music entities
 * (tracks, artists, playlists, genres). The graph engine infers these
 * relationships from listening history and metadata co-occurrence.
 *
 * ## Schema
 * ```sql
 * CREATE TABLE IF NOT EXISTS music_graph (
 *     id                INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
 *     source_type       TEXT    NOT NULL,
 *     source_id         TEXT    NOT NULL,
 *     target_type       TEXT    NOT NULL,
 *     target_id         TEXT    NOT NULL,
 *     relationship_type TEXT    NOT NULL,
 *     weight            REAL    NOT NULL DEFAULT 0.0
 * )
 * ```
 *
 * @see com.musicflow.app.domain.graph.MusicGraphEngine
 */
@Entity(
    tableName = "music_graph",
    indices = [
        Index(value = ["source_id"]),
        Index(value = ["target_id"]),
        Index(value = ["source_id", "target_id"]),
        Index(value = ["relationship_type"]),
    ],
)
data class MusicGraphEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    /** Entity type of the source node (e.g. "track", "artist", "genre"). */
    @ColumnInfo(name = "source_type")
    val sourceType: String,

    /** Unique identifier of the source node (songId, artist name, etc.). */
    @ColumnInfo(name = "source_id")
    val sourceId: String,

    /** Entity type of the target node. */
    @ColumnInfo(name = "target_type")
    val targetType: String,

    /** Unique identifier of the target node. */
    @ColumnInfo(name = "target_id")
    val targetId: String,

    /** The semantic relationship between source and target. See [GraphRelationshipType]. */
    @ColumnInfo(name = "relationship_type")
    val relationshipType: String,

    /** Strength of the relationship (0f..1f). Higher = stronger link. */
    @ColumnInfo(name = "weight")
    val weight: Double = 0.0,
)

/**
 * Vocabulary of relationship types stored in the music graph.
 *
 * String values are used instead of ordinals so adding new types
 * never breaks existing offline data.
 */
enum class GraphRelationshipType(val storageValue: String) {
    /** Two tracks share the same primary artist. */
    SIMILAR_ARTIST("similar_artist"),

    /** Two tracks appear on the same album. */
    SAME_ALBUM("same_album"),

    /** Two tracks share a genre tag. */
    SAME_GENRE("same_genre"),

    /** Two tracks were frequently played together in succession. */
    LISTENED_TOGETHER("listened_together"),

    /** Two tracks share the same mood / vibe classification. */
    SAME_MOOD("same_mood"),
}
