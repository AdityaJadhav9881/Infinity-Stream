package com.musicflow.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Int = 0, val bytesDownloaded: Long = 0, val totalBytes: Long = 0) : DownloadState()
    data class Downloaded(val apkUri: Uri) : DownloadState()
    data object Installing : DownloadState()
    data class Error(val message: String) : DownloadState()
}

@Singleton
class ApkInstaller @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ApkInstaller"
        private const val APK_DIR = "MusicFlow Updates"
        private const val APK_FILENAME = "musicflow-update.apk"
    }

    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    @Volatile private var isDownloading = false

    private fun buildGoogleDriveDownloadUrl(url: String): String {
        if (!url.contains("drive.google.com")) return url
        val fileId = when {
            url.contains("/d/") -> url.substringAfter("/d/").substringBefore("/")
            url.contains("id=") -> url.substringAfter("id=").substringBefore("&")
            else -> return url
        }
        return "https://drive.google.com/uc?export=download&id=$fileId&confirm=t"
    }

    fun downloadApk(url: String): Flow<DownloadState> = callbackFlow {
        isDownloading = true
        try {
            val directUrl = buildGoogleDriveDownloadUrl(url)
            Log.i(TAG, "Starting download from: $directUrl")

            val downloadDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                APK_DIR
            )
            if (!downloadDir.exists()) downloadDir.mkdirs()

            val apkFile = File(downloadDir, APK_FILENAME)
            if (apkFile.exists()) apkFile.delete()

            trySend(DownloadState.Downloading(0, 0, 0))

            val request = Request.Builder()
                .url(directUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
                .build()

            val response = httpClient.newCall(request).execute()

            response.use { resp ->
                if (!resp.isSuccessful) {
                    Log.e(TAG, "Download failed: HTTP ${resp.code}")
                    trySend(DownloadState.Error("Download failed: HTTP ${resp.code}"))
                    return@callbackFlow
                }

                val body = resp.body ?: run {
                    trySend(DownloadState.Error("Empty response body"))
                    return@callbackFlow
                }

                val totalBytes = body.contentLength()
                Log.i(TAG, "Download starting: totalBytes=$totalBytes")

                var bytesDownloaded = 0L
                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (!isDownloading) {
                                apkFile.delete()
                                trySend(DownloadState.Error("Download cancelled"))
                                return@callbackFlow
                            }
                            output.write(buffer, 0, bytesRead)
                            bytesDownloaded += bytesRead

                            val progress = if (totalBytes > 0) {
                                ((bytesDownloaded * 100) / totalBytes).toInt()
                            } else 0

                            trySend(DownloadState.Downloading(progress, bytesDownloaded, totalBytes))
                        }
                    }
                }

                // Verify the downloaded file is actually an APK (not HTML error page)
                if (apkFile.length() < 1024) {
                    val firstBytes = apkFile.readText()
                    if (firstBytes.contains("<!DOCTYPE") || firstBytes.contains("<html")) {
                        apkFile.delete()
                        Log.e(TAG, "Downloaded file is HTML, not APK")
                        trySend(DownloadState.Error("Download failed: got HTML page instead of APK. The file may be too large for Google Drive's virus scan."))
                        return@callbackFlow
                    }
                }

                Log.i(TAG, "Download complete: ${apkFile.absolutePath} (${apkFile.length()} bytes)")
                val fileUri = Uri.fromFile(apkFile)
                trySend(DownloadState.Downloaded(fileUri))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}", e)
            trySend(DownloadState.Error("Download failed: ${e.message}"))
        } finally {
            isDownloading = false
            close()
        }
    }

    suspend fun installApk(apkUri: Uri): Boolean = suspendCancellableCoroutine { cont ->
        try {
            val file = File(apkUri.path ?: return@suspendCancellableCoroutine cont.resume(false))
            if (!file.exists()) {
                Log.e(TAG, "APK file does not exist: ${file.absolutePath}")
                cont.resume(false)
                return@suspendCancellableCoroutine
            }
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            Log.i(TAG, "Launching package installer for: $contentUri")
            context.startActivity(intent)
            cont.resume(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install APK: ${e.message}", e)
            cont.resume(false)
        }
    }

    fun cancelDownload() {
        isDownloading = false
        Log.i(TAG, "Download cancelled")
    }

    fun getLatestDownloadedApkFile(): File {
        val downloadDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            APK_DIR
        )
        return File(downloadDir, APK_FILENAME)
    }

    fun restartApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
