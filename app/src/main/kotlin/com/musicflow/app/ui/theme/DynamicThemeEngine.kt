package com.musicflow.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.musicflow.app.audio.AudioState
import com.musicflow.app.artwork.ArtworkIntelligenceEngine
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
 * Manages dynamic theme colors derived from album artwork and audio energy.
 *
 * Blends artwork-extracted colors with real-time audio analysis to produce
 * ambient, music-reactive theme colors. All theme-consuming components
 * read from [dynamicThemeState].
 */
@Singleton
class DynamicThemeEngine @Inject constructor() {

    companion object {
        private const val BLEND_SPEED = 0.08f
        private const val ENERGY_BLEND_FACTOR = 0.15f
    }

    /**
     * Current dynamic theme state.
     */
    data class DynamicThemeState(
        val primaryAccent: Color = Color(0xFF1ED760),
        val secondaryAccent: Color = Color(0xFFA78BFA),
        val atmosphericColor: Color = Color(0x301ED760),
        val glowColor: Color = Color(0xFF4EFF8A),
        val backgroundColor: Color = Color(0xFF09090B),
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _dynamicThemeState = MutableStateFlow(DynamicThemeState())
    val dynamicThemeState: StateFlow<DynamicThemeState> = _dynamicThemeState.asStateFlow()

    @Volatile
    private var lastArtworkAnalysis: ArtworkIntelligenceEngine.ArtworkAnalysis? = null

    /**
     * Updates theme colors from artwork analysis.
     * This sets the base palette — audio energy further modulates it.
     */
    fun updateFromArtwork(analysis: ArtworkIntelligenceEngine.ArtworkAnalysis) {
        lastArtworkAnalysis = analysis
        scope.launch {
            _dynamicThemeState.value = DynamicThemeState(
                primaryAccent = analysis.primary,
                secondaryAccent = analysis.secondary,
                atmosphericColor = analysis.atmospheric,
                glowColor = analysis.glow,
                backgroundColor = if (analysis.isDark) Color(0xFF09090B) else Color(0xFFF5F5F7),
            )
        }
    }

    /**
     * Updates theme colors from audio analysis state.
     * Blends audio energy into the existing artwork-derived palette.
     */
    fun updateFromAudio(audioState: AudioState) {
        val artwork = lastArtworkAnalysis ?: return
        val energy = audioState.energy

        scope.launch {
            val prev = _dynamicThemeState.value

            // Modulate glow intensity with audio energy
            val glowAlpha = min(0.3f + energy * ENERGY_BLEND_FACTOR, 0.6f)
            val modulatedGlow = artwork.glow.copy(alpha = glowAlpha)

            // Slightly shift primary accent hue with bass energy
            val bassShift = audioState.bass * 0.05f
            val shiftedPrimary = lerpColor(artwork.primary, modulatedGlow, bassShift)

            _dynamicThemeState.value = prev.copy(
                primaryAccent = lerpColor(prev.primaryAccent, shiftedPrimary, BLEND_SPEED),
                secondaryAccent = lerpColor(prev.secondaryAccent, artwork.secondary, BLEND_SPEED),
                atmosphericColor = artwork.atmospheric.copy(alpha = 0.2f + energy * 0.15f),
                glowColor = lerpColor(prev.glowColor, modulatedGlow, BLEND_SPEED),
            )
        }
    }

    /**
     * Resets all dynamic colors to defaults.
     */
    fun reset() {
        lastArtworkAnalysis = null
        scope.launch {
            _dynamicThemeState.value = DynamicThemeState()
        }
    }

    private fun lerpColor(from: Color, to: Color, fraction: Float): Color {
        return Color(
            red = from.red + (to.red - from.red) * fraction,
            green = from.green + (to.green - from.green) * fraction,
            blue = from.blue + (to.blue - from.blue) * fraction,
            alpha = from.alpha + (to.alpha - from.alpha) * fraction,
        )
    }
}
