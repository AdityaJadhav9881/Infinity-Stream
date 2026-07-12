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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.musicflow.app.ui.theme.AccentGreen
import com.musicflow.app.ui.theme.AccentGreenLight
import com.musicflow.app.ui.theme.CardSurface
import com.musicflow.app.ui.theme.DarkSurface
import com.musicflow.app.ui.theme.DarkSurfaceVariant
import com.musicflow.app.ui.theme.ErrorRed
import com.musicflow.app.ui.theme.GlassSurface
import com.musicflow.app.ui.theme.OnBackground
import com.musicflow.app.ui.theme.OnBackgroundVariant
import com.musicflow.app.ui.theme.SecondaryPurple
import com.musicflow.app.ui.theme.TertiaryTeal
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
    onMoodMixClick: (String) -> Unit = {},
    onDailyMixClick: () -> Unit = {},
    onPlaylistPlay: (Long) -> Unit = {},
    currentPlayingSongId: String? = null,
    isNetworkAvailable: Boolean = true,
    notifications: List<HomeNotification> = emptyList(),
    onClearNotifications: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showNotifications by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp),
    ) {
        item(key = "hero") {
            HeroHeader(
                onNotificationClick = { showNotifications = true },
                onProfileClick = { showProfile = true },
                notificationCount = notifications.size,
                isNetworkAvailable = isNetworkAvailable,
            )
        }

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
                    progress = if (isPlaying) 0.5f else 0.4f,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
        }

        if (recentTracks.isNotEmpty()) {
            item(key = "recent_header") {
                SectionHeader("Recently Played", "See All", onSeeAll = onLibraryClick)
            }
            item(key = "recent_carousel") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
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

        item(key = "mixes_header") {
            SectionHeader("Mixes For You", null)
        }
        item(key = "mixes_carousel") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
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

        if (playlists.isNotEmpty()) {
            item(key = "playlists_header") {
                SectionHeader("Your Playlists", "See All", onSeeAll = onLibraryClick)
            }
            item(key = "playlists_grid") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(playlists) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistPlay(playlist.id) },
                        )
                    }
                }
            }
        }

        if (recentTracks.isNotEmpty()) {
            item(key = "trending_header") { SectionHeader("Trending Today", null) }
            itemsIndexed(recentTracks.take(5)) { index, track ->
                TrendingRow(
                    track = track,
                    rank = index + 1,
                    isPlaying = track.songId == currentPlayingSongId,
                    onClick = { onTrackSelected(track) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 3.dp),
                )
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
        ProfileDialog(onDismiss = { showProfile = false })
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
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Notifications", color = OnBackground, fontWeight = FontWeight.Bold)
                if (notifications.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Clear All", color = AccentGreen, fontSize = 13.sp)
                    }
                }
            }
        },
        text = {
            if (notifications.isEmpty()) {
                Text(
                    text = "No new notifications",
                    color = OnBackgroundVariant,
                    modifier = Modifier.padding(vertical = 20.dp),
                    textAlign = TextAlign.Center,
                )
            } else {
                Column {
                    notifications.forEach { notification ->
                        NotificationItem(notification)
                        HorizontalDivider(color = OnBackgroundVariant.copy(alpha = 0.1f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = OnBackgroundVariant)
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
        NotificationType.SUCCESS -> AccentGreen
        NotificationType.ERROR -> ErrorRed
        NotificationType.INFO -> SecondaryPurple
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
            Text(text = notification.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OnBackground)
            Text(text = notification.message, fontSize = 12.sp, color = OnBackgroundVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ── Profile Dialog ───────────────────────────────────────────────────────

@Composable
private fun ProfileDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Profile", color = OnBackground, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(AccentGreen, AccentGreenLight))
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("MusicFlow User", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OnBackground)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Free Plan", fontSize = 13.sp, color = OnBackgroundVariant)
                Spacer(modifier = Modifier.height(24.dp))
                ProfileMenuItem(icon = Icons.Filled.Favorite, label = "Favorites", color = ErrorRed)
                ProfileMenuItem(icon = Icons.Filled.CloudDownload, label = "Downloads", color = AccentGreen)
                ProfileMenuItem(icon = Icons.Filled.MusicNote, label = "Library", color = SecondaryPurple)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = OnBackgroundVariant)
            }
        },
    )
}

@Composable
private fun ProfileMenuItem(icon: ImageVector, label: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
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
        Text(text = label, fontSize = 15.sp, color = OnBackground, fontWeight = FontWeight.Medium)
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
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Network status banner
        AnimatedVisibility(visible = !isNetworkAvailable) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "You're offline. Downloaded music still plays.",
                    fontSize = 13.sp,
                    color = Color(0xFFEF4444),
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
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground,
                    letterSpacing = (-0.3).sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Continue where you left off.",
                    fontSize = 13.sp,
                    color = OnBackgroundVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Normal,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(GlassSurface)
                        .clickable(onClick = onNotificationClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        tint = OnBackgroundVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    if (notificationCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(ErrorRed)
                                .align(Alignment.TopEnd),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "$notificationCount",
                                fontSize = 9.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AccentGreen, AccentGreenLight)
                            )
                        )
                        .clickable(onClick = onProfileClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Profile",
                        tint = Color.Black,
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
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
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
                accentColor = AccentGreen,
                onClick = onSearchClick,
                modifier = Modifier.weight(1f),
            )
            QuickActionCard(
                title = "Favorites",
                subtitle = "Your liked music",
                icon = Icons.Filled.Favorite,
                accentColor = ErrorRed,
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
                accentColor = AccentGreen,
                onClick = onLibraryClick,
                modifier = Modifier.weight(1f),
            )
            QuickActionCard(
                title = "Downloads",
                subtitle = "Offline music",
                icon = Icons.Filled.CloudDownload,
                accentColor = Color(0xFF3B82F6),
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
            .height(84.dp)
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 1.dp else 3.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.08f),
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
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
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnBackground,
                    letterSpacing = (-0.2).sp,
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = OnBackgroundVariant.copy(alpha = 0.7f),
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
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            color = OnBackground,
            letterSpacing = (-0.3).sp,
        )
        if (actionText != null) {
            Text(
                text = actionText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AccentGreen,
                modifier = Modifier.clickable(onClick = onSeeAll),
            )
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
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq1",
    )
    val eqBar2 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq2",
    )
    val eqBar3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq3",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) AccentGreen.copy(alpha = 0.08f) else CardSurface
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                            Brush.verticalGradient(listOf(AccentGreen.copy(alpha = 0.3f), AccentGreen.copy(alpha = 0.1f)))
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(imageVector = Icons.Filled.MusicNote, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = track.artist,
                    fontSize = 12.sp,
                    color = OnBackgroundVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AccentGreen,
                    trackColor = DarkSurfaceVariant,
                    strokeCap = StrokeCap.Round,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            if (isPlaying) {
                Row(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AccentGreen.copy(alpha = 0.12f))
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height((eqBar1 * 16).dp.coerceAtMost(16.dp))
                            .clip(RoundedCornerShape(1.dp))
                            .background(AccentGreen)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height((eqBar2 * 16).dp.coerceAtMost(16.dp))
                            .clip(RoundedCornerShape(1.dp))
                            .background(AccentGreen)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .height((eqBar3 * 16).dp.coerceAtMost(16.dp))
                            .clip(RoundedCornerShape(1.dp))
                            .background(AccentGreen)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(OnBackgroundVariant.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = OnBackgroundVariant,
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
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "recentScale",
    )

    Column(
        modifier = Modifier
            .width(140.dp)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isPlaying) AccentGreen.copy(alpha = 0.08f) else CardSurface)
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
                        Brush.verticalGradient(listOf(AccentGreen.copy(alpha = 0.3f), AccentGreen.copy(alpha = 0.08f)))
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = Icons.Filled.MusicNote, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(28.dp))
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
                        tint = AccentGreen,
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
            color = OnBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = track.artist,
            fontSize = 10.sp,
            color = OnBackgroundVariant.copy(alpha = 0.7f),
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
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "mixScale",
    )

    Box(
        modifier = Modifier
            .width(150.dp)
            .height(180.dp)
            .scale(scale)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.1f),
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

// ── Playlist Card ───────────────────────────────────────────────────────

@Composable
private fun PlaylistCard(playlist: PlaylistEntity, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "playlistScale",
    )

    Box(
        modifier = Modifier
            .width(150.dp)
            .height(180.dp)
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
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
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column {
                Text(
                    text = playlist.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Playlist",
                    fontSize = 11.sp,
                    color = OnBackgroundVariant.copy(alpha = 0.7f),
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
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "trendScale",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPlaying) AccentGreen.copy(alpha = 0.06f) else Color.Transparent)
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
            color = if (rank <= 3) AccentGreen else OnBackgroundVariant.copy(alpha = 0.5f),
            modifier = Modifier.width(32.dp),
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceVariant),
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
                Icon(imageVector = Icons.Filled.MusicNote, contentDescription = null, tint = OnBackgroundVariant, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = OnBackground,
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
        if (isPlaying) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = "Playing",
                tint = AccentGreen,
                modifier = Modifier.size(16.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.TrendingUp,
                contentDescription = null,
                tint = AccentGreen.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
