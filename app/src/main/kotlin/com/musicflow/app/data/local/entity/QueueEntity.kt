package com.musicflow.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queue")
data class QueueEntity(
    @PrimaryKey val position: Int,
    val songId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val streamingUrl: String,
)
