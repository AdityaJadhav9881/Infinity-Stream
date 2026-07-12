package com.musicflow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.musicflow.app.data.remote.AlbumPage
import com.musicflow.app.data.remote.SearchResult
import com.musicflow.app.ui.theme.AccentGreen
import com.musicflow.app.ui.theme.Black
import com.musicflow.app.ui.theme.DarkSurfaceVariant
import com.musicflow.app.ui.theme.OnBackground
import com.musicflow.app.ui.theme.OnBackgroundVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    browseId: String,
    albumPage: AlbumPage?,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onTrackSelected: (SearchResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Black),
    ) {
        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = OnBackground,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = AccentGreen)
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = error,
                        color = OnBackgroundVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            albumPage != null -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Album header
                    item {
                        AlbumHeader(albumPage)
                    }

                    // Track list
                    itemsIndexed(
                        items = albumPage.tracks,
                        key = { _, track -> track.videoId },
                    ) { index, track ->
                        AlbumTrackItem(
                            track = track,
                            index = index + 1,
                            onClick = { onTrackSelected(track) },
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumHeader(album: AlbumPage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Album thumbnail
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (album.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(album.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = OnBackgroundVariant,
                    modifier = Modifier.size(48.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = album.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = OnBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodyLarge,
            color = AccentGreen,
        )

        if (!album.year.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = album.year,
                style = MaterialTheme.typography.bodyMedium,
                color = OnBackgroundVariant,
            )
        }
    }
}

@Composable
private fun AlbumTrackItem(
    track: SearchResult,
    index: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$index",
            style = MaterialTheme.typography.bodyMedium,
            color = OnBackgroundVariant,
            modifier = Modifier.width(28.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                color = OnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (track.artist.isNotBlank()) {
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBackgroundVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
