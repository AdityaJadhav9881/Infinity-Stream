package com.musicflow.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicflow.app.data.TrackMetadata
import com.musicflow.app.ui.theme.MFColors

@Composable
fun MiniPlayer(
    track: TrackMetadata?,
    isPlaying: Boolean,
    progress: Float,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onAddToPlaylist: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (track == null) return

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "miniPress",
    )

    val bar1 by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.3f,
        animationSpec = if (isPlaying) infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse) else tween(300),
        label = "bar1",
    )
    val bar2 by animateFloatAsState(
        targetValue = if (isPlaying) 0.2f else 0.6f,
        animationSpec = if (isPlaying) infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse) else tween(300),
        label = "bar2",
    )
    val bar3 by animateFloatAsState(
        targetValue = if (isPlaying) 0.9f else 0.4f,
        animationSpec = if (isPlaying) infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse) else tween(300),
        label = "bar3",
    )

    val miniPlayerShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .shadow(
                elevation = 8.dp,
                shape = miniPlayerShape,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.3f),
            )
            .clip(miniPlayerShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.04f),
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = MFColors.GlassBorder,
                shape = miniPlayerShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(MFColors.Accent)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MFColors.GlassMid)
                    .border(
                        width = if (isPlaying) 0.5.dp else 0.dp,
                        color = if (isPlaying) MFColors.Accent.copy(alpha = 0.2f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (track.artworkUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(track.artworkUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MFColors.TextTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            if (isPlaying) {
                Spacer(modifier = Modifier.width(6.dp))
                Row(
                    modifier = Modifier
                        .width(12.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MFColors.Accent.copy(alpha = 0.06f))
                        .padding(horizontal = 2.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Box(Modifier.weight(1f).fillMaxWidth().height((bar1 * 8).dp.coerceAtMost(8.dp)).clip(RoundedCornerShape(1.dp)).background(MFColors.Accent))
                    Box(Modifier.weight(1f).fillMaxWidth().height((bar2 * 8).dp.coerceAtMost(8.dp)).clip(RoundedCornerShape(1.dp)).background(MFColors.Accent))
                    Box(Modifier.weight(1f).fillMaxWidth().height((bar3 * 8).dp.coerceAtMost(8.dp)).clip(RoundedCornerShape(1.dp)).background(MFColors.Accent))
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title.ifBlank { "Unknown Title" },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MFColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artist.ifBlank { "Unknown Artist" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MFColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 15.dp),
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = CircleShape,
                        ambientColor = MFColors.Accent.copy(alpha = 0.2f),
                        spotColor = MFColors.Accent.copy(alpha = 0.1f),
                    )
                    .clip(CircleShape)
                    .background(MFColors.Accent)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MFColors.TextOnAccent,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
