package com.musicflow.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism composables for MusicFlow.
 *
 * Used ONLY where it improves the experience:
 * - Mini Player
 * - Bottom Navigation
 * - Dialogs
 * - Floating Menus
 * - Context Menus
 *
 * Performance-conscious: minimal blur, subtle effects.
 */
object MFGlass {

    /**
     * Glass surface modifier — semi-transparent background with subtle border.
     *
     * @param cornerRadius Shape of the glass surface.
     * @param alpha Background alpha (lower = more transparent).
     */
    fun Modifier.glass(
        cornerRadius: RoundedCornerShape = MFTokens.MediumRadius,
        alpha: Float = 0.08f,
    ): Modifier = this
        .shadow(
            elevation = MFTokens.ElevationMedium,
            shape = cornerRadius,
            ambientColor = Color.Black.copy(alpha = 0.3f),
            spotColor = Color.Black.copy(alpha = 0.2f),
        )
        .clip(cornerRadius)
        .background(MFColors.Background.copy(alpha = alpha))
        .border(
            width = 0.5.dp,
            color = MFColors.GlassBorder,
            shape = cornerRadius,
        )

    /**
     * Glass panel — a Box with glass effect applied.
     */
    @Composable
    fun GlassPanel(
        modifier: Modifier = Modifier,
        cornerRadius: RoundedCornerShape = MFTokens.MediumRadius,
        alpha: Float = 0.08f,
        content: @Composable BoxScope.() -> Unit,
    ) {
        Box(
            modifier = modifier.glass(cornerRadius, alpha),
            content = content,
        )
    }

    /**
     * Mini player glass background.
     */
    @Composable
    fun MiniPlayerGlass(
        modifier: Modifier = Modifier,
        content: @Composable BoxScope.() -> Unit,
    ) {
        Box(
            modifier = modifier
                .shadow(
                    elevation = MFTokens.ElevationHigh,
                    shape = MFTokens.LargeRadius,
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = Color.Black.copy(alpha = 0.3f),
                )
                .clip(MFTokens.LargeRadius)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MFColors.Card.copy(alpha = 0.95f),
                            MFColors.Background.copy(alpha = 0.90f),
                        )
                    )
                )
                .border(
                    width = 0.5.dp,
                    color = MFColors.GlassBorder,
                    shape = MFTokens.LargeRadius,
                ),
            content = content,
        )
    }

    /**
     * Bottom navigation glass background.
     */
    @Composable
    fun BottomNavGlass(
        modifier: Modifier = Modifier,
        content: @Composable BoxScope.() -> Unit,
    ) {
        Box(
            modifier = modifier
                .shadow(
                    elevation = MFTokens.ElevationHigh,
                    shape = MFTokens.LargeRadius,
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = Color.Black.copy(alpha = 0.3f),
                )
                .clip(MFTokens.LargeRadius)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MFColors.Background.copy(alpha = 0.92f),
                            MFColors.Background.copy(alpha = 0.85f),
                        )
                    )
                )
                .border(
                    width = 0.5.dp,
                    color = MFColors.GlassBorder,
                    shape = MFTokens.LargeRadius,
                ),
            content = content,
        )
    }

    /**
     * Dialog glass background.
     */
    @Composable
    fun DialogGlass(
        modifier: Modifier = Modifier,
        content: @Composable BoxScope.() -> Unit,
    ) {
        Box(
            modifier = modifier
                .shadow(
                    elevation = MFTokens.ElevationHigh,
                    shape = MFTokens.LargeRadius,
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = Color.Black.copy(alpha = 0.4f),
                )
                .clip(MFTokens.LargeRadius)
                .background(MFColors.Overlay)
                .border(
                    width = 0.5.dp,
                    color = MFColors.GlassBorder,
                    shape = MFTokens.LargeRadius,
                ),
            content = content,
        )
    }
}
