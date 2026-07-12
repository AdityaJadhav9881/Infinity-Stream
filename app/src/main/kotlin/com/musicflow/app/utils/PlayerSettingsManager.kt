package com.musicflow.app.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.playerDataStore: DataStore<Preferences> by preferencesDataStore(name = "player_settings")

/**
 * Manages player-related settings: skip silence, volume normalization, equalizer preset.
 * Persisted via DataStore and applied to ExoPlayer by MusicPlaybackService.
 */
@Singleton
class PlayerSettingsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val SKIP_SILENCE_KEY = booleanPreferencesKey("skip_silence")
        private val VOLUME_NORMALIZATION_KEY = booleanPreferencesKey("volume_normalization")
        private val EQUALIZER_PRESET_KEY = stringPreferencesKey("equalizer_preset")
    }

    val skipSilence: Flow<Boolean> = context.playerDataStore.data.map { prefs ->
        prefs[SKIP_SILENCE_KEY] ?: false
    }

    val volumeNormalization: Flow<Boolean> = context.playerDataStore.data.map { prefs ->
        prefs[VOLUME_NORMALIZATION_KEY] ?: false
    }

    val equalizerPreset: Flow<String> = context.playerDataStore.data.map { prefs ->
        prefs[EQUALIZER_PRESET_KEY] ?: "NORMAL"
    }

    suspend fun setSkipSilence(enabled: Boolean) {
        context.playerDataStore.edit { it[SKIP_SILENCE_KEY] = enabled }
    }

    suspend fun setVolumeNormalization(enabled: Boolean) {
        context.playerDataStore.edit { it[VOLUME_NORMALIZATION_KEY] = enabled }
    }

    suspend fun setEqualizerPreset(presetName: String) {
        context.playerDataStore.edit { it[EQUALIZER_PRESET_KEY] = presetName }
    }
}
