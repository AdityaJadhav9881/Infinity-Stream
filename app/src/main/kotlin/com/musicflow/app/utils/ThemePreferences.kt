package com.musicflow.app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Theme mode options for the application.
 */
enum class ThemeMode(val label: String) {
    DARK("Dark"),
    LIGHT("Light"),
    SYSTEM("System"),
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Manages theme mode persistence using DataStore.
 */
@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        try {
            ThemeMode.valueOf(preferences[THEME_MODE_KEY] ?: ThemeMode.DARK.name)
        } catch (_: Exception) {
            ThemeMode.DARK
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }
}