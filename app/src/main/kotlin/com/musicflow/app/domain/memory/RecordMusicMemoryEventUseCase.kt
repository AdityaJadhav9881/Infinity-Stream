package com.musicflow.app.domain.memory

import com.musicflow.app.data.TrackMetadata
import com.musicflow.app.data.repository.MusicMemoryRepository
import javax.inject.Inject

/** Domain entry point used by the playback engine and future library actions. */
class RecordMusicMemoryEventUseCase @Inject constructor(
    private val repository: MusicMemoryRepository,
) {
    suspend operator fun invoke(
        type: MusicMemoryEventType,
        track: TrackMetadata,
        positionMs: Long = 0L,
        durationMs: Long = 0L,
        listenedMs: Long = 0L,
    ) {
        repository.record(type, track, positionMs, durationMs, listenedMs)
    }
}
