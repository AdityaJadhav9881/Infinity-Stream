package com.musicflow.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation routes for the bottom navigation bar.
 *
 * @param route Unique route identifier for NavHost.
 * @param label Display name in the NavigationBar.
 * @param selectedIcon Icon when the tab is selected.
 * @param unselectedIcon Icon when the tab is unselected.
 */
enum class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home(
        route = "home",
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
    ),
    Search(
        route = "search",
        label = "Search",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
    ),
    Library(
        route = "library",
        label = "Library",
        selectedIcon = Icons.Filled.LibraryMusic,
        unselectedIcon = Icons.Outlined.LibraryMusic,
    ),
    Settings(
        route = "settings",
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
    ),
}

/** Standalone route for the full Downloads screen (not in bottom nav). */
const val DOWNLOADS_ROUTE = "downloads"

/** Library route with optional filter argument. */
const val LIBRARY_ROUTE = "library?filter={filter}"
