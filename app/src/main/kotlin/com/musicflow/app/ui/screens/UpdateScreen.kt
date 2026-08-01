package com.musicflow.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicflow.app.ui.theme.AccentGreen
import com.musicflow.app.ui.theme.ErrorRed
import com.musicflow.app.ui.theme.MFColors
import com.musicflow.app.ui.theme.OnBackground
import com.musicflow.app.ui.theme.OnBackgroundVariant
import com.musicflow.app.utils.DownloadState

@Composable
fun UpdateScreen(
    viewModel: UpdateViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MFColors.Background,
                        MFColors.Background,
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            if (!uiState.forced) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(
                        onClick = {
                            viewModel.dismissUpdate()
                            onDismiss()
                        }
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Dismiss",
                            tint = OnBackgroundVariant
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MFColors.AccentSubtle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = AccentGreen
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Update Available",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = OnBackground,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Version ${uiState.version}",
                style = MaterialTheme.typography.titleLarge,
                color = AccentGreen,
                fontWeight = FontWeight.SemiBold,
            )

            if (uiState.forced) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "This update is required to continue using the app",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErrorRed,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState.releaseNotes.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MFColors.GlassLow)
                        .padding(20.dp)
                ) {
                    Text(
                        text = "What's New",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = OnBackground,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiState.releaseNotes,
                        style = MaterialTheme.typography.bodyLarge,
                        color = OnBackgroundVariant,
                        lineHeight = 24.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            DownloadProgressSection(uiState = uiState)

            Spacer(modifier = Modifier.height(24.dp))

            ActionButtons(
                uiState = uiState,
                onDownload = { viewModel.downloadUpdate() },
                onInstall = { viewModel.installOnly() },
                onRestart = { viewModel.restartApp() },
                onDismiss = {
                    viewModel.dismissUpdate()
                    onDismiss()
                },
                onClearError = { viewModel.clearError() },
            )
        }
    }
}

@Composable
private fun DownloadProgressSection(uiState: UpdateUiState) {
    val progress by animateFloatAsState(
        targetValue = when (val state = uiState.downloadState) {
            is DownloadState.Downloading -> state.progress / 100f
            is DownloadState.Downloaded -> 1f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    AnimatedVisibility(
        visible = uiState.downloadState !is DownloadState.Idle,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MFColors.GlassLow)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (val state = uiState.downloadState) {
                is DownloadState.Downloading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Downloading...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnBackground,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${state.progress}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AccentGreen,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AccentGreen,
                        trackColor = MFColors.ProgressTrack,
                    )
                    if (state.totalBytes > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${formatBytes(state.bytesDownloaded)} / ${formatBytes(state.totalBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnBackgroundVariant,
                        )
                    }
                }

                is DownloadState.Downloaded -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download complete",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AccentGreen,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AccentGreen,
                        trackColor = MFColors.ProgressTrack,
                    )
                }

                is DownloadState.Error -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.Error,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ErrorRed,
                        )
                    }
                }

                else -> {}
            }

            if (uiState.isInstalling) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = AccentGreen,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Opening installer...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnBackground,
                    )
                }
            }
        }
    }

    uiState.error?.let { error ->
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = ErrorRed,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ActionButtons(
    uiState: UpdateUiState,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onRestart: () -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
) {
    val isDownloading = uiState.downloadState is DownloadState.Downloading
    val isDownloaded = uiState.downloadState is DownloadState.Downloaded
    val isError = uiState.downloadState is DownloadState.Error

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Show "Restart App" button after install intent was launched
        if (uiState.isInstallLaunched) {
            Button(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = MFColors.TextOnAccent,
                )
            ) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Restart App",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Update installed. Restart to apply changes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MFColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
        // Show "Download & Install" button when not yet downloaded
        else if (!isDownloaded && !uiState.isInstalling) {
            Button(
                onClick = {
                    onClearError()
                    onDownload()
                },
                enabled = !isDownloading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = MFColors.TextOnAccent,
                    disabledContainerColor = AccentGreen.copy(alpha = 0.5f),
                    disabledContentColor = MFColors.TextOnAccent.copy(alpha = 0.5f),
                )
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MFColors.TextOnAccent,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = when {
                        isDownloading -> "Downloading..."
                        isError -> "Retry Download"
                        else -> "Download & Install"
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
        }

        // Show "Install Update" button when download is complete (calls install directly)
        if (isDownloaded && !uiState.isInstalling && !uiState.isInstallLaunched) {
            Button(
                onClick = onInstall,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = MFColors.TextOnAccent,
                )
            ) {
                Icon(
                    Icons.Outlined.InstallMobile,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Install Update",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
        }

        if (!uiState.forced) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    text = "Later",
                    color = OnBackgroundVariant,
                    fontSize = 14.sp,
                )
            }
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "You must install this update to continue",
                style = MaterialTheme.typography.bodySmall,
                color = OnBackgroundVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
}
