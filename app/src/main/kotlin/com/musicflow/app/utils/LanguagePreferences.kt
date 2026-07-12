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
 * Supported search languages for YouTube Music.
 */
enum class SearchLanguage(val label: String, val param: String) {
    ENGLISH("English", "en"),
    HINDI("Hindi", "hi"),
    MARATHI("Marathi", "mr"),
    SPANISH("Spanish", "es"),
    PORTUGUESE("Portuguese", "pt"),
    JAPANESE("Japanese", "ja"),
    KOREAN("Korean", "ko"),
    FRENCH("French", "fr"),
    GERMAN("German", "de"),
    TAMIL("Tamil", "ta"),
    TELUGU("Telugu", "te"),
    BENGALI("Bengali", "bn"),
    PUNJABI("Punjabi", "pa"),
    ARABIC("Arabic", "ar"),
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "language_settings")

/**
 * Manages search language preferences using DataStore.
 */
@Singleton
class LanguagePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("search_language")
        private val LANGUAGES_KEY = stringPreferencesKey("search_languages")
        private val ONBOARDING_DONE_KEY = stringPreferencesKey("onboarding_done")
    }

    val searchLanguage: Flow<SearchLanguage> = context.dataStore.data.map { preferences ->
        try {
            SearchLanguage.valueOf(preferences[LANGUAGE_KEY] ?: SearchLanguage.ENGLISH.name)
        } catch (_: Exception) {
            SearchLanguage.ENGLISH
        }
    }

    val searchLanguages: Flow<List<SearchLanguage>> = context.dataStore.data.map { preferences ->
        try {
            val saved = preferences[LANGUAGES_KEY] ?: SearchLanguage.ENGLISH.name
            saved.split(",").mapNotNull { name ->
                try { SearchLanguage.valueOf(name.trim()) } catch (_: Exception) { null }
            }.ifEmpty { listOf(SearchLanguage.ENGLISH) }
        } catch (_: Exception) {
            listOf(SearchLanguage.ENGLISH)
        }
    }

    val isOnboardingDone: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ONBOARDING_DONE_KEY] == "true"
    }

    suspend fun setLanguage(language: SearchLanguage) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.name
        }
    }

    suspend fun setLanguages(languages: List<SearchLanguage>) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGES_KEY] = languages.joinToString(",") { it.name }
            if (languages.isNotEmpty()) {
                preferences[LANGUAGE_KEY] = languages.first().name
            }
        }
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_DONE_KEY] = "true"
        }
    }
}