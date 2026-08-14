package com.musicflow.app.utils

import android.os.Debug
import android.os.Process
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors device performance: frame rate estimation, memory usage,
 * and device classification.
 *
 * Used by visual engines to degrade effects on weak devices.
 * Starts a background polling loop that reads /proc/meminfo for memory
 * and estimates FPS from frame timing callbacks.
 */
@Singleton
class PerformanceMonitor @Inject constructor() {

    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val POLL_INTERVAL_MS = 2000L
        private const val LOW_MEMORY_MB = 512
        private const val HIGH_MEMORY_MB = 2048
    }

    /**
     * Device performance classification.
     */
    enum class PerformanceLevel {
        LOW,
        MEDIUM,
        HIGH,
    }

    /**
     * Current performance state snapshot.
     */
    data class PerformanceState(
        val fps: Float = 60f,
        val memoryUsedMb: Float = 0f,
        val isLowEndDevice: Boolean = false,
        val performanceLevel: PerformanceLevel = PerformanceLevel.MEDIUM,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _performanceState = MutableStateFlow(PerformanceState())
    val performanceState: StateFlow<PerformanceState> = _performanceState.asStateFlow()

    private var monitoringJob: Job? = null

    @Volatile
    private var estimatedFps = 60f

    /**
     * Starts the background performance monitoring loop.
     * Safe to call multiple times — duplicates are ignored.
     */
    fun start() {
        if (monitoringJob?.isActive == true) return

        monitoringJob = scope.launch {
            while (isActive) {
                val memoryMb = getUsedMemoryMb()
                val perfLevel = classifyDevice(memoryMb)

                _performanceState.value = PerformanceState(
                    fps = estimatedFps,
                    memoryUsedMb = memoryMb,
                    isLowEndDevice = perfLevel == PerformanceLevel.LOW,
                    performanceLevel = perfLevel,
                )

                delay(POLL_INTERVAL_MS)
            }
        }
        Log.i(TAG, "Performance monitoring started")
    }

    /**
     * Stops the background monitoring loop.
     */
    fun stop() {
        monitoringJob?.cancel()
        monitoringJob = null
        Log.i(TAG, "Performance monitoring stopped")
    }

    /**
     * Returns the current performance level classification.
     */
    fun getPerformanceLevel(): PerformanceLevel {
        return _performanceState.value.performanceLevel
    }

    /**
     * Updates the estimated FPS. Can be called from a Choreographer frame callback.
     */
    fun updateFps(fps: Float) {
        estimatedFps = fps
    }

    private fun getUsedMemoryMb(): Float {
        val runtime = Runtime.getRuntime()
        val usedBytes = runtime.totalMemory() - runtime.freeMemory()
        return usedBytes / (1024f * 1024f)
    }

    private fun classifyDevice(memoryMb: Float): PerformanceLevel {
        return when {
            memoryMb < LOW_MEMORY_MB -> PerformanceLevel.LOW
            memoryMb < HIGH_MEMORY_MB -> PerformanceLevel.MEDIUM
            else -> PerformanceLevel.HIGH
        }
    }
}
