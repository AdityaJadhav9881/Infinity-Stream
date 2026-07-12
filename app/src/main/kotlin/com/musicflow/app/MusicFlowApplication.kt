package com.musicflow.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import android.os.Environment
import com.musicflow.app.data.local.AppDatabase
import com.musicflow.app.data.local.LocalBackupManager
import com.musicflow.app.worker.MediaStoreReconciliationWorker
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

@HiltAndroidApp
class MusicFlowApplication : Application(), Configuration.Provider {

    companion object {
        private const val TAG_INIT = "YTDL_INIT"
        private const val TAG_UPDATE = "YTDL_UPDATE"
        private const val TAG_VERSION = "YTDL_VERSION"
        private const val TAG_RESTORE = "MusicFlowApp"

        /** Exposes engine version to UI layer via StateFlow. */
        private val _engineVersion = MutableStateFlow("Loading...")
        val engineVersion: StateFlow<String> = _engineVersion.asStateFlow()

        private val _updateStatus = MutableStateFlow("Idle")
        val updateStatus: StateFlow<String> = _updateStatus.asStateFlow()
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var localBackupManager: LocalBackupManager

    @Inject
    lateinit var database: AppDatabase

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastResumeTime = 0L

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        // Restore database backup BEFORE super.onCreate() so Room picks it up.
        // Hilt initializes in super.onCreate(), and Room opens the DB lazily
        // on first query. By restoring here, the backup file is in place
        // before any component touches the database.
        restoreDatabaseBeforeRoomInit()

        super.onCreate()

        initializeYoutubeDL()
        enqueueMediaStoreReconciliation()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {
                lastResumeTime = System.currentTimeMillis()
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                val elapsed = System.currentTimeMillis() - lastResumeTime
                if (elapsed < 1000) return // In-app navigation, not backgrounding
                applicationScope.launch {
                    try {
                        localBackupManager.backup(database)
                        Log.i("MusicFlowApp", "Backup on background succeeded")
                    } catch (e: Exception) {
                        Log.e("MusicFlowApp", "Backup on background failed: ${e.message}")
                    }
                }
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun enqueueMediaStoreReconciliation() {
        val request = OneTimeWorkRequestBuilder<MediaStoreReconciliationWorker>()
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            MediaStoreReconciliationWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
        Log.i("MusicFlowApp", "Enqueued MediaStore reconciliation worker")
    }

    /**
     * Restores the Room database from public backup BEFORE Room initializes.
     * This ensures the backup file is in place before any Hilt-injected
     * component or WorkManager worker touches the database.
     *
     * Uses SharedPreferences to track restore state so the Activity
     * can show the Keep/Start Fresh dialog without duplicating the restore.
     */
    private fun restoreDatabaseBeforeRoomInit() {
        val prefs = getSharedPreferences("musicflow_prefs", MODE_PRIVATE)
        val isFirstLaunch = !prefs.getBoolean("has_launched_before", false)
        if (!isFirstLaunch) return

        val backupDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "MusicFlow/backups"
        )
        val backupFile = File(backupDir, "musicflow.db")
        val dbFile = getDatabasePath("musicflow.db")

        if (!backupFile.exists()) {
            Log.i(TAG_RESTORE, "No backup found, skipping restore")
            return
        }

        if (backupFile.length() < 1024L) {
            Log.w(TAG_RESTORE, "Backup too small (${backupFile.length()} bytes), likely corrupt — skipping")
            return
        }

        try {
            dbFile.parentFile?.mkdirs()

            // Delete existing DB files to ensure clean restore
            if (dbFile.exists()) dbFile.delete()
            File(dbFile.path + "-wal").let { if (it.exists()) it.delete() }
            File(dbFile.path + "-shm").let { if (it.exists()) it.delete() }

            FileInputStream(backupFile).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                    output.fd.sync()
                }
            }

            // Verify restored file
            if (dbFile.length() != backupFile.length()) {
                Log.e(TAG_RESTORE, "Restore verification failed: expected=${backupFile.length()}, got=${dbFile.length()}")
                return
            }

            prefs.edit().putBoolean("pending_restore_dialog", true).apply()
            Log.i(TAG_RESTORE, "Restore complete: ${dbFile.length()} bytes from ${backupFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG_RESTORE, "Restore failed: ${e.message}", e)
        }
    }

    private fun initializeYoutubeDL() {
        applicationScope.launch {
            // ── Phase 1: Init ──────────────────────────────────────
            try {
                Log.i(TAG_INIT, "Initializing YoutubeDL engine...")
                YoutubeDL.getInstance().init(this@MusicFlowApplication)
                Log.i(TAG_INIT, "YoutubeDL engine initialized successfully")
            } catch (e: YoutubeDLException) {
                Log.e(TAG_INIT, "YoutubeDLException during init: ${e.message}", e)
                _updateStatus.value = "Init failed: ${e.message}"
                return@launch
            } catch (e: Exception) {
                Log.e(TAG_INIT, "Unexpected exception during init: ${e.message}", e)
                _updateStatus.value = "Init failed: ${e.message}"
                return@launch
            }

            // ── Phase 2: Update to nightly ────────────────────────
            _updateStatus.value = "Updating to nightly..."
            try {
                Log.i(TAG_UPDATE, "Attempting update via NIGHTLY channel...")
                val nightlyResult = YoutubeDL.getInstance().updateYoutubeDL(
                    this@MusicFlowApplication,
                    YoutubeDL.UpdateChannel.NIGHTLY
                )
                Log.i(TAG_UPDATE, "Nightly update result: $nightlyResult")
                _updateStatus.value = "Updated (nightly): $nightlyResult"
            } catch (e: YoutubeDLException) {
                Log.e(TAG_UPDATE, "YoutubeDLException during nightly update: ${e.message}", e)
                Log.i(TAG_UPDATE, "Falling back to STABLE channel...")

                // ── Phase 2b: Fallback to stable ──────────────────
                try {
                    val stableResult = YoutubeDL.getInstance().updateYoutubeDL(
                        this@MusicFlowApplication,
                        YoutubeDL.UpdateChannel.STABLE
                    )
                    Log.i(TAG_UPDATE, "Stable update result: $stableResult")
                    _updateStatus.value = "Updated (stable): $stableResult"
                } catch (e2: YoutubeDLException) {
                    Log.e(TAG_UPDATE, "YoutubeDLException during stable update: ${e2.message}", e2)
                    _updateStatus.value = "Update failed: ${e.message}"
                } catch (e2: Exception) {
                    Log.e(TAG_UPDATE, "Unexpected exception during stable update: ${e2.message}", e2)
                    _updateStatus.value = "Update failed: ${e.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG_UPDATE, "Unexpected exception during nightly update: ${e.message}", e)
                _updateStatus.value = "Update failed: ${e.message}"
            }

            // ── Phase 3: Read version ─────────────────────────────
            try {
                val version = YoutubeDL.getInstance().version(this@MusicFlowApplication) ?: "unknown"
                val versionName = YoutubeDL.getInstance().versionName(this@MusicFlowApplication) ?: "unknown"
                Log.i(TAG_VERSION, "Current version: $version ($versionName)")
                _engineVersion.value = version
            } catch (e: Exception) {
                Log.e(TAG_VERSION, "Failed to read version: ${e.message}", e)
                _engineVersion.value = "Unknown"
            }
        }
    }
}
