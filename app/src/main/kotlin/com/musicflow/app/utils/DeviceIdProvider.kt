package com.musicflow.app.utils

import android.content.Context
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val DEVICE_ID_KEY = stringPreferencesKey("im_device_id")
        private val FULL_NAME_KEY = stringPreferencesKey("im_full_name")
        private val HOW_DID_YOU_HEAR_KEY = stringPreferencesKey("im_how_did_you_hear")
        private val DEVICE_DISABLED_KEY = stringPreferencesKey("im_device_disabled")
    }

    suspend fun getDeviceId(): String {
        val cached = dataStore.data.map { it[DEVICE_ID_KEY] }.first()
        if (!cached.isNullOrBlank()) return cached

        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"

        dataStore.edit { it[DEVICE_ID_KEY] = androidId }
        return androidId
    }

    suspend fun getFullName(): String =
        dataStore.data.map { it[FULL_NAME_KEY] ?: "" }.first()

    suspend fun setFullName(name: String) {
        dataStore.edit { it[FULL_NAME_KEY] = name }
    }

    suspend fun getHowDidYouHear(): String =
        dataStore.data.map { it[HOW_DID_YOU_HEAR_KEY] ?: "" }.first()

    suspend fun setHowDidYouHear(value: String) {
        dataStore.edit { it[HOW_DID_YOU_HEAR_KEY] = value }
    }

    suspend fun isDeviceDisabled(): Boolean =
        dataStore.data.map { it[DEVICE_DISABLED_KEY] == "true" }.first()

    suspend fun setDeviceDisabled(disabled: Boolean) {
        dataStore.edit { it[DEVICE_DISABLED_KEY] = disabled.toString() }
    }

    fun getPlatform(): String = "android"

    fun getOsVersion(): String = android.os.Build.VERSION.RELEASE

    fun getAppVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }
}
