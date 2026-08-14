package com.musicflow.app.ui.zero

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicflow.app.ui.theme.MFColors
import com.musicflow.app.ui.theme.MFGlass
import com.musicflow.app.ui.theme.MFTokens
import kotlinx.coroutines.delay

/**
 * Zero-UI immersive playback overlay.
 *
 * Displays only the album artwork, a subtle atmospheric gradient, and
 * minimal song info. Controls auto-hide after [hideDelayMs] of
 * inactivity. Touching the screen reveals them again.
 *
 * @param artworkUrl URL to the current track's artwork.
 * @param title Current track title.
 * @param artist Current artist name.
 * @param isPlaying Whether playback is active.
 * @param progress Current playback progress (0f..1f).
 * @param onPlayPause Toggle play / pause.
 * @param onSkipNext Skip to next track.
 * @param onSkipPrevious Skip to previous track.
 * @param hideDelayMs Idle time in milliseconds before controls auto-hide.
 * @param modifier Modifier applied to the root container.
 */
@Composable
fun ZeroUiOverlay(
    artworkUrl: String,
    title: String,
    artist: String,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    modifier: Modifier = Modifier,
    hideDelayMs: Long = 5_000L,
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Auto-hide timer
    LaunchedEffect(controlsVisible, lastInteractionTime) {
        if (controlsVisible) {
            delay(hideDelayMs)
            controlsVisible = false
        }
    }

    fun onUserInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        controlsVisible = true
    }

    val atmosphericColor = remember(artworkUrl) {
        MFColors.DynamicGlow
    }

    // Ambient pulse for the atmospheric gradient
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val ambientAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ambientAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MFColors.Background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onUserInteraction() },
            ),
    ) {
        // Atmospheric background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                atmosphericColor.copy(alpha = ambientAlpha),
                                Color.Transparent,
                            ),
                            center = Offset(size.width / 2f, size.height * 0.35f),
                            radius = size.width * 0.8f,
                        ),
                        radius = size.width * 0.8f,
                        center = Offset(size.width / 2f, size.height * 0.35f),
                    )
                },
        )

        // Large centered artwork
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(artworkUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(MFTokens.XLRadius),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(MFTokens.XLRadius)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MFColors.Accent.copy(alpha = 0.25f),
                                    MFColors.Accent.copy(alpha = 0.05f),
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MFColors.Accent.copy(alpha = 0.6f),
                        modifier = Modifier.size(80.dp),
                    )
                }
            }
        }

        // Controls overlay — animated show/hide
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 },
            exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { it / 2 },
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Song info
                Text(
                    text = title.ifBlank { "Unknown Title" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MFColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = artist.ifBlank { "Unknown Artist" },
                    fontSize = 14.sp,
                    color = MFColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MFColors.Accent,
                    trackColor = MFColors.ProgressTrack,
                    strokeCap = StrokeCap.Round,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Transport controls
                Row(
                    modifier = Modifier.fillMaxWidth(0.7f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ZeroUiTransportButton(
                        icon = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        size = 44.dp,
                        onClick = onSkipPrevious,
                    )

                    ZeroUiPlayPauseButton(
                        isPlaying = isPlaying,
                        onClick = onPlayPause,
                    )

                    ZeroUiTransportButton(
                        icon = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        size = 44.dp,
                        onClick = onSkipNext,
                    )
                }
            }
        }

        // Mini progress indicator at the very top — always visible
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(1.dp)),
            color = MFColors.Accent,
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Round,
        )
    }
}

// ── Transport Controls ──────────────────────────────────────────────────

@Composable
private fun ZeroUiPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessHigh,
        ),
        label = "playPauseScale",
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(MFColors.Accent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = MFColors.TextOnAccent,
            modifier = Modifier.size(30.dp),
        )
    }
}

@Composable
private fun ZeroUiTransportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessHigh,
        ),
        label = "transportScale",
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(MFColors.GlassHigh)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MFColors.TextPrimary,
            modifier = Modifier.size(22.dp),
        )
    }
}
