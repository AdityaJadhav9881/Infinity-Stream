package com.musicflow.app.di

import android.content.Context
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/**
 * Hilt module that provides the ExoPlayer disk cache as a singleton.
 *
 * This cache is shared across the entire app lifecycle and survives
 * service restarts. It is intentionally NOT provided inside the
 * MusicPlaybackService to keep the service testable and decoupled
 * from DI.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    private const val CACHE_DIR_NAME = "exoplayer"
    private const val DEFAULT_MAX_BYTES = 2L * 1024 * 1024 * 1024 // 2 GB

    @Provides
    @Singleton
    fun provideSimpleCache(
        @ApplicationContext context: Context,
    ): SimpleCache {
        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val evictor = androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor(
            DEFAULT_MAX_BYTES,
        )

        return SimpleCache(
            cacheDir,
            evictor,
            StandaloneDatabaseProvider(context),
        )
    }
}
