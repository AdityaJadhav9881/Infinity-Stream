package com.musicflow.app.ui.visual

import androidx.compose.ui.graphics.Color

/**
 * Immutable snapshot of visual rendering state derived from audio analysis,
 * artwork colors, and playback state.
 *
 * All visual components read from this state to drive their rendering.
 */
data class VisualState(
    /** Top color of the background gradient. */
    val gradientTop: Color = Color(0xFF09090B),

    /** Bottom color of the background gradient. */
    val gradientBottom: Color = Color(0xFF09090B),

    /** Primary glow color (usually from artwork extraction). */
    val glowColor: Color = Color(0xFF1ED760),

    /** Glow intensity 0..1 driven by audio energy. */
    val glowIntensity: Float = 0f,

    /** Motion amplitude 0..1 for animation scaling. */
    val motionAmplitude: Float = 0f,

    /** Pulse rate in beats per minute derived from audio tempo. */
    val pulseRate: Float = 0f,

    /** Number of active particles in the particle visualizer (0 = degraded mode). */
    val particleDensity: Int = 0,
)
