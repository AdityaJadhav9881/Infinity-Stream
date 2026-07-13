package com.musicflow.app.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
import com.musicflow.app.data.TrackMetadata
import com.musicflow.app.ui.components.LyricsOverlay
import com.musicflow.app.ui.theme.MFColors
import com.musicflow.app.ui.theme.MFGlass
import com.musicflow.app.ui.theme.MFTokens
import com.musicflow.app.ui.theme.AccentGreen
import com.musicflow.app.ui.theme.DarkSurface
import com.musicflow.app.ui.theme.OnBackground
import com.musicflow.app.ui.theme.OnBackgroundVariant
import com.musicflow.app.ui.theme.ProgressTrack
import com.musicflow.app.ui.theme.ProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Premium full-screen player — flagship design for MusicFlow.
 *
 * Top bar: Close button + playing indicator (no duplicates)
 * Tab row: Now Playing / Queue
 * Extra controls: Like, Playlist, Shuffle, Repeat, Lyrics, Sleep
 * Bottom: Playback controls (prev, play/pause, next)
 */
@Composable
fun MainPlayerScreen(
    track: TrackMetadata?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    upcomingTracks: List<TrackMetadata>,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onSwipeDown: () -> Unit,
    onClose: () -> Unit = onSwipeDown,
    isLiked: Boolean = false,
    onLikeToggle: () -> Unit = {},
    isShuffleOn: Boolean = false,
    onShuffleToggle: () -> Unit = {},
    loopMode: Int = 0,
    onLoopToggle: () -> Unit = {},
    isLyricsVisible: Boolean = false,
    onLyricsToggle: () -> Unit = {},
    onSleepTimerClick: () -> Unit = {},
    sleepTimerText: String? = null,
    onAddToPlaylist: () -> Unit = {},
    onQueueItemSelected: (Int) -> Unit = {},
    onRemoveFromQueue: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // ── Dynamic Palette ──────────────────────────────────────────────
    var dominantColor by remember { mutableStateOf(Color(0xFF1A1A2E)) }
    var darkMutedColor by remember { mutableStateOf(Color(0xFF16213E)) }
    var vibrantColor by remember { mutableStateOf(AccentGreen) }

    val animatedDominant by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 1000),
        label = "dominantColor",
    )
    val animatedDarkMuted by animateColorAsState(
        targetValue = darkMutedColor,
        animationSpec = tween(durationMillis = 1000),
        label = "darkMutedColor",
    )
    val animatedVibrant by animateColorAsState(
        targetValue = vibrantColor,
        animationSpec = tween(durationMillis = 1000),
        label = "vibrantColor",
    )

    // ── Tab State ────────────────────────────────────────────────────
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Now Playing", "Queue")

    // ── Infinite pulse for playing indicator ──────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    // ── Full-Screen Layout ───────────────────────────────────────────
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MFColors.Background)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedDominant.copy(alpha = 0.35f),
                        animatedDarkMuted.copy(alpha = 0.15f),
                        Color.Transparent,
                        Color.Transparent,
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = 900f,
                )
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Top Bar ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MFTokens.ScreenHorizontalPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Close",
                    tint = MFColors.TextPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.size(30.dp),
                )
            }
            // Center: playing indicator
            if (isPlaying) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(3) { i ->
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(animatedVibrant.copy(alpha = pulseAlpha))
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.size(40.dp))
        }

        // ── Tab Row ──────────────────────────────────────────────────
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MFColors.TextPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = animatedVibrant,
                )
            },
            divider = {},
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) MFColors.TextPrimary else MFColors.TextTertiary,
                        )
                    },
                )
            }
        }

        // ── Content ──────────────────────────────────────────────────
        when (selectedTab) {
            0 -> {
                // ── Now Playing Tab ──────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Hero Album Art ───────────────────────────────
                    Box(
                        contentAlignment = Alignment.Center,
                    ) {
                        // Ambient glow behind album art
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .aspectRatio(1f)
                                .shadow(
                                    elevation = 40.dp,
                                    shape = RoundedCornerShape(28.dp),
                                    ambientColor = animatedDominant.copy(alpha = 0.5f),
                                    spotColor = animatedVibrant.copy(alpha = 0.3f),
                                )
                                .clip(RoundedCornerShape(28.dp))
                                .background(animatedDominant.copy(alpha = 0.15f))
                        )

                        AlbumArtWithPalette(
                            artworkUrl = track?.artworkUrl,
                            onDominantColor = { dominantColor = it },
                            onDarkMutedColor = { darkMutedColor = it },
                            onVibrantColor = { vibrantColor = it },
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .aspectRatio(1f)
                                .shadow(
                                    elevation = 24.dp,
                                    shape = RoundedCornerShape(28.dp),
                                    ambientColor = animatedDominant.copy(alpha = 0.4f),
                                    spotColor = animatedDominant.copy(alpha = 0.3f),
                                ),
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Track Info + Badges ──────────────────────────
                    TrackInfoPremium(
                        title = track?.title ?: "No Track Playing",
                        artist = track?.artist ?: "—",
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Progress Slider ──────────────────────────────
                    ProgressSliderPremium(
                        currentPosition = currentPosition,
                        duration = duration,
                        onSeek = onSeek,
                        accentColor = animatedVibrant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Extra Controls Row ───────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Like
                        IconButton(onClick = onLikeToggle, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isLiked) "Unlike" else "Like",
                                tint = if (isLiked) Color(0xFFE74C3C) else MFColors.TextTertiary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        // Add to Playlist
                        IconButton(onClick = onAddToPlaylist, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Filled.QueueMusic,
                                contentDescription = "Add to Playlist",
                                tint = MFColors.TextTertiary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        // Lyrics
                        IconButton(onClick = onLyricsToggle, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Lyrics,
                                contentDescription = "Lyrics",
                                tint = if (isLyricsVisible) MFColors.Accent else MFColors.TextTertiary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        // Sleep Timer
                        IconButton(onClick = onSleepTimerClick, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = "Sleep Timer",
                                tint = if (sleepTimerText != null) MFColors.Accent else MFColors.TextTertiary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // ── Playback Controls ────────────────────────────
                    PlaybackControlsPremium(
                        isPlaying = isPlaying,
                        onPlayPause = onPlayPause,
                        onSkipPrevious = onSkipPrevious,
                        onSkipNext = onSkipNext,
                        accentColor = animatedVibrant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            1 -> {
                // ── Queue Tab ────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    if (track != null) {
                        // Now Playing indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(animatedVibrant, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NOW PLAYING",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = animatedVibrant,
                                letterSpacing = 1.sp,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        QueueItem(
                            index = 0,
                            track = track,
                            accentColor = animatedVibrant,
                            isCurrent = true,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (upcomingTracks.isNotEmpty()) {
                            Text(
                                text = "UP NEXT  ·  ${upcomingTracks.size} tracks",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnBackgroundVariant,
                                letterSpacing = 1.sp,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (upcomingTracks.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(
                                items = upcomingTracks,
                                key = { index, item -> "${item.songId}_$index" },
                            ) { index, item ->
                                QueueItem(
                                    index = index + 1,
                                    track = item,
                                    accentColor = animatedVibrant,
                                    onClick = { onQueueItemSelected(index) },
                                    onRemove = { onRemoveFromQueue(index) },
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No upcoming tracks",
                                fontSize = 14.sp,
                                color = OnBackgroundVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Album Art with Palette Extraction ───────────────────────────────────

@Composable
private fun AlbumArtWithPalette(
    artworkUrl: String?,
    onDominantColor: (Color) -> Unit,
    onDarkMutedColor: (Color) -> Unit,
    onVibrantColor: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val imagePainter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(artworkUrl)
            .size(Size.ORIGINAL)
            .crossfade(true)
            .build(),
        onSuccess = { successResult ->
            val bitmap = when (val drawable = successResult.result.drawable) {
                is BitmapDrawable -> drawable.bitmap
                else -> null
            }
            if (bitmap != null) {
                extractPaletteFromBitmap(bitmap, onDominantColor, onDarkMutedColor, onVibrantColor)
            }
        },
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(DarkSurface),
        contentAlignment = Alignment.Center,
    ) {
        if (artworkUrl != null) {
            Image(
                painter = imagePainter,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = OnBackgroundVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(80.dp),
            )
        }
    }
}

private fun extractPaletteFromBitmap(
    bitmap: Bitmap,
    onDominantColor: (Color) -> Unit,
    onDarkMutedColor: (Color) -> Unit,
    onVibrantColor: (Color) -> Unit,
) {
    try {
        val palette = Palette.from(bitmap).generate()

        palette.dominantSwatch?.let { swatch ->
            onDominantColor(Color(swatch.rgb))
        }

        palette.darkMutedSwatch?.let { swatch ->
            onDarkMutedColor(Color(swatch.rgb))
        } ?: palette.darkVibrantSwatch?.let { swatch ->
            onDarkMutedColor(Color(swatch.rgb))
        }

        palette.vibrantSwatch?.let { swatch ->
            onVibrantColor(Color(swatch.rgb))
        } ?: palette.lightVibrantSwatch?.let { swatch ->
            onVibrantColor(Color(swatch.rgb))
        }
    } catch (_: Exception) {
        // Silently fail
    }
}

// ── Track Info ──────────────────────────────────────────────────────────

@Composable
private fun TrackInfoPremium(
    title: String,
    artist: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MFColors.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp,
            letterSpacing = (-0.3).sp,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = artist,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = MFColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Progress Slider ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressSliderPremium(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    var isUserDragging by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableStateOf(0f) }

    val progress = if (duration > 0) {
        if (isUserDragging) sliderPosition else currentPosition.toFloat() / duration.toFloat()
    } else 0f

    Column(modifier = modifier) {
        Slider(
            value = progress,
            onValueChange = { value ->
                isUserDragging = true
                sliderPosition = value
            },
            onValueChangeFinished = {
                isUserDragging = false
                onSeek((sliderPosition * duration).toLong())
            },
            thumb = {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .shadow(4.dp, CircleShape)
                        .background(accentColor, CircleShape),
                )
            },
            track = { sliderState ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ProgressTrack)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = sliderState.value.coerceIn(0f, 1f))
                            .fillMaxSize()
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(accentColor, accentColor.copy(alpha = 0.7f))
                                )
                            )
                    )
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = ProgressTrack,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(currentPosition),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = OnBackgroundVariant.copy(alpha = 0.8f),
            )
            Text(
                text = formatDuration(duration),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = OnBackgroundVariant.copy(alpha = 0.8f),
            )
        }
    }
}

// ── Playback Controls ───────────────────────────────────────────────────

@Composable
private fun PlaybackControlsPremium(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Skip Previous
        IconButton(onClick = onSkipPrevious, modifier = Modifier.size(52.dp)) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                tint = MFColors.TextPrimary.copy(alpha = 0.85f),
                modifier = Modifier.size(36.dp),
            )
        }

        // Play/Pause — massive animated button
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier
                .size(80.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = accentColor.copy(alpha = 0.5f),
                    spotColor = accentColor.copy(alpha = 0.4f),
                )
                .background(accentColor, CircleShape),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = Color.White,
            ),
        ) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (scaleIn(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        initialScale = 0.7f,
                    ) + fadeIn(
                        animationSpec = tween(150),
                    ) togetherWith scaleOut(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        targetScale = 0.7f,
                    ) + fadeOut(
                        animationSpec = tween(150),
                    )).using(
                        SizeTransform(clip = false)
                    )
                },
                label = "playPauseIcon",
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    modifier = Modifier.size(44.dp),
                )
            }
        }

        // Skip Next
        IconButton(onClick = onSkipNext, modifier = Modifier.size(52.dp)) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next",
                tint = MFColors.TextPrimary.copy(alpha = 0.85f),
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

// ── Queue Item ──────────────────────────────────────────────────────────

@Composable
private fun QueueItem(
    index: Int,
    track: TrackMetadata,
    accentColor: Color,
    isCurrent: Boolean = false,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    val bgColor = if (isCurrent) accentColor.copy(alpha = 0.12f) else Color.Transparent
    val numberColor = if (isCurrent) accentColor else OnBackgroundVariant.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isCurrent) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Playing",
                tint = accentColor,
                modifier = Modifier.size(16.dp),
            )
        } else {
            Text(
                text = "$index",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = numberColor,
                modifier = Modifier.width(24.dp),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurface),
            contentAlignment = Alignment.Center,
        ) {
            if (track.artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(track.artworkUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
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

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) OnBackground else OnBackground.copy(alpha = 0.9f),
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

        if (!isCurrent && onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove",
                    tint = OnBackgroundVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ── Utilities ───────────────────────────────────────────────────────────

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"

    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
