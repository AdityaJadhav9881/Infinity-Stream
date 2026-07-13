package com.musicflow.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicflow.app.data.local.entity.PlaylistEntity
import com.musicflow.app.data.local.entity.TrackEntity
import com.musicflow.app.ui.theme.MFAnimations
import com.musicflow.app.ui.theme.MFColors
import com.musicflow.app.ui.theme.MFBrushes
import com.musicflow.app.ui.theme.MFTokens
import java.util.Calendar

// ── Greeting ────────────────────────────────────────────────────────────

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }
}

// ── Main Screen ──────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    recentTracks: List<TrackEntity>,
    playlists: List<PlaylistEntity>,
    onTrackSelected: (TrackEntity) -> Unit,
    onPlaylistSelected: (Long) -> Unit,
    onSearchClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onFavoritesClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    onRecentlyPlayedSeeAll: () -> Unit = {},
    onMoodMixClick: (String) -> Unit = {},
    onDailyMixClick: () -> Unit = {},
    onPlaylistPlay: (Long) -> Unit = {},
    currentPlayingSongId: String? = null,
    isNetworkAvailable: Boolean = true,
    notifications: List<HomeNotification> = emptyList(),
    onClearNotifications: () -> Unit = {},
    trendingTracks: List<TrackEntity> = emptyList(),
    isTrendingLoading: Boolean = false,
    trendingError: String? = null,
    onRetryTrending: () -> Unit = {},
    favoriteTracks: List<TrackEntity> = emptyList(),
    playlistTracks: Map<Long, List<TrackEntity>> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    var showNotifications by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MFColors.Background),
        contentPadding = PaddingValues(bottom = 100.dp),
    ) {
        // ── Hero Greeting ───────────────────────────────────────────
        item(key = "hero") {
            HeroHeader(
                onNotificationClick = { showNotifications = true },
                onProfileClick = { showProfile = true },
                notificationCount = notifications.size,
                isNetworkAvailable = isNetworkAvailable,
            )
        }

        // ── Quick Actions ───────────────────────────────────────────
        item(key = "quick_actions") {
            QuickActionsGrid(
                trackCount = recentTracks.size,
                playlistCount = playlists.size,
                onSearchClick = onSearchClick,
                onFavoritesClick = onFavoritesClick,
                onLibraryClick = onLibraryClick,
                onDownloadsClick = onDownloadsClick,
            )
        }

        // ── Continue Listening ───────────────────────────────────────
        if (recentTracks.isNotEmpty()) {
            item(key = "continue_header") {
                SectionHeader("Continue Listening", "See All", onSeeAll = onLibraryClick)
            }
            items(recentTracks.take(5)) { track ->
                val isPlaying = track.songId == currentPlayingSongId
                ContinueListeningCard(
                    track = track,
                    isPlaying = isPlaying,
                    onClick = { onTrackSelected(track) },
                    progress = track.playbackProgress,
                    modifier = Modifier.padding(horizontal = MFTokens.ScreenHorizontalPadding, vertical = 4.dp),
                )
            }
        }

        // ── Recently Played Carousel ────────────────────────────────
        if (recentTracks.isNotEmpty()) {
            item(key = "recent_header") {
                SectionHeader("Recently Played", "See All", onSeeAll = onRecentlyPlayedSeeAll)
            }
            item(key = "recent_carousel") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = MFTokens.ScreenHorizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(MFTokens.CarouselSpacing),
                ) {
                    items(recentTracks.take(8)) { track ->
                        val isPlaying = track.songId == currentPlayingSongId
                        RecentCard(
                            track = track,
                            isPlaying = isPlaying,
                            onClick = { onTrackSelected(track) },
                        )
                    }
                }
            }
        }

        // ── Mixes ───────────────────────────────────────────────────
        item(key = "mixes_header") {
            SectionHeader("Mixes For You", null)
        }
        item(key = "mixes_carousel") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = MFTokens.ScreenHorizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(MFTokens.CarouselSpacing),
            ) {
                items(mixCards) { mix ->
                    MixCard(
                        mix = mix,
                        onClick = {
                            if (mix.query.isEmpty()) onDailyMixClick()
                            else onMoodMixClick(mix.query)
                        },
                    )
                }
            }
        }

        // ── Per-Playlist Carousels ──────────────────────────────────
        playlists.forEach { playlist ->
            val tracks = playlistTracks[playlist.id] ?: emptyList()
            if (tracks.isNotEmpty()) {
                item(key = "playlist_header_${playlist.id}") {
                    SectionHeader(playlist.name, "See All", onSeeAll = { onPlaylistSelected(playlist.id) })
                }
                item(key = "playlist_carousel_${playlist.id}") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = MFTokens.ScreenHorizontalPadding),
                        horizontalArrangement = Arrangement.spacedBy(MFTokens.CarouselSpacing),
                    ) {
                        items(tracks.take(8)) { track ->
                            val isPlaying = track.songId == currentPlayingSongId
                            RecentCard(
                                track = track,
                                isPlaying = isPlaying,
                                onClick = { onTrackSelected(track) },
                            )
                        }
                    }
                }
            }
        }

        // ── Favorites ───────────────────────────────────────────────
        if (favoriteTracks.isNotEmpty()) {
            item(key = "favorites_header") {
                SectionHeader("Your Favorites", "See All", onSeeAll = onFavoritesClick)
            }
            item(key = "favorites_carousel") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = MFTokens.ScreenHorizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(MFTokens.CarouselSpacing),
                ) {
                    items(favoriteTracks.take(8)) { track ->
                        val isPlaying = track.songId == currentPlayingSongId
                        RecentCard(
                            track = track,
                            isPlaying = isPlaying,
                            onClick = { onTrackSelected(track) },
                        )
                    }
                }
            }
        }

        // ── Trending ────────────────────────────────────────────────
        if (trendingTracks.isNotEmpty()) {
            item(key = "trending_header") { SectionHeader("Trending Today", null) }
            itemsIndexed(trendingTracks.take(5)) { index, track ->
                TrendingRow(
                    track = track,
                    rank = index + 1,
                    isPlaying = track.songId == currentPlayingSongId,
                    onClick = { onTrackSelected(track) },
                    modifier = Modifier.padding(horizontal = MFTokens.ScreenHorizontalPadding, vertical = 3.dp),
                )
            }
        } else if (isTrendingLoading) {
            item(key = "trending_loading") {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MFColors.Accent, strokeWidth = 2.dp)
                }
            }
        } else if (trendingError != null) {
            item(key = "trending_error") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = MFTokens.ScreenHorizontalPadding, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = trendingError, color = MFColors.TextTertiary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to retry",
                        color = MFColors.Accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(onClick = onRetryTrending),
                    )
                }
            }
        }
    }

    if (showNotifications) {
        NotificationsDialog(
            notifications = notifications,
            onDismiss = { showNotifications = false },
            onClearAll = {
                onClearNotifications()
                showNotifications = false
            },
        )
    }

    if (showProfile) {
        ProfileDialog(
            onDismiss = { showProfile = false },
            onFavoritesClick = { showProfile = false; onFavoritesClick() },
            onDownloadsClick = { showProfile = false; onDownloadsClick() },
            onLibraryClick = { showProfile = false; onLibraryClick() },
        )
    }
}

// ── Notification Data ────────────────────────────────────────────────────

data class HomeNotification(
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class NotificationType { SUCCESS, ERROR, INFO }

// ── Notifications Dialog ─────────────────────────────────────────────────

@Composable
private fun NotificationsDialog(
    notifications: List<HomeNotification>,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MFColors.Overlay,
        shape = MFTokens.LargeRadius,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Notifications", color = MFColors.TextPrimary, fontWeight = FontWeight.Bold)
                if (notifications.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Clear All", color = MFColors.Accent, fontSize = 13.sp)
                    }
                }
            }
        },
        text = {
            if (notifications.isEmpty()) {
                Text(
                    text = "No new notifications",
                    color = MFColors.TextTertiary,
                    modifier = Modifier.padding(vertical = 20.dp),
                    textAlign = TextAlign.Center,
                )
            } else {
                Column {
                    notifications.forEach { notification ->
                        NotificationItem(notification)
                        HorizontalDivider(color = MFColors.Divider)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MFColors.TextTertiary)
            }
        },
    )
}

@Composable
private fun NotificationItem(notification: HomeNotification) {
    val icon = when (notification.type) {
        NotificationType.SUCCESS -> Icons.Filled.CloudDownload
        NotificationType.ERROR -> Icons.Filled.Close
        NotificationType.INFO -> Icons.Filled.Notifications
    }
    val color = when (notification.type) {
        NotificationType.SUCCESS -> MFColors.Accent
        NotificationType.ERROR -> MFColors.Error
        NotificationType.INFO -> MFColors.Secondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = notification.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MFColors.TextPrimary)
            Text(text = notification.message, fontSize = 12.sp, color = MFColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ── Profile Dialog ───────────────────────────────────────────────────────

@Composable
private fun ProfileDialog(
    onDismiss: () -> Unit,
    onFavoritesClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MFColors.Overlay,
        shape = MFTokens.LargeRadius,
        title = { Text("Profile", color = MFColors.TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MFBrushes.AccentGradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MFColors.TextOnAccent,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("MusicFlow User", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MFColors.TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Free Plan", fontSize = 13.sp, color = MFColors.TextSecondary)
                Spacer(modifier = Modifier.height(24.dp))
                ProfileMenuItem(icon = Icons.Filled.Favorite, label = "Favorites", color = MFColors.Error, onClick = onFavoritesClick)
                ProfileMenuItem(icon = Icons.Filled.CloudDownload, label = "Downloads", color = MFColors.Accent, onClick = onDownloadsClick)
                ProfileMenuItem(icon = Icons.Filled.MusicNote, label = "Library", color = MFColors.Secondary, onClick = onLibraryClick)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MFColors.TextTertiary)
            }
        },
    )
}

@Composable
private fun ProfileMenuItem(icon: ImageVector, label: String, color: Color, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, fontSize = 15.sp, color = MFColors.TextPrimary, fontWeight = FontWeight.Medium)
    }
}

// ── Hero Header ──────────────────────────────────────────────────────────

@Composable
private fun HeroHeader(
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    notificationCount: Int = 0,
    isNetworkAvailable: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MFTokens.ScreenHorizontalPadding, vertical = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Network status banner
        AnimatedVisibility(visible = !isNetworkAvailable) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MFColors.Error.copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = null,
                    tint = MFColors.Error,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "You're offline. Downloaded music still plays.",
                    fontSize = 13.sp,
                    color = MFColors.Error,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getGreeting(),
                    fontSize = MFTokens.HeroTextSize,
                    fontWeight = FontWeight.ExtraBold,
                    color = MFColors.TextPrimary,
                    letterSpacing = (-1.0).sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Continue where you left off.",
                    fontSize = 13.sp,
                    color = MFColors.TextTertiary,
                    fontWeight = FontWeight.Normal,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Notification Bell
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MFColors.Elevated)
                        .clickable(onClick = onNotificationClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        tint = MFColors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                    if (notificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(MFColors.Error)
                                .align(Alignment.TopEnd),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "$notificationCount",
                                fontSize = 9.sp,
                                color = MFColors.TextOnAccent,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                // Profile Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MFBrushes.AccentGradient)
                        .clickable(onClick = onProfileClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Profile",
                        tint = MFColors.TextOnAccent,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

// ── Quick Actions 2x2 Grid ──────────────────────────────────────────────

@Composable
private fun QuickActionsGrid(
    trackCount: Int,
    playlistCount: Int,
    onSearchClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onDownloadsClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = MFTokens.ScreenHorizontalPadding, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickActionCard(
                title = "Search",
                subtitle = "Find songs instantly",
                icon = Icons.Filled.Search,
                accentColor = MFColors.Accent,
                onClick = onSearchClick,
                modifier = Modifier.weight(1f),
            )
            QuickActionCard(
                title = "Favorites",
                subtitle = "Your liked music",
                icon = Icons.Filled.Favorite,
                accentColor = MFColors.Error,
                onClick = onFavoritesClick,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickActionCard(
                title = "Library",
                subtitle = "$trackCount songs \u00B7 $playlistCount playlists",
                icon = Icons.Filled.MusicNote,
                accentColor = MFColors.Accent,
                onClick = onLibraryClick,
                modifier = Modifier.weight(1f),
            )
            QuickActionCard(
                title = "Downloads",
                subtitle = "Offline music",
                icon = Icons.Filled.CloudDownload,
                accentColor = MFColors.Tertiary,
                onClick = onDownloadsClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "cardScale",
    )

    Card(
        modifier = modifier
            .height(MFTokens.QuickActionHeight)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 1.dp else MFTokens.ElevationLow,
                shape = MFTokens.MediumRadius,
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.1f),
            )
            .clip(MFTokens.MediumRadius)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        colors = CardDefaults.cardColors(containerColor = MFColors.Card),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = MFTokens.CardTitleSize,
                    fontWeight = FontWeight.SemiBold,
                    color = MFColors.TextPrimary,
                    letterSpacing = (-0.2).sp,
                )
                Text(
                    text = subtitle,
                    fontSize = MFTokens.CardSubtitleSize,
                    color = MFColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ── Section Header ──────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, actionText: String?, onSeeAll: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MFTokens.ScreenHorizontalPadding, vertical = MFTokens.SectionSpacing / 2),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = MFTokens.SectionHeaderTextSize,
            fontWeight = FontWeight.Bold,
            color = MFColors.TextPrimary,
            letterSpacing = (-0.4).sp,
        )
        if (actionText != null) {
            Row(
                modifier = Modifier.clickable(onClick = onSeeAll),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = actionText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MFColors.Accent,
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MFColors.Accent,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ── Continue Listening Card ─────────────────────────────────────────────

@Composable
private fun ContinueListeningCard(
    track: TrackEntity,
    isPlaying: Boolean,
    onClick: () -> Unit,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "progress",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "eq")
    val eqBar1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq1",
    )
    val eqBar2 by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq2",
    )
    val eqBar3 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq3",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MFTokens.MediumRadius)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MFColors.Accent.copy(alpha = 0.08f) else MFColors.Card
        ),
    ) {
        Row(
            modifier = Modifier.padding(MFTokens.SmallCardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Artwork
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp)),
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
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(MFColors.Accent.copy(alpha = 0.3f), MFColors.Accent.copy(alpha = 0.1f)))
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(imageVector = Icons.Filled.MusicNote, contentDescription = null, tint = MFColors.Accent, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MFColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    fontSize = 12.sp,
                    color = MFColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MFColors.Accent,
                    trackColor = MFColors.ProgressTrack,
                    strokeCap = StrokeCap.Round,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            if (isPlaying) {
                Row(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MFColors.Accent.copy(alpha = 0.12f))
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Box(Modifier.weight(1f).fillMaxWidth().height((eqBar1 * 14).dp.coerceAtMost(14.dp)).clip(RoundedCornerShape(1.dp)).background(MFColors.Accent))
                    Box(Modifier.weight(1f).fillMaxWidth().height((eqBar2 * 14).dp.coerceAtMost(14.dp)).clip(RoundedCornerShape(1.dp)).background(MFColors.Accent))
                    Box(Modifier.weight(1f).fillMaxWidth().height((eqBar3 * 14).dp.coerceAtMost(14.dp)).clip(RoundedCornerShape(1.dp)).background(MFColors.Accent))
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MFColors.Elevated),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = MFColors.TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

// ── Recent Card ─────────────────────────────────────────────────────────

@Composable
private fun RecentCard(track: TrackEntity, isPlaying: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "recentScale",
    )

    Column(
        modifier = Modifier
            .width(MFTokens.RecentCardWidth)
            .scale(scale)
            .clip(MFTokens.MediumRadius)
            .background(if (isPlaying) MFColors.Accent.copy(alpha = 0.08f) else MFColors.Card)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(124.dp)
                .clip(RoundedCornerShape(12.dp)),
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
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(MFColors.Accent.copy(alpha = 0.3f), MFColors.Accent.copy(alpha = 0.08f)))
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = Icons.Filled.MusicNote, contentDescription = null, tint = MFColors.Accent, modifier = Modifier.size(28.dp))
                }
            }
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "Playing",
                        tint = MFColors.Accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MFColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = track.artist,
            fontSize = 10.sp,
            color = MFColors.TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Mix Card ────────────────────────────────────────────────────────────

private data class MixData(
    val title: String,
    val subtitle: String,
    val gradient: List<Color>,
    val icon: ImageVector,
    val query: String,
)

private val mixCards = listOf(
    MixData("Daily Mix", "Made for you", listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)), Icons.Filled.MusicNote, ""),
    MixData("Focus Mix", "Deep concentration", listOf(Color(0xFF0EA5E9), Color(0xFF06B6D4)), Icons.Filled.MusicNote, "lofi focus deep concentration study"),
    MixData("Workout Mix", "High energy", listOf(Color(0xFFEF4444), Color(0xFFF97316)), Icons.Filled.TrendingUp, "punjabi workout high energy hype hits"),
    MixData("Chill Evening", "Relax & unwind", listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)), Icons.Filled.MusicNote, "acoustic chill acoustic vibes evening relaxed"),
    MixData("Night Drive", "Late night vibes", listOf(Color(0xFF1E1B4B), Color(0xFF312E81)), Icons.Filled.MusicNote, "late night drive synthwave ambient tracks"),
)

@Composable
private fun MixCard(mix: MixData, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "mixScale",
    )

    Box(
        modifier = Modifier
            .width(150.dp)
            .height(180.dp)
            .scale(scale)
            .shadow(
                elevation = MFTokens.ElevationMedium,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.2f),
                spotColor = Color.Black.copy(alpha = 0.15f),
            )
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(colors = mix.gradient))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = mix.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column {
                Text(
                    text = mix.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.2).sp,
                )
                Text(
                    text = mix.subtitle,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
        }
    }
}

// ── Trending Row ────────────────────────────────────────────────────────

@Composable
private fun TrendingRow(
    track: TrackEntity,
    rank: Int,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "trendScale",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPlaying) MFColors.Accent.copy(alpha = 0.06f) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$rank",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (rank <= 3) MFColors.Accent else MFColors.TextTertiary,
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MFColors.Elevated),
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
                Icon(imageVector = Icons.Filled.MusicNote, contentDescription = null, tint = MFColors.TextTertiary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MFColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                fontSize = 12.sp,
                color = MFColors.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isPlaying) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = "Playing",
                tint = MFColors.Accent,
                modifier = Modifier.size(16.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.TrendingUp,
                contentDescription = null,
                tint = MFColors.Accent.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
