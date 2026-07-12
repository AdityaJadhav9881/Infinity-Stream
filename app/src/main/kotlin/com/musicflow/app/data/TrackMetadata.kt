package com.musicflow.app.data

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.RequestMetadata
import androidx.media3.common.MediaMetadata

data class TrackMetadata(
    val songId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val resolvedStreamingUrl: String,
) {

    fun toMediaItem(): MediaItem {
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setArtworkUri(Uri.parse(artworkUrl))
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()

        val requestMetadata = RequestMetadata.Builder()
            .setMediaUri(Uri.parse(resolvedStreamingUrl))
            .build()

        return MediaItem.Builder()
            .setMediaId(songId)
            .setUri(Uri.parse(resolvedStreamingUrl))
            .setCustomCacheKey(songId)
            .setMediaMetadata(mediaMetadata)
            .setRequestMetadata(requestMetadata)
            .build()
    }

    companion object {

        fun fromMediaItem(mediaItem: MediaItem): TrackMetadata? {
            val meta = mediaItem.mediaMetadata
            val uri = mediaItem.localConfiguration?.uri?.toString() ?: return null
            val songId = mediaItem.mediaId.takeIf { it.isNotBlank() } ?: return null

            return TrackMetadata(
                songId = songId,
                title = meta.title?.toString() ?: "",
                artist = meta.artist?.toString() ?: "",
                artworkUrl = meta.artworkUri?.toString() ?: "",
                resolvedStreamingUrl = uri,
            )
        }

        fun placeholderMediaItem(songId: String, streamingUrl: String): MediaItem {
            return TrackMetadata(
                songId = songId,
                title = "",
                artist = "",
                artworkUrl = "",
                resolvedStreamingUrl = streamingUrl,
            ).toMediaItem()
        }
    }
}
