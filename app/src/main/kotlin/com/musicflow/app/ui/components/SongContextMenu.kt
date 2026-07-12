package com.musicflow.app.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicflow.app.data.remote.SearchResult
import com.musicflow.app.ui.theme.AccentGreen
import com.musicflow.app.ui.theme.DarkSurface
import com.musicflow.app.ui.theme.DarkSurfaceVariant
import com.musicflow.app.ui.theme.OnBackground
import com.musicflow.app.ui.theme.OnBackgroundVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongContextMenu(
    track: SearchResult,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onShare: (SearchResult) -> Unit,
    onAddToPlaylist: (SearchResult) -> Unit,
    onStartRadio: (SearchResult) -> Unit,
    onPlayNext: (SearchResult) -> Unit,
    onEnqueue: (SearchResult) -> Unit,
    onGoToArtist: (String) -> Unit,
    onGoToAlbum: (String) -> Unit,
    onDownload: (SearchResult) -> Unit = {},
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
            // Handle
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

            // Song info header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (track.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(track.thumbnailUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize(),
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
            }

            // Action items
            ContextMenuItem(
                icon = Icons.Filled.Share,
                label = "Share",
                onClick = {
                    onShare(track)
                    onDismiss()
                },
            )
            ContextMenuItem(
                icon = Icons.Filled.QueueMusic,
                label = "Add to playlist",
                onClick = {
                    onAddToPlaylist(track)
                    onDismiss()
                },
            )
            ContextMenuItem(
                icon = Icons.Filled.Radio,
                label = "Start radio",
                onClick = {
                    onStartRadio(track)
                    onDismiss()
                },
            )
            ContextMenuItem(
                icon = Icons.Filled.PlayArrow,
                label = "Play next",
                onClick = {
                    onPlayNext(track)
                    onDismiss()
                },
            )
            ContextMenuItem(
                icon = Icons.Filled.Queue,
                label = "Enqueue",
                onClick = {
                    onEnqueue(track)
                    onDismiss()
                },
            )
            ContextMenuItem(
                icon = Icons.Filled.Download,
                label = "Download for offline",
                onClick = {
                    onDownload(track)
                    onDismiss()
                },
            )
            ContextMenuItem(
                icon = Icons.Filled.Person,
                label = "Go to artist",
                onClick = {
                    onGoToArtist(track.artist)
                    onDismiss()
                },
            )
            ContextMenuItem(
                icon = Icons.Filled.Album,
                label = "Go to album",
                onClick = {
                    onGoToAlbum(track.title)
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OnBackgroundVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = OnBackground,
        )
    }
}

fun shareTrack(context: Context, track: SearchResult) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${track.videoId}")
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, null))
}
