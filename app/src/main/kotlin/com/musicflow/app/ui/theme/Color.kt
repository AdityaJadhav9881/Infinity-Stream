package com.musicflow.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * MusicFlow Premium Design System — Color Tokens
 *
 * Futuristic glassmorphism palette with layered depth.
 * Near-black backgrounds with soft transparency layers.
 * Every surface breathes. Nothing feels flat.
 *
 * ## Surface Hierarchy (Glass Layers)
 * - Background (#09090B) — deepest void
 * - GlassLow (8%) — resting cards
 * - GlassMid (14%) — interactive elements
 * - GlassHigh (22%) — elevated floating elements
 * - GlassOverlay (30%) — dialogs, menus
 *
 * ## Accent
 * Neon green with dynamic variant support from album artwork.
 */
object MFColors {
    // ── Background Layers ───────────────────────────────────────────────

    /** Deepest background — near-black void. */
    val Background = Color(0xFF09090B)

    /** Glass layer 1 — resting cards, subtle depth. */
    val GlassLow = Color(0x14FFFFFF)

    /** Glass layer 2 — interactive elements. */
    val GlassMid = Color(0x22FFFFFF)

    /** Glass layer 3 — elevated floating elements. */
    val GlassHigh = Color(0x38FFFFFF)

    /** Glass overlay — dialogs, menus, sheets. */
    val GlassOverlay = Color(0x4DFFFFFF)

    /** Solid dialog background — opaque for readability. */
    val DialogBackground = Color(0xFF1A1A1E)

    // ── Legacy Surface Aliases ──────────────────────────────────────────
    val Card get() = GlassLow
    val Elevated get() = GlassMid
    val Overlay get() = GlassOverlay
    val Subtle = Color(0x0DFFFFFF)

    // ── Text ────────────────────────────────────────────────────────────

    /** Primary text — crisp white on any surface. */
    val TextPrimary = Color(0xFFF5F5F7)

    /** Secondary text — subtitles, descriptions. */
    val TextSecondary = Color(0xFF86888F)

    /** Tertiary text — metadata, timestamps, hints. */
    val TextTertiary = Color(0xFF4A4C54)

    /** Text on accent-colored surfaces. */
    val TextOnAccent = Color(0xFF09090B)

    // ── Accent (Neon Emerald) ───────────────────────────────────────────

    /** Primary accent — neon emerald green. */
    val Accent = Color(0xFF1ED760)

    /** Accent pressed/active state. */
    val AccentPressed = Color(0xFF18B84F)

    /** Subtle accent container. */
    val AccentSubtle = Color(0xFF1ED760).copy(alpha = 0.12f)

    /** Accent glow — artwork glow, active indicators. */
    val AccentGlow = Color(0xFF1ED760).copy(alpha = 0.30f)

    // ── Secondary & Tertiary ────────────────────────────────────────────

    /** Secondary — violet for shuffle, repeat. */
    val Secondary = Color(0xFFA78BFA)

    /** Tertiary — cyan for subtle indicators. */
    val Tertiary = Color(0xFF22D3EE)

    // ── Semantic ────────────────────────────────────────────────────────

    val Error = Color(0xFFEF4444)
    val ErrorContainer = Color(0xFFEF4444).copy(alpha = 0.12f)
    val Success = Color(0xFF1ED760)
    val Warning = Color(0xFFFBBF24)

    // ── Dividers & Borders ─────────────────────────────────────────────

    /** Ultra-subtle divider — barely visible. */
    val Divider = Color(0x0FFFFFFF)

    /** Glass border — thin white edge with low opacity. */
    val GlassBorder = Color(0x1AFFFFFF)

    /** Active border for focused inputs. */
    val BorderActive = Color(0xFF1ED760)

    // ── Progress ────────────────────────────────────────────────────────

    val ProgressTrack = Color(0x1AFFFFFF)
    val ProgressFill = Color(0xFF1ED760)
    val ProgressThumb = Color(0xFFFFFFFF)

    // ── Dynamic Color Placeholders ──────────────────────────────────────

    /** Dynamic accent — extracted from album artwork. */
    var DynamicAccent = Accent
        private set

    /** Dynamic glow — extracted from album artwork. */
    var DynamicGlow = AccentGlow
        private set

    /** Dynamic gradient — extracted from album artwork. */
    var DynamicGradient = listOf(Accent, Accent.copy(alpha = 0.6f))
        private set

    fun updateDynamicColors(
        accent: Color = Accent,
        glow: Color = AccentGlow,
        gradient: List<Color> = listOf(Accent, Accent.copy(alpha = 0.6f)),
    ) {
        DynamicAccent = accent
        DynamicGlow = glow
        DynamicGradient = gradient
    }

    fun resetDynamicColors() {
        DynamicAccent = Accent
        DynamicGlow = AccentGlow
        DynamicGradient = listOf(Accent, Accent.copy(alpha = 0.6f))
    }
}

// ── Legacy Aliases ──────────────────────────────────────────────────────

val Black get() = MFColors.Background
val DarkSurface get() = MFColors.GlassLow
val DarkSurfaceVariant get() = MFColors.GlassMid
val DarkSurfaceContainer get() = MFColors.GlassHigh
val GlassSurface get() = MFColors.GlassLow
val CardSurface get() = MFColors.GlassLow
val OnBackground get() = MFColors.TextPrimary
val OnBackgroundVariant get() = MFColors.TextSecondary
val OnAccent get() = MFColors.TextOnAccent
val AccentGreen get() = MFColors.Accent
val AccentGreenDark get() = MFColors.AccentPressed
val AccentGreenLight get() = MFColors.Accent
val SecondaryPurple get() = MFColors.Secondary
val TertiaryTeal get() = MFColors.Tertiary
val ErrorRed get() = MFColors.Error
val ErrorRedContainer get() = MFColors.ErrorContainer
val Divider get() = MFColors.Divider
val BorderActive get() = MFColors.BorderActive
val ProgressTrack get() = MFColors.ProgressTrack
val ProgressIndicator get() = MFColors.ProgressFill
val ProgressThumb get() = MFColors.ProgressThumb

// ── Brush Utilities ──────────────────────────────────────────────────────

object MFBrushes {
    /** Background gradient — subtle depth. */
    val BackgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MFColors.Background,
            MFColors.Background,
        )
    )

    /** Accent gradient — for play buttons and highlights. */
    val AccentGradient = Brush.horizontalGradient(
        colors = listOf(
            MFColors.Accent,
            MFColors.Accent.copy(alpha = 0.85f),
        )
    )

    /** Glow gradient — radial from center for artwork effects. */
    fun glowGradient(color: Color) = Brush.radialGradient(
        colors = listOf(
            color.copy(alpha = 0.45f),
            color.copy(alpha = 0.0f),
        )
    )

    /** Dynamic gradient — based on album artwork colors. */
    val DynamicGradient = Brush.verticalGradient(
        colors = MFColors.DynamicGradient + listOf(MFColors.Background)
    )
}
