package com.musicflow.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.musicflow.app.ui.theme.AccentGreen
import com.musicflow.app.ui.theme.DarkSurface
import com.musicflow.app.ui.theme.DarkSurfaceVariant
import com.musicflow.app.ui.theme.OnBackground
import com.musicflow.app.ui.theme.OnBackgroundVariant

/**
 * Dialog for selecting sleep timer duration.
 *
 * Offers preset durations (15, 30, 45, 60, 90 minutes) and a custom input field.
 */
@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onStartTimer: (Int) -> Unit, // minutes
) {
    val presets = listOf(15, 30, 45, 60, 90)
    var customMinutes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Text(
                text = "Sleep Timer",
                color = OnBackground,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column {
                Text(
                    text = "Stop playback after:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnBackgroundVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Preset buttons
                presets.chunked(3).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        row.forEach { minutes ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .clickable {
                                        onStartTimer(minutes)
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "${minutes}m",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = OnBackground,
                                )
                            }
                        }
                        // Fill remaining space if row is not full
                        repeat(3 - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom input
                Text(
                    text = "Custom (minutes):",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBackgroundVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextField(
                        value = customMinutes,
                        onValueChange = { value ->
                            if (value.all { it.isDigit() } && value.length <= 3) {
                                customMinutes = value
                            }
                        },
                        placeholder = { Text("Min", color = OnBackgroundVariant) },
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
                        modifier = Modifier.width(80.dp),
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    TextButton(
                        onClick = {
                            customMinutes.toIntOrNull()?.let { minutes ->
                                if (minutes > 0) {
                                    onStartTimer(minutes)
                                    onDismiss()
                                }
                            }
                        },
                        enabled = customMinutes.toIntOrNull()?.let { it > 0 } == true,
                    ) {
                        Text(
                            text = "Start",
                            color = if (customMinutes.toIntOrNull()?.let { it > 0 } == true) AccentGreen else OnBackgroundVariant,
                        )
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