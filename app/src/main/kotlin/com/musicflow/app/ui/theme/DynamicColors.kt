package com.musicflow.app.ui.theme

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dynamic Color Extraction from Album Artwork
 *
 * Extracts dominant colors from album artwork to create:
 * - Dynamic accent color
 * - Background gradient
 * - Player glow
 * - Button tinting
 *
 * Falls back to default accent if extraction fails.
 */
object MFDynamicColors {

    /**
     * Extract dominant colors from an image URL.
     *
     * @param imageUrl URL of the album artwork.
     * @param context Android context for image loading.
     * @return Pair of (accentColor, gradientColors) or null on failure.
     */
    suspend fun extractColors(
        imageUrl: String,
        context: android.content.Context,
    ): Pair<Color, List<Color>>? = withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()

            val result = loader.execute(request)
            if (result !is SuccessResult) return@withContext null

            val drawable = result.drawable
            val bitmap = when (drawable) {
                is BitmapDrawable -> drawable.bitmap
                else -> return@withContext null
            }

            val palette = Palette.from(bitmap).generate()

            // Extract vibrant color as accent
            val vibrant = palette.vibrantSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: palette.mutedSwatch?.rgb
                ?: return@withContext null

            val accent = Color(vibrant)

            // Extract dark vibrant for gradient
            val darkVibrant = palette.darkVibrantSwatch?.rgb
            val darkMuted = palette.darkMutedSwatch?.rgb
            val gradientBase = darkVibrant ?: darkMuted ?: vibrant

            val gradient = listOf(
                Color(gradientBase),
                Color(gradientBase).copy(alpha = 0.6f),
            )

            Pair(accent, gradient)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Update the global dynamic colors from an image URL.
     *
     * @param imageUrl URL of the album artwork.
     * @param context Android context.
     */
    suspend fun updateFromArtwork(
        imageUrl: String,
        context: android.content.Context,
    ) {
        val colors = extractColors(imageUrl, context) ?: return
        val (accent, gradient) = colors
        MFColors.updateDynamicColors(
            accent = accent,
            glow = accent.copy(alpha = 0.30f),
            gradient = gradient,
        )
    }

    /**
     * Reset dynamic colors to defaults (e.g., when no track is playing).
     */
    fun reset() {
        MFColors.resetDynamicColors()
    }
}
