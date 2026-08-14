package com.musicflow.app.data.remote

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches trending music and charts from YouTube Music via the Innertube API.
 *
 * Results are cached in memory with a configurable TTL to avoid redundant
 * network calls when the user navigates between screens.
 */
@Singleton
class TrendingClient @Inject constructor() {

    companion object {
        private const val TAG = "TrendingClient"
        private const val CLIENT_VERSION = "7.27.52"
        private const val ANDROID_SDK_VERSION = 34
        private const val BROWSE_URL = "https://music.youtube.com/youtubei/v1/browse"
        private const val TRENDING_BROWSE_ID = "FEtrending"
        private const val CHARTS_BROWSE_ID = "FEchart_corousel"
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
    }

    private val client: HttpClient by lazy { InnertubeClient.create() }

    /** In-memory cache with timestamps for TTL expiry. */
    private val cache = ConcurrentHashMap<String, CacheEntry>()

    private data class CacheEntry(
        val results: List<SearchResult>,
        val timestamp: Long,
    )

    /**
     * Fetches trending music tracks.
     *
     * @param country Country code (e.g., "US", "IN").
     * @param language Language code (e.g., "en", "hi").
     * @param limit Maximum number of results.
     * @return List of trending track search results.
     */
    suspend fun getTrending(
        country: String = "US",
        language: String = "en",
        limit: Int = 25,
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val cacheKey = "trending_${country}_$language"
        val cached = cache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
            Log.d(TAG, "Returning cached trending for $country/$language")
            return@withContext cached.results.take(limit)
        }

        try {
            val payload = buildBrowsePayload(TRENDING_BROWSE_ID, country, language)
            val response = client.post(BROWSE_URL) {
                setBody(TextContent(payload.toString(), ContentType.Application.Json))
            }
            val bodyText = response.bodyAsText()
            val results = parseBrowseResults(bodyText)

            cache[cacheKey] = CacheEntry(results, System.currentTimeMillis())
            Log.i(TAG, "Fetched trending for $country/$language: ${results.size} results")
            results.take(limit)
        } catch (e: Exception) {
            Log.e(TAG, "getTrending failed: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Fetches music charts / top tracks.
     *
     * @param country Country code.
     * @param limit Maximum number of results.
     * @return List of chart track search results.
     */
    suspend fun getCharts(
        country: String = "US",
        limit: Int = 25,
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val cacheKey = "charts_$country"
        val cached = cache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TTL_MS) {
            Log.d(TAG, "Returning cached charts for $country")
            return@withContext cached.results.take(limit)
        }

        try {
            val payload = buildBrowsePayload(CHARTS_BROWSE_ID, country, "en")
            val response = client.post(BROWSE_URL) {
                setBody(TextContent(payload.toString(), ContentType.Application.Json))
            }
            val bodyText = response.bodyAsText()
            val results = parseBrowseResults(bodyText)

            cache[cacheKey] = CacheEntry(results, System.currentTimeMillis())
            Log.i(TAG, "Fetched charts for $country: ${results.size} results")
            results.take(limit)
        } catch (e: Exception) {
            Log.e(TAG, "getCharts failed: ${e.message}", e)
            emptyList()
        }
    }

    /** Clears the in-memory cache. */
    fun clearCache() {
        cache.clear()
    }

    private fun buildBrowsePayload(browseId: String, country: String, language: String): JsonObject {
        return buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "ANDROID_MUSIC")
                    put("clientVersion", CLIENT_VERSION)
                    put("androidSdkVersion", ANDROID_SDK_VERSION)
                    put("hl", language)
                    put("gl", country)
                }
            }
            put("browseId", browseId)
        }
    }

    private fun parseBrowseResults(responseBody: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()
        try {
            val json = kotlinx.serialization.json.Json.parseToJsonElement(responseBody).jsonObject

            val contents = json["contents"]
                ?.jsonObject
                ?.get("singleColumnBrowseResultsRenderer")
                ?.jsonObject
                ?.get("tabs")
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("tabRenderer")
                ?.jsonObject
                ?.get("content")
                ?.jsonObject
                ?.get("sectionListRenderer")
                ?.jsonObject
                ?.get("contents")
                ?.jsonArray

            contents?.forEach { section ->
                val sectionObj = section.jsonObject
                val shelf = sectionObj["musicShelfRenderer"]?.jsonObject
                    ?: sectionObj["musicCarouselShelfRenderer"]?.jsonObject
                    ?: return@forEach

                shelf["contents"]?.jsonArray?.forEach { item ->
                    val renderer = item.jsonObject["musicResponsiveListItemRenderer"]?.jsonObject
                        ?: item.jsonObject["gridMusicEntryRowRenderer"]?.jsonObject
                        ?: return@forEach

                    val videoId = renderer["playlistItemData"]
                        ?.jsonObject
                        ?.get("videoId")
                        ?.jsonPrimitive
                        ?.content
                        ?: renderer["navigationEndpoint"]
                            ?.jsonObject
                            ?.get("watchEndpoint")
                            ?.jsonObject
                            ?.get("videoId")
                            ?.jsonPrimitive
                            ?.content
                        ?: return@forEach

                    val title = renderer["flexColumns"]
                        ?.jsonArray
                        ?.getOrNull(0)
                        ?.jsonObject
                        ?.get("musicResponsiveListItemFlexColumnRenderer")
                        ?.jsonObject
                        ?.get("text")
                        ?.jsonObject
                        ?.get("runs")
                        ?.jsonArray
                        ?.getOrNull(0)
                        ?.jsonObject
                        ?.get("text")
                        ?.jsonPrimitive
                        ?.content
                        ?: "Unknown Title"

                    val artist = renderer["flexColumns"]
                        ?.jsonArray
                        ?.getOrNull(1)
                        ?.jsonObject
                        ?.get("musicResponsiveListItemFlexColumnRenderer")
                        ?.jsonObject
                        ?.get("text")
                        ?.jsonObject
                        ?.get("runs")
                        ?.jsonArray
                        ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.content ?: "" }
                        ?: "Unknown Artist"

                    val thumbnail = renderer["thumbnail"]
                        ?.jsonObject
                        ?.get("musicThumbnailRenderer")
                        ?.jsonObject
                        ?.get("thumbnails")
                        ?.jsonArray
                        ?.lastOrNull()
                        ?.jsonObject
                        ?.get("url")
                        ?.jsonPrimitive
                        ?.content
                        ?: ""

                    results.add(
                        SearchResult(
                            videoId = videoId,
                            title = title,
                            artist = artist,
                            thumbnailUrl = thumbnail,
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "parseBrowseResults failed: ${e.message}", e)
        }
        return results
    }
}
