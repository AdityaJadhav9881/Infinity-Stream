package com.musicflow.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * MusicFlow Design Tokens
 *
 * Reusable design system values used across all components.
 * Everything is defined here — nothing is hardcoded elsewhere.
 */
object MFTokens {
    // ── Spacing ──────────────────────────────────────────────────────────
    /** Standard horizontal padding for screens. */
    val ScreenHorizontalPadding = 20.dp

    /** Spacing between cards. */
    val CardSpacing = 12.dp

    /** Spacing between sections. */
    val SectionSpacing = 28.dp

    /** Spacing inside cards. */
    val CardPadding = 16.dp

    /** Spacing inside small cards. */
    val SmallCardPadding = 12.dp

    /** Spacing between items in a list. */
    val ItemSpacing = 4.dp

    /** Spacing for carousel items. */
    val CarouselSpacing = 14.dp

    /** Standard vertical spacing. */
    val VerticalSpacing = 8.dp

    /** Small spacing for tight layouts. */
    val SmallSpacing = 4.dp

    /** Medium spacing. */
    val MediumSpacing = 12.dp

    /** Large spacing. */
    val LargeSpacing = 20.dp

    /** Extra large spacing. */
    val ExtraLargeSpacing = 32.dp

    // ── Corner Radius ────────────────────────────────────────────────────
    /** Small radius for chips and badges. */
    val SmallRadius = RoundedCornerShape(8.dp)

    /** Medium radius for cards. */
    val MediumRadius = RoundedCornerShape(16.dp)

    /** Large radius for bottom sheets and dialogs. */
    val LargeRadius = RoundedCornerShape(24.dp)

    /** Extra large radius for player artwork. */
    val XLRadius = RoundedCornerShape(32.dp)

    /** Pill shape for buttons and mini player. */
    val PillRadius = RoundedCornerShape(100.dp)

    // ── Elevation / Depth ────────────────────────────────────────────────
    /** No elevation. */
    val ElevationNone = 0.dp

    /** Subtle elevation for cards at rest. */
    val ElevationLow = 2.dp

    /** Medium elevation for interactive cards. */
    val ElevationMedium = 6.dp

    /** High elevation for floating elements. */
    val ElevationHigh = 12.dp

    // ── Animation Durations ──────────────────────────────────────────────
    /** Fast animation — ripples, micro-interactions. */
    val DurationFast = 150

    /** Normal animation — page transitions, card clicks. */
    val DurationNormal = 300

    /** Slow animation — hero transitions, morph effects. */
    val DurationSlow = 500

    /** Very slow animation — complex multi-step animations. */
    val DurationVerySlow = 800

    // ── Typography Sizes (convenience) ───────────────────────────────────
    /** Hero greeting text. */
    val HeroTextSize = 36.sp

    /** Section header text. */
    val SectionHeaderTextSize = 22.sp

    /** Card title text. */
    val CardTitleSize = 15.sp

    /** Card subtitle text. */
    val CardSubtitleSize = 12.sp

    /** Badge/chip text. */
    val BadgeTextSize = 11.sp

    // ── Component Sizes ──────────────────────────────────────────────────
    /** Mini player height. */
    val MiniPlayerHeight = 64.dp

    /** Bottom navigation height. */
    val BottomNavHeight = 72.dp

    /** Quick action card height. */
    val QuickActionHeight = 88.dp

    /** Playlist card width. */
    val PlaylistCardWidth = 160.dp

    /** Playlist card height. */
    val PlaylistCardHeight = 180.dp

    /** Recent card width. */
    val RecentCardWidth = 140.dp

    /** Artwork size for player. */
    val PlayerArtworkSize = 300.dp

    /** Artwork size for mini player. */
    val MiniPlayerArtworkSize = 48.dp

    /** Icon size for standard icons. */
    val IconSize = 24.dp

    /** Small icon size. */
    val IconSizeSmall = 18.dp

    /** Large icon size. */
    val IconSizeLarge = 32.dp
}
