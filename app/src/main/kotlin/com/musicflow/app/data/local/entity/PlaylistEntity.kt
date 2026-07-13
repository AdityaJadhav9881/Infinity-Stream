package com.musicflow.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room [Entity] representing a user-created playlist.
 *
 * SQL schema:
 * ```sql
 * CREATE TABLE IF NOT EXISTS playlists (
 *     id          INTEGER PRIMARY KEY AUTOINCREMENT,
 *     name        TEXT    NOT NULL,
 *     created_at  INTEGER NOT NULL
 * )
 * ```
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at", defaultValue = "0")
    val updatedAt: Long = System.currentTimeMillis(),
)
