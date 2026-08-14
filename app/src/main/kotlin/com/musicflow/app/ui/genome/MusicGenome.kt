package com.musicflow.app.ui.genome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicflow.app.ui.theme.MFColors
import com.musicflow.app.ui.theme.MFGlass
import com.musicflow.app.ui.theme.MFTokens
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A single dimension of the listening genome radar chart.
 *
 * @property label Human-readable dimension name.
 * @property value Normalised value between 0f and 1f.
 */
data class GenomeDimension(
    val label: String,
    val value: Float,
)

/**
 * Listening characteristics the radar chart visualises.
 */
object GenomeDimensions {
    val Energy = "Energy"
    val Acoustic = "Acoustic"
    val Electronic = "Electronic"
    val Tempo = "Tempo"
    val Mood = "Mood"
    val Discovery = "Discovery"
}

/**
 * Computes the dominant genome dimension and returns a matching accent colour.
 */
fun dominantGenomeColor(dimensions: List<GenomeDimension>): Color {
    if (dimensions.isEmpty()) return MFColors.Accent
    val dominant = dimensions.maxByOrNull { it.value } ?: return MFColors.Accent
    return when (dominant.label) {
        GenomeDimensions.Energy -> Color(0xFFEF4444)
        GenomeDimensions.Acoustic -> Color(0xFFF59E0B)
        GenomeDimensions.Electronic -> Color(0xFF6366F1)
        GenomeDimensions.Tempo -> Color(0xFF22D3EE)
        GenomeDimensions.Mood -> Color(0xFFEC4899)
        GenomeDimensions.Discovery -> MFColors.Accent
        else -> MFColors.Accent
    }
}

/**
 * Animated radar / spider chart that visualises the user's listening genome.
 *
 * Six axes radiate from the centre — energy, acoustic, electronic, tempo,
 * mood, discovery. The polygon interpolates from 0 to each dimension value
 * on composition. Line colour shifts toward the dominant trait.
 *
 * @param dimensions The six dimension values (0f..1f each).
 * @param modifier Modifier applied to the root container.
 * @param animationDurationMillis Duration of the draw-in animation.
 */
@Composable
fun MusicGenome(
    dimensions: List<GenomeDimension>,
    modifier: Modifier = Modifier,
    animationDurationMillis: Int = 1200,
) {
    val animProgress = remember { Animatable(0f) }
    val dominantColor = remember(dimensions) { dominantGenomeColor(dimensions) }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(dimensions) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = animationDurationMillis, easing = FastOutSlowInEasing),
        )
    }

    Column(
        modifier = modifier
            .background(MFColors.Background)
            .padding(MFTokens.ScreenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MFGlass.GlassPanel(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            cornerRadius = MFTokens.LargeRadius,
            alpha = 0.06f,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                ) {
                    drawRadarChart(
                        dimensions = dimensions,
                        progress = animProgress.value,
                        dominantColor = dominantColor,
                        textMeasurer = textMeasurer,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dimension summary row
        val half = dimensions.size / 2
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            GenomeDimensionRow(dimensions.take(half))
            Spacer(modifier = Modifier.height(4.dp))
            GenomeDimensionRow(dimensions.drop(half))
        }
    }
}

@Composable
private fun GenomeDimensionRow(dims: List<GenomeDimension>) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp),
    ) {
        dims.forEach { dim ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(dim.value * 100).toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MFColors.TextPrimary,
                )
                Text(
                    text = dim.label,
                    fontSize = 10.sp,
                    color = MFColors.TextTertiary,
                )
            }
        }
    }
}

// ── Radar Chart Drawing ─────────────────────────────────────────────────

private fun DrawScope.drawRadarChart(
    dimensions: List<GenomeDimension>,
    progress: Float,
    dominantColor: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
) {
    if (dimensions.isEmpty()) return

    val count = dimensions.size
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val maxRadius = min(centerX, centerY) * 0.72f
    val angleStep = 2 * Math.PI / count

    // Concentric guide rings (0.25, 0.5, 0.75, 1.0)
    for (ring in 1..4) {
        val ringRadius = maxRadius * ring / 4f
        val ringPath = Path().apply {
            for (i in 0 until count) {
                val angle = angleStep * i - Math.PI / 2
                val x = centerX + (ringRadius * cos(angle)).toFloat()
                val y = centerY + (ringRadius * sin(angle)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(
            path = ringPath,
            color = MFColors.GlassBorder,
            style = Stroke(width = 1f),
        )
    }

    // Axis lines
    for (i in 0 until count) {
        val angle = angleStep * i - Math.PI / 2
        val endX = centerX + (maxRadius * cos(angle)).toFloat()
        val endY = centerY + (maxRadius * sin(angle)).toFloat()
        drawLine(
            color = MFColors.GlassBorder,
            start = Offset(centerX, centerY),
            end = Offset(endX, endY),
            strokeWidth = 1f,
        )
    }

    // Data polygon — animated by progress
    val dataPath = Path().apply {
        for (i in 0 until count) {
            val angle = angleStep * i - Math.PI / 2
            val dimValue = dimensions[i].value.coerceIn(0f, 1f) * progress
            val r = maxRadius * dimValue
            val x = centerX + (r * cos(angle)).toFloat()
            val y = centerY + (r * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    // Fill
    drawPath(
        path = dataPath,
        brush = Brush.radialGradient(
            colors = listOf(
                dominantColor.copy(alpha = 0.35f),
                dominantColor.copy(alpha = 0.08f),
            ),
            center = Offset(centerX, centerY),
            radius = maxRadius,
        ),
        style = Fill,
    )

    // Stroke
    drawPath(
        path = dataPath,
        color = dominantColor,
        style = Stroke(width = 2.5f, cap = StrokeCap.Round),
    )

    // Data-point dots
    for (i in 0 until count) {
        val angle = angleStep * i - Math.PI / 2
        val dimValue = dimensions[i].value.coerceIn(0f, 1f) * progress
        val r = maxRadius * dimValue
        val x = centerX + (r * cos(angle)).toFloat()
        val y = centerY + (r * sin(angle)).toFloat()
        drawCircle(
            color = dominantColor,
            radius = 5f,
            center = Offset(x, y),
        )
        drawCircle(
            color = Color(0xFF09090B),
            radius = 3f,
            center = Offset(x, y),
        )
    }

    // Axis labels
    val labelStyle = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MFColors.TextSecondary,
    )
    for (i in 0 until count) {
        val angle = angleStep * i - Math.PI / 2
        val labelRadius = maxRadius + 32f
        val lx = centerX + (labelRadius * cos(angle)).toFloat()
        val ly = centerY + (labelRadius * sin(angle)).toFloat()
        val measured = textMeasurer.measure(dimensions[i].label, labelStyle)
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                x = lx - measured.size.width / 2f,
                y = ly - measured.size.height / 2f,
            ),
        )
    }
}
