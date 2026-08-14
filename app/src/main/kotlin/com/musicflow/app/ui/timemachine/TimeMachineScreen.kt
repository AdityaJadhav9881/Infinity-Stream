package com.musicflow.app.ui.timemachine

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicflow.app.ui.theme.MFColors
import com.musicflow.app.ui.theme.MFGlass
import com.musicflow.app.ui.theme.MFTokens
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Full-screen immersive Time Machine experience.
 *
 * Displays listening history as a vertically scrolling timeline with
 * date headers, track cards, and summary stats.
 *
 * @param onTrackSelected Called when the user taps a track card.
 * @param viewModel Injected [TimeMachineViewModel].
 */
@Composable
fun TimeMachineScreen(
    onTrackSelected: (String) -> Unit,
    viewModel: TimeMachineViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MFColors.Background),
    ) {
        // Header
        TimeMachineHeader(
            totalTracks = uiState.totalTracks,
            totalDurationMs = uiState.totalDurationMs,
            totalDiscoveries = uiState.totalDiscoveries,
        )

        // Period selector
        TimePeriodSelector(
            selected = uiState.selectedPeriod,
            onSelect = { viewModel.loadTimeline(it) },
        )

        // Timeline content
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = MFColors.Accent,
                    strokeWidth = 2.dp,
                )
            }
        } else if (uiState.timeline.isEmpty()) {
            EmptyTimeline()
        } else {
            TimelineContent(
                timeline = uiState.timeline,
                onTrackSelected = onTrackSelected,
            )
        }
    }
}

// ── Header ──────────────────────────────────────────────────────────────

@Composable
private fun TimeMachineHeader(
    totalTracks: Int,
    totalDurationMs: Long,
    totalDiscoveries: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MFTokens.ScreenHorizontalPadding, vertical = 24.dp),
    ) {
        Text(
            text = "Time Machine",
            fontSize = MFTokens.HeroTextSize,
            fontWeight = FontWeight.ExtraBold,
            color = MFColors.TextPrimary,
            letterSpacing = (-1).sp,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Your listening journey",
            fontSize = 13.sp,
            color = MFColors.TextTertiary,
        )
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                label = "Tracks",
                value = "$totalTracks",
                color = MFColors.Accent,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Duration",
                value = formatDuration(totalDurationMs),
                color = MFColors.Secondary,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Discovered",
                value = "$totalDiscoveries",
                color = MFColors.Tertiary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    MFGlass.GlassPanel(
        modifier = modifier,
        cornerRadius = MFTokens.MediumRadius,
        alpha = 0.08f,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = MFColors.TextTertiary,
            )
        }
    }
}

// ── Period Selector ─────────────────────────────────────────────────────

@Composable
private fun TimePeriodSelector(
    selected: TimePeriod,
    onSelect: (TimePeriod) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MFTokens.ScreenHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TimePeriod.entries.filter { it != TimePeriod.CUSTOM }.forEach { period ->
            val isSelected = period == selected
            val bg = if (isSelected) MFColors.Accent else Color.Transparent
            val textColor = if (isSelected) MFColors.TextOnAccent else MFColors.TextSecondary
            val borderColor = if (isSelected) MFColors.Accent else MFColors.GlassBorder

            Box(
                modifier = Modifier
                    .clip(MFTokens.PillRadius)
                    .background(bg)
                    .border(1.dp, borderColor, MFTokens.PillRadius)
                    .clickable { onSelect(period) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = period.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                )
            }
        }
    }
}

// ── Timeline ────────────────────────────────────────────────────────────

@Composable
private fun TimelineContent(
    timeline: List<TimelineDay>,
    onTrackSelected: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MFTokens.ScreenHorizontalPadding,
            end = MFTokens.ScreenHorizontalPadding,
            bottom = 120.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        timeline.forEach { day ->
            // Date header
            item(key = "header_${day.date}") {
                TimelineDateHeader(
                    date = day.date,
                    trackCount = day.tracks.size,
                    durationMs = day.durationMs,
                    discoveries = day.discoveries,
                )
            }

            // Track cards
            items(
                items = day.tracks,
                key = { it.event.id },
            ) { track ->
                TimelineTrackCard(
                    track = track,
                    onTrackSelected = { onTrackSelected(track.event.trackId) },
                )
            }

            // Day separator
            item(key = "sep_${day.date}") {
                TimelineSeparator()
            }
        }
    }
}

@Composable
private fun TimelineDateHeader(
    date: LocalDate,
    trackCount: Int,
    durationMs: Long,
    discoveries: Int,
) {
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val dateStr = date.format(DateTimeFormatter.ofPattern("MMM d"))
    val isToday = date == LocalDate.now()
    val isYesterday = date == LocalDate.now().minusDays(1)

    val displayDate = when {
        isToday -> "Today"
        isYesterday -> "Yesterday"
        else -> "$dayName, $dateStr"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = displayDate,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isToday) MFColors.Accent else MFColors.TextPrimary,
            )
            Text(
                text = "$trackCount tracks · ${formatDuration(durationMs)}" +
                    if (discoveries > 0) " · $discoveries new" else "",
                fontSize = 11.sp,
                color = MFColors.TextTertiary,
            )
        }

        if (isToday) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MFColors.Accent),
            )
        }
    }
}

@Composable
private fun TimelineTrackCard(
    track: TimelineTrack,
    onTrackSelected: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessHigh,
        ),
        label = "trackScale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (track.isDiscovery) MFColors.Accent.copy(alpha = 0.04f) else Color.Transparent,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onTrackSelected,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Artwork
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MFColors.GlassLow),
            contentAlignment = Alignment.Center,
        ) {
            if (track.artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(track.artworkUrl).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = MFColors.TextTertiary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Track info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MFColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (track.isDiscovery) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MFColors.Accent.copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "NEW",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MFColors.Accent,
                        )
                    }
                }
            }
            Text(
                text = track.artist,
                fontSize = 12.sp,
                color = MFColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Event time
        val eventTime = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
            .format(java.util.Date(track.event.occurredAt))
        Text(
            text = eventTime,
            fontSize = 11.sp,
            color = MFColors.TextTertiary,
        )
    }
}

@Composable
private fun TimelineSeparator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp),
        ) {
            drawLine(
                color = MFColors.Divider,
                start = Offset.Zero,
                end = Offset(size.width, 0f),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
            )
        }
    }
}

// ── Empty State ─────────────────────────────────────────────────────────

@Composable
private fun EmptyTimeline() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = MFColors.TextTertiary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No listening history",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MFColors.TextSecondary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Play some music to see your timeline here.",
                fontSize = 13.sp,
                color = MFColors.TextTertiary,
            )
        }
    }
}

// ── Utility ─────────────────────────────────────────────────────────────

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0m"
    val totalMinutes = ms / 1000 / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}
