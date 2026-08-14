package com.musicflow.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An immutable record of a meaningful listening action.
 *
 * Event rows deliberately keep a small metadata snapshot.  A track can be
 * removed from the cache later without making the listener's timeline
 * unreadable.  Playback URLs are never stored here.
 */
@Entity(
    tableName = "listening_events",
    indices = [
        Index(value = ["track_id"]),
        Index(value = ["occurred_at"]),
        Index(value = ["track_id", "occurred_at"]),
    ],
)
data class ListeningEventEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "track_id")
    val trackId: String,

    /** See [com.musicflow.app.domain.memory.MusicMemoryEventType.storageValue]. */
    @ColumnInfo(name = "event_type")
    val eventType: String,

    @ColumnInfo(name = "occurred_at")
    val occurredAt: Long = System.currentTimeMillis(),

    /** Position reached when the action occurred; zero when unavailable. */
    @ColumnInfo(name = "position_ms")
    val positionMs: Long = 0L,

    /** Media duration at the action time; zero when the source did not expose it. */
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long = 0L,

    /** Actual active playback elapsed since the preceding memory event. */
    @ColumnInfo(name = "listened_ms")
    val listenedMs: Long = 0L,

    @ColumnInfo(name = "title")
    val title: String = "",

    @ColumnInfo(name = "artist")
    val artist: String = "",

    @ColumnInfo(name = "artwork_url")
    val artworkUrl: String = "",
)
