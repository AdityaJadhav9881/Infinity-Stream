package com.musicflow.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.musicflow.app.data.local.LocalBackupManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker that backs up the Room database to the public
 * Music/MusicFlow/backups/ folder.
 *
 * This ensures all user data (playlists, favorites, tracks, queue,
 * lyrics, search history, offline track records) survives uninstall
 * and reinstall cycles.
 */
@HiltWorker
class DatabaseBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val localBackupManager: LocalBackupManager,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "DatabaseBackupWorker"
        const val WORK_NAME = "database_backup"
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "Starting database backup")

        return try {
            val success = localBackupManager.backup()
            if (success) {
                Log.i(TAG, "Database backup complete")
                Result.success()
            } else {
                Log.w(TAG, "Database backup returned false")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Database backup failed: ${e.message}", e)
            Result.retry()
        }
    }
}
