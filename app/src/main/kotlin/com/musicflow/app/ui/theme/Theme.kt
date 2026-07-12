package com.musicflow.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.musicflow.app.utils.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = AccentGreen,
    onPrimary = OnAccent,
    primaryContainer = AccentGreenDark,
    onPrimaryContainer = OnAccent,
    secondary = SecondaryPurple,
    onSecondary = OnAccent,
    secondaryContainer = SecondaryPurple.copy(alpha = 0.12f),
    onSecondaryContainer = OnBackground,
    tertiary = TertiaryTeal,
    onTertiary = OnAccent,
    tertiaryContainer = TertiaryTeal.copy(alpha = 0.12f),
    onTertiaryContainer = OnBackground,
    background = Color(0xFF000000),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFF9E9E9E),
    surfaceTint = AccentGreen,
    error = ErrorRed,
    onError = OnAccent,
    errorContainer = ErrorRedContainer,
    onErrorContainer = OnBackground,
    outline = Divider,
    outlineVariant = Divider,
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1DB954),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1DB954).copy(alpha = 0.12f),
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary = SecondaryPurple,
    onSecondary = Color.White,
    secondaryContainer = SecondaryPurple.copy(alpha = 0.08f),
    onSecondaryContainer = Color(0xFF1A1A1A),
    tertiary = TertiaryTeal,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryTeal.copy(alpha = 0.08f),
    onTertiaryContainer = Color(0xFF1A1A1A),
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF666666),
    surfaceTint = Color(0xFF1DB954),
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFD32F2F).copy(alpha = 0.12f),
    onErrorContainer = Color(0xFF1A1A1A),
    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFE0E0E0),
)

@Composable
fun MusicFlowTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val isDarkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MusicFlowTypography,
        content = content,
    )
}
