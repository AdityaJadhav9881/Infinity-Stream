package com.musicflow.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.sqrt

/**
 * MusicFlow Animated Components
 *
 * Micro-interactions that make every interaction feel alive:
 * - Scale on press
 * - Spring animations
 * - Glow effects
 * - Ripple effects
 */
object MFAnimations {

    /**
     * Scale modifier — card shrinks slightly when pressed.
     *
     * @param pressedScale Scale when pressed (default 0.96).
     * @param animationSpec Spring animation spec.
     */
    fun Modifier.pressScale(
        pressedScale: Float = 0.96f,
    ): Modifier = composed {
        var isPressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (isPressed) pressedScale else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh,
            ),
            label = "pressScale",
        )

        this.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    }

    /**
     * Subtle scale modifier — for larger elements like artwork.
     */
    fun Modifier.subtleScale(
        pressedScale: Float = 0.98f,
    ): Modifier = composed {
        var isPressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (isPressed) pressedScale else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh,
            ),
            label = "subtleScale",
        )

        this.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    }

    /**
     * Glow effect — draws a radial glow behind the composable.
     *
     * @param color Glow color.
     * @param radius Glow radius.
     * @param alpha Glow opacity.
     */
    fun Modifier.glow(
        color: Color = MFColors.AccentGlow,
        radius: Float = 200f,
        alpha: Float = 0.4f,
    ): Modifier = this.drawBehind {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = alpha),
                    Color.Transparent,
                ),
                center = Offset(size.width / 2, size.height / 2),
                radius = radius * density,
            ),
            radius = radius * density,
            center = Offset(size.width / 2, size.height / 2),
        )
    }

    /**
     * Accent glow — stronger glow for currently playing track.
     */
    fun Modifier.accentGlow(): Modifier = this.glow(
        color = MFColors.AccentGlow,
        radius = 250f,
        alpha = 0.35f,
    )

    /**
     * Dynamic glow — uses the current dynamic accent color.
     */
    fun Modifier.dynamicGlow(): Modifier = this.glow(
        color = MFColors.DynamicGlow,
        radius = 250f,
        alpha = 0.35f,
    )

    /**
     * MusicFlow ripple effect — subtle, premium feel.
     */
    @Composable
    fun mfRipple(
        bounded: Boolean = true,
        color: Color = MFColors.Accent,
        @Suppress("UNUSED_PARAMETER") radius: Float = 200f,
    ) = ripple(
        bounded = bounded,
        color = color,
    )
}
