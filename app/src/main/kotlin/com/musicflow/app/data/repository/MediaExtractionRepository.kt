package com.musicflow.app.data.repository

/**
 * Repository interface for resolving audio stream URLs from video IDs.
 *
 * Implementations handle the network communication with the upstream
 * YouTube Music API, parse the response, and return a direct audio
 * stream URL suitable for ExoPlayer consumption.
 *
 * Threading contract:
 * - All methods are `suspend` functions — callers may invoke them from
 *   any coroutine context.
 * - Implementations must dispatch network I/O to [Dispatchers.IO].
 * - No `runBlocking` is permitted in implementations.
 *
 * Error handling:
 * - Network failures, malformed responses, and missing audio formats
 *   are wrapped in [Result.failure] rather than thrown.
 */
interface MediaExtractionRepository {

    /**
     * Resolves the highest-quality audio-only stream URL for the given
     * YouTube video ID.
     *
     * Format priority:
     * 1. itag 251 — Opus audio (highest bitrate, ~256 kbps)
     * 2. itag 140 — AAC audio (fallback, ~128 kbps)
     *
     * If neither format is available (e.g. video is unavailable, region-
     * locked, or has no adaptive formats), the result is a
     * [Result.failure] containing an explanatory exception.
     *
     * @param videoId The YouTube video ID (e.g. "dQw4w9WgXcQ").
     * @return [Result.success] with the direct audio stream URL,
     *         or [Result.failure] with the cause of resolution failure.
     */
    suspend fun getAudioStreamUrl(videoId: String): Result<String>
}
