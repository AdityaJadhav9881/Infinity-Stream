package com.musicflow.app.data.local

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.room.RoomDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages local database backup and restore to/from the public
 * Music/MusicFlow/backups/ folder.
 *
 * Requires MANAGE_EXTERNAL_STORAGE permission (requested at startup).
 * Direct File API access — no MediaStore complexity.
 *
 * Backup safety: WAL checkpoint is performed via query() (not execSQL)
 * to ensure the checkpoint result is consumed and the WAL is fully
 * flushed into the main .db file before copying.
 */
@Singleton
class LocalBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "LocalBackupManager"
        private const val BACKUP_DIR = "MusicFlow/backups"
        private const val DB_NAME = AppDatabase.DATABASE_NAME
        private const val MIN_DB_SIZE = 1024L
    }

    private val backupDir: File
        get() = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            BACKUP_DIR
        ).also { it.mkdirs() }

    private val backupFile: File get() = File(backupDir, DB_NAME)

    private val dbFile: File get() = context.getDatabasePath(DB_NAME)

    /**
     * Backs up the Room database to public Music/MusicFlow/backups/.
     *
     * Steps:
     * 1. WAL checkpoint via query() — ensures all committed WAL frames
     *    are flushed into the main .db file.
     * 2. Raw FileInputStream→FileOutputStream copy (no JVM buffering).
     * 3. Verify backup file size matches source.
     *
     * @param database The open Room database instance (for checkpoint).
     * @return true if backup succeeded
     */
    fun backup(database: RoomDatabase? = null): Boolean {
        return try {
            if (!dbFile.exists()) {
                Log.w(TAG, "Database file does not exist, skipping backup")
                return false
            }

            // WAL checkpoint — flush all committed WAL frames into main .db
            if (database != null) {
                try {
                    val db = database.openHelper.writableDatabase
                    db.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
                        if (cursor.moveToFirst()) {
                            val busy = cursor.getInt(0)
                            val log = cursor.getInt(1)
                            val checkpointed = cursor.getInt(2)
                            Log.i(TAG, "WAL checkpoint: busy=$busy, log=$log, checkpointed=$checkpointed")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "WAL checkpoint failed: ${e.message}")
                }
            }

            // Skip if DB is suspiciously small (empty/corrupt)
            if (dbFile.length() < MIN_DB_SIZE) {
                Log.w(TAG, "DB too small (${dbFile.length()} bytes), skipping backup to avoid overwriting good backup")
                return false
            }

            // If a good backup already exists, verify current DB has data before overwriting
            if (backupFile.exists() && backupFile.length() >= MIN_DB_SIZE) {
                try {
                    val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                        dbFile.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                    )
                    val cursor = db.rawQuery("SELECT count(*) FROM tracks", null)
                    val currentCount = if (cursor.moveToFirst()) cursor.getInt(0) else 0
                    cursor.close()
                    db.close()
                    if (currentCount == 0) {
                        Log.w(TAG, "Current DB has 0 tracks — NOT overwriting existing backup")
                        return false
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "DB validation failed (old schema?): ${e.message}")
                }
            }

            backupDir.mkdirs()

            // Raw file copy — no JVM buffering, direct byte transfer
            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                    output.fd.sync()
                }
            }

            // Verify
            if (backupFile.length() != dbFile.length()) {
                Log.e(TAG, "Backup verification failed: source=${dbFile.length()}, backup=${backupFile.length()}")
                return false
            }

            Log.i(TAG, "Backup complete: ${dbFile.length()} bytes -> ${backupFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed: ${e.message}", e)
            false
        }
    }

    /**
     * Restores the Room database from public backup.
     * Validates backup before copying to avoid restoring corrupt/empty files.
     * @return true if restore succeeded
     */
    fun restore(): Boolean {
        return try {
            if (!backupFile.exists()) {
                Log.i(TAG, "No backup found, skipping restore")
                return false
            }

            // Validate backup is not too small (likely corrupt/empty)
            if (backupFile.length() < MIN_DB_SIZE) {
                Log.w(TAG, "Backup too small (${backupFile.length()} bytes), likely corrupt")
                return false
            }

            // Validate backup is a valid SQLite database
            try {
                val db = android.database.sqlite.SQLiteDatabase.openDatabase(
                    backupFile.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                )
                // Check tracks table exists and has data
                val cursor = db.rawQuery("SELECT count(*) FROM tracks", null)
                if (cursor.moveToFirst()) {
                    val count = cursor.getInt(0)
                    Log.i(TAG, "Backup validation: tracks=$count")
                    if (count == 0) {
                        Log.w(TAG, "Backup has 0 tracks — skipping restore to avoid overwriting with empty data")
                        cursor.close()
                        db.close()
                        return false
                    }
                }
                cursor.close()
                // Check favorites
                val favCursor = db.rawQuery("SELECT count(*) FROM favorites", null)
                if (favCursor.moveToFirst()) {
                    Log.i(TAG, "Backup validation: favorites=${favCursor.getInt(0)}")
                }
                favCursor.close()
                // Check playlists
                val plCursor = db.rawQuery("SELECT count(*) FROM playlists", null)
                if (plCursor.moveToFirst()) {
                    Log.i(TAG, "Backup validation: playlists=${plCursor.getInt(0)}")
                }
                plCursor.close()
                db.close()
            } catch (e: Exception) {
                Log.w(TAG, "Backup validation failed (old schema?): ${e.message}")
            }

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

            // Verify restored file size matches backup
            if (dbFile.length() != backupFile.length()) {
                Log.e(TAG, "Restore verification failed: expected=${backupFile.length()}, got=${dbFile.length()}")
                return false
            }

            Log.i(TAG, "Restore complete: ${dbFile.length()} bytes <- ${backupFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed: ${e.message}", e)
            false
        }
    }

    fun hasBackup(): Boolean = backupFile.exists()
}
