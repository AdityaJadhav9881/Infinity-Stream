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
    val currentItemIndex: Int = 0,
    val playbackPositionMs: Long = 0L,
    val isShuffleOn: Boolean = false,
    val repeatMode: Int = 0,
)
