package com.musicflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.musicflow.app.data.local.entity.PlaylistEntity
import com.musicflow.app.ui.theme.AccentGreen
import com.musicflow.app.ui.theme.DarkSurface
import com.musicflow.app.ui.theme.DarkSurfaceVariant
import com.musicflow.app.ui.theme.OnBackground
import com.musicflow.app.ui.theme.OnBackgroundVariant

/**
 * Dialog for selecting a playlist to add a track to.
 * Also allows creating a new playlist directly from the dialog.
 */
@Composable
fun AddToPlaylistDialog(
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (Long) -> Unit,
    onCreatePlaylist: (String) -> Unit = {},
) {
    var showCreateInput by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = "Add to Playlist",
                color = OnBackground,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column {
                // Create new playlist option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentGreen.copy(alpha = 0.15f))
                        .clickable { showCreateInput = !showCreateInput }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (showCreateInput) "Cancel" else "+ Create New Playlist",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AccentGreen,
                    )
                }

                if (showCreateInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            placeholder = { Text("Playlist name", color = OnBackgroundVariant) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant,
                                focusedTextColor = OnBackground,
                                unfocusedTextColor = OnBackground,
                                cursorColor = AccentGreen,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                if (newPlaylistName.isNotBlank()) {
                                    onCreatePlaylist(newPlaylistName)
                                    newPlaylistName = ""
                                    showCreateInput = false
                                }
                            },
                            enabled = newPlaylistName.isNotBlank(),
                        ) {
                            Text("Create", color = if (newPlaylistName.isNotBlank()) AccentGreen else OnBackgroundVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Existing playlists
                if (playlists.isEmpty() && !showCreateInput) {
                    Text(
                        text = "No playlists yet. Create one above!",
                        color = OnBackgroundVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn {
                        items(playlists) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onPlaylistSelected(playlist.id)
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = OnBackground,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OnBackgroundVariant)
            }
        },
    )
}