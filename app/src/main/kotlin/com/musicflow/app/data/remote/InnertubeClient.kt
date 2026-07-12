package com.musicflow.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLDecoder

/**
 * Lightweight HTTP client that spoofs the YouTube Music Android client
 * to bypass ad injection at the network level.
 *
 * This client is **not** a full Innertube wrapper — it only provides
 * the minimal surface area required for audio stream resolution.
 *
 * Usage:
 * ```kotlin
 * val client = InnertubeClient.create()
 * val response = client.postPlayerRequest(videoId)
 * client.close()
 * ```
 *
 * Threading:
 * All network I/O is dispatched to OkHttp's internal thread pool.
 * Callers must invoke suspend functions from a [Dispatchers.IO] context.
 */
object InnertubeClient {

    private const val SEARCH_URL = "https://music.youtube.com/youtubei/v1/search"

    /** Spoofed User-Agent that mimics the official YouTube Music Android client. */
    private const val USER_AGENT =
        "com.google.android.apps.youtube.music/"

    /** Client version — should match a recent release of YouTube Music. */
    private const val CLIENT_VERSION = "7.27.52"

    /** Android SDK version reported in the spoofed context. */
    private const val ANDROID_SDK_VERSION = 34

    /**
     * Creates a pre-configured [HttpClient] using the OkHttp engine.
     *
     * Configuration:
     * - Engine: OkHttp (mature, supports HTTP/2, connection pooling)
     * - Serialization: kotlinx.serialization via ContentNegotiation
     * - Headers: Spoofed client context injected via [defaultRequest]
     */
    fun create(): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                config {
                    followRedirects(true)
                    followSslRedirects(true)
                }
            }

            defaultRequest {
                header("User-Agent", USER_AGENT)
                header("X-YouTube-Client-Name", "21")
                header("X-YouTube-Client-Version", CLIENT_VERSION)
            }
        }
    }

    /**
     * Builds the JSON payload for searching YouTube Music.
     *
     * The payload structure:
     * ```json
     * {
     *   "context": {
     *     "client": {
     *       "clientName": "ANDROID_MUSIC",
     *       "clientVersion": "7.27.52",
     *       "androidSdkVersion": 34,
     *       "hl": "en",
     *       "gl": "US"
     *     }
     *   },
     *   "query": "<search_query>",
     *   "params": "EgWKAQIIA"
     * }
     * ```
     *
     * @param query The search query.
     * @return A [JsonObject] ready for serialization.
     */
    /**
     * Builds the JSON payload for searching YouTube Music.
     * @param query The search query.
     * @param language Optional language code (e.g., "en", "hi", "es").
     */
    /**
     * Builds the JSON payload for the /next endpoint (track info + lyrics).
     */
    fun buildNextPayload(videoId: String): JsonObject {
        return buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "ANDROID_MUSIC")
                    put("clientVersion", CLIENT_VERSION)
                    put("androidSdkVersion", ANDROID_SDK_VERSION)
                    put("hl", "en")
                    put("gl", "US")
                }
            }
            put("videoId", videoId)
        }
    }

    fun buildSearchPayload(query: String, language: String = "en"): JsonObject {
        return buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "ANDROID_MUSIC")
                    put("clientVersion", CLIENT_VERSION)
                    put("androidSdkVersion", ANDROID_SDK_VERSION)
                    put("hl", language)
                    put("gl", "US")
                }
            }
            put("query", query)
            put("params", "EgWKAQIIA")  // Filter for songs only
        }
    }

    /**
     * Builds the JSON payload for filtered search on YouTube Music.
     *
     * @param query The search query.
     * @param filter The filter type (SONGS, ALBUMS, ARTISTS, PLAYLISTS).
     * @param language Optional language code.
     */
    fun buildSearchPayloadWithFilter(query: String, filter: String, language: String = "en"): JsonObject {
        val params = when (filter) {
            "ALBUMS" -> "EgWKAQIQAWoKEAMQBRAJEAoQBQ=="
            "ARTISTS" -> "EgWKAQIYAWoKEAMQBRAJEAoQBQ=="
            "PLAYLISTS" -> "EgQIARgB"
            else -> "EgWKAQIIA" // default songs
        }

        return buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "ANDROID_MUSIC")
                    put("clientVersion", CLIENT_VERSION)
                    put("androidSdkVersion", ANDROID_SDK_VERSION)
                    put("hl", language)
                    put("gl", "US")
                }
            }
            put("query", query)
            put("params", params)
        }
    }

    /**
     * Parses search results filtered by type.
     *
     * @param responseBody The raw JSON response from the search endpoint.
     * @param filter The filter type used for the search.
     * @return A list of parsed search result items.
     */
    fun parseSearchResultsByType(responseBody: String, filter: String): List<SearchResult> {
        return when (filter) {
            "SONGS", "ALL" -> parseSearchResults(responseBody)
            else -> parseSearchResults(responseBody) // fallback to songs for now
        }
    }

    /**
     * Builds a search payload with filter params for albums, artists, or playlists.
     *
     * @param query The search query.
     * @param params Base64-encoded protobuf params for the filter.
     */
    fun buildSearchPayloadWithParams(query: String, params: String): JsonObject {
        return buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "ANDROID_MUSIC")
                    put("clientVersion", CLIENT_VERSION)
                    put("androidSdkVersion", ANDROID_SDK_VERSION)
                    put("hl", "en")
                    put("gl", "US")
                }
            }
            put("query", query)
            put("params", params)
        }
    }

    /**
     * Builds the JSON payload for fetching an artist page.
     *
     * @param browseId The artist browse ID (e.g., "UCxxxxxxx").
     */
    fun buildArtistPayload(browseId: String): JsonObject {
        return buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "ANDROID_MUSIC")
                    put("clientVersion", CLIENT_VERSION)
                    put("androidSdkVersion", ANDROID_SDK_VERSION)
                    put("hl", "en")
                    put("gl", "US")
                }
            }
            put("browseId", browseId)
        }
    }

    /**
     * Builds the JSON payload for fetching an album page.
     *
     * @param browseId The album browse ID (e.g., "MPREb_xxx").
     */
    fun buildAlbumPayload(browseId: String): JsonObject {
        return buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "ANDROID_MUSIC")
                    put("clientVersion", CLIENT_VERSION)
                    put("androidSdkVersion", ANDROID_SDK_VERSION)
                    put("hl", "en")
                    put("gl", "US")
                }
            }
            put("browseId", browseId)
        }
    }

    /**
     * Parses an artist page response from the /browse endpoint.
     *
     * @param responseBody Raw JSON response from the browse endpoint.
     * @return Parsed [ArtistPage], or null if parsing fails.
     */
    fun parseArtistPage(responseBody: String): ArtistPage? {
        try {
            val json = Json.parseToJsonElement(responseBody).jsonObject
            val header = json["header"]
                ?.jsonObject
                ?.get("musicImmersiveHeaderRenderer")
                ?.jsonObject
                ?: json["header"]
                    ?.jsonObject
                    ?.get("musicVisualHeaderRenderer")
                    ?.jsonObject

            val name = header
                ?.get("title")
                ?.jsonObject
                ?.get("runs")
                ?.jsonArray
                ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.content ?: "" }
                ?: return null

            val thumbnailUrl = header
                ?.get("backgroundArt")
                ?.jsonObject
                ?.get("thumbnails")
                ?.jsonArray
                ?.lastOrNull()
                ?.jsonObject
                ?.get("url")
                ?.jsonPrimitive
                ?.content
                ?: ""

            val description = json["header"]
                ?.jsonObject
                ?.get("musicImmersiveHeaderRenderer")
                ?.jsonObject
                ?.get("description")
                ?.jsonObject
                ?.get("runs")
                ?.jsonArray
                ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.content ?: "" }

            // Parse content sections
            val contents = json["contents"]
                ?.jsonObject
                ?.get("singleColumnBrowseResultsRenderer")
                ?.jsonObject
                ?.get("tabs")
                ?.jsonArray
                ?.getOrNull(0)
                ?.jsonObject
                ?.get("tabRenderer")
                ?.jsonObject
                ?.get("content")
                ?.jsonObject
                ?.get("sectionListRenderer")
                ?.jsonObject
                ?.get("contents")
                ?.jsonArray

            val songs = mutableListOf<SearchResult>()
            val albums = mutableListOf<AlbumInfo>()

            contents?.forEach { section ->
                val sectionObj = section.jsonObject
                val shelf = sectionObj["musicShelfRenderer"]?.jsonObject
                    ?: sectionObj["gridRenderer"]?.jsonObject
                    ?: return@forEach

                val title = shelf["title"]
                    ?.jsonObject
                    ?.get("runs")
                    ?.jsonArray
                    ?.getOrNull(0)
                    ?.jsonObject
                    ?.get("text")
                    ?.jsonPrimitive
                    ?.content

                when {
                    title?.contains("Song", ignoreCase = true) == true -> {
                        shelf["contents"]?.jsonArray?.forEach { item ->
                            parseMusicResponsiveListItem(item.jsonObject)?.let { songs.add(it) }
                        }
                    }
                    title?.contains("Album", ignoreCase = true) == true -> {
                        shelf["contents"]?.jsonArray?.forEach { item ->
                            parseAlbumItem(item.jsonObject)?.let { albums.add(it) }
                        }
                    }
                    else -> {
                        // Try both parsers
                        shelf["contents"]?.jsonArray?.forEach { item ->
                            parseMusicResponsiveListItem(item.jsonObject)?.let { songs.add(it) }
                                ?: parseAlbumItem(item.jsonObject)?.let { albums.add(it) }
                        }
                    }
                }
            }

            return ArtistPage(
                name = name,
                thumbnailUrl = thumbnailUrl,
                description = description,
                songs = songs,
                albums = albums,
            )
        } catch (e: Exception) {
            android.util.Log.e("InnertubeClient", "Failed to parse artist page: ${e.message}", e)
            return null
        }
    }

    /**
     * Parses an album page response from the /browse endpoint.
     *
     * @param responseBody Raw JSON response from the browse endpoint.
     * @return Parsed [AlbumPage], or null if parsing fails.
     */
    fun parseAlbumPage(responseBody: String): AlbumPage? {
        try {
            val json = Json.parseToJsonElement(responseBody).jsonObject

            val header = json["header"]
                ?.jsonObject
                ?.get("musicResponsiveHeaderRenderer")
                ?.jsonObject

            val title = header
                ?.get("title")
                ?.jsonObject
                ?.get("runs")
                ?.jsonArray
                ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.content ?: "" }
                ?: return null

            val artist = header
                ?.get("subtitle")
                ?.jsonObject
                ?.get("runs")
                ?.jsonArray
                ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.content ?: "" }
                ?: ""

            val thumbnailUrl = header
                ?.get("image")
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

            val year = header
                ?.get("subtitle")
                ?.jsonObject
                ?.get("runs")
                ?.jsonArray
                ?.lastOrNull()
                ?.jsonObject
                ?.get("text")
                ?.jsonPrimitive
                ?.content
                ?.takeIf { it.length == 4 && it.all { c -> c.isDigit() } }

            // Parse track list
            val contents = json["contents"]
                ?.jsonObject
                ?.get("singleColumnBrowseResultsRenderer")
                ?.jsonObject
                ?.get("tabs")
                ?.jsonArray
                ?.getOrNull(0)
                ?.jsonObject
                ?.get("tabRenderer")
                ?.jsonObject
                ?.get("content")
                ?.jsonObject
                ?.get("sectionListRenderer")
                ?.jsonObject
                ?.get("contents")
                ?.jsonArray

            val tracks = mutableListOf<SearchResult>()
            contents?.forEach { section ->
                section.jsonObject["musicShelfRenderer"]
                    ?.jsonObject
                    ?.get("contents")
                    ?.jsonArray
                    ?.forEach { item ->
                        parseMusicResponsiveListItem(item.jsonObject)?.let { tracks.add(it) }
                    }
            }

            return AlbumPage(
                title = title,
                artist = artist,
                thumbnailUrl = thumbnailUrl,
                year = year,
                tracks = tracks,
            )
        } catch (e: Exception) {
            android.util.Log.e("InnertubeClient", "Failed to parse album page: ${e.message}", e)
            return null
        }
    }

    private fun parseMusicResponsiveListItem(renderer: JsonObject): SearchResult? {
        val videoId = renderer["navigationEndpoint"]
            ?.jsonObject
            ?.get("watchEndpoint")
            ?.jsonObject
            ?.get("videoId")
            ?.jsonPrimitive
            ?.content
            ?: renderer["playlistItemData"]
                ?.jsonObject
                ?.get("videoId")
                ?.jsonPrimitive
                ?.content
            ?: return null

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

        return SearchResult(
            videoId = videoId,
            title = title,
            artist = artist,
            thumbnailUrl = thumbnail,
        )
    }

    private fun parseAlbumItem(item: JsonObject): AlbumInfo? {
        val renderer = item["musicResponsiveListItemRenderer"]?.jsonObject
            ?: item["gridMusicCollectionItemRenderer"]?.jsonObject
            ?: return null

        val browseId = renderer["navigationEndpoint"]
            ?.jsonObject
            ?.get("browseEndpoint")
            ?.jsonObject
            ?.get("browseId")
            ?.jsonPrimitive
            ?.content
            ?: return null

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
            ?: "Unknown Album"

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

        return AlbumInfo(
            browseId = browseId,
            title = title,
            thumbnailUrl = thumbnail,
            artist = artist,
        )
    }

    /**
     * Builds the JSON payload for the "Up Next" / Radio endpoint.
     *
     * The RDAMVM prefix tells YouTube to generate an endless radio mix
     * for the specific videoId.
     *
     * ```json
     * {
     *   "context": { "client": { "clientName": "ANDROID_MUSIC", ... } },
     *   "videoId": "<video_id>",
     *   "playlistId": "RDAMVM<video_id>"
     * }
     * ```
     */
    fun buildUpNextPayload(videoId: String): JsonObject {
        return buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "ANDROID_MUSIC")
                    put("clientVersion", CLIENT_VERSION)
                    put("androidSdkVersion", ANDROID_SDK_VERSION)
                    put("hl", "en")
                    put("gl", "US")
                }
            }
            put("videoId", videoId)
            put("playlistId", "RDAMVM$videoId")
        }
    }

    /**
     * Parses "Up Next" results from the /youtubei/v1/next endpoint.
     *
     * Defensive parsing path:
     * ```
     * contents
     *   → singleColumnMusicWatchNextResultsRenderer
     *   → tabbedRenderer
     *   → watchNextTabbedResultsRenderer
     *   → tabs[0]
     *   → tabRenderer
     *   → content
     *   → musicQueueRenderer
     *   → content
     *   → playlistPanelRenderer
     *   → contents
     *     → playlistPanelVideoRenderer (each item)
     * ```
     *
     * @param responseBody Raw JSON response from the /next endpoint.
     * @param currentVideoId The currently playing video ID (excluded from results).
     * @return List of similar tracks, or empty list if parsing fails.
     */
    fun parseUpNextResults(responseBody: String, currentVideoId: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()

        try {
            val json = Json.parseToJsonElement(responseBody).jsonObject

            val playlistContents = json["contents"]
                ?.jsonObject
                ?.get("singleColumnMusicWatchNextResultsRenderer")
                ?.jsonObject
                ?.get("tabbedRenderer")
                ?.jsonObject
                ?.get("watchNextTabbedResultsRenderer")
                ?.jsonObject
                ?.get("tabs")
                ?.jsonArray
                ?.getOrNull(0)
                ?.jsonObject
                ?.get("tabRenderer")
                ?.jsonObject
                ?.get("content")
                ?.jsonObject
                ?.get("musicQueueRenderer")
                ?.jsonObject
                ?.get("content")
                ?.jsonObject
                ?.get("playlistPanelRenderer")
                ?.jsonObject
                ?.get("contents")
                ?.jsonArray

            playlistContents?.forEach { item ->
                val panelItem = item.jsonObject
                val renderer = panelItem["playlistPanelVideoRenderer"]?.jsonObject ?: return@forEach

                // Extract videoId — required, skip if missing
                val videoId = renderer["videoId"]
                    ?.jsonPrimitive?.content
                    ?: renderer["playlistItemData"]
                        ?.jsonObject?.get("videoId")
                        ?.jsonPrimitive?.content
                    ?: return@forEach

                // Skip the currently playing track
                if (videoId == currentVideoId) return@forEach

                // Extract title — defensive fallback
                val title = renderer["title"]
                    ?.jsonObject?.get("runs")
                    ?.jsonArray?.getOrNull(0)
                    ?.jsonObject?.get("text")
                    ?.jsonPrimitive?.content
                    ?: renderer["shortBylineText"]
                    ?.jsonObject?.get("runs")
                    ?.jsonArray?.getOrNull(0)
                    ?.jsonObject?.get("text")
                    ?.jsonPrimitive?.content
                    ?: "Unknown Title"

                // Extract artist — defensive fallback
                val artist = renderer["shortBylineText"]
                    ?.jsonObject?.get("runs")
                    ?.jsonArray
                    ?.joinToString("") { run ->
                        run.jsonObject["text"]?.jsonPrimitive?.content ?: ""
                    }
                    ?: renderer["longBylineText"]
                    ?.jsonObject?.get("runs")
                    ?.jsonArray
                    ?.joinToString("") { run ->
                        run.jsonObject["text"]?.jsonPrimitive?.content ?: ""
                    }
                    ?: "Unknown Artist"

                // Extract thumbnail — defensive fallback
                val thumbnail = renderer["thumbnail"]
                    ?.jsonObject?.get("thumbnails")
                    ?.jsonArray
                    ?.lastOrNull()?.jsonObject
                    ?.get("url")?.jsonPrimitive?.content
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
        } catch (e: Exception) {
            android.util.Log.e("InnertubeClient", "Failed to parse Up Next results: ${e.message}", e)
        }

        return results
    }

    /**
     * Parses search results from YouTube Music response.
     *
     * @param responseBody The raw JSON response from the search endpoint.
     * @return A list of parsed search result items.
     */
    fun parseSearchResults(responseBody: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()

        try {
            val json = Json.parseToJsonElement(responseBody).jsonObject

            val contents = json["contents"]
                ?.jsonObject
                ?.get("tabbedSearchResultsRenderer")
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
                val sectionKey = sectionObj.keys.firstOrNull() ?: return@forEach

                val itemArray = when (sectionKey) {
                    "musicShelfRenderer" -> {
                        sectionObj["musicShelfRenderer"]?.jsonObject?.get("contents")?.jsonArray
                    }
                    "itemSectionRenderer" -> {
                        sectionObj["itemSectionRenderer"]?.jsonObject?.get("contents")?.jsonArray
                    }
                    else -> null
                }

                itemArray?.forEach { item ->
                    val itemObj = item.jsonObject
                    parseItemFromElementRenderer(itemObj)?.let { results.add(it) }
                        ?: parseItemFromMusicResponsiveListItem(itemObj)?.let { results.add(it) }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("InnertubeClient", "Failed to parse search results: ${e.message}", e)
        }

        return results
    }

    private fun parseItemFromElementRenderer(item: JsonObject): SearchResult? {
        val er = item["elementRenderer"]?.jsonObject ?: return null
        val data = er["newElement"]?.jsonObject
            ?.get("type")?.jsonObject
            ?.get("componentType")?.jsonObject
            ?.get("model")?.jsonObject
            ?.get("musicListItemWrapperModel")?.jsonObject
            ?.get("musicListItemData")?.jsonObject
            ?: return null

        val title = data["title"]?.jsonPrimitive?.content ?: return null
        val subtitle = data["subtitle"]?.jsonPrimitive?.content ?: ""

        val onTap = data["onTap"]?.jsonObject
            ?.get("innertubeCommand")?.jsonObject

        val watchEp = onTap?.get("watchEndpoint")?.jsonObject
        val videoId = watchEp?.get("videoId")?.jsonPrimitive?.content

        if (videoId == null) return null

        val thumbnailSources = data["thumbnail"]?.jsonObject
            ?.get("image")?.jsonObject
            ?.get("sources")?.jsonArray

        val thumbnailUrl = thumbnailSources
            ?.lastOrNull()?.jsonObject
            ?.get("url")?.jsonPrimitive?.content ?: ""

        return SearchResult(
            videoId = videoId,
            title = title,
            artist = subtitle.substringAfter(" • ").trim().ifEmpty { subtitle },
            thumbnailUrl = thumbnailUrl,
        )
    }

    private fun parseItemFromMusicResponsiveListItem(item: JsonObject): SearchResult? {
        val renderer = item["musicResponsiveListItemRenderer"]?.jsonObject ?: return null

        val videoId = renderer["navigationEndpoint"]
            ?.jsonObject
            ?.get("watchEndpoint")
            ?.jsonObject
            ?.get("videoId")
            ?.jsonPrimitive
            ?.content
            ?: renderer["playlistItemData"]
                ?.jsonObject
                ?.get("videoId")
                ?.jsonPrimitive
                ?.content
            ?: return null

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
            ?.joinToString("") { run ->
                run.jsonObject["text"]?.jsonPrimitive?.content ?: ""
            }
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

        return SearchResult(
            videoId = videoId,
            title = title,
            artist = artist,
            thumbnailUrl = thumbnail,
        )
    }
}

/**
 * Data class representing a search result from YouTube Music.
 *
 * @property videoId The YouTube video ID.
 * @property title The track title.
 * @property artist The artist name.
 * @property thumbnailUrl URL to the track thumbnail.
 */
data class SearchResult(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
)

data class ArtistPage(
    val name: String,
    val thumbnailUrl: String,
    val description: String?,
    val songs: List<SearchResult>,
    val albums: List<AlbumInfo>,
)

data class AlbumInfo(
    val browseId: String,
    val title: String,
    val thumbnailUrl: String,
    val artist: String,
)

data class AlbumPage(
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val year: String?,
    val tracks: List<SearchResult>,
)
