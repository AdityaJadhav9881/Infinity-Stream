package com.musicflow.app.player

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.musicflow.app.data.remote.AudioHeaderStore
import com.musicflow.app.utils.EqualizerManager
import com.musicflow.app.utils.PlayerSettingsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Modern Media3 [MediaSessionService] that manages background music playback.
 *
 * Key differences from the ViMusic approach:
 * - Uses [MediaSessionService] instead of a raw [Service] + legacy MediaSession.
 * - No `runBlocking` — all operations are async or callback-driven.
 * - No binary Parcel serialization for queue persistence.
 * - Audio focus is managed declaratively via [AudioAttributes].
 * - Foreground lifecycle is handled by the Media3 framework itself.
 *
 * ## Architecture
 *
 * ```
 * MusicPlaybackService
 *   ├── ExoPlayer (custom config)
 *   │     ├── DefaultMediaSourceFactory
 *   │     │     └── CacheDataSource → SimpleCache (disk)
 *   │     │           └── OkHttpDataSource (upstream with yt-dlp headers)
 *   │     └── DefaultAudioSink (silence skipping, no offload)
 *   └── MediaSession (Media3 native — handles notifications, BT, lockscreen)
 * ```
 */
@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    companion object {
        private const val TAG = "MusicPlaybackService"

        /** Connect timeout for upstream HTTP fetches. */
        private const val CONNECT_TIMEOUT_MS = 16_000

        /** Read timeout for upstream HTTP fetches. */
        private const val READ_TIMEOUT_MS = 8_000

        /** Fallback User-Agent if yt-dlp headers not available. */
        private const val FALLBACK_USER_AGENT =
            "com.google.android.apps.youtube.music/7.27.52 (Linux; U; Android 14)"

        @Volatile var currentSongId: String = ""
    }

    // ── ExoPlayer & Session ──────────────────────────────────────────────

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    // ── Disk Cache (injected via Hilt singleton) ─────────────────────────

    @Inject lateinit var simpleCache: SimpleCache

    // ── Audio Effects (injected via Hilt singleton) ──────────────────────

    @Inject lateinit var equalizerManager: EqualizerManager

    // ── Player Settings (injected via Hilt singleton) ────────────────────

    @Inject lateinit var playerSettingsManager: PlayerSettingsManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ── Lifecycle ─────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()

        val dataSourceFactory = buildDataSourceFactory(simpleCache)
        val extractorsFactory = buildExtractorsFactory()

        val audioSink = buildAudioSink()
        val renderersFactory = buildRenderersFactory(audioSink)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)

        val player = ExoPlayer.Builder(this, renderersFactory, mediaSourceFactory)
            .setAudioAttributes(
                buildAudioAttributes(),
                /* handleAudioFocus= */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setSkipSilenceEnabled(false)
            .setSeekBackIncrementMs(10_000)
            .setSeekForwardIncrementMs(10_000)
            .build()

        player.addListener(PlayerEventListener())

        exoPlayer = player

        val session = MediaSession.Builder(this, player)
            .apply {
                val launchIntent = packageManager
                    .getLaunchIntentForPackage(packageName)
                    ?.let { PendingIntent.getActivity(
                        this@MusicPlaybackService,
                        0,
                        it,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ) }
                if (launchIntent != null) {
                    setSessionActivity(launchIntent)
                }
            }
            .build()

        mediaSession = session

        // Initialize audio effects — but only after AudioTrack is active.
        // DefaultAudioSink creates AudioTrack lazily, so session may be invalid here.
        // We also try in onIsPlayingChanged as a fallback.

        // Re-apply saved player settings
        serviceScope.launch {
            playerSettingsManager.skipSilence
                .distinctUntilChanged()
                .collect { enabled ->
                    exoPlayer?.let { p ->
                        p.setSkipSilenceEnabled(enabled)
                        Log.d(TAG, "Skip silence set to $enabled")
                    }
                }
        }

        serviceScope.launch {
            playerSettingsManager.volumeNormalization
                .distinctUntilChanged()
                .collect { enabled ->
                    equalizerManager.setVolumeNormalization(enabled)
                    Log.d(TAG, "Volume normalization set to $enabled")
                }
        }

        // Re-apply saved equalizer preset whenever it changes
        serviceScope.launch {
            playerSettingsManager.equalizerPreset
                .collect { presetName ->
                    val preset = try {
                        com.musicflow.app.utils.EqualizerPreset.valueOf(presetName)
                    } catch (_: Exception) {
                        com.musicflow.app.utils.EqualizerPreset.NORMAL
                    }
                    equalizerManager.applyPreset(preset)
                    Log.d(TAG, "Equalizer preset applied: ${preset.label}")
                }
        }

        Log.i(TAG, "Service created — ExoPlayer + MediaSession initialized")
    }

    /**
     * Called by the framework when a new [MediaSession] client binds (e.g. the
     * UI activity). Returning a non-null session makes this service a
     * "started" service that won't be destroyed until all clients unbind AND
     * playback stops.
     */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    /**
     * When the system needs to stop the service (e.g. user swipes it away from
     * recent tasks), we release all resources cleanly.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = exoPlayer ?: return

        // If playback is still active, don't kill the service — let the user
        // explicitly stop via notification or UI.
        if (player.playbackState != Player.STATE_IDLE && player.playWhenReady) {
            Log.d(TAG, "Task removed but playback active — keeping service alive")
            return
        }

        Log.i(TAG, "Task removed, no active playback — releasing")
        releaseResources()
        stopSelf()
    }

    override fun onDestroy() {
        releaseResources()
        super.onDestroy()
    }

    // ── Audio Attributes ──────────────────────────────────────────────────

    /**
     * Builds the [AudioAttributes] that declare this app as a music player.
     *
     * When `handleAudioFocus = true` is passed to ExoPlayer, the player will
     * automatically:
     *   - Request audio focus on play
     *   - Pause / duck on transient focus loss (e.g. navigation prompt)
     *   - Resume after transient loss ends
     *   - Stop on permanent focus loss (e.g. phone call)
     */
    private fun buildAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
    }

    // ── Audio Sink ────────────────────────────────────────────────────────

    /**
     * Custom [DefaultAudioSink] with silence-skipping support.
     *
     * Offload mode is intentionally disabled: software decoding gives us
     * full control over audio processing and avoids hardware-specific quirks.
     */
    private fun buildAudioSink(): AudioSink {
        return DefaultAudioSink.Builder()
            .setEnableFloatOutput(false)
            .setAudioProcessorChain(
                DefaultAudioSink.DefaultAudioProcessorChain(
                    SilenceSkippingAudioProcessor(),
                ),
            )
            .build()
    }

    // ── Renderers Factory ─────────────────────────────────────────────────

    /**
     * Renders factory that produces **audio-only** renderers.
     * No video renderer is instantiated — this is a music app.
     */
    private fun buildRenderersFactory(audioSink: AudioSink): androidx.media3.exoplayer.RenderersFactory {
        return androidx.media3.exoplayer.RenderersFactory { handler, _, audioListener, _, _ ->
            arrayOf(
                androidx.media3.exoplayer.audio.MediaCodecAudioRenderer(
                    this,
                    androidx.media3.exoplayer.mediacodec.MediaCodecSelector.DEFAULT,
                    handler,
                    audioListener,
                    audioSink,
                ),
            )
        }
    }

    // ── Extractors Factory ────────────────────────────────────────────────

    /**
     * Only the two extractors required for YouTube audio streams:
     * - [MatroskaExtractor] for WebM/Opus
     * - [FragmentedMp4Extractor] for fMP4/AAC
     */
    private fun buildExtractorsFactory(): ExtractorsFactory {
        return ExtractorsFactory {
            arrayOf(MatroskaExtractor(), FragmentedMp4Extractor(), Mp4Extractor())
        }
    }

    // ── DataSource Factory ────────────────────────────────────────────────

    /**
     * Builds a [CacheDataSource.Factory] that:
     * 1. Serves from local disk cache when available.
     * 2. Falls back to HTTP for cache misses using yt-dlp extracted headers.
     *
     * The OkHttpDataSource uses headers extracted by yt-dlp to mimic the
     * exact request pattern that YouTube's CDN expects, bypassing 403 errors.
     */
    private fun buildDataSourceFactory(cache: SimpleCache): DataSource.Factory {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .build()

        val okHttpFactory = OkHttpDataSource.Factory(okHttpClient)
            .setDefaultRequestProperties(
                mapOf("User-Agent" to FALLBACK_USER_AGENT)
            )

        val httpDataSourceFactory = DataSource.Factory {
            val ds = okHttpFactory.createDataSource()
            val songId = currentSongId

            if (songId.isNotEmpty()) {
                val headers = AudioHeaderStore.get(songId)
                if (headers.isNotEmpty()) {
                    for ((key, value) in headers) {
                        ds.setRequestProperty(key, value)
                    }
                    Log.i(TAG, "Injected ${headers.size} headers for song $songId")
                } else {
                    Log.w(TAG, "No stored headers for song $songId — using fallback UA")
                }
            }
            ds
        }

        // DefaultDataSource handles file:// (local) AND http:// (streaming)
        // automatically — OkHttp for HTTP, FileDataSource for local files.
        val defaultDataSourceFactory = DefaultDataSource.Factory(this, httpDataSourceFactory)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(defaultDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    // ── Player Event Listener ─────────────────────────────────────────────

    /**
     * Handles player-level events: errors, state transitions, and logging.
     *
     * Unlike ViMusic, we do NOT perform database writes from within the
     * player listener. Play-time tracking and event logging should be
     * observed via [Player.Listener] in the repository/domain layer,
     * keeping the service focused on playback only.
     */
    private inner class PlayerEventListener : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_IDLE -> Log.d(TAG, "Playback: IDLE")
                Player.STATE_BUFFERING -> Log.d(TAG, "Playback: BUFFERING")
                Player.STATE_READY -> Log.d(TAG, "Playback: READY")
                Player.STATE_ENDED -> {
                    Log.d(TAG, "Playback: ENDED")
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Playback error: ${error.errorCodeName}", error)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val id = mediaItem?.mediaId ?: "null"
            currentSongId = mediaItem?.mediaId ?: ""
            Log.d(TAG, "Media transition → $id (reason=$reason)")
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(TAG, "isPlaying=$isPlaying")
            if (isPlaying) {
                // AudioTrack is now active — safe to create audio effects.
                exoPlayer?.let { p ->
                    val sessionId = p.audioSessionId
                    if (sessionId != 0) {
                        equalizerManager.initialize(sessionId)
                    }
                }
            }
        }
    }

    // ── Resource Cleanup ──────────────────────────────────────────────────

    private fun releaseResources() {
        equalizerManager.release()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        exoPlayer = null

        Log.i(TAG, "All resources released")
    }
}
