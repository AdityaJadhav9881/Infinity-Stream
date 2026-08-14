package com.musicflow.app.ui.visual

import com.musicflow.app.audio.AudioState
import com.musicflow.app.artwork.ArtworkIntelligenceEngine
import com.musicflow.app.utils.PerformanceMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Combines audio analysis, artwork colors, and playback state into
 * a unified [VisualState] that drives all visual components.
 *
 * The engine applies smoothing, intensity scaling, and performance-based
 * degradation to ensure smooth rendering on all devices.
 */
@Singleton
class VisualEngine @Inject constructor(
    private val performanceMonitor: PerformanceMonitor,
) {

    companion object {
        private const val SMOOTHING = 0.25f
        private const val BASE_PARTICLE_COUNT = 60
        private const val LOW_END_PARTICLE_COUNT = 20
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _visualState = MutableStateFlow(VisualState())
    val visualState: StateFlow<VisualState> = _visualState.asStateFlow()

    @Volatile
    private var currentGlowColor = ArtworkIntelligenceEngine.ArtworkAnalysis(
        primary = androidx.compose.ui.graphics.Color(0xFF1ED760),
        secondary = androidx.compose.ui.graphics.Color(0xFFA78BFA),
        atmospheric = androidx.compose.ui.graphics.Color(0x301ED760),
        glow = androidx.compose.ui.graphics.Color(0xFF4EFF8A),
        foreground = androidx.compose.ui.graphics.Color(0xFFF5F5F7),
        isDark = true,
        contrast = 5.0f,
    )

    @Volatile
    private var isPlaying = false

    /**
     * Updates the visual state from new audio analysis data.
     */
    fun update(
        audioState: AudioState,
        artworkAnalysis: ArtworkIntelligenceEngine.ArtworkAnalysis,
        isPlaying: Boolean,
    ) {
        this.isPlaying = isPlaying
        this.currentGlowColor = artworkAnalysis

        val targetGlowIntensity = if (isPlaying) audioState.energy else 0f
        val targetMotionAmplitude = if (isPlaying) audioState.amplitude else 0f

        val prev = _visualState.value
        val perfLevel = performanceMonitor.getPerformanceLevel()

        val particleDensity = when (perfLevel) {
            PerformanceMonitor.PerformanceLevel.LOW -> LOW_END_PARTICLE_COUNT
            PerformanceMonitor.PerformanceLevel.MEDIUM -> BASE_PARTICLE_COUNT / 2
            PerformanceMonitor.PerformanceLevel.HIGH -> BASE_PARTICLE_COUNT
        }

        val smoothedGlow = lerp(prev.glowIntensity, targetGlowIntensity, SMOOTHING)
        val smoothedMotion = lerp(prev.motionAmplitude, targetMotionAmplitude, SMOOTHING)

        // Gradient colors from artwork, blended with audio energy
        val energyBlend = min(audioState.energy * 0.3f, 0.3f)
        val gradientTop = artworkAnalysis.primary.copy(alpha = 1f - energyBlend)
        val gradientBottom = artworkAnalysis.atmospheric

        _visualState.value = VisualState(
            gradientTop = gradientTop,
            gradientBottom = gradientBottom,
            glowColor = artworkAnalysis.glow,
            glowIntensity = smoothedGlow,
            motionAmplitude = smoothedMotion,
            pulseRate = audioState.energy * 120f, // Scale to ~120 BPM range
            particleDensity = particleDensity,
        )
    }

    /**
     * Resets visual state to defaults (used when playback stops).
     */
    fun reset() {
        scope.launch {
            _visualState.value = VisualState()
        }
    }

    private fun lerp(start: Float, stop: Float, fraction: Float): Float {
        return start + (stop - start) * fraction
    }
}
