package com.musicflow.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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

// ── Data Classes ─────────────────────────────────────────────────────────

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
    val totalSize: String = "0 MB",
    val maxStorage: String = "2 GB",
    val remainingStorage: String = "2 GB",
    val usedPercent: Float = 0f,
)

// ── Main Screen ──────────────────────────────────────────────────────────

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
        val usedMB = offlineStorageUsedBytes / (1024.0 * 1024.0)
        StorageInfo(
            musicSize = formatFileSize(offlineStorageUsedBytes),
            totalSize = "${"%.0f".format(usedMB + 89)} MB",
            remainingStorage = "${"%.1f".format(2048.0 - usedMB - 89)} MB",
            usedPercent = ((usedMB + 89) / 2048.0 * 100).toFloat(),
        )
    }

    val filters = listOf("All", "Downloaded", "Failed")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnBackground)
                    }
                },
                actions = {
                    IconButton(onClick = { showStorageManager = !showStorageManager }) {
                        Icon(Icons.Filled.MusicNote, "Storage", tint = OnBackground)
                    }
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(Icons.Filled.Settings, "Settings", tint = OnBackground)
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "Downloads",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Downloaded Songs: ${stats.downloadedCount}",
                        fontSize = 14.sp,
                        color = OnBackgroundVariant
                    )
                    Text(
                        text = "Storage Used: ${stats.storageUsed} / ${storage.maxStorage}",
                        fontSize = 14.sp,
                        color = OnBackgroundVariant
                    )
                    Text(
                        text = "Remaining Offline Storage: ${storage.remainingStorage}",
                        fontSize = 14.sp,
                        color = OnBackgroundVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StorageProgressBar(percent = storage.usedPercent)
                }
            }

            // Active Download Status
            if (isDownloading && downloadingTrackName != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FileDownload,
                                    contentDescription = null,
                                    tint = AccentGreen,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Downloading...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentGreen,
                                )
                                Text(
                                    text = downloadingTrackName,
                                    fontSize = 13.sp,
                                    color = OnBackgroundVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AccentGreen,
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                }
            }

            // Download Success Message
            if (downloadSuccess != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FileDownload,
                                    contentDescription = null,
                                    tint = AccentGreen,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Download Complete",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentGreen,
                                )
                                Text(
                                    text = downloadSuccess,
                                    fontSize = 13.sp,
                                    color = OnBackgroundVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = onDismissDownloadMessage, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Dismiss",
                                    tint = OnBackgroundVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Download Error Message
            if (downloadError != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ErrorRed.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Download Failed",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ErrorRed,
                                )
                                Text(
                                    text = downloadError,
                                    fontSize = 13.sp,
                                    color = OnBackgroundVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = onDismissDownloadMessage, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Dismiss",
                                    tint = OnBackgroundVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Download Statistics Cards
            item {
                DownloadStatsCards(stats = stats)
            }

            // Filter Chips
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                selectedContainerColor = AccentGreen,
                                selectedLabelColor = Color.Black,
                                containerColor = DarkSurfaceVariant,
                                labelColor = OnBackgroundVariant,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        )
                    }
                }
            }

            // Downloaded Music List
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
                            color = OnBackground
                        )
                        Text(
                            text = "${displayTracks.size} songs",
                            fontSize = 13.sp,
                            color = AccentGreen,
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

            // Storage Manager Card
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

            // Download Settings
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

            // Empty State
            if (offlineTracks.isEmpty() && selectedFilter == "All") {
                item {
                    EmptyDownloadsState(onBrowseMusic = onBrowseMusic)
                }
            }

            // Failed filter empty state
            if (selectedFilter == "Failed" && displayTracks.isEmpty() && !isDownloading) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(AccentGreen.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No failed downloads",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnBackground,
                        )
                        Text(
                            text = "All downloads completed successfully.",
                            fontSize = 13.sp,
                            color = OnBackgroundVariant,
                        )
                    }
                }
            }
        }
    }

    // Delete all downloads confirmation dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            containerColor = DarkSurface,
            title = { Text("Delete All Downloads?", color = OnBackground) },
            text = {
                Text(
                    "This will remove all ${offlineTracks.size} downloaded songs. You can re-download them later.",
                    color = OnBackgroundVariant,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onDeleteAllDownloads()
                    showDeleteAllDialog = false
                }) {
                    Text("Delete All", color = ErrorRed)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel", color = OnBackgroundVariant)
                }
            },
        )
    }
}

// ── Components ───────────────────────────────────────────────────────────

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
            .background(DarkSurfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = animatedPercent / 100f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(AccentGreenDark, AccentGreen, AccentGreenLight)
                    )
                )
        )
    }
}

@Composable
private fun DownloadStatsCards(stats: DownloadStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatMiniCard(
            icon = Icons.Filled.FileDownload,
            label = "Downloaded",
            value = "${stats.downloadedCount}",
            color = AccentGreen,
            modifier = Modifier.weight(1f)
        )
        StatMiniCard(
            icon = Icons.Filled.MusicNote,
            label = "Storage",
            value = stats.storageUsed,
            color = TertiaryTeal,
            modifier = Modifier.weight(1f)
        )
        StatMiniCard(
            icon = Icons.Filled.FileDownload,
            label = "Active",
            value = "${stats.activeDownloads}",
            color = SecondaryPurple,
            modifier = Modifier.weight(1f)
        )
        StatMiniCard(
            icon = Icons.Filled.ErrorOutline,
            label = "Failed",
            value = "${stats.failedDownloads}",
            color = ErrorRed,
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
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = OnBackground
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = OnBackgroundVariant
        )
    }
}

@Composable
private fun DownloadedTrackRow(
    track: OfflineTrackEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Artwork
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant),
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
                        tint = OnBackgroundVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Track info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    fontSize = 13.sp,
                    color = OnBackgroundVariant,
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
                        color = OnBackgroundVariant.copy(alpha = 0.7f)
                    )
                    Text("·", fontSize = 11.sp, color = OnBackgroundVariant.copy(alpha = 0.5f))
                    Text(
                        text = "320 kbps",
                        fontSize = 11.sp,
                        color = OnBackgroundVariant.copy(alpha = 0.7f)
                    )
                    // Offline badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "Offline",
                            fontSize = 9.sp,
                            color = AccentGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Delete button
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove",
                    tint = ErrorRed,
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Storage Manager",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            StorageRow("Music", storage.musicSize, AccentGreen)
            StorageRow("Cache", cacheSize, SecondaryPurple)
            HorizontalDivider(color = Divider, modifier = Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OnBackground)
                Text(storage.musicSize, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AccentGreen)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onClearCache,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryPurple)
                ) {
                    Text("Clear Cache", fontSize = 13.sp, color = SecondaryPurple)
                }
                OutlinedButton(
                    onClick = onDeleteAllDownloads,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                ) {
                    Text("Delete Downloads", fontSize = 13.sp, color = ErrorRed)
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
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(text = label, fontSize = 14.sp, color = OnBackgroundVariant)
        }
        Text(text = size, fontSize = 14.sp, color = OnBackground, fontWeight = FontWeight.Medium)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Download Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackground
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Download Quality", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = OnBackgroundVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                qualities.forEach { quality ->
                    val selected = quality == downloadQuality
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) AccentGreen else DarkSurfaceVariant)
                            .clickable { onQualityChange(quality) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = quality,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selected) OnAccent else OnBackgroundVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Divider)

            Spacer(modifier = Modifier.height(12.dp))
            SettingToggle("Download using Wi-Fi only", "Save mobile data", wifiOnly, onWifiOnlyChange)
            SettingToggle("Auto Download Liked Songs", "Download songs when you like them", autoDownloadLiked, onAutoDownloadChange)
            SettingToggle("Smart Downloads", "Automatically download recommended music", smartDownloads, onSmartDownloadsChange)
        }
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = OnBackground)
            Text(text = subtitle, fontSize = 12.sp, color = OnBackgroundVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnAccent,
                checkedTrackColor = AccentGreen,
                uncheckedThumbColor = OnBackgroundVariant,
                uncheckedTrackColor = DarkSurfaceVariant
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
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(DarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.FileDownload,
                contentDescription = null,
                tint = OnBackgroundVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No downloaded music",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = OnBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Download songs to listen offline.",
            fontSize = 14.sp,
            color = OnBackgroundVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onBrowseMusic,
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Browse Music",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = OnAccent
            )
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
    }
}
