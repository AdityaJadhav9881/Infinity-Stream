package com.musicflow.app.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.util.Log
import com.musicflow.app.data.TrackMetadata
import com.musicflow.app.data.local.dao.OfflineTrackDao
import com.musicflow.app.data.local.entity.OfflineTrackEntity
import com.musicflow.app.data.remote.AudioHeaderStore
import com.musicflow.app.data.repository.SearchRepository
import com.musicflow.app.utils.DownloadSettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val offlineTrackDao: OfflineTrackDao,
    private val searchRepository: SearchRepository,
    private val downloadSettingsManager: DownloadSettingsManager,
) {
    companion object {
        private const val TAG = "OfflineDownloadManager"
        const val MUSICFLOW_DIR = "MusicFlow"
    }

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                .build()
            chain.proceed(request)
        }
        .build()

    private val publicMusicDir: File
        get() = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            MUSICFLOW_DIR
        ).also {
            it.mkdirs()
            // .nomedia prevents gallery/media scanner from indexing artwork images
            val nomedia = File(it, ".nomedia")
            if (!nomedia.exists()) nomedia.createNewFile()
        }

    suspend fun downloadTrack(metadata: TrackMetadata, headers: Map<String, String> = emptyMap()): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Check wifi-only setting
            val wifiOnly = downloadSettingsManager.wifiOnly.first()
            if (wifiOnly && !isOnWifi()) {
                return@withContext Result.failure(Exception("Wi-Fi required. Disable Wi-Fi only in download settings to use mobile data."))
            }

            Log.i(TAG, "Downloading: ${metadata.title}")

            val safeName = metadata.title.replace(Regex("[^a-zA-Z0-9\\s\\-]"), "").trim()
                .ifBlank { metadata.songId }
            val fileName = "$safeName.m4a"
            val outputFile = File(publicMusicDir, fileName)

            val requestBuilder = Request.Builder().url(metadata.resolvedStreamingUrl)

            val mergedHeaders = mutableMapOf<String, String>()
            mergedHeaders.putAll(AudioHeaderStore.get(metadata.songId))
            mergedHeaders.putAll(headers)
            mergedHeaders.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            val request = requestBuilder.build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Download failed: HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Response body is null"))

            body.byteStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }

            val fileSize = outputFile.length()

            // Download album art alongside audio
            val artworkFile = File(publicMusicDir, "$safeName.jpg")
            try {
                if (metadata.artworkUrl.isNotBlank() && metadata.artworkUrl.startsWith("http")) {
                    val artRequest = Request.Builder().url(metadata.artworkUrl).build()
                    val artResponse = client.newCall(artRequest).execute()
                    if (artResponse.isSuccessful) {
                        artResponse.body?.byteStream()?.use { input ->
                            FileOutputStream(artworkFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        Log.i(TAG, "Artwork saved: ${artworkFile.absolutePath}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Artwork download failed (non-fatal): ${e.message}")
            }

            offlineTrackDao.insert(
                OfflineTrackEntity(
                    songId = metadata.songId,
                    title = metadata.title,
                    artist = metadata.artist,
                    artworkUrl = metadata.artworkUrl,
                    localFilePath = outputFile.absolutePath,
                    fileSize = fileSize,
                )
            )

            // Save metadata JSON so reconciliation can recover artist/artwork after reinstall
            try {
                val metaFile = File(publicMusicDir, "$safeName.meta.json")
                metaFile.writeText(
                    """{"songId":"${metadata.songId}","title":"${metadata.title.replace("\"", "\\\"")}","artist":"${metadata.artist.replace("\"", "\\\"")}","artworkUrl":"${metadata.artworkUrl.replace("\"", "\\\"")}"}"""
                )
            } catch (e: Exception) {
                Log.w(TAG, "Metadata save failed (non-fatal): ${e.message}")
            }

            Log.i(TAG, "Download complete: ${metadata.title} -> ${outputFile.absolutePath} (${fileSize} bytes)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteTrack(songId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = offlineTrackDao.getBySongId(songId)
            if (entity != null) {
                val entityFile = File(entity.localFilePath)
                if (entityFile.exists()) entityFile.delete()
                // Delete artwork file (same name, .jpg extension)
                val artworkFile = File(entityFile.parent, entityFile.nameWithoutExtension + ".jpg")
                if (artworkFile.exists()) artworkFile.delete()
                // Also try old format
                val legacyFile = File(publicMusicDir, "$songId.m4a")
                if (legacyFile.exists()) legacyFile.delete()
                offlineTrackDao.deleteBySongId(songId)
                Log.i(TAG, "Deleted track: $songId")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Delete failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun isTrackOffline(songId: String): Boolean {
        val entity = offlineTrackDao.getBySongId(songId) ?: return false
        // Check the actual file path stored in entity (matches download filename)
        val entityFile = File(entity.localFilePath)
        if (entityFile.exists()) return true
        // Fallback: check old format (songId.m4a) for backwards compatibility
        val legacyFile = File(publicMusicDir, "$songId.m4a")
        return legacyFile.exists()
    }

    fun getOfflineTracks(): Flow<List<OfflineTrackEntity>> = offlineTrackDao.observeAll()

    suspend fun getTotalStorageUsed(): Long = offlineTrackDao.getTotalSize()

    suspend fun clearAllOfflineTracks(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val tracks = offlineTrackDao.getAll()
            tracks.forEach { entity ->
                val entityFile = File(entity.localFilePath)
                if (entityFile.exists()) entityFile.delete()
                val artworkFile = File(entityFile.parent, entityFile.nameWithoutExtension + ".jpg")
                if (artworkFile.exists()) artworkFile.delete()
            }
            offlineTrackDao.deleteAll()
            Log.i(TAG, "Cleared all offline tracks")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Clear failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun isOnWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
