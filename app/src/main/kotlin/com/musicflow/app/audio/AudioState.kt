package com.musicflow.app.audio

/**
 * Immutable snapshot of real-time audio analysis output.
 *
 * All values are normalized to 0..1 range. Timestamps are epoch millis.
 */
data class AudioState(
    val amplitude: Float = 0f,
    val energy: Float = 0f,
    val bass: Float = 0f,
    val mid: Float = 0f,
    val treble: Float = 0f,
    val beatDetected: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
)
