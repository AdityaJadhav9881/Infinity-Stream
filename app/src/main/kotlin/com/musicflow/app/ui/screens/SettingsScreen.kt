package com.musicflow.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicflow.app.data.local.LocalBackupManager
import com.musicflow.app.ui.theme.AccentGreen
import com.musicflow.app.ui.theme.DarkSurface
import com.musicflow.app.ui.theme.ErrorRed
import com.musicflow.app.ui.theme.MFColors
import com.musicflow.app.ui.theme.MFTokens
import com.musicflow.app.ui.theme.OnBackground
import com.musicflow.app.ui.theme.OnBackgroundVariant
import com.musicflow.app.utils.ThemeMode
import com.musicflow.app.utils.ThemePreferences
import com.musicflow.app.utils.EqualizerManager
import com.musicflow.app.utils.EqualizerPreset
import com.musicflow.app.utils.LanguagePreferences
import com.musicflow.app.utils.PlayerSettingsManager
import com.musicflow.app.utils.SearchLanguage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    engineInfoViewModel: EngineInfoViewModel,
    themePreferences: ThemePreferences? = null,
    languagePreferences: LanguagePreferences? = null,
    equalizerManager: EqualizerManager? = null,
    playerSettingsManager: PlayerSettingsManager? = null,
    localBackupManager: LocalBackupManager? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val engineVersion by engineInfoViewModel.engineVersion.collectAsState()
    val updateStatus by engineInfoViewModel.updateStatus.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val themeModeState = themePreferences?.themeMode?.collectAsState(initial = ThemeMode.DARK)
    val themeMode = themeModeState?.value ?: ThemeMode.DARK
    val selectedLanguagesState = languagePreferences?.searchLanguages?.collectAsState(initial = listOf(SearchLanguage.ENGLISH))
    val selectedLanguages = selectedLanguagesState?.value ?: listOf(SearchLanguage.ENGLISH)
    val skipSilenceState = playerSettingsManager?.skipSilence?.collectAsState(initial = false)
    val skipSilence = skipSilenceState?.value ?: false
    val volumeNormState = playerSettingsManager?.volumeNormalization?.collectAsState(initial = false)
    val volumeNormalization = volumeNormState?.value ?: false
    val equalizerPresetState = playerSettingsManager?.equalizerPreset?.collectAsState(initial = "NORMAL")
    val currentPresetName = equalizerPresetState?.value ?: "NORMAL"
    val currentPreset = try { EqualizerPreset.valueOf(currentPresetName) } catch (_: Exception) { EqualizerPreset.NORMAL }

    var showLanguageDropdown by remember { mutableStateOf(false) }
    var showEqualizerDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MFColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MFTokens.ScreenHorizontalPadding, vertical = 24.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MFColors.TextPrimary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Appearance Section ────────────────────────────────────
        SectionHeader(title = "Appearance")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                ThemeMode.values().forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                themePreferences?.let { prefs ->
                                    coroutineScope.launch {
                                        prefs.setThemeMode(mode)
                                    }
                                }
                            }
                            .padding(vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            tint = if (themeMode == mode) AccentGreen else OnBackgroundVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        if (themeMode == mode) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Selected",
                                tint = AccentGreen,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Language Section ──────────────────────────────────────
        SectionHeader(title = "Search Language")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Show selected languages summary + expand arrow
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showLanguageDropdown = !showLanguageDropdown }
                        .padding(vertical = 8.dp),
                ) {
                    Text(
                        text = if (selectedLanguages.size == 1) {
                            selectedLanguages.first().label
                        } else {
                            "${selectedLanguages.size} languages selected"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = AccentGreen,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = if (showLanguageDropdown) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Toggle",
                        tint = OnBackgroundVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Expandable language list
                AnimatedVisibility(visible = showLanguageDropdown) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        SearchLanguage.values().forEach { language ->
                            val isSelected = language in selectedLanguages
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            val prefs = languagePreferences ?: return@launch
                                            val current = prefs.searchLanguages.first()
                                            val updated = if (isSelected) {
                                                current.filter { lang -> lang != language }.ifEmpty { listOf(SearchLanguage.ENGLISH) }
                                            } else {
                                                current + language
                                            }
                                            prefs.setLanguages(updated)
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                            ) {
                                Text(
                                    text = language.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) AccentGreen else MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f),
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = AccentGreen,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Player & Audio Section ───────────────────────────────
        SectionHeader(title = "Player & Audio")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            coroutineScope.launch {
                                playerSettingsManager?.setSkipSilence(!skipSilence)
                            }
                        }
                        .padding(vertical = 8.dp),
                ) {
                    Text(
                        text = "Skip Silence",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (skipSilence) "ON" else "OFF",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (skipSilence) AccentGreen else OnBackgroundVariant,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            coroutineScope.launch {
                                playerSettingsManager?.setVolumeNormalization(!volumeNormalization)
                            }
                        }
                        .padding(vertical = 8.dp),
                ) {
                    Text(
                        text = "Volume Normalization",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (volumeNormalization) "ON" else "OFF",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (volumeNormalization) AccentGreen else OnBackgroundVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Equalizer Section ─────────────────────────────────────
        SectionHeader(title = "Equalizer")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { showEqualizerDropdown = !showEqualizerDropdown },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Current: ${currentPreset.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = if (showEqualizerDropdown) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (showEqualizerDropdown) "Collapse" else "Expand",
                        tint = OnBackgroundVariant,
                    )
                }

                AnimatedVisibility(visible = showEqualizerDropdown) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        EqualizerPreset.values().forEach { preset ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        equalizerManager?.applyPreset(preset)
                                        coroutineScope.launch {
                                            playerSettingsManager?.setEqualizerPreset(preset.name)
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                            ) {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (currentPreset == preset) AccentGreen else MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f),
                                )
                                if (currentPreset == preset) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = AccentGreen,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Engine Section ────────────────────────────────────────
        SectionHeader(title = "Engine")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Engineering,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "yt-dlp Version",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = engineVersion,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnBackgroundVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val isUpToDate = updateStatus.contains("ALREADY_UP_TO_DATE") ||
                            updateStatus.contains("Updated")
                    val isError = updateStatus.contains("failed") ||
                            updateStatus.contains("Failed")

                    Icon(
                        imageVector = when {
                            isError -> Icons.Filled.Warning
                            isUpToDate -> Icons.Filled.CheckCircle
                            else -> Icons.Filled.Info
                        },
                        contentDescription = null,
                        tint = when {
                            isError -> ErrorRed
                            isUpToDate -> AccentGreen
                            else -> OnBackgroundVariant
                        },
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Update Status",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = updateStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnBackgroundVariant,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Backup & Restore Section ─────────────────────────────
        SectionHeader(title = "Backup & Restore")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Backup saves playlists, favorites, and library to /sdcard/Music/MusicFlow/backups/.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBackgroundVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Backup button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            coroutineScope.launch {
                                val success = localBackupManager?.backup()
                                val msg = if (success == true) "Backup saved successfully" else "Backup failed"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Backup,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Backup Now",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Restore button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val hasBackup = localBackupManager?.hasBackup() == true
                            if (!hasBackup) {
                                Toast.makeText(context, "No backup found", Toast.LENGTH_SHORT).show()
                                return@clickable
                            }
                            coroutineScope.launch {
                                val success = localBackupManager?.restore()
                                if (success == true) {
                                    Toast.makeText(context, "Restored! Restarting...", Toast.LENGTH_SHORT).show()
                                    // Restart the app to reconnect Room to the restored database
                                    val pm = context.packageManager
                                    val intent = pm.getLaunchIntentForPackage(context.packageName)
                                    if (intent != null) {
                                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    }
                                    Runtime.getRuntime().exit(0)
                                } else {
                                    Toast.makeText(context, "Restore failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .padding(vertical = 10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restore,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Restore from Backup",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "Requires app restart after restoring",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnBackgroundVariant.copy(alpha = 0.7f),
                        )
                    }
                }

                if (localBackupManager?.hasBackup() == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Backup exists at /sdcard/Music/MusicFlow/backups/musicflow.db",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentGreen.copy(alpha = 0.7f),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── About Section ─────────────────────────────────────────
        SectionHeader(title = "About")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MusicFlow",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ad-free YouTube Music streaming",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBackgroundVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Version 2.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnBackgroundVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MFColors.TextSecondary,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}
