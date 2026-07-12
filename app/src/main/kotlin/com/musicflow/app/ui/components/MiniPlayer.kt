package com.musicflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicflow.app.data.TrackMetadata
import com.musicflow.app.ui.theme.AccentGreen
import com.musicflow.app.ui.theme.DarkSurfaceContainer
import com.musicflow.app.ui.theme.OnBackground
import com.musicflow.app.ui.theme.OnBackgroundVariant
import com.musicflow.app.ui.theme.ProgressIndicator
import com.musicflow.app.ui.theme.ProgressTrack

/**
 * Persistent mini player bar displayed at the bottom of the screen.
 *
 * This composable shows the currently playing track and provides
 * quick access to play/pause without navigating to the full player.
 *
 * ## Layout
 * ```
 * ┌─────────────────────────────────────────┐
 * │ [Art] Title              ▶/⏸           │
 * │        Artist ──────────────────────    │
 * │             ▓▓▓▓▓▓░░░░░░░░░░░░░░░░     │  ← Progress bar
 * └─────────────────────────────────────────┘
 * ```
 *
 * ## Usage
 * Place this composable at the bottom of a `Scaffold` or `Box` layout:
 * ```kotlin
 * Box(Modifier.fillMaxSize()) {
 *     // Main content here
 *
 *     if (currentTrack != null) {
 *         MiniPlayer(
 *             track = currentTrack,
 *             isPlaying = isPlaying,
 *             progress = progress,
 *             onClick = { navigateToFullPlayer() },
 *             onPlayPause = { togglePlayPause() },
 *             modifier = Modifier.align(Alignment.BottomCenter),
 *         )
 *     }
 * }
 * ```
 *
 * @param track The current track metadata (null hides the mini player).
 * @param isPlaying Whether the player is currently playing.
 * @param progress Current progress as a float (0f to 1f).
 * @param onClick Callback when the mini player is tapped (expand to full).
 * @param onPlayPause Callback when play/pause button is tapped.
 * @param modifier Modifier applied to the outer container.
 */
@Composable
fun MiniPlayer(
    track: TrackMetadata?,
    isPlaying: Boolean,
    progress: Float,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (track == null) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurfaceContainer)
            .clickable(onClick = onClick),
    ) {
        // ── Progress Bar (thin, at top) ─────────────────────────────
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            color = ProgressIndicator,
            trackColor = ProgressTrack,
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
        )

        // ── Content Row ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album Art Thumbnail
            MiniAlbumArt(
                artworkUrl = track.artworkUrl,
                modifier = Modifier.size(48.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Track Info (expanded to fill available space)
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = track.title.ifBlank { "Unknown Title" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = track.artist.ifBlank { "Unknown Artist" },
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBackgroundVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Add to Playlist Button
            IconButton(
                onClick = onAddToPlaylist,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add to Playlist",
                    tint = AccentGreen,
                    modifier = Modifier.size(22.dp),
                )
            }

            // Play/Pause Button
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = if (isPlaying) {
                        Icons.Rounded.Pause
                    } else {
                        Icons.Rounded.PlayArrow
                    },
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = OnBackground,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

// ── Mini Album Art ──────────────────────────────────────────────────────

/**
 * Small circular album art thumbnail for the mini player.
 */
@Composable
private fun MiniAlbumArt(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(OnBackgroundVariant.copy(alpha = 0.2f)),
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
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = OnBackgroundVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
