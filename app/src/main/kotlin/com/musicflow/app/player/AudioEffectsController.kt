package com.musicflow.app.player

import com.musicflow.app.utils.EqualizerManager
import com.musicflow.app.utils.EqualizerPreset
import com.musicflow.app.utils.PlayerSettingsManager

/**
 * Clean interface for audio effects operations.
 * Wraps EqualizerManager and PlayerSettingsManager for equalizer,
 * bass boost, virtualizer, and volume normalization.
 */
interface AudioEffectsController {

    /** Applies a named equalizer preset. */
    fun setEqualizerPreset(preset: EqualizerPreset)

    /** Sets the bass boost strength (0-1000). */
    fun setBassBoost(strength: Int)

    /** Sets the virtualizer strength (0-1000). */
    fun setVirtualizer(strength: Int)

    /** Enables or disables volume normalization. */
    fun setVolumeNormalization(enabled: Boolean)

    /** Returns the current equalizer preset. */
    fun getCurrentPreset(): EqualizerPreset

    /** Releases all audio effect resources. */
    fun release()
}

/**
 * Default implementation of [AudioEffectsController] that wraps
 * [EqualizerManager] and [PlayerSettingsManager].
 */
class DefaultAudioEffectsController(
    private val equalizerManager: EqualizerManager,
    private val playerSettingsManager: PlayerSettingsManager,
) : AudioEffectsController {

    @Volatile
    private var currentPreset: EqualizerPreset = EqualizerPreset.BASS_BOOST

    override fun setEqualizerPreset(preset: EqualizerPreset) {
        currentPreset = preset
        equalizerManager.applyPreset(preset)
    }

    override fun setBassBoost(strength: Int) {
        equalizerManager.setBassBoost(strength)
    }

    override fun setVirtualizer(strength: Int) {
        equalizerManager.setVirtualizer(strength)
    }

    override fun setVolumeNormalization(enabled: Boolean) {
        equalizerManager.setVolumeNormalization(enabled)
    }

    override fun getCurrentPreset(): EqualizerPreset = currentPreset

    override fun release() {
        equalizerManager.release()
    }
}
