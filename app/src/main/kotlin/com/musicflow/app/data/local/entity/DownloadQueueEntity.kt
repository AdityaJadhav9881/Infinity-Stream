package com.musicflow.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_queue")
data class DownloadQueueEntity(
    @PrimaryKey
    @ColumnInfo(name = "song_id")
    val songId: String,

    val title: String,
    val artist: String,
    val artworkUrl: String,
    val streamingUrl: String = "",

    @ColumnInfo(name = "status")
    val status: String = "QUEUED", // QUEUED, DOWNLOADING, COMPLETED, FAILED, PAUSED, CANCELLED

    @ColumnInfo(name = "progress")
    val progress: Float = 0f,

    @ColumnInfo(name = "speed_bytes_per_sec")
    val speedBytesPerSec: Long = 0L,

    @ColumnInfo(name = "total_bytes")
    val totalBytes: Long = 0L,

    @ColumnInfo(name = "downloaded_bytes")
    val downloadedBytes: Long = 0L,

    @ColumnInfo(name = "error_reason")
    val errorReason: String = "",

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "started_at")
    val startedAt: Long = 0L,

    @ColumnInfo(name = "completed_at")
    val completedAt: Long = 0L,

    @ColumnInfo(name = "local_file_path")
    val localFilePath: String = "",
)
