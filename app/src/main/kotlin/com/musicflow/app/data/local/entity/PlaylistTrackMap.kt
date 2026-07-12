package com.musicflow.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * Room [Entity] linking tracks to playlists (many-to-many).
 *
 * SQL schema:
 * ```sql
 * CREATE TABLE IF NOT EXISTS playlist_track_map (
 *     playlist_id  INTEGER NOT NULL,
 *     song_id      TEXT    NOT NULL,
 *     position     INTEGER NOT NULL,
 *     PRIMARY KEY (playlist_id, song_id)
 * )
 * ```
 */
@Entity(
    tableName = "playlist_track_map",
    primaryKeys = ["playlist_id", "song_id"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlist_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PlaylistTrackMap(
    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,

    @ColumnInfo(name = "song_id")
    val songId: String,

    @ColumnInfo(name = "position")
    val position: Int = 0,
)
