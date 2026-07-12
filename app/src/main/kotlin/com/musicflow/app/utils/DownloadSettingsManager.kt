package com.musicflow.app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.downloadDataStore: DataStore<Preferences> by preferencesDataStore(name = "download_settings")

@Singleton
class DownloadSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val QUALITY_KEY = stringPreferencesKey("download_quality")
        private val WIFI_ONLY_KEY = booleanPreferencesKey("wifi_only")
        private val AUTO_DOWNLOAD_LIKED_KEY = booleanPreferencesKey("auto_download_liked")
        private val SMART_DOWNLOADS_KEY = booleanPreferencesKey("smart_downloads")
    }

    val downloadQuality: Flow<String> = context.downloadDataStore.data.map { prefs ->
        prefs[QUALITY_KEY] ?: "High"
    }

    val wifiOnly: Flow<Boolean> = context.downloadDataStore.data.map { prefs ->
        prefs[WIFI_ONLY_KEY] ?: true
    }

    val autoDownloadLiked: Flow<Boolean> = context.downloadDataStore.data.map { prefs ->
        prefs[AUTO_DOWNLOAD_LIKED_KEY] ?: false
    }

    val smartDownloads: Flow<Boolean> = context.downloadDataStore.data.map { prefs ->
        prefs[SMART_DOWNLOADS_KEY] ?: false
    }

    suspend fun setDownloadQuality(quality: String) {
        context.downloadDataStore.edit { it[QUALITY_KEY] = quality }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.downloadDataStore.edit { it[WIFI_ONLY_KEY] = enabled }
    }

    suspend fun setAutoDownloadLiked(enabled: Boolean) {
        context.downloadDataStore.edit { it[AUTO_DOWNLOAD_LIKED_KEY] = enabled }
    }

    suspend fun setSmartDownloads(enabled: Boolean) {
        context.downloadDataStore.edit { it[SMART_DOWNLOADS_KEY] = enabled }
    }
}
