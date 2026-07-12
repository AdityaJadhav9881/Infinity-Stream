package com.musicflow.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room [Entity] storing lyrics for a track.
 *
 * Stores both plain text lyrics and optional LRC (synchronized lyrics)
 * format. LRC format includes timing information for karaoke-style display.
 *
 * SQL schema:
 * ```sql
 * CREATE TABLE IF NOT EXISTS lyrics (
 *     song_id TEXT PRIMARY KEY NOT NULL,
 *     plain_text TEXT,
 *     synced_lrc TEXT,
 *     fetched_at INTEGER NOT NULL
 * )
 * ```
 */
@Entity(tableName = "lyrics")
data class LyricsEntity(

    /** YouTube video ID — primary key, references the track. */
    @PrimaryKey
    @ColumnInfo(name = "song_id")
    val songId: String,

    /** Plain text lyrics without timing information. */
    @ColumnInfo(name = "plain_text")
    val plainText: String? = null,

    /** Synchronized LRC format lyrics with timing tags. */
    @ColumnInfo(name = "synced_lrc")
    val syncedLrc: String? = null,

    /** Epoch millis when lyrics were fetched. */
    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Long = System.currentTimeMillis()
)