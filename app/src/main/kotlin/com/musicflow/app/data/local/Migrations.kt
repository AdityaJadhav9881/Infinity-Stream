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

    val ALL_MIGRATIONS = arrayOf(MIGRATION_7_8, MIGRATION_8_9)
}
