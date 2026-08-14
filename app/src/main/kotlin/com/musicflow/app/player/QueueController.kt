package com.musicflow.app.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController

/**
 * Clean interface for queue operations.
 * Wraps MediaController for add, remove, move, and query operations.
 */
interface QueueController {

    /** Adds a track to the end of the queue. */
    fun addToQueue(mediaItem: MediaItem)

    /** Inserts a track to play next (after the current track). */
    fun playNext(mediaItem: MediaItem)

    /** Removes the track at the given index from the queue. */
    fun removeFromQueue(index: Int)

    /** Moves a track from one position to another in the queue. */
    fun moveItem(fromIndex: Int, toIndex: Int)

    /** Skips to a specific item in the queue by index. */
    fun skipToItem(index: Int)

    /** Returns the current queue as a list of MediaItems. */
    fun getCurrentQueue(): List<MediaItem>

    /** Returns the current playback index within the queue. */
    val currentMediaItemIndex: Int

    /** Returns the total number of items in the queue. */
    val mediaItemCount: Int
}

/**
 * Default implementation of [QueueController] that wraps a MediaController.
 */
class MediaControllerQueueController(
    private val controller: MediaController,
) : QueueController {

    override fun addToQueue(mediaItem: MediaItem) {
        controller.addMediaItem(controller.mediaItemCount, mediaItem)
    }

    override fun playNext(mediaItem: MediaItem) {
        val insertIndex = controller.currentMediaItemIndex + 1
        controller.addMediaItem(insertIndex, mediaItem)
    }

    override fun removeFromQueue(index: Int) {
        val targetIndex = controller.currentMediaItemIndex + 1 + index
        if (targetIndex in 0 until controller.mediaItemCount) {
            controller.removeMediaItem(targetIndex)
        }
    }

    override fun moveItem(fromIndex: Int, toIndex: Int) {
        val absFrom = controller.currentMediaItemIndex + 1 + fromIndex
        val absTo = controller.currentMediaItemIndex + 1 + toIndex
        if (absFrom in 0 until controller.mediaItemCount &&
            absTo in 0 until controller.mediaItemCount
        ) {
            controller.moveMediaItem(absFrom, absTo)
        }
    }

    override fun skipToItem(index: Int) {
        val targetIndex = controller.currentMediaItemIndex + 1 + index
        if (targetIndex in 0 until controller.mediaItemCount) {
            controller.seekTo(targetIndex, 0)
        }
    }

    override fun getCurrentQueue(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val timeline = controller.currentTimeline
        if (timeline.windowCount > 0) {
            for (i in 0 until controller.mediaItemCount) {
                items.add(controller.getMediaItemAt(i))
            }
        }
        return items
    }

    override val currentMediaItemIndex: Int
        get() = controller.currentMediaItemIndex

    override val mediaItemCount: Int
        get() = controller.mediaItemCount
}
