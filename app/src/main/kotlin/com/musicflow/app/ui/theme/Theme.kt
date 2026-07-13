package com.musicflow.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.musicflow.app.utils.ThemeMode

/**
 * MusicFlow Premium Theme
 *
 * Dark-only Material 3 theme with layered surfaces.
 * No pure black. Every surface has depth and warmth.
 *
 * Surface hierarchy creates visual depth:
 * - Background: #0B0B0E (deepest)
 * - Card: #15161A (resting)
 * - Elevated: #1B1C20 (interactive)
 * - Overlay: #22242A (floating)
 */
private val MFColorScheme = darkColorScheme(
    primary = MFColors.Accent,
    onPrimary = MFColors.TextOnAccent,
    primaryContainer = MFColors.AccentPressed,
    onPrimaryContainer = MFColors.TextOnAccent,
    secondary = MFColors.Secondary,
    onSecondary = MFColors.TextOnAccent,
    secondaryContainer = MFColors.Secondary.copy(alpha = 0.12f),
    onSecondaryContainer = MFColors.TextPrimary,
    tertiary = MFColors.Tertiary,
    onTertiary = MFColors.TextOnAccent,
    tertiaryContainer = MFColors.Tertiary.copy(alpha = 0.12f),
    onTertiaryContainer = MFColors.TextPrimary,
    background = MFColors.Background,
    onBackground = MFColors.TextPrimary,
    surface = MFColors.Card,
    onSurface = MFColors.TextPrimary,
    surfaceVariant = MFColors.Elevated,
    onSurfaceVariant = MFColors.TextSecondary,
    surfaceTint = MFColors.Accent,
    error = MFColors.Error,
    onError = MFColors.TextOnAccent,
    errorContainer = MFColors.ErrorContainer,
    onErrorContainer = MFColors.TextPrimary,
    outline = MFColors.Divider,
    outlineVariant = MFColors.Divider,
)

@Composable
fun MusicFlowTheme(
    @Suppress("UNUSED_PARAMETER") themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = MFColorScheme,
        typography = MFTypography,
        content = content,
    )
}
