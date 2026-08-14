package com.musicflow.app.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.musicflow.app.audio.AudioAnalysisEngine
import com.musicflow.app.artwork.ArtworkIntelligenceEngine
import com.musicflow.app.ui.theme.DynamicThemeEngine
import com.musicflow.app.ui.visual.VisualEngine
import com.musicflow.app.ui.engine.MotionEngine
import com.musicflow.app.ui.theme.MFColors
import com.musicflow.app.ui.theme.AccentGreen

private val DeepBlack = Color(0xFF000000)
private val SurfaceBlack = Color(0xFF0A0A0A)
private val SubtleSurface = Color(0xFF141414)
private val CardSurface = Color(0xFF1A1A1A)
private val TextWhite = Color(0xFFF5F5F7)
private val TextMuted = Color(0xFF8E8E93)
private val TextDim = Color(0xFF636366)
private val GreenAccent = AccentGreen
private val ErrorRed = Color(0xFFFF453A)

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
    playbackStatus: com.musicflow.app.ui.screens.PlaybackStatus = com.musicflow.app.ui.screens.PlaybackStatus.IDLE,
    artworkEngine: ArtworkIntelligenceEngine? = null,
    dynamicThemeEngine: DynamicThemeEngine? = null,
    audioAnalysisEngine: AudioAnalysisEngine? = null,
    visualEngine: VisualEngine? = null,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(track?.artworkUrl) {
        val url = track?.artworkUrl
        if (url != null && artworkEngine != null) {
            try {
                val analysis = artworkEngine.analyze(url)
                dynamicThemeEngine?.updateFromArtwork(analysis)
            } catch (_: Exception) { }
        }
    }

    val dynamicThemeState = dynamicThemeEngine?.dynamicThemeState?.collectAsState()?.value
    val audioState = audioAnalysisEngine?.audioState?.collectAsState()?.value
        ?: com.musicflow.app.audio.AudioState()

    LaunchedEffect(audioState, isPlaying) {
        if (visualEngine != null && artworkEngine != null) {
            val url = track?.artworkUrl
            if (url != null) {
                try {
                    val analysis = artworkEngine.analyze(url)
                    visualEngine.update(audioState, analysis, isPlaying)
                } catch (_: Exception) { }
            }
        }
    }

    var dominantColor by remember { mutableStateOf(Color(0xFF1A1A2E)) }
    var darkMutedColor by remember { mutableStateOf(Color(0xFF16213E)) }
    var vibrantColor by remember { mutableStateOf(GreenAccent) }

    val effectiveDominant = dynamicThemeState?.primaryAccent ?: dominantColor
    val effectiveDarkMuted = dynamicThemeState?.atmosphericColor ?: darkMutedColor
    val effectiveVibrant = dynamicThemeState?.secondaryAccent ?: vibrantColor

    val animatedDominant by animateColorAsState(
        targetValue = effectiveDominant,
        animationSpec = tween(durationMillis = MotionEngine.SLOW_DURATION_MS),
        label = "dominantColor",
    )
    val animatedVibrant by animateColorAsState(
        targetValue = effectiveVibrant,
        animationSpec = tween(durationMillis = MotionEngine.SLOW_DURATION_MS),
        label = "vibrantColor",
    )

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Now Playing", "Queue")

    val showIndicator = playbackStatus != com.musicflow.app.ui.screens.PlaybackStatus.IDLE
    val isStatusLoading = playbackStatus in listOf(
        com.musicflow.app.ui.screens.PlaybackStatus.EXTRACTING,
        com.musicflow.app.ui.screens.PlaybackStatus.PREPARING,
        com.musicflow.app.ui.screens.PlaybackStatus.BUFFERING,
        com.musicflow.app.ui.screens.PlaybackStatus.RESTORING,
        com.musicflow.app.ui.screens.PlaybackStatus.FILLING_QUEUE,
    )
    val isError = playbackStatus == com.musicflow.app.ui.screens.PlaybackStatus.ERROR
    val isPaused = playbackStatus == com.musicflow.app.ui.screens.PlaybackStatus.PAUSED
    val isPlayingStatus = playbackStatus == com.musicflow.app.ui.screens.PlaybackStatus.PLAYING

    val dotTarget1 = when {
        isError -> 0.9f
        isPaused -> 0.2f
        isStatusLoading -> 1f
        isPlayingStatus -> 1f
        else -> 0.3f
    }
    val dotDuration1 = when {
        playbackStatus == com.musicflow.app.ui.screens.PlaybackStatus.EXTRACTING -> 300
        playbackStatus == com.musicflow.app.ui.screens.PlaybackStatus.BUFFERING -> 1500
        playbackStatus == com.musicflow.app.ui.screens.PlaybackStatus.RESTORING -> 400
        isPlayingStatus -> 1200
        else -> 800
    }
    val dotAlpha1 by animateFloatAsState(
        targetValue = dotTarget1,
        animationSpec = if (isStatusLoading || isPlaying) {
            infiniteRepeatable(
                animation = tween(dotDuration1, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            )
        } else {
            tween(400)
        },
        label = "dot1",
    )

    val dotTarget2 = when {
        isError -> 0.9f
        isPaused -> 0.2f
        isStatusLoading -> 0.6f
        isPlayingStatus -> 0.6f
        else -> 0.3f
    }
    val dotDuration2 = when {
        playbackStatus == com.musicflow.app.ui.screens.PlaybackStatus.EXTRACTING -> 300
        playbackStatus == com.musicflow.app.ui.screens.PlaybackStatus.BUFFERING -> 1500
        playbackStatus == com.musicflow.app.ui.screens.PlaybackStatus.RESTORING -> 400
        isPlayingStatus -> 1200
        else -> 800
    }
    val dotAlpha2 by animateFloatAsState(
        targetValue = dotTarget2,
        animationSpec = if (isStatusLoading || isPlaying) {
            infiniteRepeatable(
                animation = tween(dotDuration2, easing = FastOutSlowInEasing, delayMillis = 200),
                repeatMode = RepeatMode.Reverse,
            )
        } else {
            tween(400, delayMillis = 100)
        },
        label = "dot2",
    )

    val dotTarget3 = when {
        isError -> 0.9f
        isPaused -> 0.2f
        isStatusLoading -> 0.8f
        isPlayingStatus -> 0.8f
        else -> 0.3f
    }
    val dotDuration3 = when {
        playbackStatus == com.musicflow.app.ui.screens.PlaybackStatus.EXTRACTING -> 300
        playbackStatus == com.musicflow.app.ui.screens.PlaybackStatus.BUFFERING -> 1500
        playbackStatus == com.musicflow.app.ui.screens.PlaybackStatus.RESTORING -> 400
        isPlayingStatus -> 1200
        else -> 800
    }
    val dotAlpha3 by animateFloatAsState(
        targetValue = dotTarget3,
        animationSpec = if (isStatusLoading || isPlaying) {
            infiniteRepeatable(
                animation = tween(dotDuration3, easing = FastOutSlowInEasing, delayMillis = 400),
                repeatMode = RepeatMode.Reverse,
            )
        } else {
            tween(400, delayMillis = 200)
        },
        label = "dot3",
    )

    val dotColor = when {
        isError -> ErrorRed
        else -> animatedVibrant
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBlack),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarPadding.calculateTopPadding()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Close",
                        tint = TextMuted,
                        modifier = Modifier.size(24.dp),
                    )
                }
                if (showIndicator) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(dotColor.copy(alpha = dotAlpha1))
                            )
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(dotColor.copy(alpha = dotAlpha2))
                            )
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(dotColor.copy(alpha = dotAlpha3))
                            )
                        }
                        if (isStatusLoading || isPaused || isError) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = playbackStatus.label,
                                color = if (isError) ErrorRed else TextDim,
                                fontSize = 9.sp,
                                maxLines = 1,
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(40.dp))
                }
                Spacer(modifier = Modifier.size(40.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, title ->
                    val isActive = selectedTab == index
                    val tabAlpha by animateFloatAsState(
                        targetValue = if (isActive) 1f else 0.4f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "tabAlpha_$index",
                    )
                    val glowAlpha by animateFloatAsState(
                        targetValue = if (isActive) 0.08f else 0f,
                        animationSpec = tween(350),
                        label = "tabGlow_$index",
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(animatedVibrant.copy(alpha = glowAlpha))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { selectedTab = index }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            color = TextWhite.copy(alpha = tabAlpha),
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 24.dp)
                            .padding(bottom = navigationBarPadding.calculateBottomPadding()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.62f)
                                    .aspectRatio(1f)
                                    .shadow(
                                        elevation = if (isPlaying) 60.dp else 40.dp,
                                        shape = RoundedCornerShape(28.dp),
                                        ambientColor = animatedDominant.copy(alpha = if (isPlaying) 0.12f else 0.06f),
                                        spotColor = animatedDominant.copy(alpha = if (isPlaying) 0.08f else 0.03f),
                                    )
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(Color.Transparent)
                            )

                            AlbumArtWithPalette(
                                artworkUrl = track?.artworkUrl,
                                onDominantColor = { dominantColor = it },
                                onDarkMutedColor = { darkMutedColor = it },
                                onVibrantColor = { vibrantColor = it },
                                isPlaying = isPlaying,
                                modifier = Modifier
                                    .fillMaxWidth(0.62f)
                                    .aspectRatio(1f)
                                    .shadow(
                                        elevation = 30.dp,
                                        shape = RoundedCornerShape(28.dp),
                                        ambientColor = Color.Black.copy(alpha = 0.8f),
                                        spotColor = Color.Black.copy(alpha = 0.6f),
                                    ),
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        TrackInfoSection(
                            title = track?.title ?: "No Track Playing",
                            artist = track?.artist ?: "—",
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        ProgressSection(
                            currentPosition = currentPosition,
                            duration = duration,
                            onSeek = onSeek,
                            accentColor = animatedVibrant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SecondaryControl(
                                icon = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isLiked) "Unlike" else "Like",
                                tint = if (isLiked) ErrorRed else TextDim,
                                onClick = onLikeToggle,
                            )
                            SecondaryControl(
                                icon = Icons.Filled.QueueMusic,
                                contentDescription = "Add to Playlist",
                                tint = TextDim,
                                onClick = onAddToPlaylist,
                            )
                            SecondaryControl(
                                icon = Icons.Filled.Lyrics,
                                contentDescription = "Lyrics",
                                tint = if (isLyricsVisible) animatedVibrant else TextDim,
                                onClick = onLyricsToggle,
                            )
                            SecondaryControl(
                                icon = Icons.Filled.Timer,
                                contentDescription = "Sleep Timer",
                                tint = if (sleepTimerText != null) animatedVibrant else TextDim,
                                onClick = onSleepTimerClick,
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TransportControls(
                            isPlaying = isPlaying,
                            onPlayPause = onPlayPause,
                            onSkipPrevious = onSkipPrevious,
                            onSkipNext = onSkipNext,
                            accentColor = animatedVibrant,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                1 -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 24.dp),
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        if (track != null) {
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
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = animatedVibrant,
                                    letterSpacing = 1.5.sp,
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            QueueItem(
                                index = 0,
                                track = track,
                                isCurrent = true,
                                accentColor = animatedVibrant,
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            if (upcomingTracks.isNotEmpty()) {
                                Text(
                                    text = "UP NEXT  \u00B7  ${upcomingTracks.size} tracks",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextDim,
                                    letterSpacing = 1.5.sp,
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
                                        onClick = { onQueueItemSelected(index) },
                                        onRemove = { onRemoveFromQueue(index) },
                                        accentColor = animatedVibrant,
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
                                    color = TextDim,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecondaryControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.82f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "secondaryPress",
    )

    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val prevInteractionSource = remember { MutableInteractionSource() }
    val isPrevPressed by prevInteractionSource.collectIsPressedAsState()
    val prevPressScale by animateFloatAsState(
        targetValue = if (isPrevPressed) 0.8f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "prevPress",
    )

    val nextInteractionSource = remember { MutableInteractionSource() }
    val isNextPressed by nextInteractionSource.collectIsPressedAsState()
    val nextPressScale by animateFloatAsState(
        targetValue = if (isNextPressed) 0.8f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "nextPress",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "playPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "playPulseScale",
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onSkipPrevious,
            interactionSource = prevInteractionSource,
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    scaleX = prevPressScale
                    scaleY = prevPressScale
                },
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                tint = TextWhite.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp),
            )
        }

        Box(
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                    .shadow(
                        elevation = if (isPlaying) 24.dp else 12.dp,
                        shape = CircleShape,
                        ambientColor = accentColor.copy(alpha = if (isPlaying) 0.45f else 0.18f),
                        spotColor = accentColor.copy(alpha = if (isPlaying) 0.35f else 0.12f),
                    )
                    .background(accentColor, CircleShape),
            ) {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        (scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                            initialScale = 0.6f,
                        ) + fadeIn(
                            animationSpec = tween(120),
                        ) togetherWith scaleOut(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium,
                            ),
                            targetScale = 0.6f,
                        ) + fadeOut(
                            animationSpec = tween(120),
                        )).using(
                            SizeTransform(clip = false)
                        )
                    },
                    label = "playPauseIcon",
                ) { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                        tint = DeepBlack,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }

        IconButton(
            onClick = onSkipNext,
            interactionSource = nextInteractionSource,
            modifier = Modifier
                .size(48.dp)
                .graphicsLayer {
                    scaleX = nextPressScale
                    scaleY = nextPressScale
                },
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next",
                tint = TextWhite.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun AlbumArtWithPalette(
    artworkUrl: String?,
    onDominantColor: (Color) -> Unit,
    onDarkMutedColor: (Color) -> Unit,
    onVibrantColor: (Color) -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "artworkBreathing")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.012f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breatheScale",
    )
    val breatheAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 0.97f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breatheAlpha",
    )

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
            .graphicsLayer {
                scaleX = breatheScale
                scaleY = breatheScale
                alpha = breatheAlpha
            }
            .clip(RoundedCornerShape(28.dp))
            .background(SurfaceBlack),
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
                tint = TextDim,
                modifier = Modifier.size(64.dp),
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
        palette.dominantSwatch?.let { onDominantColor(Color(it.rgb)) }
        palette.darkMutedSwatch?.let { onDarkMutedColor(Color(it.rgb)) }
            ?: palette.darkVibrantSwatch?.let { onDarkMutedColor(Color(it.rgb)) }
        palette.vibrantSwatch?.let { onVibrantColor(Color(it.rgb)) }
            ?: palette.lightVibrantSwatch?.let { onVibrantColor(Color(it.rgb)) }
    } catch (e: Exception) {
        Log.w("MainPlayerScreen", "Failed to extract palette: ${e.message}")
    }
}

@Composable
private fun TrackInfoSection(
    title: String,
    artist: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextWhite,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp,
            letterSpacing = (-0.3).sp,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = artist,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            letterSpacing = 0.2.sp,
        )
    }
}

@Composable
private fun ProgressSection(
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    var isUserDragging by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    val progress = if (duration > 0) {
        if (isUserDragging) sliderPosition else currentPosition.toFloat() / duration.toFloat()
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = if (isUserDragging) tween(0) else tween(300, easing = FastOutSlowInEasing),
        label = "progressAnim",
    )

    val thumbScale by animateFloatAsState(
        targetValue = if (isUserDragging) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "thumbScale",
    )

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SubtleSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = animatedProgress.coerceIn(0f, 1f))
                        .fillMaxSize()
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(accentColor, accentColor.copy(alpha = 0.6f))
                            )
                        )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = animatedProgress.coerceIn(0f, 1f))
                        .fillMaxSize(),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .graphicsLayer {
                                scaleX = thumbScale
                                scaleY = thumbScale
                            }
                            .shadow(4.dp, CircleShape)
                            .background(TextWhite, CircleShape)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(currentPosition),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = TextDim,
                letterSpacing = 0.3.sp,
            )
            Text(
                text = formatDuration(duration),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = TextDim,
                letterSpacing = 0.3.sp,
            )
        }
    }
}

@Composable
private fun QueueItem(
    index: Int,
    track: TrackMetadata,
    isCurrent: Boolean = false,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
    accentColor: Color = GreenAccent,
) {
    val bgColor = if (isCurrent) SubtleSurface else Color.Transparent
    val numberColor = if (isCurrent) accentColor else TextDim

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ) else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
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
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceBlack),
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
                    tint = TextDim,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontSize = 13.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isCurrent) TextWhite else TextWhite.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                fontSize = 12.sp,
                color = TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!isCurrent && onRemove != null) {
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove",
                    tint = TextDim,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

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
