package com.musicflow.app.utils

import android.util.Log
import com.musicflow.app.data.remote.InfinityMasterClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelemetryTracker @Inject constructor(
    private val client: InfinityMasterClient
) {
    companion object {
        private const val TAG = "TelemetryTracker"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun trackEvent(eventType: String, payload: JsonObject? = null) {
        scope.launch {
            try {
                client.trackEvent(eventType, payload)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to track event $eventType: ${e.message}")
            }
        }
    }

    fun trackAppLaunch() {
        trackEvent("app_launch")
    }

    fun trackSongPlay(trackId: String, source: String) {
        trackEvent("song_play", buildJsonObject {
            put("trackId", JsonPrimitive(trackId))
            put("source", JsonPrimitive(source))
        })
    }

    fun trackSearch(query: String, resultCount: Int) {
        trackEvent("search", buildJsonObject {
            put("query", JsonPrimitive(query))
            put("resultCount", JsonPrimitive(resultCount))
        })
    }

    fun trackDownloadComplete(trackId: String, quality: String) {
        trackEvent("download_complete", buildJsonObject {
            put("trackId", JsonPrimitive(trackId))
            put("quality", JsonPrimitive(quality))
        })
    }

    fun trackPlaylistCreate(trackCount: Int) {
        trackEvent("playlist_create", buildJsonObject {
            put("trackCount", JsonPrimitive(trackCount))
        })
    }

    fun trackSettingsChange(setting: String, value: String) {
        trackEvent("settings_change", buildJsonObject {
            put("setting", JsonPrimitive(setting))
            put("value", JsonPrimitive(value))
        })
    }
}
