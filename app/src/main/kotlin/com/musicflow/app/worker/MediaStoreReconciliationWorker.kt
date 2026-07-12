package com.musicflow.app.worker

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.musicflow.app.data.local.dao.OfflineTrackDao
import com.musicflow.app.data.local.dao.TrackDao
import com.musicflow.app.data.local.entity.OfflineTrackEntity
import com.musicflow.app.data.local.entity.TrackEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

/**
 * Background worker that reconciles files in the public Music/MusicFlow/
 * directory with the Room database.
 *
 * On every app startup this worker runs once. It scans the MusicFlow directory
 * directly using [File] API (not MediaStore, since .nomedia blocks indexing).
 * For each audio file found that is not already present in the [TrackEntity] or
 * [OfflineTrackEntity] tables, it reconstructs metadata from the filename and
 * upserts it silently.
 */
@HiltWorker
class MediaStoreReconciliationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val trackDao: TrackDao,
    private val offlineTrackDao: OfflineTrackDao,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "MediaStoreReconcile"
        const val WORK_NAME = "media_store_reconciliation"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting file-based reconciliation scan")

        return try {
            val musicFlowDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                "MusicFlow"
            )

            if (!musicFlowDir.exists() || !musicFlowDir.isDirectory) {
                Log.i(TAG, "MusicFlow directory does not exist — nothing to reconcile")
                return Result.success()
            }

            val audioFiles = musicFlowDir.listFiles { file ->
                file.isFile && file.extension.equals("m4a", ignoreCase = true)
            } ?: emptyArray()

            var reconciled = 0

            for (file in audioFiles) {
                val displayName = file.name
                val songId = displayName.substringBeforeLast(".")
                    .ifBlank { "offline_${file.absolutePath.hashCode()}" }

                // Derive title from the safe filename (underscores/hyphens → spaces)
                val titleFromFilename = songId
                    .replace(Regex("[_-]"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .ifBlank { displayName }

                val fileSize = file.length()

                // Try to read metadata from .meta.json sidecar file
                var title = titleFromFilename
                var artist = ""
                var artworkUrl = ""
                try {
                    val metaFile = File(file.parent, file.nameWithoutExtension + ".meta.json")
                    if (metaFile.exists()) {
                        val json = metaFile.readText()
                        title = Regex("\"title\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: titleFromFilename
                        artist = Regex("\"artist\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: ""
                        artworkUrl = Regex("\"artworkUrl\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: ""
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to read metadata for $songId: ${e.message}")
                }

                // Check if already in TrackEntity
                val existsInTracks = trackDao.isTrackCached(songId)
                if (!existsInTracks) {
                    trackDao.upsertTrack(
                        TrackEntity(
                            songId = songId,
                            title = title,
                            artist = artist,
                            artworkUrl = artworkUrl,
                            durationMs = 0L,
                        )
                    )
                    Log.d(TAG, "Upserted track: $songId ($title)")
                }

                // Check if already in OfflineTrackEntity (by songId OR by file path)
                val existsOffline = offlineTrackDao.getBySongId(songId)
                val existsByPath = offlineTrackDao.getByFilePath(file.absolutePath)
                if (existsOffline == null && existsByPath == null) {
                    offlineTrackDao.insert(
                        OfflineTrackEntity(
                            songId = songId,
                            title = title,
                            artist = artist,
                            artworkUrl = artworkUrl,
                            localFilePath = file.absolutePath,
                            fileSize = fileSize,
                            downloadedAt = file.lastModified(),
                        )
                    )
                    reconciled++
                    Log.d(TAG, "Reconciled offline track: $songId ($title, artist=$artist, ${fileSize}B)")
                } else {
                    val existingId = existsOffline?.songId ?: existsByPath?.songId
                    Log.d(TAG, "Skipped duplicate: $songId (already exists as $existingId)")
                }
            }

            Log.i(TAG, "Reconciliation complete — scanned=${audioFiles.size}, reconciled=$reconciled")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Reconciliation failed: ${e.message}", e)
            Result.retry()
        }
    }
}
