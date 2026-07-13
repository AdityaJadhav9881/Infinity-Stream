package com.musicflow.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicflow.app.data.remote.SearchResult
import com.musicflow.app.ui.components.ShimmerLoading
import com.musicflow.app.ui.theme.AccentGreen
import com.musicflow.app.ui.theme.Black
import com.musicflow.app.ui.theme.DarkSurface
import com.musicflow.app.ui.theme.DarkSurfaceVariant
import com.musicflow.app.ui.theme.MFColors
import com.musicflow.app.ui.theme.MFTokens
import com.musicflow.app.ui.theme.OnBackground
import com.musicflow.app.ui.theme.OnBackgroundVariant
import com.musicflow.app.data.local.entity.PlaylistEntity

/**
 * Search screen with autocomplete suggestions, search history, and filters.
 */
@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onTrackSelected: (SearchResult) -> Unit,
    onErrorDismissed: () -> Unit,
    onSuggestionSelected: (String) -> Unit = {},
    onFilterChange: (SearchFilter) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onDeleteHistoryItem: (String) -> Unit = {},
    onSuggestionsDismissed: () -> Unit = {},
    onAddToPlaylist: (SearchResult) -> Unit = {},
    onTrackLongPress: (SearchResult) -> Unit = {},
    onArtistSelected: (SearchResult) -> Unit = {},
    onAlbumSelected: (SearchResult) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showSuggestions by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            onErrorDismissed()
        }
    }

    LaunchedEffect(uiState.query) {
        showSuggestions = uiState.query.isNotBlank() && uiState.suggestions.isNotEmpty()
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MFColors.Background)
                .padding(horizontal = MFTokens.ScreenHorizontalPadding),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Search Bar ──────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()) {
                SearchBar(
                    query = uiState.query,
                    onQueryChange = onSearchQueryChange,
                    onClear = onClearSearch,
                    isLoading = uiState.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                )

                // ── Suggestions Dropdown ────────────────────────────
                if (showSuggestions) {
                    DropdownMenu(
                        expanded = showSuggestions,
                        onDismissRequest = onSuggestionsDismissed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface),
                    ) {
                        uiState.suggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Search,
                                            contentDescription = null,
                                            tint = OnBackgroundVariant,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = suggestion,
                                            color = OnBackground,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                },
                                onClick = {
                                    onSuggestionSelected(suggestion)
                                    showSuggestions = false
                                },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Filter Chips ────────────────────────────────────────
            if (uiState.query.isNotBlank() || uiState.results.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(SearchFilter.entries) { filter ->
                        val selected = uiState.selectedFilter == filter
                        val bgColor = if (selected) AccentGreen else DarkSurfaceVariant
                        val textColor = if (selected) Black else OnBackgroundVariant

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(bgColor)
                                .clickable { onFilterChange(filter) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = textColor,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Search History (when query is empty) ────────────────
            if (uiState.query.isBlank() && uiState.searchHistory.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Recent Searches",
                        style = MaterialTheme.typography.titleSmall,
                        color = OnBackgroundVariant,
                    )
                    TextButton(onClick = onClearHistory) {
                        Text("Clear all", color = AccentGreen)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(
                        items = uiState.searchHistory,
                        key = { it },
                    ) { query ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSearchQueryChange(query)
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = OnBackgroundVariant,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = query,
                                style = MaterialTheme.typography.bodyMedium,
                                color = OnBackground,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { onDeleteHistoryItem(query) },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Remove",
                                    tint = OnBackgroundVariant,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                    }
                }
            } else {
                // ── Search Results ──────────────────────────────────
                when {
                    uiState.isLoading && uiState.results.isEmpty() -> {
                        ShimmerLoading(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            itemCount = 6,
                        )
                    }

                    uiState.results.isEmpty() && uiState.query.isNotBlank() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = OnBackgroundVariant,
                                    modifier = Modifier.size(64.dp),
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No results found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = OnBackgroundVariant,
                                )
                            }
                        }
                    }

                    uiState.query.isBlank() && uiState.searchHistory.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = OnBackgroundVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(80.dp),
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Search for songs",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = OnBackgroundVariant,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Find your favorite music",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnBackgroundVariant.copy(alpha = 0.6f),
                                )
                            }
                        }
                    }

                    else -> {
                        val filteredResults = when (uiState.selectedFilter) {
                            SearchFilter.ALL -> uiState.results
                            SearchFilter.SONGS -> uiState.results
                            SearchFilter.ALBUMS,
                            SearchFilter.ARTISTS,
                            SearchFilter.PLAYLISTS -> uiState.results
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            when (uiState.selectedFilter) {
                                SearchFilter.ARTISTS -> {
                                    items(
                                        items = filteredResults,
                                        key = { it.videoId },
                                    ) { result ->
                                        ArtistResultItem(
                                            result = result,
                                            onClick = { onArtistSelected(result) },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                                SearchFilter.ALBUMS -> {
                                    items(
                                        items = filteredResults,
                                        key = { it.videoId },
                                    ) { result ->
                                        AlbumResultItem(
                                            result = result,
                                            onClick = { onAlbumSelected(result) },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                                SearchFilter.PLAYLISTS -> {
                                    items(
                                        items = filteredResults,
                                        key = { it.videoId },
                                    ) { result ->
                                        PlaylistResultItem(
                                            result = result,
                                            onClick = { onTrackSelected(result) },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                                else -> {
                                    items(
                                        items = filteredResults,
                                        key = { it.videoId },
                                    ) { result ->
                                        SearchResultItem(
                                            result = result,
                                            onClick = { onTrackSelected(result) },
                                            onLongClick = { onTrackLongPress(result) },
                                            onAddToPlaylist = { onAddToPlaylist(result) },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Snackbar Host ──────────────────────────────────────────
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = DarkSurfaceVariant,
                contentColor = OnBackground,
            )
        }
    }
}

// ── Search Bar ──────────────────────────────────────────────────────────

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = "Search songs, artists...",
                color = MFColors.TextTertiary,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = MFColors.TextTertiary,
            )
        },
        trailingIcon = {
            Row {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MFColors.Accent,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(4.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                AnimatedVisibility(
                    visible = query.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Clear",
                            tint = MFColors.TextTertiary,
                        )
                    }
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MFColors.Elevated,
            unfocusedContainerColor = MFColors.Elevated,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = MFColors.TextPrimary,
            unfocusedTextColor = MFColors.TextPrimary,
            cursorColor = MFColors.Accent,
        ),
        shape = MFTokens.MediumRadius,
        singleLine = true,
        modifier = modifier,
    )
}

// ── Search Result Item ──────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResultItem(
    result: SearchResult,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onAddToPlaylist: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(MFTokens.MediumRadius)
            .background(MFColors.Card)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchResultThumbnail(
            thumbnailUrl = result.thumbnailUrl,
            modifier = Modifier.size(56.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MFColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = result.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MFColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(
            onClick = onAddToPlaylist,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.QueueMusic,
                contentDescription = "Add to Playlist",
                tint = MFColors.TextTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ── Search Result Thumbnail ─────────────────────────────────────────────

@Composable
private fun SearchResultThumbnail(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MFColors.Elevated),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailUrl)
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
                tint = MFColors.TextTertiary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

// ── Artist Result Item ──────────────────────────────────────────────────

@Composable
private fun ArtistResultItem(
    result: SearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(OnBackgroundVariant.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            if (result.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(result.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
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

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Artist",
                style = MaterialTheme.typography.bodyMedium,
                color = OnBackgroundVariant,
                maxLines = 1,
            )
        }
    }
}

// ── Album Result Item ───────────────────────────────────────────────────

@Composable
private fun AlbumResultItem(
    result: SearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchResultThumbnail(
            thumbnailUrl = result.thumbnailUrl,
            modifier = Modifier.size(56.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = result.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = OnBackgroundVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = "Album",
            style = MaterialTheme.typography.labelSmall,
            color = OnBackgroundVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

// ── Playlist Result Item ────────────────────────────────────────────────

@Composable
private fun PlaylistResultItem(
    result: SearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(OnBackgroundVariant.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            if (result.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(result.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.QueueMusic,
                    contentDescription = null,
                    tint = OnBackgroundVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = result.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = OnBackgroundVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = "Playlist",
            style = MaterialTheme.typography.labelSmall,
            color = OnBackgroundVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}