package com.musicflow.app.ui.screens

import android.widget.Toast
import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicflow.app.data.local.entity.TrackEntity
import com.musicflow.app.ui.theme.AccentGreen
import com.musicflow.app.ui.theme.DarkSurface
import com.musicflow.app.ui.theme.DarkSurfaceVariant
import com.musicflow.app.ui.theme.ErrorRed
import com.musicflow.app.ui.theme.OnBackground
import com.musicflow.app.ui.theme.OnBackgroundVariant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlistName: String,
    playlistViewModel: PlaylistViewModel,
    onTrackSelected: (TrackEntity) -> Unit,
    onTrackSelectedWithContext: (TrackEntity, List<TrackEntity>, Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tracks by playlistViewModel.getPlaylistTracksFlow(playlistId)
        .collectAsState(initial = emptyList())
    var hasLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(tracks) {
        if (!hasLoaded && tracks.isNotEmpty()) {
            hasLoaded = true
        } else if (!hasLoaded && tracks.isEmpty()) {
            // Wait a bit to distinguish initial load from genuinely empty
            kotlinx.coroutines.delay(300)
            hasLoaded = true
        }
    }

    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(playlistName) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = playlistName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = OnBackground,
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    newName = playlistName
                    showRenameDialog = true
                }) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Rename",
                        tint = OnBackgroundVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(onClick = { showDuplicateDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Duplicate",
                        tint = OnBackgroundVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(onClick = {
                    val shareText = buildString {
                        append("Playlist: $playlistName\n")
                        append("${tracks.size} tracks\n\n")
                        tracks.forEachIndexed { index, track ->
                            append("${index + 1}. ${track.title} - ${track.artist}\n")
                        }
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Playlist"))
                }) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = OnBackgroundVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = ErrorRed,
                        modifier = Modifier.size(22.dp),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
        )

        // Playlist Stats
        if (tracks.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                StatItem(label = "Tracks", value = "${tracks.size}")
                StatItem(
                    label = "Duration",
                    value = formatTotalDuration(tracks.sumOf { it.durationMs })
                )
            }
        }

        Text(
            text = "${tracks.size} tracks",
            style = MaterialTheme.typography.bodySmall,
            color = OnBackgroundVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )

        if (!hasLoaded) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AccentGreen, strokeWidth = 2.dp)
            }
        } else if (tracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.LibraryMusic,
                        contentDescription = null,
                        tint = OnBackgroundVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(80.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No tracks yet",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnBackgroundVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add songs from the library",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnBackgroundVariant.copy(alpha = 0.6f),
                    )
                }
            }
        } else {
            // Play All button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                androidx.compose.material3.Button(
                    onClick = {
                        if (tracks.isNotEmpty()) {
                            onTrackSelectedWithContext(tracks.first(), tracks, 0)
                        }
                    },
                    shape = RoundedCornerShape(25.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = AccentGreen
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 24.dp, vertical = 10.dp
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Play All", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(items = tracks, key = { _, track -> track.songId }) { index, track ->
                    PlaylistTrackItem(
                        track = track,
                        index = index + 1,
                        onClick = { onTrackSelectedWithContext(track, tracks, index) },
                        onRemove = {
                            playlistViewModel.removeTrackFromPlaylist(playlistId, track.songId)
                            Toast.makeText(context, "Removed from playlist", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = DarkSurface,
            title = { Text("Rename Playlist", color = OnBackground) },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Playlist name", color = OnBackgroundVariant) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = OnBackground,
                        unfocusedTextColor = OnBackground,
                        cursorColor = AccentGreen,
                        focusedIndicatorColor = AccentGreen,
                        unfocusedIndicatorColor = OnBackgroundVariant,
                    ),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        playlistViewModel.renamePlaylist(playlistId, newName)
                        showRenameDialog = false
                        Toast.makeText(context, "Playlist renamed", Toast.LENGTH_SHORT).show()
                    },
                    enabled = newName.isNotBlank() && newName != playlistName,
                ) {
                    Text("Rename", color = if (newName.isNotBlank() && newName != playlistName) AccentGreen else OnBackgroundVariant)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = OnBackgroundVariant)
                }
            },
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = DarkSurface,
            title = { Text("Delete Playlist?", color = OnBackground) },
            text = {
                Text(
                    "Are you sure you want to delete \"$playlistName\"? This action cannot be undone.",
                    color = OnBackgroundVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    playlistViewModel.deletePlaylist(playlistId)
                    showDeleteDialog = false
                    onBack()
                }) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = OnBackgroundVariant)
                }
            },
        )
    }

    // Duplicate Dialog
    if (showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = { showDuplicateDialog = false },
            containerColor = DarkSurface,
            title = { Text("Duplicate Playlist?", color = OnBackground) },
            text = {
                Text(
                    "Create a copy of \"$playlistName\" with all ${tracks.size} tracks?",
                    color = OnBackgroundVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    playlistViewModel.duplicatePlaylist(playlistId)
                    showDuplicateDialog = false
                    Toast.makeText(context, "Playlist duplicated", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Duplicate", color = AccentGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateDialog = false }) {
                    Text("Cancel", color = OnBackgroundVariant)
                }
            },
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AccentGreen,
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = OnBackgroundVariant,
        )
    }
}

private fun formatTotalDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0 min"
    val totalMinutes = durationMs / 60000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes} min"
}

@Composable
private fun PlaylistTrackItem(
    track: TrackEntity,
    index: Int,
    onClick: () -> Unit,
    onRemove: () -> Unit,
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
        Text(
            text = "$index",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = OnBackgroundVariant,
            modifier = Modifier.width(28.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(OnBackgroundVariant.copy(alpha = 0.2f)),
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
                    tint = OnBackgroundVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = OnBackgroundVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Remove from playlist",
                tint = ErrorRed,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
