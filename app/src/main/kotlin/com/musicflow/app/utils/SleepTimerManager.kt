package com.musicflow.app.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coroutine-based sleep timer manager.
 *
 * Counts down from a specified duration and emits the remaining time.
 * When the timer expires, it notifies the caller to pause playback.
 */
@Singleton
class SleepTimerManager @Inject constructor() {

    companion object {
        private const val TAG = "SleepTimerManager"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null

    private val _remainingTimeMs = MutableStateFlow<Long?>(null)
    val remainingTimeMs: StateFlow<Long?> = _remainingTimeMs.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    /** Callback invoked when the timer expires. */
    var onTimerExpired: (() -> Unit)? = null

    /**
     * Starts the sleep timer with the specified duration.
     *
     * @param durationMs Duration in milliseconds until the timer expires.
     */
    fun start(durationMs: Long) {
        cancel()
        Log.i(TAG, "Starting sleep timer: ${durationMs / 1000}s")
        _isRunning.value = true

        timerJob = scope.launch {
            var remaining = durationMs
            while (remaining > 0) {
                _remainingTimeMs.value = remaining
                delay(1000L)
                remaining -= 1000L
            }
            _remainingTimeMs.value = 0L
            _isRunning.value = false
            Log.i(TAG, "Sleep timer expired")
            onTimerExpired?.invoke()
        }
    }

    /**
     * Starts the sleep timer with minutes.
     */
    fun startMinutes(minutes: Int) {
        start(minutes.toLong() * 60 * 1000)
    }

    /**
     * Cancels the running sleep timer.
     */
    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _remainingTimeMs.value = null
        _isRunning.value = false
        Log.i(TAG, "Sleep timer cancelled")
    }

    /**
     * Returns the remaining time formatted as "MM:SS" or "HH:MM:SS".
     */
    fun formatRemainingTime(remainingMs: Long): String {
        if (remainingMs <= 0) return "0:00"
        val totalSeconds = remainingMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}