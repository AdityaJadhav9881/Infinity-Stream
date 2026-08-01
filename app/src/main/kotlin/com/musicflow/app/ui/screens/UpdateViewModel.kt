package com.musicflow.app.ui.screens

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicflow.app.data.remote.InfinityMasterClient
import com.musicflow.app.utils.ApkInstaller
import com.musicflow.app.utils.DownloadState
import com.musicflow.app.utils.UpdateChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdateUiState(
    val isUpdateAvailable: Boolean = false,
    val isDismissed: Boolean = false,
    val version: String = "",
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val forced: Boolean = false,
    val downloadState: DownloadState = DownloadState.Idle,
    val isInstalling: Boolean = false,
    val isInstallLaunched: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateChecker: UpdateChecker,
    private val apkInstaller: ApkInstaller,
    private val client: InfinityMasterClient
) : ViewModel() {

    companion object {
        private const val TAG = "UpdateViewModel"
    }

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    init {
        observeUpdates()
    }

    private fun observeUpdates() {
        viewModelScope.launch {
            updateChecker.observeUpdateInfo()
                .catch { e ->
                    Log.e(TAG, "Error observing updates: ${e.message}")
                }
                .collect { updateInfo ->
                    if (updateInfo != null && updateInfo.version.isNotEmpty()) {
                        // Only show if not dismissed by user
                        val shouldShow = !_uiState.value.isDismissed
                        _uiState.value = _uiState.value.copy(
                            isUpdateAvailable = shouldShow,
                            version = updateInfo.version,
                            releaseNotes = updateInfo.releaseNotes,
                            downloadUrl = updateInfo.downloadUrl,
                            forced = updateInfo.forced,
                        )
                        Log.i(TAG, "Update available: ${updateInfo.version}, showing: $shouldShow")
                    } else {
                        _uiState.value = _uiState.value.copy(isUpdateAvailable = false)
                    }
                }
        }
    }

    fun downloadUpdate() {
        val url = _uiState.value.downloadUrl
        if (url.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "No download URL available")
            return
        }

        _uiState.value = _uiState.value.copy(
            downloadState = DownloadState.Downloading(0, 0, 0),
            error = null
        )

        viewModelScope.launch {
            apkInstaller.downloadApk(url)
                .catch { e ->
                    Log.e(TAG, "Download error: ${e.message}")
                    _uiState.value = _uiState.value.copy(
                        downloadState = DownloadState.Error("Download failed: ${e.message}"),
                        error = "Download failed: ${e.message}"
                    )
                }
                .collect { state ->
                    _uiState.value = _uiState.value.copy(downloadState = state)

                    when (state) {
                        is DownloadState.Downloaded -> {
                            Log.i(TAG, "APK downloaded, starting install...")
                            installApk(state.apkUri)
                        }
                        is DownloadState.Error -> {
                            _uiState.value = _uiState.value.copy(error = state.message)
                        }
                        else -> {}
                    }
                }
        }
    }

    private fun installApk(apkUri: Uri) {
        _uiState.value = _uiState.value.copy(isInstalling = true)
        viewModelScope.launch {
            try {
                val success = apkInstaller.installApk(apkUri)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        isInstalling = false,
                        isInstallLaunched = true,
                    )
                    Log.i(TAG, "Install intent launched — user should see Package Installer")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isInstalling = false,
                        error = "Failed to open installer"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Install error: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isInstalling = false,
                    error = "Install failed: ${e.message}"
                )
            }
        }
    }

    /**
     * Install the already-downloaded APK directly (for "Install Update" button).
     */
    fun installOnly() {
        val apkFile = apkInstaller.getLatestDownloadedApkFile()
        if (!apkFile.exists()) {
            _uiState.value = _uiState.value.copy(error = "APK file not found. Re-download.")
            return
        }
        val apkUri = android.net.Uri.fromFile(apkFile)
        installApk(apkUri)
    }

    /**
     * Restart the app to apply the newly installed update.
     */
    fun restartApp() {
        apkInstaller.restartApp()
    }

    fun dismissUpdate() {
        if (_uiState.value.forced) {
            Log.w(TAG, "Cannot dismiss forced update")
            _uiState.value = _uiState.value.copy(error = "This update is required")
            return
        }
        // Don't clear from DataStore — just hide the overlay
        // User can check for updates again from Settings
        _uiState.value = _uiState.value.copy(isDismissed = true)
        Log.i(TAG, "Update dismissed (not cleared from storage)")
    }

    fun showUpdate() {
        // Re-show the update overlay if an update is available
        val current = _uiState.value
        if (current.version.isNotEmpty()) {
            _uiState.value = current.copy(isDismissed = false)
        }
    }

    fun checkForUpdateFromSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDismissed = false)
            updateChecker.checkOnStartup()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
