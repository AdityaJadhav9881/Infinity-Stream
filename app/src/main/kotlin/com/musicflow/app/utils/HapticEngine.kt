package com.musicflow.app.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized haptic feedback engine.
 *
 * Provides named haptic methods that degrade gracefully when the device
 * lacks a vibrator or when haptics are disabled. Uses the platform
 * [HapticFeedbackConstants] API when possible for system-level patterns,
 * falling back to [VibrationEffect] for older devices.
 *
 * The engine does not hold a reference to any View — callers should
 * supply a View reference for [performClick] and [performLongPress]
 * haptic calls that go through the View system.
 */
@Singleton
class HapticEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        private const val TAG = "HapticEngine"
    }

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val hasVibrator: Boolean
        get() = vibrator?.hasVibrator() == true

    /**
     * Performs a click haptic — light, crisp tap.
     * Uses View-level haptic for system-consistent feedback.
     */
    fun performClick(view: View? = null) {
        try {
            view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                ?: vibrateEffect(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } catch (e: Exception) {
            Log.w(TAG, "performClick failed: ${e.message}")
        }
    }

    /**
     * Performs a long press haptic — heavier confirmation feedback.
     */
    fun performLongPress(view: View? = null) {
        try {
            view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                ?: vibrateEffect(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } catch (e: Exception) {
            Log.w(TAG, "performLongPress failed: ${e.message}")
        }
    }

    /**
     * Performs a success haptic — double-tap confirmation pattern.
     */
    fun performSuccess(view: View? = null) {
        try {
            if (view != null) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(
                    longArrayOf(0, 30, 50, 30),
                    intArrayOf(0, 120, 0, 200),
                    -1,
                )
                vibrateEffect(effect)
            }
        } catch (e: Exception) {
            Log.w(TAG, "performSuccess failed: ${e.message}")
        }
    }

    /**
     * Performs a heavy click haptic — for destructive actions like delete.
     */
    fun performHeavyClick(view: View? = null) {
        try {
            view?.performHapticFeedback(HapticFeedbackConstants.REJECT)
                ?: vibrateEffect(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } catch (e: Exception) {
            Log.w(TAG, "performHeavyClick failed: ${e.message}")
        }
    }

    private fun vibrateEffect(effect: VibrationEffect) {
        if (!hasVibrator) return
        try {
            vibrator?.vibrate(effect)
        } catch (e: Exception) {
            Log.w(TAG, "vibrateEffect failed: ${e.message}")
        }
    }
}
