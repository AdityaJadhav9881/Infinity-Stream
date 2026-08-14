package com.musicflow.app.ui.visual

import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin
import kotlin.random.Random

/**
 * Composable that renders audio-reactive visualizations.
 *
 * Supports four modes: WAVEFORM, FREQUENCY_BARS, PARTICLES, ATMOSPHERIC.
 * Automatically degrades on weak devices by reducing particle count and
 * simplifying rendering.
 *
 * Must be used within a Compose context that provides a [VisualEngine].
 */
@Composable
fun VisualizerRenderer(
    visualEngine: VisualEngine,
    modifier: Modifier = Modifier,
    mode: VisualizerMode = VisualizerMode.ATMOSPHERIC,
) {
    val state by visualEngine.visualState.collectAsState()

    when (mode) {
        VisualizerMode.WAVEFORM -> WaveformRenderer(state, modifier)
        VisualizerMode.FREQUENCY_BARS -> FrequencyBarsRenderer(state, modifier)
        VisualizerMode.PARTICLES -> ParticleRenderer(state, modifier)
        VisualizerMode.ATMOSPHERIC -> AtmosphericRenderer(state, modifier)
    }
}

@Composable
private fun WaveformRenderer(state: VisualState, modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val amplitude = state.motionAmplitude * height * 0.3f

        val path = Path().apply {
            moveTo(0f, centerY)
            var x = 0f
            while (x <= width) {
                val y = centerY + sin(x * 0.02f + phase) * amplitude *
                    (1f + state.glowIntensity * 0.5f)
                lineTo(x, y)
                x += 2f
            }
        }

        drawPath(
            path = path,
            color = state.glowColor,
            style = Stroke(width = 3f),
        )

        // Glow layer
        drawPath(
            path = path,
            color = state.glowColor.copy(alpha = state.glowIntensity * 0.3f),
            style = Stroke(width = 8f),
        )
    }
}

@Composable
private fun FrequencyBarsRenderer(state: VisualState, modifier: Modifier) {
    val barCount = 32

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val barWidth = width / barCount * 0.7f
        val gap = width / barCount * 0.3f

        for (i in 0 until barCount) {
            val fraction = i.toFloat() / barCount
            val energy = when {
                fraction < 0.33f -> state.glowIntensity * 0.8f
                fraction < 0.66f -> state.glowIntensity * 0.6f
                else -> state.glowIntensity * 0.4f
            }
            val barHeight = energy * height * 0.8f + height * 0.02f

            val x = i * (barWidth + gap) + gap / 2
            val top = height - barHeight

            drawRect(
                color = state.glowColor.copy(alpha = 0.6f + energy * 0.4f),
                topLeft = Offset(x, top),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
            )
        }
    }
}

@Composable
private fun ParticleRenderer(state: VisualState, modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "particlePhase",
    )

    val particles = remember(state.particleDensity, animPhase) {
        List(state.particleDensity.coerceAtLeast(1)) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 6f + 2f,
                speed = Random.nextFloat() * 0.5f + 0.2f,
                alpha = Random.nextFloat() * 0.8f + 0.2f,
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        for (particle in particles) {
            val x = ((particle.x * width + animPhase * particle.speed * 50f) % width)
            val y = ((particle.y * height - animPhase * particle.speed * 20f + height) % height)
            val effectiveAlpha = particle.alpha * state.glowIntensity

            drawCircle(
                color = state.glowColor.copy(alpha = effectiveAlpha),
                radius = particle.size * (1f + state.motionAmplitude * 0.5f),
                center = Offset(x, y),
            )
        }
    }
}

@Composable
private fun AtmosphericRenderer(state: VisualState, modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "atmospheric")
    val glowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPhase",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Background gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(state.gradientTop, state.gradientBottom),
            ),
        )

        // Central glow orb
        val glowRadius = (width * 0.3f + state.glowIntensity * width * 0.2f) *
            (0.9f + glowPhase * 0.1f)
        val glowAlpha = state.glowIntensity * 0.4f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    state.glowColor.copy(alpha = glowAlpha),
                    state.glowColor.copy(alpha = 0f),
                ),
                center = Offset(width / 2f, height / 2f),
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = Offset(width / 2f, height / 2f),
        )

        // Subtle secondary glow
        if (state.glowIntensity > 0.3f) {
            val secondaryRadius = glowRadius * 0.6f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        state.glowColor.copy(alpha = glowAlpha * 0.5f),
                        state.glowColor.copy(alpha = 0f),
                    ),
                    center = Offset(width * 0.7f, height * 0.3f),
                    radius = secondaryRadius,
                ),
                radius = secondaryRadius,
                center = Offset(width * 0.7f, height * 0.3f),
            )
        }
    }
}

private data class Particle(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float,
)
