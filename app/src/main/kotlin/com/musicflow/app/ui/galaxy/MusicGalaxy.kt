package com.musicflow.app.ui.galaxy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.musicflow.app.ui.theme.MFColors
import kotlin.math.cos
import kotlin.math.sin

private const val LONG_PRESS_DURATION_MS = 500L

/**
 * Interactive 2-D canvas that renders the user's library as a galaxy of nodes.
 *
 * Each [GalaxyNode] is a tappable circle with artwork, glow, and label.
 * [GalaxyEdge]s are semi-transparent connecting lines between related nodes.
 *
 * Built-in gestures:
 * - **Tap** → [onNodeTap]
 * - **Long press** → [onNodeLongPress]
 * - **Pinch-zoom** → zooms the canvas
 * - **Pan** → drags the canvas viewport
 *
 * @param nodes List of nodes to render.
 * @param edges Connecting lines between nodes.
 * @param onNodeTap Called when a node is tapped.
 * @param onNodeLongPress Called when a node is long-pressed.
 * @param modifier Modifier applied to the root Canvas.
 */
@Composable
fun MusicGalaxy(
    nodes: List<GalaxyNode>,
    edges: List<GalaxyEdge>,
    onNodeTap: (GalaxyNode) -> Unit,
    onNodeLongPress: (GalaxyNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewportOffset by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }

    val textMeasurer = rememberTextMeasurer()

    // Long press state — tracks press start time and which node was pressed
    var pressStartTime by remember { mutableLongStateOf(0L) }
    var pressedNodeId by remember { mutableStateOf<String?>(null) }
    var longPressFired by remember { mutableStateOf(false) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        zoom = (zoom * zoomChange).coerceIn(0.3f, 4f)
        viewportOffset += panChange / zoom
    }

    Box(
        modifier = modifier
            .background(MFColors.Background)
            .transformable(state = transformState),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            for (change in event.changes) {
                                val canvasSize = size
                                val worldPos = (change.position - Offset(canvasSize.width / 2f, canvasSize.height / 2f)) / zoom - viewportOffset

                                when (event.type) {
                                    PointerEventType.Press -> {
                                        val hitNode = nodes.firstOrNull { node ->
                                            (node.position - worldPos).getDistance() < node.size / 2f
                                        }
                                        if (hitNode != null) {
                                            pressStartTime = System.currentTimeMillis()
                                            pressedNodeId = hitNode.id
                                            longPressFired = false
                                        }
                                        change.consume()
                                    }

                                    PointerEventType.Move -> {
                                        if (pressedNodeId != null && !longPressFired) {
                                            val elapsed = System.currentTimeMillis() - pressStartTime
                                            if (elapsed >= LONG_PRESS_DURATION_MS) {
                                                val hitNode = nodes.find { it.id == pressedNodeId }
                                                if (hitNode != null) {
                                                    longPressFired = true
                                                    onNodeLongPress(hitNode)
                                                }
                                            }
                                        }
                                        change.consume()
                                    }

                                    PointerEventType.Release -> {
                                        if (pressedNodeId != null && !longPressFired) {
                                            val elapsed = System.currentTimeMillis() - pressStartTime
                                            if (elapsed < LONG_PRESS_DURATION_MS) {
                                                val hitNode = nodes.find { it.id == pressedNodeId }
                                                if (hitNode != null) {
                                                    onNodeTap(hitNode)
                                                }
                                            }
                                        }
                                        pressedNodeId = null
                                        pressStartTime = 0L
                                        longPressFired = false
                                        change.consume()
                                    }
                                }
                            }
                        }
                    }
                },
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)

            withTransform({
                translate(left = center.x, top = center.y)
                scale(scaleX = zoom, scaleY = zoom, pivot = Offset.Zero)
                translate(left = -center.x + viewportOffset.x, top = -center.y + viewportOffset.y)
            }) {
                // Draw edges first (behind nodes)
                edges.forEach { edge ->
                    val from = nodes.find { it.id == edge.fromId }
                    val to = nodes.find { it.id == edge.toId }
                    if (from != null && to != null) {
                        drawEdge(from.position, to.position, edge.strength)
                    }
                }

                // Draw nodes
                nodes.forEach { node ->
                    drawGalaxyNode(node, textMeasurer)
                }
            }
        }
    }
}

// ── Drawing Helpers ──────────────────────────────────────────────────────

private fun DrawScope.drawEdge(from: Offset, to: Offset, strength: Float) {
    val alpha = (0.08f + strength * 0.22f).coerceIn(0f, 1f)
    drawLine(
        color = MFColors.Accent.copy(alpha = alpha),
        start = from,
        end = to,
        strokeWidth = 1f,
    )
}

private fun DrawScope.drawGalaxyNode(node: GalaxyNode, textMeasurer: TextMeasurer) {
    val radius = node.size / 2f
    val glowRadius = radius * 1.8f

    // Outer glow
    drawCircle(
        brush = androidx.compose.ui.graphics.Brush.radialGradient(
            colors = listOf(
                node.color.copy(alpha = 0.18f * node.weight),
                node.color.copy(alpha = 0f),
            ),
            center = node.position,
            radius = glowRadius,
        ),
        radius = glowRadius,
        center = node.position,
    )

    // Artwork placeholder — dark circle with colored border
    drawCircle(
        color = Color(0xFF1A1A1E),
        radius = radius,
        center = node.position,
    )

    // Node ring
    drawCircle(
        color = node.color.copy(alpha = 0.7f),
        radius = radius,
        center = node.position,
        style = Stroke(width = 2f),
    )

    // Weight indicator — filled arc slice
    if (node.weight > 0f) {
        val sweepAngle = 360f * node.weight.coerceIn(0f, 1f)
        val arcPath = Path().apply {
            addArc(
                oval = Rect(
                    center = node.position,
                    radius = radius - 2f,
                ),
                startAngleDegrees = -90f,
                sweepAngleDegrees = sweepAngle,
            )
        }
        drawPath(
            path = arcPath,
            color = node.color.copy(alpha = 0.4f),
            style = Stroke(width = 3f),
        )
    }

    // Type badge — small dot in the bottom-right of the node circle
    val badgeOffset = Offset(
        x = node.position.x + radius * 0.65f,
        y = node.position.y + radius * 0.65f,
    )
    drawCircle(
        color = node.type.defaultColor(),
        radius = 6f,
        center = badgeOffset,
    )
    drawCircle(
        color = MFColors.Background,
        radius = 4f,
        center = badgeOffset,
    )
    drawCircle(
        color = node.type.defaultColor(),
        radius = 2.5f,
        center = badgeOffset,
    )

    // Label
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = MFColors.TextSecondary,
    )
    val measured = textMeasurer.measure(node.label, labelStyle)
    val labelPos = Offset(
        x = node.position.x - measured.size.width / 2f,
        y = node.position.y + radius + 10f,
    )
    drawText(textLayoutResult = measured, topLeft = labelPos)
}

/**
 * Generates initial galaxy node positions using a simple circular layout.
 *
 * Nodes are distributed evenly around a circle with radius proportional
 * to the total count. This avoids a force-directed simulation startup cost
 * while still looking organic.
 *
 * @param nodes Nodes whose [GalaxyNode.position] will be updated.
 * @return New list with computed positions.
 */
fun generateInitialPositions(nodes: List<GalaxyNode>): List<GalaxyNode> {
    if (nodes.isEmpty()) return emptyList()
    val count = nodes.size
    val baseRadius = 200f + count * 15f
    val angleStep = 2 * Math.PI / count

    return nodes.mapIndexed { index, node ->
        val angle = angleStep * index - Math.PI / 2
        val r = baseRadius * (0.7f + 0.3f * node.weight)
        node.copy(
            position = Offset(
                x = (r * cos(angle)).toFloat(),
                y = (r * sin(angle)).toFloat(),
            ),
        )
    }
}
