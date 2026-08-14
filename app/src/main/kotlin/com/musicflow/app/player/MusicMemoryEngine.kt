package com.musicflow.app.player

import android.util.Log
import androidx.media3.common.Player
import com.musicflow.app.data.TrackMetadata
import com.musicflow.app.domain.memory.MusicMemoryEventType
import com.musicflow.app.domain.memory.RecordMusicMemoryEventUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts real Media3 lifecycle callbacks into durable Music Memory events.
 *
 * It deliberately has no UI dependency and performs persistence on an IO
 * scope.  The playback service remains the sole source of playback facts;
 * screens only observe the resulting state through repositories.
 */
@Singleton
class MusicMemoryEngine @Inject constructor(
    private val recordEvent: RecordMusicMemoryEventUseCase,
) {
    companion object {
        private const val TAG = "MusicMemoryEngine"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeTrack: TrackMetadata? = null
    private var pendingTrack: TrackMetadata? = null
    private var activePlaybackStartedAtMs: Long? = null

    fun onTrackTransition(
        nextTrack: TrackMetadata?,
        isPlaying: Boolean,
        endedNaturally: Boolean,
        previousPositionMs: Long,
        previousDurationMs: Long,
    ) {
        val previous = activeTrack
        if (previous != null && previous.songId != nextTrack?.songId) {
            emit(
                if (endedNaturally) {
                    MusicMemoryEventType.PLAYBACK_COMPLETED
                } else {
                    MusicMemoryEventType.PLAYBACK_SKIPPED
                },
                previous,
                previousPositionMs,
                previousDurationMs,
                consumeActiveListeningMs(),
            )
            activeTrack = null
        }

        pendingTrack = nextTrack
        if (isPlaying) startPendingTrack()
    }

    fun onPlaybackStarted() {
        startPendingTrack()
    }

    fun onPlaybackPaused(positionMs: Long, durationMs: Long) {
        activeTrack?.let { track ->
            emit(
                MusicMemoryEventType.PLAYBACK_PAUSED,
                track,
                positionMs,
                durationMs,
                consumeActiveListeningMs(),
            )
        }
    }

    fun onPlaybackError(positionMs: Long, durationMs: Long) {
        activeTrack?.let { track ->
            emit(
                MusicMemoryEventType.PLAYBACK_ERROR,
                track,
                positionMs,
                durationMs,
                consumeActiveListeningMs(),
            )
        }
        activeTrack = null
    }

    fun onPlaybackCompleted(positionMs: Long, durationMs: Long) {
        activeTrack?.let { track ->
            emit(
                MusicMemoryEventType.PLAYBACK_COMPLETED,
                track,
                positionMs,
                durationMs,
                consumeActiveListeningMs(),
            )
        }
        activeTrack = null
    }

    private fun startPendingTrack() {
        val track = pendingTrack ?: return
        if (activeTrack?.songId == track.songId) {
            if (activePlaybackStartedAtMs == null) {
                activePlaybackStartedAtMs = System.currentTimeMillis()
            }
            return
        }
        activeTrack = track
        activePlaybackStartedAtMs = System.currentTimeMillis()
        emit(MusicMemoryEventType.PLAYBACK_STARTED, track)
    }

    private fun emit(
        type: MusicMemoryEventType,
        track: TrackMetadata,
        positionMs: Long = 0L,
        durationMs: Long = 0L,
        listenedMs: Long = 0L,
    ) {
        scope.launch {
            try {
                recordEvent(type, track, positionMs, durationMs, listenedMs)
            } catch (error: Exception) {
                // Music history is useful, but must never interrupt playback.
                Log.w(TAG, "Unable to record ${type.storageValue}", error)
            }
        }
    }

    private fun consumeActiveListeningMs(nowMs: Long = System.currentTimeMillis()): Long {
        val startedAt = activePlaybackStartedAtMs ?: return 0L
        activePlaybackStartedAtMs = null
        return (nowMs - startedAt).coerceAtLeast(0L)
    }
}
