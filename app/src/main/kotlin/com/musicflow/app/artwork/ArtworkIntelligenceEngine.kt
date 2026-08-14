package com.musicflow.app.artwork

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.imageLoader
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Enhanced artwork intelligence engine using Android Palette API
 * for extracting dominant colors from album artwork.
 *
 * Results are cached by artwork URL to avoid redundant extraction.
 * Generates readable foreground colors based on background darkness.
 */
@Singleton
class ArtworkIntelligenceEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        private const val TAG = "ArtworkIntelligenceEngine"
        private const val SWATCH_COUNT = 16
        private const val DARK_THRESHOLD = 0.4f
    }

    /**
     * Analysis result containing extracted color information from artwork.
     */
    data class ArtworkAnalysis(
        val primary: androidx.compose.ui.graphics.Color,
        val secondary: androidx.compose.ui.graphics.Color,
        val atmospheric: androidx.compose.ui.graphics.Color,
        val glow: androidx.compose.ui.graphics.Color,
        val foreground: androidx.compose.ui.graphics.Color,
        val isDark: Boolean,
        val contrast: Float,
    )

    /** In-memory cache keyed by artwork URL. */
    private val cache = ConcurrentHashMap<String, ArtworkAnalysis>()

    /**
     * Analyzes an artwork URL and extracts dominant colors.
     *
     * @param artworkUrl The URL of the artwork image.
     * @return [ArtworkAnalysis] with extracted colors, or a default palette on failure.
     */
    suspend fun analyze(artworkUrl: String): ArtworkAnalysis = withContext(Dispatchers.IO) {
        if (artworkUrl.isBlank()) return@withContext defaultAnalysis()

        cache[artworkUrl]?.let { return@withContext it }

        try {
            val loader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(artworkUrl)
                .allowHardware(false)
                .build()

            val result = loader.execute(request)
            val bitmap = when (result) {
                is SuccessResult -> (result.drawable as? BitmapDrawable)?.bitmap
                else -> null
            }

            if (bitmap == null) {
                Log.w(TAG, "Failed to load bitmap for $artworkUrl")
                return@withContext defaultAnalysis()
            }

            val analysis = extractColors(bitmap)
            cache[artworkUrl] = analysis
            Log.d(TAG, "Extracted colors for $artworkUrl: primary=${analysis.primary}")
            analysis
        } catch (e: Exception) {
            Log.e(TAG, "analyze failed: ${e.message}", e)
            defaultAnalysis()
        }
    }

    /** Clears the in-memory cache. */
    fun clearCache() {
        cache.clear()
    }

    private fun extractColors(bitmap: Bitmap): ArtworkAnalysis {
        val palette = Palette.from(bitmap)
            .maximumColorCount(SWATCH_COUNT)
            .generate()

        val dominantSwatch = palette.dominantSwatch
        val vibrantSwatch = palette.vibrantSwatch
        val mutedSwatch = palette.mutedSwatch
        val darkMutedSwatch = palette.darkMutedSwatch

        val primaryColor = vibrantSwatch?.rgb ?: dominantSwatch?.rgb ?: 0xFF1ED760.toInt()
        val secondaryColor = mutedSwatch?.rgb ?: darkMutedSwatch?.rgb ?: 0xFFA78BFA.toInt()

        val primary = androidx.compose.ui.graphics.Color(primaryColor)
        val secondary = androidx.compose.ui.graphics.Color(secondaryColor)

        // Atmospheric: desaturated, slightly transparent version of primary
        val atmosphericHsl = FloatArray(3)
        android.graphics.Color.colorToHSV(primaryColor, atmosphericHsl)
        atmosphericHsl[1] = atmosphericHsl[1] * 0.3f // Reduce saturation
        val atmospheric = androidx.compose.ui.graphics.Color(
            android.graphics.Color.HSVToColor(180, atmosphericHsl) // Semi-transparent
        )

        // Glow: brighter, more saturated version of primary
        val glowHsl = FloatArray(3)
        android.graphics.Color.colorToHSV(primaryColor, glowHsl)
        glowHsl[2] = (glowHsl[2] * 1.3f).coerceAtMost(1f) // Increase brightness
        val glow = androidx.compose.ui.graphics.Color(
            android.graphics.Color.HSVToColor(glowHsl)
        )

        // Determine if the artwork is dark
        val luminance = (0.299 * ((primaryColor shr 16) and 0xFF) +
            0.587 * ((primaryColor shr 8) and 0xFF) +
            0.114 * (primaryColor and 0xFF)) / 255.0
        val isDark = luminance < DARK_THRESHOLD

        // Foreground: readable text color based on background darkness
        val foreground = if (isDark) {
            androidx.compose.ui.graphics.Color(0xFFF5F5F7) // Light text on dark bg
        } else {
            androidx.compose.ui.graphics.Color(0xFF09090B) // Dark text on light bg
        }

        // Contrast ratio between primary and foreground
        val contrast = computeContrast(primaryColor, if (isDark) 0xFFFFFFFF.toInt() else 0xFF09090B.toInt())

        return ArtworkAnalysis(
            primary = primary,
            secondary = secondary,
            atmospheric = atmospheric,
            glow = glow,
            foreground = foreground,
            isDark = isDark,
            contrast = contrast,
        )
    }

    private fun computeContrast(color1: Int, color2: Int): Float {
        val l1 = relativeLuminance(color1)
        val l2 = relativeLuminance(color2)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return ((lighter + 0.05f) / (darker + 0.05f))
    }

    private fun relativeLuminance(color: Int): Float {
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val rs = if (r <= 0.03928f) r / 12.92f else ((r + 0.055f) / 1.055f).pow(2.4f)
        val gs = if (g <= 0.03928f) g / 12.92f else ((g + 0.055f) / 1.055f).pow(2.4f)
        val bs = if (b <= 0.03928f) b / 12.92f else ((b + 0.055f) / 1.055f).pow(2.4f)
        return 0.2126f * rs + 0.7152f * gs + 0.0722f * bs
    }

    private fun Float.pow(exponent: Float): Float {
        return Math.pow(this.toDouble(), exponent.toDouble()).toFloat()
    }

    private fun defaultAnalysis(): ArtworkAnalysis {
        return ArtworkAnalysis(
            primary = androidx.compose.ui.graphics.Color(0xFF1ED760),
            secondary = androidx.compose.ui.graphics.Color(0xFFA78BFA),
            atmospheric = androidx.compose.ui.graphics.Color(0x301ED760),
            glow = androidx.compose.ui.graphics.Color(0xFF4EFF8A),
            foreground = androidx.compose.ui.graphics.Color(0xFFF5F5F7),
            isDark = true,
            contrast = 5.0f,
        )
    }
}
