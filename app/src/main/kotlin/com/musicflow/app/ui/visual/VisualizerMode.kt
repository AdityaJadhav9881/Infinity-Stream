package com.musicflow.app.ui.visual

/**
 * Available visualization modes for the audio-reactive visualizer.
 */
enum class VisualizerMode {
    /** Classic waveform rendering from PCM amplitude data. */
    WAVEFORM,

    /** Vertical frequency bars showing bass/mid/treble energy. */
    FREQUENCY_BARS,

    /** Particle system driven by audio energy and beat detection. */
    PARTICLES,

    /** Ambient gradient with glow effects responding to music mood. */
    ATMOSPHERIC,
}
