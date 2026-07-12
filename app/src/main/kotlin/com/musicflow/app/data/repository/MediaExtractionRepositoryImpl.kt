package com.musicflow.app.data.repository

import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MediaExtractionRepositoryImpl : MediaExtractionRepository {

    override suspend fun getAudioStreamUrl(videoId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val result = extractAudioWithHeaders(videoId)
                Result.success(result.url)
            } catch (e: Exception) {
                Log.e("MediaExtraction", "Failed: ${e.message}", e)
                Result.failure(e)
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
                Log.i("MediaExtraction", "Extracted ${headers.size} headers for $videoId: ${headers.keys}")
            } else {
                Log.w("MediaExtraction", "No http_headers in response for $videoId")
            }

            Log.i("MediaExtraction", "Audio stream resolved for $videoId")
            AudioExtractionResult(url = url, headers = headers)
        }
    }
}
