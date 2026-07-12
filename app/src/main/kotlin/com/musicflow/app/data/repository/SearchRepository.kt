package com.musicflow.app.data.repository

import android.content.Context
import android.util.Log
import com.musicflow.app.data.remote.AlbumInfo
import com.musicflow.app.data.remote.AlbumPage
import com.musicflow.app.data.remote.ArtistPage
import com.musicflow.app.data.remote.InnertubeClient
import com.musicflow.app.data.remote.SearchResult
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor() {

    private val client: HttpClient by lazy { InnertubeClient.create() }

    suspend fun search(query: String, language: String = "en"): List<com.musicflow.app.data.remote.SearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                val payload = InnertubeClient.buildSearchPayload(query, language)

                val response = client.post(
                    "https://music.youtube.com/youtubei/v1/search"
                ) {
                    setBody(TextContent(payload.toString(), ContentType.Application.Json))
                }

                val bodyText = response.bodyAsText()
                val results = InnertubeClient.parseSearchResults(bodyText)
                Log.i("SearchRepository", "Search query='$query' -> ${results.size} results")
                results
            } catch (e: Exception) {
                Log.e("SearchRepository", "Search failed: ${e.javaClass.simpleName}: ${e.message}", e)
                emptyList()
            }
        }
    }

    /**
     * Performs a filtered search on YouTube Music.
     *
     * @param query The search query.
     * @param filter The filter type (SONGS, ALBUMS, ARTISTS, PLAYLISTS).
     * @param language Optional language code.
     * @return List of search results matching the filter.
     */
    suspend fun searchWithFilter(query: String, filter: String, language: String = "en"): List<com.musicflow.app.data.remote.SearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                val payload = InnertubeClient.buildSearchPayloadWithFilter(query, filter, language)

                val response = client.post(
                    "https://music.youtube.com/youtubei/v1/search"
                ) {
                    setBody(TextContent(payload.toString(), ContentType.Application.Json))
                }

                val bodyText = response.bodyAsText()
                val results = InnertubeClient.parseSearchResultsByType(bodyText, filter)
                Log.i("SearchRepository", "Filtered search='$filter' query='$query' -> ${results.size} results")
                results
            } catch (e: Exception) {
                Log.e("SearchRepository", "Filtered search failed: ${e.javaClass.simpleName}: ${e.message}", e)
                emptyList()
            }
        }
    }

    /**
     * Fetches "Up Next" (Radio) tracks for a given video ID.
     *
     * Calls the /youtubei/v1/next endpoint with RDAMVM radio parameters
     * to get similar tracks from YouTube Music's algorithm.
     *
     * @param videoId The currently playing video ID.
     * @return List of similar tracks, or empty list on failure.
     */
    suspend fun getUpNext(videoId: String): List<com.musicflow.app.data.remote.SearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                val payload = InnertubeClient.buildUpNextPayload(videoId)

                val response = client.post(
                    "https://music.youtube.com/youtubei/v1/next"
                ) {
                    setBody(TextContent(payload.toString(), ContentType.Application.Json))
                }

                val bodyText = response.bodyAsText()
                val results = InnertubeClient.parseUpNextResults(bodyText, videoId)
                Log.i("SearchRepository", "getUpNext for $videoId -> ${results.size} similar tracks")
                results
            } catch (e: Exception) {
                Log.e("SearchRepository", "getUpNext failed: ${e.javaClass.simpleName}: ${e.message}", e)
                emptyList()
            }
        }
    }

    suspend fun getAudioStreamUrl(videoId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val result = extractAudioWithHeaders(videoId)
                result.url
            } catch (e: Exception) {
                Log.e("SearchRepository", "Audio URL extraction failed: ${e.message}", e)
                null
            }
        }
    }

    suspend fun extractAudioWithHeaders(videoId: String): AudioExtractionResult {
        return withContext(Dispatchers.IO) {
            val videoUrl = "https://www.youtube.com/watch?v=$videoId"
            val request = YoutubeDLRequest(videoUrl)
            request.addOption("-f", "bestaudio")
            request.addOption("--no-playlist")
            request.addOption("--no-warnings")
            request.addOption("--dump-json")

            val response = YoutubeDL.getInstance().execute(request)
            val output = response.out

            val json = JSONObject(output)
            val url = json.optString("url", "")

            if (url.isBlank()) {
                throw Exception("No audio URL in response")
            }

            // Extract http_headers from yt-dlp response
            val headers = mutableMapOf<String, String>()
            val headersJson = json.optJSONObject("http_headers")
            if (headersJson != null) {
                for (key in headersJson.keys()) {
                    headers[key] = headersJson.optString(key, "")
                }
                Log.i("SearchRepository", "Extracted ${headers.size} headers for $videoId: ${headers.keys}")
            } else {
                Log.w("SearchRepository", "No http_headers in response for $videoId")
            }

            Log.i("SearchRepository", "Audio stream resolved for $videoId")
            AudioExtractionResult(url = url, headers = headers)
        }
    }

    /**
     * Fetches next track info (including lyrics) for a video.
     *
     * Calls the /youtubei/v1/next endpoint to get track metadata,
     * including any lyrics available for the track.
     *
     * @param videoId The video ID to fetch next info for.
     * @return JsonElement containing the response, or null on failure.
     */
    suspend fun getNextTrack(videoId: String): kotlinx.serialization.json.JsonElement? {
        return withContext(Dispatchers.IO) {
            try {
                val payload = InnertubeClient.buildNextPayload(videoId)

                val response = client.post(
                    "https://music.youtube.com/youtubei/v1/next"
                ) {
                    setBody(TextContent(payload.toString(), ContentType.Application.Json))
                }

                val bodyText = response.bodyAsText()
                val json = kotlinx.serialization.json.Json.parseToJsonElement(bodyText)
                Log.i("SearchRepository", "Fetched next info for $videoId")
                json
            } catch (e: Exception) {
                Log.e("SearchRepository", "getNextTrack failed: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Fetches search suggestions from YouTube Music.
     *
     * @param query The search query prefix.
     * @return List of suggestion strings.
     */
    suspend fun getSuggestions(query: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val payload = buildJsonObject {
                    putJsonObject("context") {
                        putJsonObject("client") {
                            put("clientName", "ANDROID_MUSIC")
                            put("clientVersion", "7.27.52")
                            put("hl", "en")
                            put("gl", "US")
                        }
                    }
                    put("input", query)
                }

                val response = client.post(
                    "https://music.youtube.com/youtubei/v1/music/get_search_suggestions"
                ) {
                    setBody(TextContent(payload.toString(), ContentType.Application.Json))
                }

                val bodyText = response.bodyAsText()
                val json = kotlinx.serialization.json.Json.parseToJsonElement(bodyText)

                val suggestions = json.jsonObject
                    .get("suggestions")
                    ?.jsonArray
                    ?.mapNotNull { item ->
                        item.jsonObject
                            .get("suggestion")
                            ?.jsonObject
                            ?.get("text")
                            ?.jsonPrimitive
                            ?.content
                    }
                    ?: emptyList()

                Log.i("SearchRepository", "Got ${suggestions.size} suggestions for '$query'")
                suggestions
            } catch (e: Exception) {
                Log.e("SearchRepository", "getSuggestions failed: ${e.message}", e)
                emptyList()
            }
        }
    }

    /**
     * Fetches an artist page from YouTube Music.
     *
     * @param browseId The artist browse ID (e.g., "UCxxxxxxx").
     * @return [ArtistPage] or null on failure.
     */
    suspend fun getArtistPage(browseId: String): ArtistPage? {
        return withContext(Dispatchers.IO) {
            try {
                val payload = InnertubeClient.buildArtistPayload(browseId)

                val response = client.post(
                    "https://music.youtube.com/youtubei/v1/browse"
                ) {
                    setBody(TextContent(payload.toString(), ContentType.Application.Json))
                }

                val bodyText = response.bodyAsText()
                val result = InnertubeClient.parseArtistPage(bodyText)
                Log.i("SearchRepository", "Fetched artist page for $browseId: ${result?.name}")
                result
            } catch (e: Exception) {
                Log.e("SearchRepository", "getArtistPage failed: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Fetches an album page from YouTube Music.
     *
     * @param browseId The album browse ID (e.g., "MPREb_xxx").
     * @return [AlbumPage] or null on failure.
     */
    suspend fun getAlbumPage(browseId: String): AlbumPage? {
        return withContext(Dispatchers.IO) {
            try {
                val payload = InnertubeClient.buildAlbumPayload(browseId)

                val response = client.post(
                    "https://music.youtube.com/youtubei/v1/browse"
                ) {
                    setBody(TextContent(payload.toString(), ContentType.Application.Json))
                }

                val bodyText = response.bodyAsText()
                val result = InnertubeClient.parseAlbumPage(bodyText)
                Log.i("SearchRepository", "Fetched album page for $browseId: ${result?.title}")
                result
            } catch (e: Exception) {
                Log.e("SearchRepository", "getAlbumPage failed: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Searches YouTube Music with filter params (albums, artists, playlists).
     *
     * @param query The search query.
     * @param params Base64-encoded protobuf filter params.
     * @return List of matching results.
     */
    suspend fun searchWithFilter(query: String, params: String): List<SearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                val payload = InnertubeClient.buildSearchPayloadWithParams(query, params)

                val response = client.post(
                    "https://music.youtube.com/youtubei/v1/search"
                ) {
                    setBody(TextContent(payload.toString(), ContentType.Application.Json))
                }

                val bodyText = response.bodyAsText()
                val results = InnertubeClient.parseSearchResults(bodyText)
                Log.i("SearchRepository", "Search with filter query='$query' -> ${results.size} results")
                results
            } catch (e: Exception) {
                Log.e("SearchRepository", "searchWithFilter failed: ${e.message}", e)
                emptyList()
            }
        }
    }
}
