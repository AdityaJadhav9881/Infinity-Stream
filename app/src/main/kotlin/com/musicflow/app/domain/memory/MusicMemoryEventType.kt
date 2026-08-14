package com.musicflow.app.domain.memory

/**
 * Stable event vocabulary for Music Memory.
 *
 * Storage uses explicit strings instead of ordinal values so that adding a
 * new event never changes the interpretation of existing offline data.
 */
enum class MusicMemoryEventType(val storageValue: String) {
    PLAYBACK_STARTED("playback_started"),
    PLAYBACK_PAUSED("playback_paused"),
    PLAYBACK_COMPLETED("playback_completed"),
    PLAYBACK_SKIPPED("playback_skipped"),
    PLAYBACK_ERROR("playback_error"),
    FAVORITED("favorited"),
    PLAYLIST_ADDED("playlist_added"),
    DOWNLOADED("downloaded"),
}
