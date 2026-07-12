package com.musicflow.app.utils

import android.content.Context
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class EqualizerPreset(val label: String, val bass: Int, val virtualizer: Int) {
    NORMAL("Normal", 0, 0),
    BASS_BOOST("Bass Boost", 1000, 500),
    BASS_DEEP("Deep Bass", 1000, 800),
    VOCAL("Vocal", 0, 300),
    ROCK("Rock", 600, 400),
    POP("Pop", 400, 500),
    CLASSICAL("Classical", 200, 300),
    JAZZ("Jazz", 300, 400),
    ELECTRONIC("Electronic", 800, 700),
}

@Singleton
class EqualizerManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentSessionId: Int = -1
    private var currentPreset: EqualizerPreset = EqualizerPreset.NORMAL
    private var volumeNormalizationEnabled: Boolean = false

    fun initialize(audioSessionId: Int) {
        if (audioSessionId == currentSessionId && bassBoost != null) {
            return
        }
        release()

        try {
            currentSessionId = audioSessionId
            bassBoost = BassBoost(1, audioSessionId).apply { enabled = true }
            virtualizer = Virtualizer(1, audioSessionId).apply { enabled = true }
            equalizer = Equalizer(1, audioSessionId).apply { enabled = true }
            loudnessEnhancer = LoudnessEnhancer(audioSessionId)

            applyPreset(currentPreset)
            loudnessEnhancer?.let { enhancer ->
                enhancer.enabled = volumeNormalizationEnabled
                if (volumeNormalizationEnabled) enhancer.setTargetGain(6)
            }
        } catch (e: Exception) {
            currentSessionId = -1
            bassBoost = null
            virtualizer = null
            equalizer = null
            loudnessEnhancer = null
        }
    }

    fun applyPreset(preset: EqualizerPreset) {
        currentPreset = preset
        try {
            bassBoost?.setStrength(preset.bass.toShort())
            virtualizer?.setStrength(preset.virtualizer.toShort())
        } catch (e: Exception) {
        }
    }

    fun setBassBoost(strength: Int) {
        try {
            bassBoost?.setStrength(strength.coerceIn(0, 1000).toShort())
        } catch (e: Exception) {
        }
    }

    fun setVirtualizer(strength: Int) {
        try {
            virtualizer?.setStrength(strength.coerceIn(0, 1000).toShort())
        } catch (e: Exception) {
        }
    }

    fun setVolumeNormalization(enabled: Boolean) {
        volumeNormalizationEnabled = enabled
        try {
            loudnessEnhancer?.let { enhancer ->
                enhancer.enabled = enabled
                if (enabled) {
                    enhancer.setTargetGain(6)
                }
            }
        } catch (e: Exception) {
        }
    }

    fun setEnabled(enabled: Boolean) {
        try {
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
            equalizer?.enabled = enabled
        } catch (e: Exception) {
        }
    }

    fun release() {
        try {
            bassBoost?.release()
            virtualizer?.release()
            equalizer?.release()
            loudnessEnhancer?.release()
        } catch (_: Exception) {}
        bassBoost = null
        virtualizer = null
        equalizer = null
        loudnessEnhancer = null
        currentSessionId = -1
    }
}
