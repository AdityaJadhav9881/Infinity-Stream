package com.musicflow.app.utils

import android.util.Log
import com.musicflow.app.data.remote.InfinityMasterClient
import com.musicflow.app.data.remote.UpdateInfo
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateChecker @Inject constructor(
    private val client: InfinityMasterClient,
    private val deviceIdProvider: DeviceIdProvider,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private const val TAG = "UpdateChecker"
        private val UPDATE_AVAILABLE_KEY = booleanPreferencesKey("im_update_available")
        private val UPDATE_VERSION_KEY = stringPreferencesKey("im_update_version")
        private val UPDATE_NOTES_KEY = stringPreferencesKey("im_update_notes")
        private val UPDATE_URL_KEY = stringPreferencesKey("im_update_url")
        private val UPDATE_FORCED_KEY = booleanPreferencesKey("im_update_forced")
    }

    suspend fun checkOnStartup() {
        try {
            val currentVersion = deviceIdProvider.getAppVersion()
            val result = client.checkForUpdate(currentVersion)
            val updateInfo = result.getOrNull()
            if (updateInfo != null) {
                dataStore.edit { prefs ->
                    prefs[UPDATE_AVAILABLE_KEY] = true
                    prefs[UPDATE_VERSION_KEY] = updateInfo.version
                    prefs[UPDATE_NOTES_KEY] = updateInfo.releaseNotes
                    prefs[UPDATE_URL_KEY] = updateInfo.downloadUrl
                    prefs[UPDATE_FORCED_KEY] = updateInfo.forced
                }
                Log.i(TAG, "Update available: ${updateInfo.version}")
            } else {
                dataStore.edit { it[UPDATE_AVAILABLE_KEY] = false }
                Log.i(TAG, "App is up to date")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Update check failed: ${e.message}")
        }
    }

    fun observeUpdateInfo(): Flow<UpdateInfo?> = dataStore.data.map { prefs ->
        if (prefs[UPDATE_AVAILABLE_KEY] == true) {
            UpdateInfo(
                version = prefs[UPDATE_VERSION_KEY] ?: "",
                releaseNotes = prefs[UPDATE_NOTES_KEY] ?: "",
                downloadUrl = prefs[UPDATE_URL_KEY] ?: "",
                forced = prefs[UPDATE_FORCED_KEY] ?: false
            )
        } else null
    }

    suspend fun clearUpdate() {
        dataStore.edit { prefs ->
            prefs[UPDATE_AVAILABLE_KEY] = false
            prefs.remove(UPDATE_VERSION_KEY)
            prefs.remove(UPDATE_NOTES_KEY)
            prefs.remove(UPDATE_URL_KEY)
            prefs.remove(UPDATE_FORCED_KEY)
        }
        Log.i(TAG, "Update cleared")
    }
}
