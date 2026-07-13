package com.musicflow.app.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.util.Log
import com.musicflow.app.data.TrackMetadata
import com.musicflow.app.data.local.dao.DownloadQueueDao
import com.musicflow.app.data.local.dao.OfflineTrackDao
import com.musicflow.app.data.local.entity.DownloadQueueEntity
import com.musicflow.app.data.local.entity.OfflineTrackEntity
import com.musicflow.app.data.remote.AudioHeaderStore
import com.musicflow.app.utils.DownloadSettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Professional download manager with queue system, concurrent downloads,
 * pause/resume/cancel/retry support, and persistent state.
 */
@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadQueueDao: DownloadQueueDao,
    private val offlineTrackDao: OfflineTrackDao,
    private val downloadSettingsManager: DownloadSettingsManager,
) {
    companion object {
        private const val TAG = "DownloadManager"
        const val MUSICFLOW_DIR = "MusicFlow"
        private const val MAX_CONCURRENT_DOWNLOADS = 3
        private const val MAX_RETRIES = 3
        private const val SPEED_UPDATE_INTERVAL_MS = 1000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()
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

    private val _activeDownloads = MutableStateFlow(0)
    val activeDownloads: StateFlow<Int> = _activeDownloads.asStateFlow()

    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()

    private val publicMusicDir: File
        get() = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            MUSICFLOW_DIR
        ).also {
            it.mkdirs()
            val nomedia = File(it, ".nomedia")
            if (!nomedia.exists()) nomedia.createNewFile()
        }

    init {
        // Observe queue changes to update counts
        scope.launch {
            downloadQueueDao.observeActive().collect { active ->
                _queueSize.value = active.size
                _activeDownloads.value = active.count { it.status == "DOWNLOADING" }
            }
        }
        // Process any leftover queued downloads from app restart
        scope.launch {
            delay(5000) // Wait for app initialization
            processQueue()
        }
    }

    /**
     * Enqueues a track for download. Prevents duplicates.
     */
    suspend fun enqueue(
        songId: String,
        title: String,
        artist: String,
        artworkUrl: String,
        streamingUrl: String = "",
    ): Boolean {
        if (downloadQueueDao.isAlreadyQueued(songId)) {
            Log.w(TAG, "Track already queued: $songId")
            return false
        }

        // Check wifi-only setting
        val wifiOnly = downloadSettingsManager.wifiOnly.first()
        if (wifiOnly && !isOnWifi()) {
            Log.w(TAG, "Wifi required but not connected")
            return false
        }

        downloadQueueDao.upsert(
            DownloadQueueEntity(
                songId = songId,
                title = title,
                artist = artist,
                artworkUrl = artworkUrl,
                streamingUrl = streamingUrl,
                status = "QUEUED",
            )
        )

        Log.i(TAG, "Enqueued: $title ($songId)")
        processQueue()
        return true
    }

    /**
     * Downloads a track directly (for backward compatibility).
     */
    suspend fun downloadTrack(metadata: TrackMetadata, headers: Map<String, String> = emptyMap()): Result<Unit> {
        val enqueued = enqueue(
            songId = metadata.songId,
            title = metadata.title,
            artist = metadata.artist,
            artworkUrl = metadata.artworkUrl,
            streamingUrl = metadata.resolvedStreamingUrl,
        )
        if (!enqueued) return Result.failure(Exception("Already queued or wifi required"))

        // Wait for this specific download to complete
        downloadQueueDao.observeBySongId(metadata.songId).first { entity ->
            entity?.status in listOf("COMPLETED", "FAILED", "CANCELLED")
        }?.let { entity ->
            return if (entity.status == "COMPLETED") {
                Result.success(Unit)
            } else {
                Result.failure(Exception(entity.errorReason.ifBlank { "Download failed" }))
            }
        }
        return Result.failure(Exception("Download cancelled"))
    }

    /**
     * Processes the download queue, starting downloads up to the concurrency limit.
     */
    private fun processQueue() {
        scope.launch {
            val activeCount = downloadQueueDao.getActiveDownloadCount()
            if (activeCount >= MAX_CONCURRENT_DOWNLOADS) return@launch

            val slotsAvailable = MAX_CONCURRENT_DOWNLOADS - activeCount
            val nextDownloads = downloadQueueDao.getNextQueued(slotsAvailable)

            for (entity in nextDownloads) {
                startDownload(entity)
            }
        }
    }

    /**
     * Starts downloading a single track.
     */
    private fun startDownload(entity: DownloadQueueEntity) {
        val job = scope.launch {
            try {
                downloadQueueDao.updateProgress(entity.songId, "DOWNLOADING", 0f)
                Log.i(TAG, "Starting download: ${entity.title}")

                // Resolve streaming URL if not provided
                var streamingUrl = entity.streamingUrl
                if (streamingUrl.isBlank()) {
                    // Need to extract URL - but we need the SearchRepository for this
                    // For now, mark as failed if no URL
                    downloadQueueDao.markFailed(entity.songId, "No streaming URL available")
                    processQueue()
                    return@launch
                }

                val safeName = entity.title.replace(Regex("[^a-zA-Z0-9\\s\\-]"), "").trim()
                    .ifBlank { entity.songId }
                val fileName = "$safeName.m4a"
                val outputFile = File(publicMusicDir, fileName)

                val requestBuilder = Request.Builder().url(streamingUrl)
                val headers = AudioHeaderStore.get(entity.songId)
                headers.forEach { (key, value) ->
                    requestBuilder.addHeader(key, value)
                }

                val request = requestBuilder.build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val reason = "HTTP ${response.code}"
                    if (entity.retryCount < MAX_RETRIES) {
                        downloadQueueDao.retry(entity.songId)
                        Log.w(TAG, "Download failed ($reason), retrying: ${entity.title}")
                    } else {
                        downloadQueueDao.markFailed(entity.songId, reason)
                    }
                    processQueue()
                    return@launch
                }

                val body = response.body ?: run {
                    downloadQueueDao.markFailed(entity.songId, "Empty response body")
                    processQueue()
                    return@launch
                }

                val totalBytes = body.contentLength()
                var downloadedBytes = 0L
                var lastSpeedUpdate = System.currentTimeMillis()
                var lastBytesAtSpeedUpdate = 0L

                body.byteStream().use { input ->
                    FileOutputStream(outputFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            // Check if cancelled
                            val current = downloadQueueDao.getBySongId(entity.songId)
                            if (current?.status == "CANCELLED") {
                                outputFile.delete()
                                return@launch
                            }
                            // Check if paused
                            if (current?.status == "PAUSED") {
                                while (downloadQueueDao.getBySongId(entity.songId)?.status == "PAUSED") {
                                    delay(500)
                                }
                                // Check again if cancelled after unpause
                                val afterPause = downloadQueueDao.getBySongId(entity.songId)
                                if (afterPause?.status == "CANCELLED") {
                                    outputFile.delete()
                                    return@launch
                                }
                            }

                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            // Update progress
                            val progress = if (totalBytes > 0) {
                                (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            // Calculate speed every second
                            val now = System.currentTimeMillis()
                            val speed = if (now - lastSpeedUpdate >= SPEED_UPDATE_INTERVAL_MS) {
                                val bytesInInterval = downloadedBytes - lastBytesAtSpeedUpdate
                                val speedBps = bytesInInterval * 1000 / (now - lastSpeedUpdate)
                                lastSpeedUpdate = now
                                lastBytesAtSpeedUpdate = downloadedBytes
                                speedBps
                            } else 0L

                            downloadQueueDao.updateProgress(entity.songId, "DOWNLOADING", progress, speed)
                        }
                    }
                }

                // Download artwork
                val artworkFile = File(publicMusicDir, "$safeName.jpg")
                try {
                    if (entity.artworkUrl.isNotBlank() && entity.artworkUrl.startsWith("http")) {
                        val artRequest = Request.Builder().url(entity.artworkUrl).build()
                        val artResponse = client.newCall(artRequest).execute()
                        if (artResponse.isSuccessful) {
                            artResponse.body?.byteStream()?.use { input ->
                                FileOutputStream(artworkFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Artwork download failed (non-fatal): ${e.message}")
                }

                // Save to offline tracks
                val fileSize = outputFile.length()
                offlineTrackDao.insert(
                    OfflineTrackEntity(
                        songId = entity.songId,
                        title = entity.title,
                        artist = entity.artist,
                        artworkUrl = entity.artworkUrl,
                        localFilePath = outputFile.absolutePath,
                        fileSize = fileSize,
                    )
                )

                // Mark as completed in queue
                downloadQueueDao.markCompleted(entity.songId, System.currentTimeMillis(), outputFile.absolutePath)
                Log.i(TAG, "Download complete: ${entity.title} (${fileSize} bytes)")

            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${entity.title} - ${e.message}", e)
                if (entity.retryCount < MAX_RETRIES) {
                    downloadQueueDao.retry(entity.songId)
                    Log.w(TAG, "Retrying: ${entity.title}")
                } else {
                    downloadQueueDao.markFailed(entity.songId, e.message ?: "Unknown error")
                }
            } finally {
                activeJobs.remove(entity.songId)
                processQueue()
            }
        }
        activeJobs[entity.songId] = job
    }

    /**
     * Pauses a specific download.
     */
    fun pause(songId: String) {
        scope.launch {
            downloadQueueDao.updateProgress(songId, "PAUSED", 0f)
            activeJobs[songId]?.cancel()
            activeJobs.remove(songId)
            Log.i(TAG, "Paused: $songId")
        }
    }

    /**
     * Resumes a paused download.
     */
    fun resume(songId: String) {
        scope.launch {
            downloadQueueDao.updateProgress(songId, "QUEUED", 0f)
            processQueue()
            Log.i(TAG, "Resumed: $songId")
        }
    }

    /**
     * Cancels a download.
     */
    fun cancel(songId: String) {
        scope.launch {
            downloadQueueDao.cancel(songId)
            activeJobs[songId]?.cancel()
            activeJobs.remove(songId)
            Log.i(TAG, "Cancelled: $songId")
        }
    }

    /**
     * Retries a failed download.
     */
    fun retry(songId: String) {
        scope.launch {
            downloadQueueDao.retry(songId)
            processQueue()
            Log.i(TAG, "Retrying: $songId")
        }
    }

    /**
     * Checks if a track is already downloaded.
     */
    suspend fun isTrackOffline(songId: String): Boolean {
        val entity = offlineTrackDao.getBySongId(songId) ?: return false
        return File(entity.localFilePath).exists()
    }

    private fun isOnWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
