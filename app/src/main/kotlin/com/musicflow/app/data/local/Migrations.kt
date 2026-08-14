package com.musicflow.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {

    /**
     * Migration from version 7 to 8.
     * Adds proper @ColumnInfo annotations and enables schema export.
     * Since v7 already had all tables, this migration is a no-op
     * but ensures the version bump is handled safely.
     */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // No schema changes needed — version bump only to establish
            // migration infrastructure. Future migrations will build on this.
        }
    }

    /**
     * Migration from version 8 to 9.
     * Adds queue state columns to the queue table for playback restoration:
     * - currentItemIndex: which track in the queue was playing
     * - playbackPositionMs: position within that track
     * - isShuffleOn: shuffle mode state
     * - repeatMode: repeat mode state
     */
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE queue ADD COLUMN currentItemIndex INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE queue ADD COLUMN playbackPositionMs INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE queue ADD COLUMN isShuffleOn INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE queue ADD COLUMN repeatMode INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * Migration from version 9 to 10.
     *
     * Music Memory stores immutable listening events separately from the
     * mutable track cache, preserving a timeline even when metadata changes.
     */
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `listening_events` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `track_id` TEXT NOT NULL,
                    `event_type` TEXT NOT NULL,
                    `occurred_at` INTEGER NOT NULL,
                    `position_ms` INTEGER NOT NULL,
                    `duration_ms` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `artist` TEXT NOT NULL,
                    `artwork_url` TEXT NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_listening_events_track_id` ON `listening_events` (`track_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_listening_events_occurred_at` ON `listening_events` (`occurred_at`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_listening_events_track_id_occurred_at` ON `listening_events` (`track_id`, `occurred_at`)")
        }
    }

    /** Adds measured active listening time to each immutable memory event. */
    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `listening_events` ADD COLUMN `listened_ms` INTEGER NOT NULL DEFAULT 0",
            )
        }
    }

    val ALL_MIGRATIONS = arrayOf(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
}
