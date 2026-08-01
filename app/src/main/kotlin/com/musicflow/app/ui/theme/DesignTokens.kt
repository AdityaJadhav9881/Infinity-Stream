package com.musicflow.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * MusicFlow Premium Design Tokens
 *
 * Unified design system for a flagship 2026 app.
 * Consistent spacing, radii, elevation, typography.
 * Everything should feel handcrafted, soft, luxurious.
 */
object MFTokens {
    // ── Spacing ──────────────────────────────────────────────────────────
    val ScreenHorizontalPadding = 22.dp
    val CardSpacing = 14.dp
    val SectionSpacing = 32.dp
    val CardPadding = 18.dp
    val SmallCardPadding = 14.dp
    val ItemSpacing = 5.dp
    val CarouselSpacing = 16.dp
    val VerticalSpacing = 10.dp
    val SmallSpacing = 7.dp
    val MediumSpacing = 14.dp
    val LargeSpacing = 24.dp
    val ExtraLargeSpacing = 36.dp

    // ── Corner Radius ────────────────────────────────────────────────────
    /** Small radius — chips, badges, tags. */
    val SmallRadius = RoundedCornerShape(10.dp)

    /** Medium radius — cards, inputs, list items. */
    val MediumRadius = RoundedCornerShape(18.dp)

    /** Large radius — bottom sheets, dialogs, floating panels. */
    val LargeRadius = RoundedCornerShape(24.dp)

    /** Extra large radius — player artwork, hero elements. */
    val XLRadius = RoundedCornerShape(28.dp)

    /** Pill shape — buttons, mini player, nav items. */
    val PillRadius = RoundedCornerShape(100.dp)

    // ── Elevation / Depth ────────────────────────────────────────────────
    val ElevationNone = 0.dp
    val ElevationLow = 3.dp
    val ElevationMedium = 6.dp
    val ElevationHigh = 14.dp
    val ElevationFloating = 20.dp

    // ── Animation Durations ──────────────────────────────────────────────
    val DurationFast = 120
    val DurationNormal = 250
    val DurationSlow = 400
    val DurationVerySlow = 650

    // ── Typography Sizes ─────────────────────────────────────────────────
    val HeroTextSize = 36.sp
    val SectionHeaderTextSize = 22.sp
    val CardTitleSize = 14.sp
    val CardSubtitleSize = 12.sp
    val BadgeTextSize = 10.sp

    // ── Component Sizes ──────────────────────────────────────────────────
    val MiniPlayerHeight = 64.dp
    val BottomNavHeight = 62.dp
    val QuickActionHeight = 84.dp
    val PlaylistCardWidth = 156.dp
    val PlaylistCardHeight = 180.dp
    val RecentCardWidth = 144.dp
    val PlayerArtworkSize = 288.dp
    val MiniPlayerArtworkSize = 46.dp
    val IconSize = 22.dp
    val IconSizeSmall = 18.dp
    val IconSizeLarge = 28.dp
}
