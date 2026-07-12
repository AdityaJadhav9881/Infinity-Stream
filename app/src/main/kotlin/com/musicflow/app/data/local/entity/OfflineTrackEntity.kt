package com.musicflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_tracks")
data class OfflineTrackEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val localFilePath: String,
    val fileSize: Long,
    val downloadedAt: Long = System.currentTimeMillis(),
)
