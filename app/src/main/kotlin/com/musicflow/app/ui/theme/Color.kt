package com.musicflow.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Strict dark-mode color palette for MusicFlow.
 *
 * All colors are designed for OLED/AMOLED displays with true blacks
 * (#000000) for power savings and visual consistency.
 *
 * ## Palette Structure
 * - **Background/Surface**: Deep blacks and dark grays
 * - **On-Background/On-Surface**: Light grays for readable text
 * - **Primary/Accent**: Vibrant green for play buttons, progress bars
 * - **Error**: Muted red for destructive actions
 *
 * Dynamic theming is disabled to enforce this aesthetic across all devices.
 */

// ── Backgrounds & Surfaces ─────────────────────────────────────────────

/** Premium dark background — slightly warmer than pure black. */
val Black = Color(0xFF0D0D0D)

/** Dark surface for cards, sheets, and elevated components. */
val DarkSurface = Color(0xFF161616)

/** Slightly lighter surface for interactive elements (buttons, chips). */
val DarkSurfaceVariant = Color(0xFF1E1E1E)

/** Surface for mini player and bottom navigation bars. */
val DarkSurfaceContainer = Color(0xFF0A0A0A)

/** Glass surface for glassmorphism effects. */
val GlassSurface = Color(0xFF1A1A1A)

/** Card surface with subtle elevation. */
val CardSurface = Color(0xFF141414)

// ── Text & Icons ───────────────────────────────────────────────────────

/** Primary text on dark backgrounds — high contrast, readable. */
val OnBackground = Color(0xFFE0E0E0)

/** Secondary text (subtitles, timestamps) — reduced contrast. */
val OnBackgroundVariant = Color(0xFF9E9E9E)

/** Text/icons on accent-colored surfaces (e.g., play button text). */
val OnAccent = Color(0xFF000000)

// ── Accent (Primary) ───────────────────────────────────────────────────

/** Vibrant green accent — used for play buttons, progress bars, highlights. */
val AccentGreen = Color(0xFF22C55E)

/** Slightly darker green for pressed states and hover effects. */
val AccentGreenDark = Color(0xFF16A34A)

/** Lighter green for subtle highlights and ripple effects. */
val AccentGreenLight = Color(0xFF4ADE80)

// ── Secondary & Tertiary ───────────────────────────────────────────────

/** Muted purple for secondary actions (shuffle, repeat icons). */
val SecondaryPurple = Color(0xFFBB86FC)

/** Teal for tertiary highlights and subtle indicators. */
val TertiaryTeal = Color(0xFF03DAC6)

// ── Error & Warning ────────────────────────────────────────────────────

/** Muted red for error states and destructive actions. */
val ErrorRed = Color(0xFFCF6679)

/** Dark red background for error containers. */
val ErrorRedContainer = Color(0xFF3D1C24)

// ── Dividers & Borders ─────────────────────────────────────────────────

/** Subtle divider color for separating list items. */
val Divider = Color(0xFF2C2C2C)

/** Border color for focused input fields and active states. */
val BorderActive = Color(0xFF1DB954)

// ── Progress Bar Specific ──────────────────────────────────────────────

/** Track background (unfilled portion of progress bar). */
val ProgressTrack = Color(0xFF333333)

/** Filled portion of the progress bar — matches accent. */
val ProgressIndicator = AccentGreen

/** Thumb/handle color for interactive sliders. */
val ProgressThumb = AccentGreen

// ── Light Theme Colors ─────────────────────────────────────────────────

/** Light theme background — clean white. */
val LightBackground = Color(0xFFFAFAFA)

/** Light theme surface for cards and sheets. */
val LightSurface = Color(0xFFFFFFFF)

/** Light theme surface variant for interactive elements. */
val LightSurfaceVariant = Color(0xFFF0F0F0)

/** Light theme surface container for mini player and nav bars. */
val LightSurfaceContainer = Color(0xFFF5F5F5)

/** Light theme primary text — dark for readability. */
val LightOnBackground = Color(0xFF1A1A1A)

/** Light theme secondary text — muted gray. */
val LightOnBackgroundVariant = Color(0xFF666666)

/** Light theme accent green — slightly adjusted for light backgrounds. */
val LightAccentGreen = Color(0xFF1DB954)

/** Light theme error red. */
val LightErrorRed = Color(0xFFD32F2F)

/** Light theme divider. */
val LightDivider = Color(0xFFE0E0E0)
