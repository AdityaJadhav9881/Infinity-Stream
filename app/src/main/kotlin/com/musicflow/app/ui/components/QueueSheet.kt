package com.musicflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicflow.app.data.TrackMetadata
import com.musicflow.app.ui.theme.AccentGreen
import com.musicflow.app.ui.theme.DarkSurface
import com.musicflow.app.ui.theme.DarkSurfaceVariant
import com.musicflow.app.ui.theme.ErrorRed
import com.musicflow.app.ui.theme.OnBackground
import com.musicflow.app.ui.theme.OnBackgroundVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    sheetState: SheetState,
    currentTrack: TrackMetadata?,
    upcomingTracks: List<TrackMetadata>,
    isShuffleOn: Boolean,
    loopMode: Int,
    onDismiss: () -> Unit,
    onTrackSelected: (Int) -> Unit,
    onShuffleToggle: () -> Unit,
    onLoopToggle: () -> Unit,
    onMoveItem: (from: Int, to: Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            // ── Handle ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(OnBackgroundVariant.copy(alpha = 0.4f)),
                )
            }

            // ── Header ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Queue",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Shuffle button
                    IconButton(
                        label = "Shuffle",
                        isActive = isShuffleOn,
                        onClick = onShuffleToggle,
                    )

                    // Loop button
                    IconButton(
                        label = when (loopMode) {
                            0 -> "Off"
                            1 -> "All"
                            2 -> "One"
                            else -> "Off"
                        },
                        isActive = loopMode != 0,
                        onClick = onLoopToggle,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Now Playing ─────────────────────────────────────
            if (currentTrack != null) {
                Text(
                    text = "Now Playing",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentGreen,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                QueueTrackItem(
                    track = currentTrack,
                    isPlaying = true,
                    index = null,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Up Next ─────────────────────────────────────────
            if (upcomingTracks.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Up Next",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnBackgroundVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${upcomingTracks.size})",
                        fontSize = 11.sp,
                        color = OnBackgroundVariant.copy(alpha = 0.6f),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(
                        items = upcomingTracks,
                        key = { _, item -> item.songId },
                    ) { index, track ->
                        QueueTrackItem(
                            track = track,
                            isPlaying = false,
                            index = index + 1,
                            onClick = { onTrackSelected(index) },
                            onMoveUp = {
                                if (index > 0) onMoveItem(index, index - 1)
                            },
                            onMoveDown = {
                                if (index < upcomingTracks.lastIndex) onMoveItem(index, index + 1)
                            },
                            onRemove = { onRemoveFromQueue(index) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Queue is empty",
                        color = OnBackgroundVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
private fun IconButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote, // placeholder
            contentDescription = label,
            tint = if (isActive) AccentGreen else OnBackgroundVariant,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isActive) AccentGreen else OnBackgroundVariant,
        )
    }
}

@Composable
private fun QueueTrackItem(
    track: TrackMetadata,
    isPlaying: Boolean,
    index: Int?,
    onClick: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPlaying) AccentGreen.copy(alpha = 0.1f) else DarkSurfaceVariant.copy(alpha = 0.3f))
            .clickable(enabled = !isPlaying, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Index or playing indicator
        if (index != null) {
            Text(
                text = "$index",
                fontSize = 13.sp,
                color = OnBackgroundVariant.copy(alpha = 0.5f),
                modifier = Modifier.width(24.dp),
            )
        } else {
            // Pulsing indicator for current track
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AccentGreen.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = ">>",
                    fontSize = 10.sp,
                    color = AccentGreen,
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Album art
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurface),
            contentAlignment = Alignment.Center,
        ) {
            if (track.artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(track.artworkUrl).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = OnBackgroundVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontSize = 14.sp,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                color = if (isPlaying) AccentGreen else OnBackground.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                fontSize = 12.sp,
                color = OnBackgroundVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Move controls
        if (!isPlaying) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Move up",
                    tint = OnBackgroundVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(enabled = onMoveUp != null) { onMoveUp?.invoke() },
                )
                Spacer(modifier = Modifier.height(6.dp))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Move down",
                    tint = OnBackgroundVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(enabled = onMoveDown != null) { onMoveDown?.invoke() },
                )
                Spacer(modifier = Modifier.height(6.dp))
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove from queue",
                    tint = ErrorRed,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(enabled = onRemove != null) { onRemove?.invoke() },
                )
            }
        }
    }
}
