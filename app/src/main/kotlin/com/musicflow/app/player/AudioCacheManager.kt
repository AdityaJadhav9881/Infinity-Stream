package com.musicflow.app.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages ExoPlayer's disk cache for audio streaming.
 *
 * This class is responsible for:
 * - Initializing [SimpleCache] with a size-bounded LRU eviction policy.
 * - Providing a [CacheDataSource.Factory] that transparently caches
 *   audio chunks during streaming.
 *
 * ## Thread Safety
 * - [SimpleCache] initialization is synchronized and only performed once.
 * - The cache directory is created lazily on first access.
 * - No `runBlocking` is used — all operations are non-blocking.
 *
 * ## Cache Behavior
 * - Audio chunks are cached on first access (cache-miss → fetch → cache).
 * - Subsequent requests for the same byte range are served from disk.
 * - When the cache exceeds [MAX_CACHE_BYTES], the LRU evictor removes
 *   the least recently accessed chunks first.
 *
 * ## Hilt Integration
 * This class is provided as a `@Singleton` via [CacheModule].
 * Inject it wherever a [CacheDataSource.Factory] is needed.
 */
@Singleton
class AudioCacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        /** Cache directory name under `context.cacheDir`. */
        private const val CACHE_DIR_NAME = "audio_chunks"

        /** Maximum cache size: 512 MB. */
        private const val MAX_CACHE_BYTES = 512L * 1024 * 1024

        /** Connect timeout for upstream HTTP fetches (ms). */
        private const val CONNECT_TIMEOUT_MS = 16_000

        /** Read timeout for upstream HTTP fetches (ms). */
        private const val READ_TIMEOUT_MS = 8_000

        /** User-Agent for upstream audio requests. */
        private const val USER_AGENT =
            "com.google.android.apps.youtube.music/"
    }

    // ── Lazy-initialized cache ─────────────────────────────────────────

    private val simpleCache: SimpleCache by lazy { createCache() }

    /**
     * Returns a [CacheDataSource.Factory] that wraps the upstream
     * [DefaultHttpDataSource.Factory] with disk caching.
     *
     * Usage in ExoPlayer builder:
     * ```kotlin
     * val cacheManager = AudioCacheManager(context)
     * val mediaSourceFactory = DefaultMediaSourceFactory(
     *     cacheManager.getCacheDataSourceFactory()
     * )
     * ```
     *
     * Flow:
     * 1. ExoPlayer requests a byte range from the factory.
     * 2. [CacheDataSource] checks if the range is in [simpleCache].
     * 3. If cached → serve from disk (zero network usage).
     * 4. If not cached → fetch from upstream, write to cache, return.
     */
    @OptIn(UnstableApi::class)
    fun getCacheDataSourceFactory(): DataSource.Factory {
        val upstreamFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(READ_TIMEOUT_MS)
            .setUserAgent(USER_AGENT)

        return CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Returns the underlying [SimpleCache] instance.
     *
     * Useful for cache statistics, manual eviction, or passing to
     * other components that need direct cache access.
     */
    fun getCache(): SimpleCache = simpleCache

    /**
     * Releases the cache resources.
     *
     * Should be called when the cache is no longer needed (e.g. app
     * termination). After calling this, the cache cannot be reused.
     */
    fun release() {
        simpleCache.release()
    }

    // ── Private ────────────────────────────────────────────────────────

    /**
     * Creates the [SimpleCache] instance with LRU eviction.
     *
     * The cache directory is created lazily on first access. The
     * [StandaloneDatabaseProvider] is used for Room-based index
     * management (required by SimpleCache).
     *
     * This method is called at most once due to `by lazy`.
     */
    @OptIn(UnstableApi::class)
    private fun createCache(): SimpleCache {
        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES)

        return SimpleCache(
            cacheDir,
            evictor,
            StandaloneDatabaseProvider(context),
        )
    }
}
