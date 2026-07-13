package com.musicflow.app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.musicflow.app.ui.components.MusicFlowBottomNavBar
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.musicflow.app.ui.components.AddToPlaylistDialog
import com.musicflow.app.ui.components.MainPlayerScreen
import com.musicflow.app.ui.components.MiniPlayer
import com.musicflow.app.ui.components.SongContextMenu
import com.musicflow.app.ui.components.SleepTimerDialog
import com.musicflow.app.ui.components.shareTrack
import com.musicflow.app.ui.navigation.DOWNLOADS_ROUTE
import com.musicflow.app.ui.navigation.LIBRARY_ROUTE
import com.musicflow.app.ui.navigation.Screen
import com.musicflow.app.ui.screens.AlbumScreen
import com.musicflow.app.ui.screens.AlbumViewModel
import com.musicflow.app.ui.screens.ArtistScreen
import com.musicflow.app.ui.screens.ArtistViewModel
import com.musicflow.app.ui.screens.EngineInfoViewModel
import com.musicflow.app.ui.screens.DownloadsScreen
import com.musicflow.app.ui.screens.HomeScreen
import com.musicflow.app.ui.screens.HomeViewModel
import com.musicflow.app.ui.screens.LibraryScreen
import com.musicflow.app.ui.screens.LibraryFilter
import com.musicflow.app.ui.screens.LibraryViewModel
import com.musicflow.app.ui.screens.OnboardingScreen
import com.musicflow.app.ui.screens.PlaylistDetailScreen
import com.musicflow.app.ui.screens.PlaylistViewModel
import com.musicflow.app.ui.screens.PlayerViewModel
import com.musicflow.app.ui.screens.SearchScreen
import com.musicflow.app.ui.screens.SearchViewModel
import com.musicflow.app.ui.screens.SettingsScreen
import com.musicflow.app.data.local.LocalBackupManager
import com.musicflow.app.ui.theme.AccentGreen
import com.musicflow.app.ui.theme.DarkSurface
import com.musicflow.app.ui.theme.MFColors
import com.musicflow.app.ui.theme.MusicFlowTheme
import com.musicflow.app.ui.theme.OnBackground
import com.musicflow.app.ui.theme.OnBackgroundVariant
import com.musicflow.app.utils.EqualizerManager
import com.musicflow.app.utils.LanguagePreferences
import com.musicflow.app.utils.PlayerSettingsManager
import com.musicflow.app.utils.DownloadSettingsManager
import com.musicflow.app.utils.ThemeMode
import com.musicflow.app.utils.ThemePreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var themePreferences: ThemePreferences
    @Inject lateinit var languagePreferences: LanguagePreferences
    @Inject lateinit var equalizerManager: EqualizerManager
    @Inject lateinit var playerSettingsManager: PlayerSettingsManager
    @Inject lateinit var localBackupManager: LocalBackupManager
    @Inject lateinit var downloadSettingsManager: DownloadSettingsManager

    private lateinit var prefs: SharedPreferences

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* user granted or denied — we check below */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefs = getSharedPreferences("musicflow_prefs", MODE_PRIVATE)

        // Step 1: Request MANAGE_EXTERNAL_STORAGE if not granted
        requestStoragePermissionIfNeeded()

        // Step 2: Check if a restore happened in Application.onCreate()
        // The restore was done BEFORE Room opened the DB.
        val pendingRestore = prefs.getBoolean("pending_restore_dialog", false)
        if (pendingRestore) {
            prefs.edit().remove("pending_restore_dialog").apply()
        }
        val shouldRestore = pendingRestore

        setContent {
            val themeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.DARK)
            val isOnboardingDone by languagePreferences.isOnboardingDone.collectAsState(initial = null)

            // Show dialog only if we just restored — ask to keep or discard
            var showRestoreConfirm by remember { mutableStateOf(shouldRestore) }

            MusicFlowTheme(themeMode = themeMode) {
                // Restore confirmation dialog — BLOCKS everything until decided
                if (showRestoreConfirm) {
                    AlertDialog(
                        onDismissRequest = { },
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = { Text("Data Restored", color = OnBackground) },
                        text = {
                            Text(
                                "Your previous playlists, favorites, and queue have been restored.\n\nKeep this data or start fresh?",
                                color = OnBackgroundVariant,
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                prefs.edit().putBoolean("has_launched_before", true).apply()
                                showRestoreConfirm = false
                            }) {
                                Text("Keep Data", color = AccentGreen)
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                // Delete restored DB, recreate fresh
                                deleteRestoredDatabase()
                                prefs.edit().putBoolean("has_launched_before", true).apply()
                                showRestoreConfirm = false
                                recreate()
                            }) {
                                Text("Start Fresh", color = OnBackgroundVariant)
                            }
                        },
                    )
                }

                when {
                    showRestoreConfirm -> { /* dialog blocks UI */ }
                    isOnboardingDone == null -> { /* loading */ }
                    isOnboardingDone == false -> {
                        OnboardingScreen(onLanguageSelected = { languages ->
                            kotlinx.coroutines.MainScope().launch {
                                languagePreferences.setLanguages(languages)
                                languagePreferences.setOnboardingDone()
                            }
                        })
                    }
                    else -> {
                        MusicFlowApp(themePreferences, languagePreferences, equalizerManager, playerSettingsManager, localBackupManager, downloadSettingsManager)
                    }
                }
            }
        }
    }

    private fun deleteRestoredDatabase() {
        try {
            val dbFile = getDatabasePath("musicflow.db")
            if (dbFile.exists()) dbFile.delete()
            val walFile = java.io.File(dbFile.path + "-wal")
            if (walFile.exists()) walFile.delete()
            val shmFile = java.io.File(dbFile.path + "-shm")
            if (shmFile.exists()) shmFile.delete()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to delete restored DB: ${e.message}")
        }
    }

    private fun requestStoragePermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                storagePermissionLauncher.launch(intent)
            }
        }
    }
}

private fun formatCacheSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
}

@Composable
private fun CreatePlaylistDialogInline(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Create Playlist", color = OnBackground) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Playlist name", color = OnBackgroundVariant) },
                colors = androidx.compose.material3.TextFieldDefaults.colors(
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
            androidx.compose.material3.TextButton(
                onClick = { onCreate(name) },
                enabled = name.isNotBlank(),
            ) {
                Text("Create", color = if (name.isNotBlank()) AccentGreen else OnBackgroundVariant)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel", color = OnBackgroundVariant)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicFlowApp(
    themePreferences: ThemePreferences,
    languagePreferences: LanguagePreferences,
    equalizerManager: EqualizerManager,
    playerSettingsManager: PlayerSettingsManager,
    localBackupManager: com.musicflow.app.data.local.LocalBackupManager,
    downloadSettingsManager: DownloadSettingsManager,
) {
    val navController = rememberNavController()
    val searchViewModel: SearchViewModel = hiltViewModel()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val engineInfoViewModel: EngineInfoViewModel = hiltViewModel()
    val libraryViewModel: LibraryViewModel = hiltViewModel()
    val playlistViewModel: PlaylistViewModel = hiltViewModel()
    val homeViewModel: HomeViewModel = hiltViewModel()

    val searchUiState by searchViewModel.uiState.collectAsState()
    val playerUiState by playerViewModel.uiState.collectAsState()
    val libraryUiState by libraryViewModel.uiState.collectAsState()
    val playlistUiState by playlistViewModel.uiState.collectAsState()
    val homeUiState by homeViewModel.uiState.collectAsState()

    var showFullPlayer by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var addToPlaylistTrack by remember { mutableStateOf<com.musicflow.app.data.remote.SearchResult?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuTrack by remember { mutableStateOf<com.musicflow.app.data.remote.SearchResult?>(null) }
    val homeNotifications = remember { mutableListOf<com.musicflow.app.ui.screens.HomeNotification>() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val autoDownloadLiked by downloadSettingsManager.autoDownloadLiked.collectAsState(initial = false)

    // Helper: toggle favorite AND auto-download if setting is enabled
    // Uses SharedMusicState as single source of truth - updates propagate to all screens
    fun toggleFavoriteAndMaybeDownload(track: com.musicflow.app.data.remote.SearchResult) {
        libraryViewModel.onToggleFavorite(track.videoId)
        if (autoDownloadLiked) {
            coroutineScope.launch {
                val alreadyOffline = playerViewModel.isTrackOffline(track.videoId)
                if (!alreadyOffline) {
                    playerViewModel.downloadTrack(track)
                }
            }
        }
    }

    LaunchedEffect(currentRoute) {
        if (showFullPlayer) showFullPlayer = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            MusicFlowBottomNavBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    NavHost(navController = navController, startDestination = Screen.Home.route) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                recentTracks = homeUiState.continueListening,
                                playlists = homeUiState.playlists,
                                onTrackSelected = { track ->
                                    playerViewModel.playFromLibrary(track.songId, track.title, track.artist, track.artworkUrl)
                                },
                                onPlaylistSelected = { playlistId ->
                                    libraryUiState.playlists.find { it.id == playlistId }?.let { p ->
                                        navController.navigate("playlist_detail/$playlistId/${p.name}")
                                    }
                                },
                                onSearchClick = {
                                    navController.navigate(Screen.Search.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onLibraryClick = {
                                    navController.navigate(Screen.Library.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onFavoritesClick = {
                                    navController.navigate("library?filter=FAVORITES") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onDownloadsClick = {
                                    navController.navigate(DOWNLOADS_ROUTE)
                                },
                                onRecentlyPlayedSeeAll = {
                                    navController.navigate("library?filter=RECENT") {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onMoodMixClick = { query ->
                                    playerViewModel.playMoodMix(query)
                                },
                                onDailyMixClick = {
                                    playerViewModel.playDailyMix()
                                },
                                onPlaylistPlay = { playlistId ->
                                    playerViewModel.playPlaylistQueue(playlistId)
                                },
                                currentPlayingSongId = playerUiState.currentTrack?.songId,
                                isNetworkAvailable = playerUiState.isNetworkAvailable,
                                notifications = homeNotifications,
                                onClearNotifications = { homeNotifications.clear() },
                                trendingTracks = homeUiState.trending,
                                isTrendingLoading = homeUiState.isTrendingLoading,
                                trendingError = homeUiState.trendingError,
                                onRetryTrending = { homeViewModel.loadTrending() },
                                favoriteTracks = homeUiState.favoriteTracks,
                                playlistTracks = homeUiState.playlistTracks,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        composable(Screen.Search.route) {
                            SearchScreen(
                                uiState = searchUiState,
                                onSearchQueryChange = searchViewModel::onSearchQueryChange,
                                onClearSearch = searchViewModel::onClearSearch,
                                onTrackSelected = { result ->
                                    coroutineScope.launch {
                                        val audioUrl = searchViewModel.onTrackSelected(result)
                                        if (audioUrl != null) playerViewModel.playTrack(result, audioUrl)
                                    }
                                },
                                onErrorDismissed = searchViewModel::onErrorDismissed,
                                onSuggestionSelected = searchViewModel::onSuggestionSelected,
                                onFilterChange = searchViewModel::onFilterChange,
                                onClearHistory = searchViewModel::onClearHistory,
                                onDeleteHistoryItem = searchViewModel::onDeleteHistoryItem,
                                onSuggestionsDismissed = searchViewModel::onSuggestionsDismissed,
                                onAddToPlaylist = { result ->
                                    addToPlaylistTrack = result
                                    showAddToPlaylistDialog = true
                                },
                                onTrackLongPress = { result ->
                                    contextMenuTrack = result
                                    showContextMenu = true
                                },
                                onArtistSelected = { result ->
                                    coroutineScope.launch {
                                        val artistBrowseId = searchViewModel.findArtistBrowseId(result.artist)
                                        if (artistBrowseId != null) {
                                            navController.navigate("artist/$artistBrowseId")
                                        }
                                    }
                                },
                                onAlbumSelected = { result ->
                                    coroutineScope.launch {
                                        val albumBrowseId = searchViewModel.findAlbumBrowseId(result.title)
                                        if (albumBrowseId != null) {
                                            navController.navigate("album/$albumBrowseId")
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        composable("artist/{browseId}") { backStackEntry ->
                            val browseId = backStackEntry.arguments?.getString("browseId") ?: ""
                            val artistViewModel: ArtistViewModel = hiltViewModel()
                            val artistUiState by artistViewModel.uiState.collectAsState()

                            LaunchedEffect(browseId) {
                                artistViewModel.loadArtist(browseId)
                            }

                            ArtistScreen(
                                browseId = browseId,
                                artistPage = artistUiState.artistPage,
                                isLoading = artistUiState.isLoading,
                                error = artistUiState.error,
                                onBack = { navController.popBackStack() },
                                onTrackSelected = { result ->
                                    coroutineScope.launch {
                                        val audioUrl = searchViewModel.onTrackSelected(result)
                                        if (audioUrl != null) playerViewModel.playTrack(result, audioUrl)
                                    }
                                },
                                onAlbumSelected = { albumBrowseId ->
                                    navController.navigate("album/$albumBrowseId")
                                },
                                onRetry = { artistViewModel.loadArtist(browseId) },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        composable("album/{browseId}") { backStackEntry ->
                            val browseId = backStackEntry.arguments?.getString("browseId") ?: ""
                            val albumViewModel: AlbumViewModel = hiltViewModel()
                            val albumUiState by albumViewModel.uiState.collectAsState()

                            LaunchedEffect(browseId) {
                                albumViewModel.loadAlbum(browseId)
                            }

                            AlbumScreen(
                                browseId = browseId,
                                albumPage = albumUiState.albumPage,
                                isLoading = albumUiState.isLoading,
                                error = albumUiState.error,
                                onBack = { navController.popBackStack() },
                                onTrackSelected = { result ->
                                    coroutineScope.launch {
                                        val audioUrl = searchViewModel.onTrackSelected(result)
                                        if (audioUrl != null) playerViewModel.playTrack(result, audioUrl)
                                    }
                                },
                                onRetry = { albumViewModel.loadAlbum(browseId) },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        composable(Screen.Library.route) {
                            LibraryScreen(
                                uiState = libraryUiState,
                                onSearchQueryChange = libraryViewModel::onSearchQueryChange,
                                onFilterChange = libraryViewModel::onFilterChange,
                                onTrackSelected = { track ->
                                    playerViewModel.playFromLibrary(track.songId, track.title, track.artist, track.artworkUrl)
                                },
                                onTrackLongPress = { track ->
                                    contextMenuTrack = com.musicflow.app.data.remote.SearchResult(
                                        videoId = track.songId,
                                        title = track.title,
                                        artist = track.artist,
                                        thumbnailUrl = track.artworkUrl,
                                    )
                                    showContextMenu = true
                                },
                                onToggleFavorite = { songId ->
                                    val track = libraryUiState.items.find { it.track.songId == songId }
                                    if (track != null) {
                                        toggleFavoriteAndMaybeDownload(
                                            com.musicflow.app.data.remote.SearchResult(
                                                videoId = track.track.songId,
                                                title = track.track.title,
                                                artist = track.track.artist,
                                                thumbnailUrl = track.track.artworkUrl,
                                            )
                                        )
                                    } else {
                                        libraryViewModel.onToggleFavorite(songId)
                                    }
                                },
                                onPlayOfflineTrack = { offlineTrack ->
                                    playerViewModel.playOfflineTrack(
                                        songId = offlineTrack.songId,
                                        title = offlineTrack.title,
                                        artist = offlineTrack.artist,
                                        artworkUrl = offlineTrack.artworkUrl,
                                        localFilePath = offlineTrack.localFilePath,
                                    )
                                },
                                onDeleteOfflineTrack = libraryViewModel::deleteOfflineTrack,
                                onPlaylistSelected = { playlistId ->
                                    libraryUiState.playlists.find { it.id == playlistId }?.let { p ->
                                        navController.navigate("playlist_detail/$playlistId/${p.name}")
                                    }
                                },
                                onCreatePlaylist = { showCreatePlaylistDialog = true },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        composable(LIBRARY_ROUTE) { backStackEntry ->
                            val filterName = backStackEntry.arguments?.getString("filter") ?: "ALL"
                            val filter = try { LibraryFilter.valueOf(filterName) } catch (_: Exception) { LibraryFilter.ALL }
                            LaunchedEffect(filter) {
                                libraryViewModel.onFilterChange(filter)
                            }
                            LibraryScreen(
                                uiState = libraryUiState,
                                onSearchQueryChange = libraryViewModel::onSearchQueryChange,
                                onFilterChange = libraryViewModel::onFilterChange,
                                onTrackSelected = { track ->
                                    playerViewModel.playFromLibrary(track.songId, track.title, track.artist, track.artworkUrl)
                                },
                                onTrackLongPress = { track ->
                                    contextMenuTrack = com.musicflow.app.data.remote.SearchResult(
                                        videoId = track.songId,
                                        title = track.title,
                                        artist = track.artist,
                                        thumbnailUrl = track.artworkUrl,
                                    )
                                    showContextMenu = true
                                },
                                onToggleFavorite = { songId ->
                                    val track = libraryUiState.items.find { it.track.songId == songId }
                                    if (track != null) {
                                        toggleFavoriteAndMaybeDownload(
                                            com.musicflow.app.data.remote.SearchResult(
                                                videoId = track.track.songId,
                                                title = track.track.title,
                                                artist = track.track.artist,
                                                thumbnailUrl = track.track.artworkUrl,
                                            )
                                        )
                                    } else {
                                        libraryViewModel.onToggleFavorite(songId)
                                    }
                                },
                                onPlayOfflineTrack = { offlineTrack ->
                                    playerViewModel.playOfflineTrack(
                                        songId = offlineTrack.songId,
                                        title = offlineTrack.title,
                                        artist = offlineTrack.artist,
                                        artworkUrl = offlineTrack.artworkUrl,
                                        localFilePath = offlineTrack.localFilePath,
                                    )
                                },
                                onDeleteOfflineTrack = libraryViewModel::deleteOfflineTrack,
                                onPlaylistSelected = { playlistId ->
                                    libraryUiState.playlists.find { it.id == playlistId }?.let { p ->
                                        navController.navigate("playlist_detail/$playlistId/${p.name}")
                                    }
                                },
                                onCreatePlaylist = { showCreatePlaylistDialog = true },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        composable("playlist_detail/{playlistId}/{playlistName}") { backStackEntry ->
                            val playlistId = backStackEntry.arguments?.getString("playlistId")?.toLongOrNull() ?: 0L
                            val playlistName = backStackEntry.arguments?.getString("playlistName") ?: "Playlist"
                            PlaylistDetailScreen(
                                playlistId = playlistId,
                                playlistName = java.net.URLDecoder.decode(playlistName, "UTF-8"),
                                playlistViewModel = playlistViewModel,
                                onTrackSelected = { track ->
                                    playerViewModel.playFromLibrary(track.songId, track.title, track.artist, track.artworkUrl)
                                },
                                onTrackSelectedWithContext = { track, allTracks, index ->
                                    playerViewModel.playFromPlaylistContext(
                                        songId = track.songId,
                                        title = track.title,
                                        artist = track.artist,
                                        artworkUrl = track.artworkUrl,
                                        allPlaylistTracks = allTracks,
                                        startIndex = index,
                                    )
                                },
                                onBack = { navController.popBackStack() },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                engineInfoViewModel = engineInfoViewModel,
                                themePreferences = themePreferences,
                                languagePreferences = languagePreferences,
                                equalizerManager = equalizerManager,
                                playerSettingsManager = playerSettingsManager,
                                localBackupManager = localBackupManager,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        composable(DOWNLOADS_ROUTE) {
                            val downloadQuality by downloadSettingsManager.downloadQuality.collectAsState(initial = "High")
                            val wifiOnly by downloadSettingsManager.wifiOnly.collectAsState(initial = true)
                            val autoDownloadLiked by downloadSettingsManager.autoDownloadLiked.collectAsState(initial = false)
                            val smartDownloads by downloadSettingsManager.smartDownloads.collectAsState(initial = false)

                            DownloadsScreen(
                                offlineTracks = libraryUiState.offlineTracks,
                                offlineStorageUsedBytes = libraryUiState.offlineStorageUsedBytes,
                                onPlayOfflineTrack = { offlineTrack ->
                                    playerViewModel.playOfflineTrack(
                                        songId = offlineTrack.songId,
                                        title = offlineTrack.title,
                                        artist = offlineTrack.artist,
                                        artworkUrl = offlineTrack.artworkUrl,
                                        localFilePath = offlineTrack.localFilePath,
                                    )
                                },
                                onDeleteOfflineTrack = libraryViewModel::deleteOfflineTrack,
                                onBack = { navController.popBackStack() },
                                onBrowseMusic = {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                isDownloading = playerUiState.isDownloading,
                                downloadingTrackName = playerUiState.downloadingTrackId,
                                downloadSuccess = playerUiState.downloadSuccess,
                                downloadError = playerUiState.downloadError,
                                onDismissDownloadMessage = playerViewModel::clearDownloadMessages,
                                downloadQuality = downloadQuality,
                                onDownloadQualityChange = { quality ->
                                    coroutineScope.launch { downloadSettingsManager.setDownloadQuality(quality) }
                                },
                                wifiOnly = wifiOnly,
                                onWifiOnlyChange = { enabled ->
                                    coroutineScope.launch { downloadSettingsManager.setWifiOnly(enabled) }
                                },
                                autoDownloadLiked = autoDownloadLiked,
                                onAutoDownloadChange = { enabled ->
                                    coroutineScope.launch { downloadSettingsManager.setAutoDownloadLiked(enabled) }
                                },
                                smartDownloads = smartDownloads,
                                onSmartDownloadsChange = { enabled ->
                                    coroutineScope.launch { downloadSettingsManager.setSmartDownloads(enabled) }
                                },
                                onClearCache = {
                                    try {
                                        context.cacheDir?.deleteRecursively()
                                        Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to clear cache", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onDeleteAllDownloads = {
                                    coroutineScope.launch {
                                        libraryViewModel.clearAllOfflineTracks()
                                        Toast.makeText(context, "All downloads deleted", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                cacheSize = run {
                                    val size = context.cacheDir?.let { dir ->
                                        dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                                    } ?: 0L
                                    formatCacheSize(size)
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = playerUiState.currentTrack != null && !showFullPlayer,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 }),
                ) {
                    MiniPlayer(
                        track = playerUiState.currentTrack,
                        isPlaying = playerUiState.isPlaying,
                        progress = if (playerUiState.duration > 0) playerUiState.currentPosition.toFloat() / playerUiState.duration.toFloat() else 0f,
                        onClick = { showFullPlayer = true },
                        onPlayPause = playerViewModel::togglePlayPause,
                        onAddToPlaylist = {
                            playerUiState.currentTrack?.let { track ->
                                addToPlaylistTrack = com.musicflow.app.data.remote.SearchResult(track.songId, track.title, track.artist, track.artworkUrl)
                                showAddToPlaylistDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (showFullPlayer && playerUiState.currentTrack != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MFColors.Background)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {}
                        ),
                ) {
                    MainPlayerScreen(
                        track = playerUiState.currentTrack,
                        isPlaying = playerUiState.isPlaying,
                        currentPosition = playerUiState.currentPosition,
                        duration = playerUiState.duration,
                        upcomingTracks = playerUiState.upcomingTracks,
                        onPlayPause = playerViewModel::togglePlayPause,
                        onSkipPrevious = playerViewModel::skipToPrevious,
                        onSkipNext = playerViewModel::skipToNext,
                        onSeek = playerViewModel::seekTo,
                        onSwipeDown = { showFullPlayer = false },
                        isLiked = playerUiState.isCurrentTrackLiked,
                        onLikeToggle = {
                            playerUiState.currentTrack?.let { track ->
                                toggleFavoriteAndMaybeDownload(
                                    com.musicflow.app.data.remote.SearchResult(
                                        videoId = track.songId,
                                        title = track.title,
                                        artist = track.artist,
                                        thumbnailUrl = track.artworkUrl,
                                    )
                                )
                            }
                        },
                        isShuffleOn = playerUiState.isShuffleOn,
                        onShuffleToggle = playerViewModel::toggleShuffle,
                        loopMode = playerUiState.loopMode,
                        onLoopToggle = playerViewModel::toggleLoop,
                        isLyricsVisible = playerUiState.isLyricsVisible,
                        onLyricsToggle = playerViewModel::toggleLyricsVisibility,
                        onSleepTimerClick = { showSleepTimerDialog = true },
                        sleepTimerText = if (playerUiState.isSleepTimerRunning) playerViewModel.getSleepTimerFormatted() else null,
                        onAddToPlaylist = {
                            playerUiState.currentTrack?.let { track ->
                                addToPlaylistTrack = com.musicflow.app.data.remote.SearchResult(track.songId, track.title, track.artist, track.artworkUrl)
                                showAddToPlaylistDialog = true
                            }
                        },
                        onQueueItemSelected = playerViewModel::skipToQueueItem,
                        onRemoveFromQueue = playerViewModel::removeFromQueue,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            if (showAddToPlaylistDialog) {
                AddToPlaylistDialog(
                    playlists = playlistUiState.playlists,
                    onDismiss = { showAddToPlaylistDialog = false; addToPlaylistTrack = null },
                    onPlaylistSelected = { playlistId ->
                        addToPlaylistTrack?.let { playlistViewModel.addTrackToPlaylist(playlistId, it.videoId) }
                        showAddToPlaylistDialog = false; addToPlaylistTrack = null
                    },
                    onCreatePlaylist = { name -> playlistViewModel.createPlaylist(name) },
                )
            }
            if (showSleepTimerDialog) {
                SleepTimerDialog(
                    onDismiss = { showSleepTimerDialog = false },
                    onStartTimer = { minutes -> playerViewModel.startSleepTimer(minutes); showSleepTimerDialog = false },
                )
            }
            if (showCreatePlaylistDialog) {
                CreatePlaylistDialogInline(
                    onDismiss = { showCreatePlaylistDialog = false },
                    onCreate = { name ->
                        playlistViewModel.createPlaylist(name)
                        showCreatePlaylistDialog = false
                    },
                )
            }
            if (showContextMenu && contextMenuTrack != null) {
                SongContextMenu(
                    track = contextMenuTrack!!,
                    sheetState = rememberModalBottomSheetState(),
                    onDismiss = { showContextMenu = false; contextMenuTrack = null },
                    onShare = { shareTrack(context, it) },
                    onAddToPlaylist = { result ->
                        addToPlaylistTrack = result
                        showAddToPlaylistDialog = true
                    },
                    onStartRadio = { result ->
                        coroutineScope.launch {
                            val audioUrl = searchViewModel.onTrackSelected(result)
                            if (audioUrl != null) {
                                playerViewModel.playTrack(result, audioUrl)
                            }
                        }
                    },
                    onPlayNext = { result ->
                        playerViewModel.playNext(result)
                    },
                    onEnqueue = { result ->
                        playerViewModel.enqueue(result)
                    },
                    onGoToArtist = { artistName ->
                        coroutineScope.launch {
                            val artistBrowseId = searchViewModel.findArtistBrowseId(artistName)
                            if (artistBrowseId != null) {
                                navController.navigate("artist/$artistBrowseId")
                            }
                        }
                    },
                    onGoToAlbum = { albumTitle ->
                        coroutineScope.launch {
                            val albumBrowseId = searchViewModel.findAlbumBrowseId(albumTitle)
                            if (albumBrowseId != null) {
                                navController.navigate("album/$albumBrowseId")
                            }
                        }
                    },
                    onDownload = { result ->
                        playerViewModel.downloadTrack(result)
                    },
                )
            }
            LaunchedEffect(playerUiState.downloadSuccess) {
                playerUiState.downloadSuccess?.let {
                    Toast.makeText(context, "Downloaded: $it", Toast.LENGTH_SHORT).show()
                    homeNotifications.add(0, com.musicflow.app.ui.screens.HomeNotification(
                        title = "Download Complete",
                        message = "\"$it\" is ready for offline playback.",
                        type = com.musicflow.app.ui.screens.NotificationType.SUCCESS,
                    ))
                    playerViewModel.clearDownloadMessages()
                }
            }
            LaunchedEffect(playerUiState.downloadError) {
                playerUiState.downloadError?.let {
                    Toast.makeText(context, "Download failed: $it", Toast.LENGTH_SHORT).show()
                    homeNotifications.add(0, com.musicflow.app.ui.screens.HomeNotification(
                        title = "Download Failed",
                        message = it,
                        type = com.musicflow.app.ui.screens.NotificationType.ERROR,
                    ))
                    playerViewModel.clearDownloadMessages()
                }
            }
        }
    }
}