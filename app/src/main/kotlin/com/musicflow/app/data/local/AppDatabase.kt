package com.musicflow.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.musicflow.app.data.local.dao.FavoriteDao
import com.musicflow.app.data.local.dao.LyricsDao
import com.musicflow.app.data.local.dao.OfflineTrackDao
import com.musicflow.app.data.local.dao.PlaylistDao
import com.musicflow.app.data.local.dao.QueueDao
import com.musicflow.app.data.local.dao.SearchHistoryDao
import com.musicflow.app.data.local.dao.TrackDao
import com.musicflow.app.data.local.dao.DownloadQueueDao
import com.musicflow.app.data.local.entity.DownloadQueueEntity
import com.musicflow.app.data.local.entity.FavoriteEntity
import com.musicflow.app.data.local.entity.LyricsEntity
import com.musicflow.app.data.local.entity.OfflineTrackEntity
import com.musicflow.app.data.local.entity.PlaylistEntity
import com.musicflow.app.data.local.entity.PlaylistTrackMap
import com.musicflow.app.data.local.entity.QueueEntity
import com.musicflow.app.data.local.entity.SearchHistoryEntity
import com.musicflow.app.data.local.entity.TrackEntity

/**
 * Room database for the MusicFlow application.
 *
 * This database stores cached track metadata using only version-safe,
 * primitive column types. No `ByteArray`, no `Parcel` serialization.
 *
 * ## Schema Management
 * - Version 1: Initial schema (tracks table).
 * - Version 2: Added favorites and search_history tables.
 * - Future versions: Add columns via `ALTER TABLE` migrations, or
 *   bump the version for destructive fallback during development.
 *
 * ## Thread Safety
 * - Room enforces that all database operations run off the main thread.
 * - Write transactions are serialized internally by Room's executor.
 * - Read queries are concurrent and non-blocking.
 *
 * ## Usage (Hilt injection)
 * ```kotlin
 * @Provides @Singleton
 * fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
 *     return Room.databaseBuilder(context, AppDatabase::class.java, "musicflow.db")
 *         .build()
 * }
 *
 * @Provides
 * fun provideTrackDao(db: AppDatabase): TrackDao = db.trackDao()
 * ```
 */
@Database(
    entities = [
        TrackEntity::class,
        FavoriteEntity::class,
        SearchHistoryEntity::class,
        PlaylistEntity::class,
        PlaylistTrackMap::class,
        LyricsEntity::class,
        QueueEntity::class,
        OfflineTrackEntity::class,
        DownloadQueueEntity::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao

    abstract fun favoriteDao(): FavoriteDao

    abstract fun searchHistoryDao(): SearchHistoryDao

    abstract fun playlistDao(): PlaylistDao

    abstract fun lyricsDao(): LyricsDao

    abstract fun queueDao(): QueueDao

    abstract fun offlineTrackDao(): OfflineTrackDao

    abstract fun downloadQueueDao(): DownloadQueueDao

    companion object {

        /** Database file name on disk. */
        const val DATABASE_NAME = "musicflow.db"

        /**
         * Creates a [RoomDatabase.Builder] configured for this database.
         *
         * Callers should apply their own lifecycle management
         * (e.g. Hilt's `@Singleton` scope) to avoid multiple instances.
         *
         * @param context Application context for file access.
         * @return A pre-configured database builder.
         */
        fun builder(context: Context): Builder<AppDatabase> {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME,
            )
        }
    }
}
