package com.musicflow.app.utils

import android.util.Log
import com.musicflow.app.data.remote.InfinityMasterClient
import java.io.PrintWriter
import java.io.StringWriter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashReporter @Inject constructor(
    private val client: InfinityMasterClient,
    private val deviceIdProvider: DeviceIdProvider
) {
    companion object {
        private const val TAG = "CrashReporter"
    }

    private var originalHandler: Thread.UncaughtExceptionHandler? = null

    fun install() {
        originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val stackTrace = sw.toString()
                val errorMessage = "${throwable.javaClass.name}: ${throwable.message}"

                Log.e(TAG, "Uncaught exception: $errorMessage")

                kotlinx.coroutines.runBlocking {
                    client.reportCrash(errorMessage, stackTrace, deviceIdProvider.getAppVersion())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report crash: ${e.message}")
            }

            originalHandler?.uncaughtException(thread, throwable)
        }
        Log.i(TAG, "Crash reporter installed")
    }
}
