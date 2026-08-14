package com.musicflow.app.player

import androidx.media3.common.Player
import androidx.media3.session.MediaController

/**
 * Clean interface for playback operations, extracted from PlayerViewModel.
 * Wraps MediaController for play, pause, seek, skip, shuffle, and loop.
 */
interface PlaybackController {

    /** Starts or resumes playback. */
    fun play()

    /** Pauses playback. */
    fun pause()

    /** Toggles between play and pause. */
    fun togglePlayPause()

    /** Seeks to the specified position in the current track. */
    fun seekTo(positionMs: Long)

    /** Skips to the next track in the queue. */
    fun skipNext()

    /** Skips to the previous track (or restarts current if > 3s in). */
    fun skipPrevious()

    /** Toggles shuffle mode on/off. */
    fun toggleShuffle()

    /** Cycles loop modes: Off -> All -> One -> Off. */
    fun toggleLoop()

    /** Sets the loop mode explicitly. */
    fun setLoopMode(mode: Int)

    /** Whether playback is currently active. */
    val isPlaying: Boolean

    /** Current playback position in milliseconds. */
    val currentPosition: Long

    /** Duration of the current track in milliseconds. */
    val duration: Long

    /** Current playback state from MediaController. */
    val playbackState: Int
}

/**
 * Default implementation of [PlaybackController] that wraps a MediaController.
 */
class MediaControllerPlaybackController(
    private val controller: MediaController,
) : PlaybackController {

    override fun play() {
        controller.play()
    }

    override fun pause() {
        controller.pause()
    }

    override fun togglePlayPause() {
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    override fun seekTo(positionMs: Long) {
        controller.seekTo(positionMs)
    }

    override fun skipNext() {
        if (controller.hasNextMediaItem()) {
            controller.seekToNext()
        }
    }

    override fun skipPrevious() {
        if (controller.currentPosition > 3_000) {
            controller.seekTo(0)
        } else {
            controller.seekToPrevious()
        }
    }

    override fun toggleShuffle() {
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
    }

    override fun toggleLoop() {
        controller.repeatMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    override fun setLoopMode(mode: Int) {
        controller.repeatMode = mode
    }

    override val isPlaying: Boolean
        get() = controller.isPlaying

    override val currentPosition: Long
        get() = controller.currentPosition

    override val duration: Long
        get() = controller.duration

    override val playbackState: Int
        get() = controller.playbackState
}
