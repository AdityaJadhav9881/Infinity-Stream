package com.musicflow.app.ui.screens

import androidx.lifecycle.ViewModel
import com.musicflow.app.MusicFlowApplication
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * ViewModel that exposes yt-dlp engine metadata to the UI.
 *
 * Reads directly from [MusicFlowApplication]'s companion StateFlows,
 * which are updated during the background init/update coroutine.
 */
@HiltViewModel
class EngineInfoViewModel @Inject constructor() : ViewModel() {

    /** Current yt-dlp binary version string (e.g., "2026.07.09.234832"). */
    val engineVersion: StateFlow<String> = MusicFlowApplication.engineVersion

    /** Latest status of the update attempt (e.g., "Updated (nightly): ..."). */
    val updateStatus: StateFlow<String> = MusicFlowApplication.updateStatus
}
