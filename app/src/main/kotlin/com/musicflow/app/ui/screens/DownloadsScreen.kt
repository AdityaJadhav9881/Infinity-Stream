package com.musicflow.app.ui.screens

import com.musicflow.app.ui.util.formatFileSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicflow.app.data.local.entity.OfflineTrackEntity
import com.musicflow.app.ui.theme.*

data class DownloadStats(
    val downloadedCount: Int = 0,
    val storageUsed: String = "0 MB",
    val activeDownloads: Int = 0,
    val failedDownloads: Int = 0,
)

data class StorageInfo(
    val musicSize: String = "0 MB",
    val cacheSize: String = "64 MB",
    val artworkSize: String = "25 MB",
    val usedPercent: Float = 0f,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    offlineTracks: List<OfflineTrackEntity>,
    offlineStorageUsedBytes: Long,
    onPlayOfflineTrack: (OfflineTrackEntity) -> Unit,
    onDeleteOfflineTrack: (String) -> Unit,
    onBack: () -> Unit,
    onBrowseMusic: () -> Unit,
    isDownloading: Boolean = false,
    downloadingTrackName: String? = null,
    downloadSuccess: String? = null,
    downloadError: String? = null,
    onDismissDownloadMessage: () -> Unit = {},
    downloadQuality: String = "High",
    onDownloadQualityChange: (String) -> Unit = {},
    wifiOnly: Boolean = true,
    onWifiOnlyChange: (Boolean) -> Unit = {},
    autoDownloadLiked: Boolean = false,
    onAutoDownloadChange: (Boolean) -> Unit = {},
    smartDownloads: Boolean = false,
    onSmartDownloadsChange: (Boolean) -> Unit = {},
    onClearCache: () -> Unit = {},
    onDeleteAllDownloads: () -> Unit = {},
    cacheSize: String = "0 MB",
    modifier: Modifier = Modifier,
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var showSettings by remember { mutableStateOf(false) }
    var showStorageManager by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val stats = remember(offlineTracks, offlineStorageUsedBytes) {
        DownloadStats(
            downloadedCount = offlineTracks.size,
            storageUsed = formatFileSize(offlineStorageUsedBytes),
        )
    }
    val storage = remember(offlineStorageUsedBytes) {
        StorageInfo(
            musicSize = formatFileSize(offlineStorageUsedBytes),
            usedPercent = 0f,
        )
    }

    val filters = listOf("All", "Downloaded", "Failed")

    Scaffold(
        containerColor = MFColors.Background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MFColors.TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showStorageManager = !showStorageManager }) {
                        Icon(Icons.Filled.MusicNote, "Storage", tint = MFColors.TextPrimary)
                    }
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(Icons.Filled.Settings, "Settings", tint = MFColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Downloads",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = MFColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Downloaded Songs: ${stats.downloadedCount}",
                        fontSize = 14.sp,
                        color = MFColors.TextSecondary
                    )
                    Text(
                        text = "Storage Used: ${stats.storageUsed}",
                        fontSize = 14.sp,
                        color = MFColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            if (showStorageManager) {
                item {
                    StorageManagerCard(
                        storage = storage,
                        cacheSize = cacheSize,
                        onClearCache = onClearCache,
                        onDeleteAllDownloads = { showDeleteAllDialog = true },
                    )
                }
            }

            if (showSettings) {
                item {
                    DownloadSettingsCard(
                        downloadQuality = downloadQuality,
                        onQualityChange = onDownloadQualityChange,
                        wifiOnly = wifiOnly,
                        onWifiOnlyChange = onWifiOnlyChange,
                        autoDownloadLiked = autoDownloadLiked,
                        onAutoDownloadChange = onAutoDownloadChange,
                        smartDownloads = smartDownloads,
                        onSmartDownloadsChange = onSmartDownloadsChange,
                    )
                }
            }

            if (isDownloading && downloadingTrackName != null) {
                item {
                    MFGlass.GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = MFTokens.MediumRadius,
                        alpha = 0.10f,
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MFColors.Accent.copy(alpha = 0.2f))
                                    .border(width = 0.5.dp, color = MFColors.Accent.copy(alpha = 0.25f), shape = RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FileDownload,
                                    contentDescription = null,
                                    tint = MFColors.Accent,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Downloading...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MFColors.Accent,
                                )
                                Text(
                                    text = downloadingTrackName,
                                    fontSize = 13.sp,
                                    color = MFColors.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MFColors.Accent,
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
            }

            if (downloadSuccess != null) {
                item {
                    MFGlass.GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = MFTokens.MediumRadius,
                        alpha = 0.10f,
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MFColors.Accent.copy(alpha = 0.2f))
                                    .border(width = 0.5.dp, color = MFColors.Accent.copy(alpha = 0.25f), shape = RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FileDownload,
                                    contentDescription = null,
                                    tint = MFColors.Accent,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Download Complete",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MFColors.Accent,
                                )
                                Text(
                                    text = downloadSuccess,
                                    fontSize = 13.sp,
                                    color = MFColors.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = onDismissDownloadMessage, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Dismiss",
                                    tint = MFColors.TextTertiary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (downloadError != null) {
                item {
                    MFGlass.GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = MFTokens.MediumRadius,
                        alpha = 0.10f,
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MFColors.Error.copy(alpha = 0.2f))
                                    .border(width = 0.5.dp, color = MFColors.Error.copy(alpha = 0.25f), shape = RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = MFColors.Error,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Download Failed",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MFColors.Error,
                                )
                                Text(
                                    text = downloadError,
                                    fontSize = 13.sp,
                                    color = MFColors.TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = onDismissDownloadMessage, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Dismiss",
                                    tint = MFColors.TextTertiary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            item {
                DownloadStatsCards(stats = stats)
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    filters.forEach { filter ->
                        val selected = selectedFilter == filter
                        FilterChip(
                            selected = selected,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    text = filter,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MFColors.Accent,
                                selectedLabelColor = MFColors.TextOnAccent,
                                containerColor = MFColors.GlassMid,
                                labelColor = MFColors.TextSecondary,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = MFColors.GlassBorder,
                                selectedBorderColor = MFColors.Accent.copy(alpha = 0.3f),
                                enabled = true,
                                selected = selected,
                            ),
                        )
                    }
                }
            }

            val displayTracks = when (selectedFilter) {
                "Downloaded" -> offlineTracks
                "Failed" -> emptyList()
                else -> offlineTracks
            }

            if (displayTracks.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Downloaded Music",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MFColors.TextPrimary
                        )
                        Text(
                            text = "${displayTracks.size} songs",
                            fontSize = 13.sp,
                            color = MFColors.Accent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                items(displayTracks) { track ->
                    DownloadedTrackRow(
                        track = track,
                        onClick = { onPlayOfflineTrack(track) },
                        onDelete = { onDeleteOfflineTrack(track.songId) },
                    )
                }
            }

            if (offlineTracks.isEmpty() && selectedFilter == "All") {
                item {
                    EmptyDownloadsState(onBrowseMusic = onBrowseMusic)
                }
            }

            if (selectedFilter == "Failed" && displayTracks.isEmpty() && !isDownloading) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        MFGlass.GlassPanel(
                            modifier = Modifier.size(90.dp),
                            cornerRadius = RoundedCornerShape(100.dp),
                            alpha = 0.08f,
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = MFColors.Accent,
                                    modifier = Modifier.size(40.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "No failed downloads",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MFColors.TextPrimary,
                        )
                        Text(
                            text = "All downloads completed successfully.",
                            fontSize = 13.sp,
                            color = MFColors.TextSecondary,
                        )
                    }
                }
            }
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            containerColor = MFColors.DialogBackground,
            shape = MFTokens.LargeRadius,
            title = { Text("Delete All Downloads?", color = MFColors.TextPrimary) },
            text = {
                Text(
                    "This will remove all ${offlineTracks.size} downloaded songs. You can re-download them later.",
                    color = MFColors.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteAllDownloads()
                    showDeleteAllDialog = false
                }) {
                    Text("Delete All", color = MFColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel", color = MFColors.TextTertiary)
                }
            },
        )
    }
}

@Composable
private fun StorageProgressBar(percent: Float) {
    val animatedPercent by animateFloatAsState(
        targetValue = percent,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "storage"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MFColors.GlassMid)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = animatedPercent / 100f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(MFBrushes.AccentGradient)
        )
    }
}

@Composable
private fun DownloadStatsCards(stats: DownloadStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatMiniCard(
            icon = Icons.Filled.FileDownload,
            label = "Downloaded",
            value = "${stats.downloadedCount}",
            color = MFColors.Accent,
            modifier = Modifier.weight(1f)
        )
        StatMiniCard(
            icon = Icons.Filled.MusicNote,
            label = "Storage",
            value = stats.storageUsed,
            color = MFColors.Tertiary,
            modifier = Modifier.weight(1f)
        )
        StatMiniCard(
            icon = Icons.Filled.FileDownload,
            label = "Active",
            value = "${stats.activeDownloads}",
            color = MFColors.Secondary,
            modifier = Modifier.weight(1f)
        )
        StatMiniCard(
            icon = Icons.Filled.ErrorOutline,
            label = "Failed",
            value = "${stats.failedDownloads}",
            color = MFColors.Error,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatMiniCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    MFGlass.GlassPanel(
        modifier = modifier,
        cornerRadius = MFTokens.MediumRadius,
        alpha = 0.08f,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MFColors.TextPrimary
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = MFColors.TextTertiary
            )
        }
    }
}

@Composable
private fun DownloadedTrackRow(
    track: OfflineTrackEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    MFGlass.GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = MFTokens.MediumRadius,
        alpha = 0.06f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MFColors.GlassMid)
                    .border(width = 0.5.dp, color = MFColors.GlassBorder, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                val context = LocalContext.current
                val localArtworkFile = remember(track.localFilePath) {
                    val audioFile = java.io.File(track.localFilePath)
                    java.io.File(audioFile.parent, audioFile.nameWithoutExtension + ".jpg")
                }
                if (localArtworkFile.exists()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(localArtworkFile).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (track.artworkUrl.isNotBlank() && track.artworkUrl.startsWith("http")) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(track.artworkUrl).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MFColors.TextTertiary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MFColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    fontSize = 13.sp,
                    color = MFColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatFileSize(track.fileSize),
                        fontSize = 11.sp,
                        color = MFColors.TextTertiary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MFColors.Accent.copy(alpha = 0.15f))
                            .border(width = 0.5.dp, color = MFColors.Accent.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Offline",
                            fontSize = 9.sp,
                            color = MFColors.Accent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove",
                    tint = MFColors.Error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun StorageManagerCard(
    storage: StorageInfo,
    cacheSize: String = "0 MB",
    onClearCache: () -> Unit = {},
    onDeleteAllDownloads: () -> Unit = {},
) {
    MFGlass.GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = MFTokens.MediumRadius,
        alpha = 0.08f,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Storage Manager",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MFColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(18.dp))
            StorageRow("Music", storage.musicSize, MFColors.Accent)
            StorageRow("Cache", cacheSize, MFColors.Secondary)
            HorizontalDivider(color = MFColors.Divider, modifier = Modifier.padding(vertical = 10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MFColors.TextPrimary)
                Text(storage.musicSize, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MFColors.Accent)
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedButton(
                    onClick = onClearCache,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MFColors.Secondary)
                ) {
                    Text("Clear Cache", fontSize = 13.sp, color = MFColors.Secondary)
                }
                OutlinedButton(
                    onClick = onDeleteAllDownloads,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MFColors.Error)
                ) {
                    Text("Delete Downloads", fontSize = 13.sp, color = MFColors.Error)
                }
            }
        }
    }
}

@Composable
private fun StorageRow(label: String, size: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(text = label, fontSize = 14.sp, color = MFColors.TextSecondary)
        }
        Text(text = size, fontSize = 14.sp, color = MFColors.TextPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DownloadSettingsCard(
    downloadQuality: String,
    onQualityChange: (String) -> Unit,
    wifiOnly: Boolean,
    onWifiOnlyChange: (Boolean) -> Unit,
    autoDownloadLiked: Boolean,
    onAutoDownloadChange: (Boolean) -> Unit,
    smartDownloads: Boolean,
    onSmartDownloadsChange: (Boolean) -> Unit
) {
    val qualities = listOf("Low", "Medium", "High", "Lossless")
    MFGlass.GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = MFTokens.MediumRadius,
        alpha = 0.08f,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Download Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MFColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(18.dp))

            Text("Download Quality", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MFColors.TextSecondary)
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                qualities.forEach { quality ->
                    val selected = quality == downloadQuality
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) MFColors.Accent else MFColors.GlassMid)
                            .border(width = 0.5.dp, color = if (selected) MFColors.Accent.copy(alpha = 0.3f) else MFColors.GlassBorder, shape = RoundedCornerShape(10.dp))
                            .clickable { onQualityChange(quality) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = quality,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selected) MFColors.TextOnAccent else MFColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MFColors.Divider)

            Spacer(modifier = Modifier.height(14.dp))
            SettingToggle("Download using Wi-Fi only", "Save mobile data", wifiOnly, onWifiOnlyChange)
            SettingToggle("Auto Download Liked Songs", "Download songs when you like them", autoDownloadLiked, onAutoDownloadChange)
            SettingToggle("Smart Downloads", "Automatically download recommended music", smartDownloads, onSmartDownloadsChange)
        }
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MFColors.TextPrimary)
            Text(text = subtitle, fontSize = 12.sp, color = MFColors.TextTertiary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MFColors.TextOnAccent,
                checkedTrackColor = MFColors.Accent,
                uncheckedThumbColor = MFColors.TextTertiary,
                uncheckedTrackColor = MFColors.GlassMid
            )
        )
    }
}

@Composable
private fun EmptyDownloadsState(onBrowseMusic: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MFGlass.GlassPanel(
            modifier = Modifier.size(120.dp),
            cornerRadius = RoundedCornerShape(100.dp),
            alpha = 0.08f,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.FileDownload,
                    contentDescription = null,
                    tint = MFColors.TextTertiary.copy(alpha = 0.5f),
                    modifier = Modifier.size(56.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No downloaded music",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MFColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Download songs to listen offline.",
            fontSize = 14.sp,
            color = MFColors.TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(26.dp))
        Button(
            onClick = onBrowseMusic,
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MFColors.Accent),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Browse Music",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MFColors.TextOnAccent
            )
        }
    }
}

