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
import androidx.compose.ui.unit.dp

object MFGlass {

    fun Modifier.glass(
        cornerRadius: RoundedCornerShape = MFTokens.MediumRadius,
        alpha: Float = 0.10f,
    ): Modifier = this
        .shadow(
            elevation = MFTokens.ElevationLow,
            shape = cornerRadius,
            ambientColor = Color.Black.copy(alpha = 0.30f),
            spotColor = Color.Black.copy(alpha = 0.18f),
        )
        .clip(cornerRadius)
        .background(
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = alpha),
                    Color.White.copy(alpha = alpha * 0.5f),
                )
            )
        )
        .border(
            width = 0.5.dp,
            color = MFColors.GlassBorder,
            shape = cornerRadius,
        )

    @Composable
    fun GlassPanel(
        modifier: Modifier = Modifier,
        cornerRadius: RoundedCornerShape = MFTokens.MediumRadius,
        alpha: Float = 0.10f,
        content: @Composable BoxScope.() -> Unit,
    ) {
        Box(
            modifier = modifier.glass(cornerRadius, alpha),
            content = content,
        )
    }

    @Composable
    fun MiniPlayerGlass(
        modifier: Modifier = Modifier,
        content: @Composable BoxScope.() -> Unit,
    ) {
        Box(
            modifier = modifier
                .shadow(
                    elevation = MFTokens.ElevationMedium,
                    shape = MFTokens.LargeRadius,
                    ambientColor = Color.Black.copy(alpha = 0.40f),
                    spotColor = Color.Black.copy(alpha = 0.28f),
                )
                .clip(MFTokens.LargeRadius)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.05f),
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

    @Composable
    fun BottomNavGlass(
        modifier: Modifier = Modifier,
        content: @Composable BoxScope.() -> Unit,
    ) {
        Box(
            modifier = modifier
                .shadow(
                    elevation = MFTokens.ElevationMedium,
                    shape = MFTokens.LargeRadius,
                    ambientColor = Color.Black.copy(alpha = 0.45f),
                    spotColor = Color.Black.copy(alpha = 0.32f),
                )
                .clip(MFTokens.LargeRadius)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.04f),
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
                    ambientColor = Color.Black.copy(alpha = 0.50f),
                    spotColor = Color.Black.copy(alpha = 0.38f),
                )
                .clip(MFTokens.LargeRadius)
                .background(MFColors.GlassOverlay)
                .border(
                    width = 0.5.dp,
                    color = MFColors.GlassBorder,
                    shape = MFTokens.LargeRadius,
                ),
            content = content,
        )
    }
}
