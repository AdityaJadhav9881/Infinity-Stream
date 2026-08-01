package com.musicflow.app.ui.screens

import com.musicflow.app.ui.util.formatFileSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicflow.app.data.local.entity.OfflineTrackEntity
import com.musicflow.app.data.local.entity.PlaylistEntity
import com.musicflow.app.data.local.entity.TrackEntity
import com.musicflow.app.ui.theme.MFColors
import com.musicflow.app.ui.theme.MFGlass
import com.musicflow.app.ui.theme.MFTokens

// ── Main Screen ──────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (LibraryFilter) -> Unit,
    onTrackSelected: (TrackEntity) -> Unit,
    onTrackLongPress: (TrackEntity) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPlayOfflineTrack: (OfflineTrackEntity) -> Unit,
    onDeleteOfflineTrack: (String) -> Unit,
    onPlaylistSelected: (Long) -> Unit,
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSortMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MFColors.Background),
        contentPadding = PaddingValues(bottom = 120.dp),
    ) {
        item(key = "header") {
            Column(modifier = Modifier.padding(horizontal = MFTokens.ScreenHorizontalPadding, vertical = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Your Library",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MFColors.TextPrimary,
                        letterSpacing = (-0.8).sp,
                    )
                    Row {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Sort,
                                    contentDescription = "Sort",
                                    tint = MFColors.TextSecondary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Recently Added") },
                                    onClick = {
                                        onFilterChange(LibraryFilter.ALL)
                                        showSortMenu = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Recently Played") },
                                    onClick = {
                                        onFilterChange(LibraryFilter.RECENT)
                                        showSortMenu = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Title A-Z") },
                                    onClick = {
                                        onFilterChange(LibraryFilter.TITLE_ASC)
                                        showSortMenu = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Artist A-Z") },
                                    onClick = {
                                        onFilterChange(LibraryFilter.ARTIST_ASC)
                                        showSortMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "search") {
            MFGlass.GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MFTokens.ScreenHorizontalPadding),
                cornerRadius = MFTokens.MediumRadius,
                alpha = 0.08f,
            ) {
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search your library...", color = MFColors.TextTertiary) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = MFColors.TextTertiary, modifier = Modifier.size(20.dp))
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = MFColors.TextPrimary,
                        unfocusedTextColor = MFColors.TextPrimary,
                        cursorColor = MFColors.Accent,
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item(key = "filters") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(LibraryFilter.entries.filter { it != LibraryFilter.TITLE_ASC && it != LibraryFilter.ARTIST_ASC }) { filter ->
                    val isSelected = uiState.selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterChange(filter) },
                        label = {
                            Text(
                                text = filter.label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MFColors.Accent,
                            selectedLabelColor = MFColors.TextOnAccent,
                            containerColor = MFColors.GlassMid,
                            labelColor = MFColors.TextSecondary,
                        ),
                        shape = RoundedCornerShape(100.dp),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MFColors.GlassBorder,
                            selectedBorderColor = MFColors.Accent.copy(alpha = 0.3f),
                            enabled = true,
                            selected = isSelected,
                        ),
                    )
                }
            }
        }

        when (uiState.selectedFilter) {
            LibraryFilter.ALL -> {
                val recentTracks = uiState.items.filter { it.track.lastPlayedAt > 0 }
                    .sortedByDescending { it.track.lastPlayedAt }
                    .take(10)
                if (recentTracks.isNotEmpty()) {
                    item(key = "continue_listening_header") {
                        SectionTitle("Continue Listening")
                    }
                    item(key = "continue_listening") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 22.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            itemsIndexed(recentTracks) { index, item ->
                                ContinueListeningCard(
                                    track = item.track,
                                    onClick = { onTrackSelected(item.track) },
                                    onLongClick = { onTrackLongPress(item.track) },
                                )
                            }
                        }
                    }
                }

                item(key = "playlists_header") {
                    SectionTitle("Your Playlists")
                }
                item(key = "playlists_grid") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            LikedSongsCard(
                                count = uiState.items.count { it.isFavorite },
                                onClick = { onFilterChange(LibraryFilter.FAVORITES) },
                            )
                        }
                        item {
                            CreatePlaylistCard(onClick = onCreatePlaylist)
                        }
                        items(uiState.playlists) { playlist ->
                            PlaylistGridCard(
                                playlist = playlist,
                                onClick = { onPlaylistSelected(playlist.id) },
                            )
                        }
                    }
                }

                if (uiState.offlineTracks.isNotEmpty()) {
                    item(key = "downloads_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = MFTokens.ScreenHorizontalPadding, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Downloads",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MFColors.TextPrimary,
                            )
                            Text(
                                text = formatFileSize(uiState.offlineStorageUsedBytes),
                                fontSize = 13.sp,
                                color = MFColors.Accent,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    item(key = "downloads_list") {
                        Column(modifier = Modifier.padding(horizontal = MFTokens.ScreenHorizontalPadding)) {
                            uiState.offlineTracks.take(5).forEach { offlineTrack ->
                                OfflineTrackRow(
                                    track = offlineTrack,
                                    onClick = { onPlayOfflineTrack(offlineTrack) },
                                    onDelete = { onDeleteOfflineTrack(offlineTrack.songId) },
                                )
                            }
                        }
                    }
                }

                val mostPlayed = uiState.items.filter { it.track.playCount > 0 }
                    .sortedByDescending { it.track.playCount }
                    .take(10)
                if (mostPlayed.isNotEmpty()) {
                    item(key = "most_played_header") {
                        SectionTitle("Most Played")
                    }
                    itemsIndexed(mostPlayed) { index, item ->
                        MostPlayedRow(
                            track = item.track,
                            rank = index + 1,
                            isFavorite = item.isFavorite,
                            onClick = { onTrackSelected(item.track) },
                            onLongClick = { onTrackLongPress(item.track) },
                            onToggleFavorite = { onToggleFavorite(item.track.songId) },
                            modifier = Modifier.padding(horizontal = MFTokens.ScreenHorizontalPadding),
                        )
                    }
                }
            }

            LibraryFilter.TITLE_ASC, LibraryFilter.ARTIST_ASC -> {
                if (uiState.items.isEmpty()) {
                    item(key = "empty_sorted") {
                        FilterEmptyState(
                            icon = Icons.Filled.MusicNote,
                            title = "No tracks",
                            subtitle = "Songs you play will appear here.",
                        )
                    }
                } else {
                    item(key = "sorted_count") {
                        Text(
                            text = "${uiState.items.size} tracks",
                            fontSize = 14.sp,
                            color = MFColors.Accent,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                        )
                    }
                    itemsIndexed(uiState.items) { index, item ->
                        MostPlayedRow(
                            track = item.track,
                            rank = index + 1,
                            isFavorite = item.isFavorite,
                            onClick = { onTrackSelected(item.track) },
                            onLongClick = { onTrackLongPress(item.track) },
                            onToggleFavorite = { onToggleFavorite(item.track.songId) },
                            modifier = Modifier.padding(horizontal = MFTokens.ScreenHorizontalPadding),
                        )
                    }
                }
            }

            LibraryFilter.FAVORITES -> {
                val favoriteTracks = uiState.items.filter { it.isFavorite }
                if (favoriteTracks.isEmpty()) {
                    item(key = "empty_favorites") {
                        FilterEmptyState(
                            icon = Icons.Filled.Favorite,
                            title = "No liked songs",
                            subtitle = "Songs you like will appear here.\nTap the heart icon on any track to add it.",
                        )
                    }
                } else {
                    item(key = "favorites_count") {
                        Text(
                            text = "${favoriteTracks.size} liked songs",
                            fontSize = 14.sp,
                            color = MFColors.Accent,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                        )
                    }
                    itemsIndexed(favoriteTracks) { index, item ->
                        MostPlayedRow(
                            track = item.track,
                            rank = index + 1,
                            isFavorite = true,
                            onClick = { onTrackSelected(item.track) },
                            onLongClick = { onTrackLongPress(item.track) },
                            onToggleFavorite = { onToggleFavorite(item.track.songId) },
                            modifier = Modifier.padding(horizontal = MFTokens.ScreenHorizontalPadding),
                        )
                    }
                }
            }

            LibraryFilter.RECENT -> {
                val recentTracks = uiState.items.filter { it.track.lastPlayedAt > 0 }
                    .sortedByDescending { it.track.lastPlayedAt }
                if (recentTracks.isEmpty()) {
                    item(key = "empty_recent") {
                        FilterEmptyState(
                            icon = Icons.Filled.Star,
                            title = "No recent tracks",
                            subtitle = "Songs you play will appear here.",
                        )
                    }
                } else {
                    item(key = "recent_count") {
                        Text(
                            text = "${recentTracks.size} recent tracks",
                            fontSize = 14.sp,
                            color = MFColors.Accent,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                        )
                    }
                    itemsIndexed(recentTracks) { index, item ->
                        MostPlayedRow(
                            track = item.track,
                            rank = index + 1,
                            isFavorite = item.isFavorite,
                            onClick = { onTrackSelected(item.track) },
                            onLongClick = { onTrackLongPress(item.track) },
                            onToggleFavorite = { onToggleFavorite(item.track.songId) },
                            modifier = Modifier.padding(horizontal = MFTokens.ScreenHorizontalPadding),
                        )
                    }
                }
            }

            LibraryFilter.DOWNLOADS -> {
                if (uiState.offlineTracks.isEmpty()) {
                    item(key = "empty_downloads") {
                        FilterEmptyState(
                            icon = Icons.Filled.CloudDownload,
                            title = "No downloads",
                            subtitle = "Download songs to listen offline.\nLong-press a track and tap Download.",
                        )
                    }
                } else {
                    item(key = "downloads_count") {
                        Text(
                            text = "${uiState.offlineTracks.size} downloaded songs \u00B7 ${formatFileSize(uiState.offlineStorageUsedBytes)}",
                            fontSize = 14.sp,
                            color = MFColors.Accent,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                        )
                    }
                    items(uiState.offlineTracks) { offlineTrack ->
                        OfflineTrackRow(
                            track = offlineTrack,
                            onClick = { onPlayOfflineTrack(offlineTrack) },
                            onDelete = { onDeleteOfflineTrack(offlineTrack.songId) },
                            modifier = Modifier.padding(horizontal = MFTokens.ScreenHorizontalPadding),
                        )
                    }
                }
            }

            LibraryFilter.PLAYLISTS -> {
                if (uiState.playlists.isEmpty()) {
                    item(key = "empty_playlists") {
                        FilterEmptyState(
                            icon = Icons.Filled.PlaylistPlay,
                            title = "No playlists",
                            subtitle = "Create a playlist to organize your music.",
                            actionLabel = "Create Playlist",
                            onAction = onCreatePlaylist,
                        )
                    }
                } else {
                    item(key = "playlists_count") {
                        Text(
                            text = "${uiState.playlists.size} playlists",
                            fontSize = 14.sp,
                            color = MFColors.Accent,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
                        )
                    }
                    item(key = "create_playlist_inline") {
                        CreatePlaylistCard(onClick = onCreatePlaylist, modifier = Modifier.padding(horizontal = MFTokens.ScreenHorizontalPadding))
                    }
                    items(uiState.playlists) { playlist ->
                        PlaylistRow(
                            playlist = playlist,
                            onClick = { onPlaylistSelected(playlist.id) },
                            modifier = Modifier.padding(horizontal = MFTokens.ScreenHorizontalPadding),
                        )
                    }
                }
            }

            LibraryFilter.ALBUMS -> {
                item(key = "empty_albums") {
                    FilterEmptyState(
                        icon = Icons.Filled.Album,
                        title = "No albums",
                        subtitle = "Albums you listen to will appear here.",
                    )
                }
            }

            LibraryFilter.ARTISTS -> {
                item(key = "empty_artists") {
                    FilterEmptyState(
                        icon = Icons.Filled.Person,
                        title = "No artists",
                        subtitle = "Artists you follow will appear here.",
                    )
                }
            }
        }

        if (uiState.isLoading) {
            item(key = "loading") {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MFColors.Accent, strokeWidth = 2.dp)
                }
            }
        }

        if (!uiState.isLoading && uiState.allItems.isEmpty() && uiState.playlists.isEmpty()) {
            item(key = "empty") {
                EmptyLibraryState()
            }
        }
    }
}

// ── Components ───────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MFColors.TextPrimary,
        letterSpacing = (-0.4).sp,
        modifier = Modifier.padding(horizontal = MFTokens.ScreenHorizontalPadding, vertical = 10.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueListeningCard(
    track: TrackEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    MFGlass.GlassPanel(
        modifier = Modifier.width(136.dp),
        cornerRadius = MFTokens.MediumRadius,
        alpha = 0.08f,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(RoundedCornerShape(14.dp)),
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
                        Icon(imageVector = Icons.Filled.MusicNote, contentDescription = null, tint = MFColors.Accent, modifier = Modifier.size(32.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = track.title,
                fontSize = 11.sp,
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
}

@Composable
private fun LikedSongsCard(count: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(136.dp)
            .height(170.dp)
            .clip(MFTokens.MediumRadius)
            .background(
                Brush.verticalGradient(
                    colors = listOf(MFColors.Accent, MFColors.Accent.copy(alpha = 0.7f))
                )
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = MFColors.TextOnAccent,
                modifier = Modifier.size(28.dp),
            )
            Column {
                Text(
                    text = "Liked Songs",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MFColors.TextOnAccent,
                )
                Text(
                    text = "$count tracks",
                    fontSize = 11.sp,
                    color = MFColors.TextOnAccent.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun CreatePlaylistCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    MFGlass.GlassPanel(
        modifier = modifier.width(136.dp).height(170.dp),
        cornerRadius = MFTokens.MediumRadius,
        alpha = 0.08f,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MFColors.Accent.copy(alpha = 0.15f))
                        .border(width = 0.5.dp, color = MFColors.Accent.copy(alpha = 0.2f), shape = RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = MFColors.Accent, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Create", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MFColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun PlaylistGridCard(playlist: PlaylistEntity, onClick: () -> Unit) {
    MFGlass.GlassPanel(
        modifier = Modifier.width(148.dp).height(184.dp),
        cornerRadius = MFTokens.MediumRadius,
        alpha = 0.08f,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().clickable(onClick = onClick).padding(16.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MFColors.Accent.copy(alpha = 0.12f))
                        .border(width = 0.5.dp, color = MFColors.Accent.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = Icons.Filled.PlaylistPlay, contentDescription = null, tint = MFColors.Accent, modifier = Modifier.size(24.dp))
                }
                Column {
                    Text(
                        text = playlist.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MFColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Playlist",
                        fontSize = 11.sp,
                        color = MFColors.TextTertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun OfflineTrackRow(track: OfflineTrackEntity, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MFColors.GlassMid)
                .border(width = 0.5.dp, color = MFColors.GlassBorder, shape = RoundedCornerShape(10.dp)),
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
                Icon(imageVector = Icons.Filled.MusicNote, contentDescription = null, tint = MFColors.TextTertiary, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = track.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MFColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = track.artist, fontSize = 11.sp, color = MFColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(text = formatFileSize(track.fileSize), fontSize = 10.sp, color = MFColors.Accent, modifier = Modifier.padding(end = 6.dp))
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "Remove", tint = MFColors.Error, modifier = Modifier.size(16.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MostPlayedRow(
    track: TrackEntity,
    rank: Int,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$rank",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (rank <= 3) MFColors.Accent else MFColors.TextTertiary,
            modifier = Modifier.width(28.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MFColors.GlassMid)
                .border(width = 0.5.dp, color = MFColors.GlassBorder, shape = RoundedCornerShape(12.dp)),
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
                Icon(imageVector = Icons.Filled.MusicNote, contentDescription = null, tint = MFColors.TextTertiary, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MFColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                fontSize = 11.sp,
                color = MFColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isFavorite) "Unlike" else "Like",
                tint = if (isFavorite) MFColors.Error else MFColors.TextTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun FilterEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 50.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MFGlass.GlassPanel(
            modifier = Modifier.size(88.dp),
            cornerRadius = RoundedCornerShape(88.dp),
            alpha = 0.08f,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MFColors.Accent.copy(alpha = 0.5f),
                    modifier = Modifier.size(42.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MFColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = MFColors.TextSecondary,
            lineHeight = 20.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            androidx.compose.material3.Button(
                onClick = onAction,
                shape = RoundedCornerShape(25.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MFColors.Accent),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 10.dp),
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MFColors.TextOnAccent,
                )
            }
        }
    }
}

@Composable
private fun PlaylistRow(playlist: PlaylistEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MFColors.Accent.copy(alpha = 0.12f))
                .border(width = 0.5.dp, color = MFColors.Accent.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlaylistPlay,
                contentDescription = null,
                tint = MFColors.Accent,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MFColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Playlist",
                fontSize = 13.sp,
                color = MFColors.TextTertiary,
            )
        }
    }
}

@Composable
private fun EmptyLibraryState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MFGlass.GlassPanel(
            modifier = Modifier.size(88.dp),
            cornerRadius = RoundedCornerShape(88.dp),
            alpha = 0.08f,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.LibraryMusic,
                    contentDescription = null,
                    tint = MFColors.Accent.copy(alpha = 0.5f),
                    modifier = Modifier.size(42.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "No music yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MFColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Songs you play will appear here.\nSearch for music to get started.",
            fontSize = 14.sp,
            color = MFColors.TextSecondary,
            lineHeight = 20.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────

