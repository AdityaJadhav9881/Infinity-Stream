package com.musicflow.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * MusicFlow Design System — Color Tokens
 *
 * Premium dark-only palette with layered surfaces.
 * No pure black. Every surface has depth and intention.
 *
 * ## Surface Hierarchy
 * - Background (#0B0B0E) — deepest layer
 * - Card (#15161A) — resting surface
 * - Elevated (#1B1C20) — interactive surface
 * - Overlay (#22242A) — highest layer
 *
 * ## Accent
 * Modern emerald green with dynamic variant support.
 * Dynamic accent colors can be generated from album artwork.
 */
object MFColors {
    // ── Background Layers ───────────────────────────────────────────────

    /** Deepest background layer — warm dark, never pure black. */
    val Background = Color(0xFF0B0B0E)

    /** Card surface — resting state for content blocks. */
    val Card = Color(0xFF15161A)

    /** Elevated surface — interactive elements, buttons, chips. */
    val Elevated = Color(0xFF1B1C20)

    /** Highest overlay layer — dialogs, menus, floating elements. */
    val Overlay = Color(0xFF22242A)

    /** Subtle surface for thin dividers and borders. */
    val Subtle = Color(0xFF2A2C33)

    // ── Text ────────────────────────────────────────────────────────────

    /** Primary text — high contrast, readable on all surfaces. */
    val TextPrimary = Color(0xFFF0F0F5)

    /** Secondary text — subtitles, descriptions. */
    val TextSecondary = Color(0xFF9496A0)

    /** Tertiary text — timestamps, metadata, hints. */
    val TextTertiary = Color(0xFF5C5E6A)

    /** Text on accent-colored surfaces. */
    val TextOnAccent = Color(0xFF0B0B0E)

    // ── Accent (Modern Emerald) ─────────────────────────────────────────

    /** Primary accent — modern emerald green. */
    val Accent = Color(0xFF1ED760)

    /** Accent for pressed/active states. */
    val AccentPressed = Color(0xFF1AAE50)

    /** Subtle accent for containers and highlights. */
    val AccentSubtle = Color(0xFF1ED760).copy(alpha = 0.12f)

    /** Accent glow — used for artwork glow effects. */
    val AccentGlow = Color(0xFF1ED760).copy(alpha = 0.30f)

    // ── Secondary & Tertiary ────────────────────────────────────────────

    /** Secondary accent — violet for shuffle, repeat, special actions. */
    val Secondary = Color(0xFFA78BFA)

    /** Tertiary accent — cyan for subtle indicators. */
    val Tertiary = Color(0xFF22D3EE)

    // ── Semantic ────────────────────────────────────────────────────────

    /** Error / destructive actions. */
    val Error = Color(0xFFEF4444)

    /** Error container. */
    val ErrorContainer = Color(0xFFEF4444).copy(alpha = 0.12f)

    /** Success states. */
    val Success = Color(0xFF1ED760)

    /** Warning states. */
    val Warning = Color(0xFFFBBF24)

    // ── Dividers & Borders ─────────────────────────────────────────────

    /** Subtle divider between list items. */
    val Divider = Color(0xFF1E2028)

    /** Active border for focused inputs. */
    val BorderActive = Color(0xFF1ED760)

    // ── Progress ────────────────────────────────────────────────────────

    /** Track background (unfilled portion). */
    val ProgressTrack = Color(0xFF2A2C33)

    /** Filled portion — matches accent. */
    val ProgressFill = Color(0xFF1ED760)

    /** Thumb/handle for sliders. */
    val ProgressThumb = Color(0xFFFFFFFF)

    // ── Glass ───────────────────────────────────────────────────────────

    /** Glassmorphism background — semi-transparent with blur. */
    val GlassBackground = Color(0x14FFFFFF)

    /** Glass border — subtle white edge. */
    val GlassBorder = Color(0x1AFFFFFF)

    /** Glass surface for mini player, bottom nav. */
    val GlassSurface = Color(0x0DFFFFFF)

    // ── Dynamic Color Placeholders ──────────────────────────────────────
    // These are replaced at runtime with colors extracted from album artwork.

    /** Dynamic accent — extracted from current album artwork. */
    var DynamicAccent = Accent
        private set

    /** Dynamic glow — extracted from current album artwork. */
    var DynamicGlow = AccentGlow
        private set

    /** Dynamic gradient — extracted from current album artwork. */
    var DynamicGradient = listOf(Accent, Accent.copy(alpha = 0.6f))
        private set

    /** Update dynamic colors from album artwork palette. */
    fun updateDynamicColors(
        accent: Color = Accent,
        glow: Color = AccentGlow,
        gradient: List<Color> = listOf(Accent, Accent.copy(alpha = 0.6f)),
    ) {
        DynamicAccent = accent
        DynamicGlow = glow
        DynamicGradient = gradient
    }

    /** Reset dynamic colors to defaults. */
    fun resetDynamicColors() {
        DynamicAccent = Accent
        DynamicGlow = AccentGlow
        DynamicGradient = listOf(Accent, Accent.copy(alpha = 0.6f))
    }
}

// ── Legacy Aliases (for backward compatibility) ──────────────────────────

val Black get() = MFColors.Background
val DarkSurface get() = MFColors.Card
val DarkSurfaceVariant get() = MFColors.Elevated
val DarkSurfaceContainer get() = MFColors.Overlay
val GlassSurface get() = MFColors.GlassSurface
val CardSurface get() = MFColors.Card
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
    /** Background gradient — top-to-bottom depth. */
    val BackgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MFColors.Background,
            MFColors.Background.copy(alpha = 0.95f),
        )
    )

    /** Card gradient — subtle top-to-bottom for depth. */
    val CardGradient = Brush.verticalGradient(
        colors = listOf(
            MFColors.Card,
            MFColors.Card.copy(alpha = 0.98f),
        )
    )

    /** Accent gradient — for play buttons and highlights. */
    val AccentGradient = Brush.horizontalGradient(
        colors = listOf(
            MFColors.Accent,
            MFColors.Accent.copy(alpha = 0.8f),
        )
    )

    /** Glow gradient — radial from center for artwork effects. */
    fun glowGradient(color: Color) = Brush.radialGradient(
        colors = listOf(
            color.copy(alpha = 0.4f),
            color.copy(alpha = 0.0f),
        )
    )

    /** Dynamic gradient — based on album artwork colors. */
    val DynamicGradient = Brush.verticalGradient(
        colors = MFColors.DynamicGradient + listOf(MFColors.Background)
    )
}
