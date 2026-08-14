package com.musicflow.app.data.remote

import android.util.Log
import com.musicflow.app.BuildConfig
import com.musicflow.app.utils.DeviceIdProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val version: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val forced: Boolean
)

data class ConfigEntry(
    val key: String,
    val value: String,
    val description: String?
)

@Singleton
class InfinityMasterClient @Inject constructor(
    private val httpClient: HttpClient,
    private val deviceIdProvider: DeviceIdProvider
) {
    companion object {
        private const val TAG = "InfinityMasterClient"
        private const val BASE_URL = "https://infinity-master.vercel.app"
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun io.ktor.client.request.HttpRequestBuilder.apiKeyHeader() {
        header("X-API-Key", BuildConfig.API_KEY)
    }

    suspend fun registerDevice(
        fullName: String? = null,
        howDidYouHear: String? = null
    ): Result<Unit> = runCatching {
        val deviceId = deviceIdProvider.getDeviceId()
        val platform = deviceIdProvider.getPlatform()
        val osVersion = deviceIdProvider.getOsVersion()
        val appVersion = deviceIdProvider.getAppVersion()
        val body = buildJsonObject {
            put("deviceId", JsonPrimitive(deviceId))
            put("platform", JsonPrimitive(platform))
            put("osVersion", JsonPrimitive(osVersion))
            put("appVersion", JsonPrimitive(appVersion))
            if (!fullName.isNullOrBlank()) put("fullName", JsonPrimitive(fullName))
            if (!howDidYouHear.isNullOrBlank()) put("howDidYouHear", JsonPrimitive(howDidYouHear))
        }
        val response = httpClient.post("$BASE_URL/api/devices") {
            apiKeyHeader()
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        val statusCode = response.status.value
        if (statusCode == 403) {
            deviceIdProvider.setDeviceDisabled(true)
            Log.w(TAG, "Device disabled by admin")
            throw SecurityException("Device disabled")
        }
        Log.i(TAG, "Device registered")
    }

    suspend fun checkDeviceStatus(): Result<Boolean> = runCatching {
        val deviceId = deviceIdProvider.getDeviceId()
        val platform = deviceIdProvider.getPlatform()
        val osVersion = deviceIdProvider.getOsVersion()
        val appVersion = deviceIdProvider.getAppVersion()
        val body = buildJsonObject {
            put("deviceId", JsonPrimitive(deviceId))
            put("platform", JsonPrimitive(platform))
            put("osVersion", JsonPrimitive(osVersion))
            put("appVersion", JsonPrimitive(appVersion))
        }
        val response = httpClient.post("$BASE_URL/api/devices") {
            apiKeyHeader()
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        val statusCode = response.status.value
        if (statusCode == 403) {
            deviceIdProvider.setDeviceDisabled(true)
            return@runCatching true
        }
        deviceIdProvider.setDeviceDisabled(false)
        false
    }

    suspend fun reportCrash(
        errorMessage: String,
        stackTrace: String,
        version: String
    ): Result<Unit> = runCatching {
        val deviceId = deviceIdProvider.getDeviceId()
        val platform = deviceIdProvider.getPlatform()
        val osVersion = deviceIdProvider.getOsVersion()
        val body = buildJsonObject {
            put("deviceId", JsonPrimitive(deviceId))
            put("version", JsonPrimitive(version))
            put("platform", JsonPrimitive(platform))
            put("osVersion", JsonPrimitive(osVersion))
            put("errorMessage", JsonPrimitive(errorMessage))
            put("stackTrace", JsonPrimitive(stackTrace))
        }
        httpClient.post("$BASE_URL/api/crashes/report") {
            apiKeyHeader()
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        Log.i(TAG, "Crash reported")
    }

    suspend fun trackEvent(
        eventType: String,
        payload: JsonObject? = null
    ): Result<Unit> = runCatching {
        val deviceId = deviceIdProvider.getDeviceId()
        val platform = deviceIdProvider.getPlatform()
        val appVersion = deviceIdProvider.getAppVersion()
        val body = buildJsonObject {
            put("deviceId", JsonPrimitive(deviceId))
            put("eventType", JsonPrimitive(eventType))
            put("appVersion", JsonPrimitive(appVersion))
            put("platform", JsonPrimitive(platform))
            if (payload != null) put("payload", payload)
        }
        httpClient.post("$BASE_URL/api/telemetry/event") {
            apiKeyHeader()
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
    }

    suspend fun checkForUpdate(currentVersion: String): Result<UpdateInfo?> = runCatching {
        val deviceId = deviceIdProvider.getDeviceId()
        val platform = deviceIdProvider.getPlatform()
        val osVersion = deviceIdProvider.getOsVersion()
        val body = buildJsonObject {
            put("deviceId", JsonPrimitive(deviceId))
            put("platform", JsonPrimitive(platform))
            put("osVersion", JsonPrimitive(osVersion))
            put("currentVersion", JsonPrimitive(currentVersion))
        }
        val response = httpClient.post("$BASE_URL/api/updates/check") {
            apiKeyHeader()
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        val text = response.bodyAsText()
        val obj = json.parseToJsonElement(text) as JsonObject
        val available = obj["updateAvailable"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
        if (!available) return@runCatching null
        UpdateInfo(
            version = obj["version"]?.jsonPrimitive?.content ?: "",
            releaseNotes = obj["releaseNotes"]?.jsonPrimitive?.content ?: "",
            downloadUrl = obj["downloadUrl"]?.jsonPrimitive?.content ?: "",
            forced = obj["forced"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
        )
    }

    suspend fun fetchRemoteConfig(): Result<List<ConfigEntry>> = runCatching {
        val response = httpClient.get("$BASE_URL/api/config?platform=android") {
            apiKeyHeader()
        }
        val text = response.bodyAsText()
        val arr = json.parseToJsonElement(text) as? kotlinx.serialization.json.JsonArray
            ?: return@runCatching emptyList()
        arr.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            ConfigEntry(
                key = obj["key"]?.toString()?.removeSurrounding("\"") ?: return@mapNotNull null,
                value = obj["value"]?.toString()?.removeSurrounding("\"") ?: "",
                description = obj["description"]?.toString()?.removeSurrounding("\"")
            )
        }
    }
}
