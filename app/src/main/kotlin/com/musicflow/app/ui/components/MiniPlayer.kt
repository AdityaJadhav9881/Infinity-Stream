package com.musicflow.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.musicflow.app.ui.theme.MFGlass
import com.musicflow.app.ui.theme.MFTokens

/**
 * Premium Mini Player — Floating glass card with luxurious spacing.
 *
 * Design principles:
 * - Generous whitespace between all elements
 * - Artwork and waveform grouped on left
 * - Play button floats near right edge with room to breathe
 * - Every element has space to exist
 * - Feels calm, not compressed
 */
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

    // Waveform animation
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar1",
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar2",
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bar3",
    )

    // Play button scale animation
    val playScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "playScale",
    )

    MFGlass.MiniPlayerGlass(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
    ) {
        // Progress bar at top
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            color = MFColors.Accent,
            trackColor = MFColors.ProgressTrack,
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopCenter),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 20.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album Art with glow
            Box(contentAlignment = Alignment.Center) {
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MFColors.AccentGlow)
                    )
                }
                MiniAlbumArt(
                    artworkUrl = track.artworkUrl,
                    isPlaying = isPlaying,
                    modifier = Modifier.size(50.dp),
                )
            }

            // Waveform (close to artwork)
            if (isPlaying) {
                Spacer(modifier = Modifier.width(14.dp))
                Row(
                    modifier = Modifier
                        .width(20.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MFColors.Accent.copy(alpha = 0.10f))
                        .padding(horizontal = 4.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.5.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Box(Modifier.weight(1f).fillMaxWidth().height((bar1 * 14).dp.coerceAtMost(14.dp)).clip(RoundedCornerShape(1.5.dp)).background(MFColors.Accent))
                    Box(Modifier.weight(1f).fillMaxWidth().height((bar2 * 14).dp.coerceAtMost(14.dp)).clip(RoundedCornerShape(1.5.dp)).background(MFColors.Accent))
                    Box(Modifier.weight(1f).fillMaxWidth().height((bar3 * 14).dp.coerceAtMost(14.dp)).clip(RoundedCornerShape(1.5.dp)).background(MFColors.Accent))
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Track Info — fills available space
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title.ifBlank { "Unknown Title" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MFColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artist.ifBlank { "Unknown Artist" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MFColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Play/Pause — floats near right edge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .scale(playScale)
                    .shadow(
                        elevation = MFTokens.ElevationLow,
                        shape = CircleShape,
                        ambientColor = MFColors.Accent.copy(alpha = 0.25f),
                        spotColor = MFColors.Accent.copy(alpha = 0.15f),
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
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun MiniAlbumArt(
    artworkUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MFColors.Elevated)
            .border(
                width = if (isPlaying) 1.5.dp else 0.dp,
                color = if (isPlaying) MFColors.Accent.copy(alpha = 0.25f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (artworkUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artworkUrl)
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
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
