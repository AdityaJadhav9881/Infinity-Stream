package com.musicflow.app.di

import android.content.Context
import androidx.room.Room
import com.musicflow.app.data.local.AppDatabase
import com.musicflow.app.data.local.dao.FavoriteDao
import com.musicflow.app.data.local.dao.LyricsDao
import com.musicflow.app.data.local.dao.OfflineTrackDao
import com.musicflow.app.data.local.dao.PlaylistDao
import com.musicflow.app.data.local.dao.QueueDao
import com.musicflow.app.data.local.dao.SearchHistoryDao
import com.musicflow.app.data.local.dao.TrackDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Room database and all DAOs as singletons.
 *
 * Uses destructive fallback for development — during production
 * this should be replaced with proper migrations.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase {
        return AppDatabase.builder(context)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideTrackDao(database: AppDatabase): TrackDao {
        return database.trackDao()
    }

    @Provides
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    fun provideSearchHistoryDao(database: AppDatabase): SearchHistoryDao {
        return database.searchHistoryDao()
    }

    @Provides
    fun providePlaylistDao(database: AppDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Provides
    fun provideLyricsDao(database: AppDatabase): LyricsDao {
        return database.lyricsDao()
    }

    @Provides
    fun provideQueueDao(database: AppDatabase): QueueDao {
        return database.queueDao()
    }

    @Provides
    fun provideOfflineTrackDao(database: AppDatabase): OfflineTrackDao {
        return database.offlineTrackDao()
    }
}
