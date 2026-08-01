package com.musicflow.app.utils

import android.util.Log
import com.musicflow.app.data.remote.InfinityMasterClient
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigManager @Inject constructor(
    private val client: InfinityMasterClient,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private const val TAG = "RemoteConfigManager"
        private const val PREFIX = "im_rc_"
        private val FETCHED_KEY = booleanPreferencesKey("im_rc_fetched")
    }

    suspend fun fetchOnStartup() {
        try {
            val result = client.fetchRemoteConfig()
            val entries = result.getOrNull() ?: return
            dataStore.edit { prefs ->
                for (entry in entries) {
                    prefs[stringPreferencesKey(PREFIX + entry.key)] = entry.value
                }
                prefs[FETCHED_KEY] = true
            }
            Log.i(TAG, "Fetched ${entries.size} remote config entries")
        } catch (e: Exception) {
            Log.w(TAG, "Remote config fetch failed: ${e.message}")
        }
    }

    suspend fun getString(key: String, default: String = ""): String {
        return dataStore.data.map { prefs ->
            prefs[stringPreferencesKey(PREFIX + key)] ?: default
        }.first()
    }

    suspend fun getBoolean(key: String, default: Boolean = false): Boolean {
        val raw = getString(key, default.toString())
        return raw.toBooleanStrictOrNull() ?: default
    }

    suspend fun getInt(key: String, default: Int = 0): Int {
        val raw = getString(key, default.toString())
        return raw.toIntOrNull() ?: default
    }
}
