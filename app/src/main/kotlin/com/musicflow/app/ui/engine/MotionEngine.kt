package com.musicflow.app.ui.engine

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing

/**
 * Centralized animation constants for the MusicFlow UI.
 *
 * All motion parameters live here to ensure visual consistency
 * and make it trivial to tune the entire app's feel from one place.
 */
object MotionEngine {

    // ── Duration Constants ──────────────────────────────────────────────

    /** Fast transition — micro-interactions, button feedback. */
    const val FAST_DURATION_MS = 120

    /** Normal transition — card presses, toggles, standard navigation. */
    const val NORMAL_DURATION_MS = 250

    /** Slow transition — page-level transitions, drawer open/close. */
    const val SLOW_DURATION_MS = 400

    /** Very slow transition — dramatic reveals, hero animations. */
    const val VERY_SLOW_DURATION_MS = 650

    // ── Easing Curves ──────────────────────────────────────────────────

    /** Standard easing — balanced, general-purpose. */
    val StandardEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    /** Emphasized easing — more pronounced, for hero/featured elements. */
    val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    /** Decelerate — entering elements slow to a stop. */
    val StandardDecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f)

    /** Accelerate — exiting elements speed up from rest. */
    val StandardAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f)

    /** Linear — constant speed, for looping continuous animations. */
    val LinearEasingValue: androidx.compose.animation.core.Easing = LinearEasing

    /** Fast-out-slow-in — Material Design standard. */
    val FastOutSlowInEasingValue: androidx.compose.animation.core.Easing = FastOutSlowInEasing

    // ── Spring Profiles ────────────────────────────────────────────────

    /** Bouncy spring — playful, overshooting animations. */
    fun <T> bouncySpring() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    )

    /** Gentle spring — smooth, subtle motion. */
    fun <T> gentleSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessVeryLow,
    )

    /** Snappy spring — quick response, minimal overshoot. */
    fun <T> snappySpring() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    /** Stiff spring — fast, rigid motion with almost no overshoot. */
    fun <T> stiffSpring() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh,
    )

    // ── Page Transitions ───────────────────────────────────────────────

    /** Slide in from right — standard forward navigation. */
    val SlideInFromRight = slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(
            durationMillis = NORMAL_DURATION_MS,
            easing = EmphasizedEasing,
        ),
    )

    /** Slide out to left — standard forward navigation exit. */
    val SlideOutToLeft = slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth / 3 },
        animationSpec = tween(
            durationMillis = NORMAL_DURATION_MS,
            easing = StandardDecelerateEasing,
        ),
    )

    /** Slide in from left — back navigation. */
    val SlideInFromLeft = slideInHorizontally(
        initialOffsetX = { fullWidth -> -fullWidth / 3 },
        animationSpec = tween(
            durationMillis = NORMAL_DURATION_MS,
            easing = StandardDecelerateEasing,
        ),
    )

    /** Slide out to right — back navigation exit. */
    val SlideOutToRight = slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(
            durationMillis = NORMAL_DURATION_MS,
            easing = EmphasizedEasing,
        ),
    )

    /** Fade in — general-purpose appearance. */
    val FadeIn = fadeIn(
        animationSpec = tween(
            durationMillis = NORMAL_DURATION_MS,
            easing = StandardDecelerateEasing,
        ),
    )

    /** Fade out — general-purpose disappearance. */
    val FadeOut = fadeOut(
        animationSpec = tween(
            durationMillis = NORMAL_DURATION_MS,
            easing = StandardAccelerateEasing,
        ),
    )

    /** Fast fade in — for overlays, toasts, ephemeral UI. */
    val FastFadeIn = fadeIn(
        animationSpec = tween(
            durationMillis = FAST_DURATION_MS,
            easing = StandardDecelerateEasing,
        ),
    )

    /** Fast fade out — for overlays, toasts, ephemeral UI. */
    val FastFadeOut = fadeOut(
        animationSpec = tween(
            durationMillis = FAST_DURATION_MS,
            easing = StandardAccelerateEasing,
        ),
    )

    // ── Shared Element Transitions ─────────────────────────────────────

    /** Duration spec for shared element transitions. */
    const val SHARED_ELEMENT_DURATION_MS = SLOW_DURATION_MS

    /** Transition spec for shared element enter. */
    val SharedElementEnterTransition = fadeIn(
        animationSpec = tween(
            durationMillis = SHARED_ELEMENT_DURATION_MS,
            easing = EmphasizedEasing,
        ),
    )

    /** Transition spec for shared element exit. */
    val SharedElementExitTransition = fadeOut(
        animationSpec = tween(
            durationMillis = SHARED_ELEMENT_DURATION_MS,
            easing = StandardAccelerateEasing,
        ),
    )
}
