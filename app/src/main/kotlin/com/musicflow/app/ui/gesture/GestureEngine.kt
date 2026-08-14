package com.musicflow.app.ui.gesture

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Centralized gesture detection engine.
 *
 * Provides Modifier extension functions that attach gesture detection
 * callbacks. Each method returns a new Modifier with the specified
 * gesture detector attached.
 */
object GestureEngine {

    /**
     * Detects swipe gestures in all four directions.
     *
     * @param onSwipeUp Called when the user swipes upward.
     * @param onSwipeDown Called when the user swipes downward.
     * @param onSwipeLeft Called when the user swipes left.
     * @param onSwipeRight Called when the user swipes right.
     * @param threshold Minimum drag distance in pixels to trigger a swipe.
     */
    fun Modifier.detectSwipes(
        onSwipeUp: (() -> Unit)? = null,
        onSwipeDown: (() -> Unit)? = null,
        onSwipeLeft: (() -> Unit)? = null,
        onSwipeRight: (() -> Unit)? = null,
        threshold: Float = 100f,
    ): Modifier = this.pointerInput(Unit) {
        var swipeFired = false
        detectDragGestures(
            onDragEnd = { swipeFired = false },
            onDragCancel = { swipeFired = false },
            onDrag = { change, dragAmount ->
                change.consume()
                if (swipeFired) return@detectDragGestures
                val (x, y) = dragAmount
                if (kotlin.math.abs(x) > threshold || kotlin.math.abs(y) > threshold) {
                    swipeFired = true
                    if (kotlin.math.abs(x) > kotlin.math.abs(y)) {
                        if (x > 0) onSwipeRight?.invoke() else onSwipeLeft?.invoke()
                    } else {
                        if (y > 0) onSwipeDown?.invoke() else onSwipeUp?.invoke()
                    }
                }
            },
        )
    }

    /**
     * Detects a long press gesture.
     *
     * @param onLongPress Called when the user long-presses.
     * @param durationMs Minimum press duration in milliseconds.
     */
    fun Modifier.detectLongPress(
        onLongPress: () -> Unit,
        durationMs: Long = 500L,
    ): Modifier = this.pointerInput(Unit) {
        detectTapGestures(
            onLongPress = { onLongPress() },
        )
    }

    /**
     * Detects a tap gesture.
     *
     * @param onTap Called when the user taps.
     */
    fun Modifier.detectTap(
        onTap: () -> Unit,
    ): Modifier = this.pointerInput(Unit) {
        detectTapGestures(
            onTap = { onTap() },
        )
    }

    /**
     * Detects a pinch gesture (zoom in/out).
     *
     * @param onPinch Called with a scale factor (>1 = zoom in, <1 = zoom out).
     */
    fun Modifier.detectPinch(
        onPinch: (scale: Float) -> Unit,
    ): Modifier = this.pointerInput(Unit) {
        var initialDistance = 0f
        detectDragGestures(
            onDragStart = { initialDistance = 0f },
            onDragEnd = {},
            onDragCancel = {},
            onDrag = { change, dragAmount ->
                change.consume()
                val currentDistance = kotlin.math.sqrt(
                    dragAmount.x * dragAmount.x + dragAmount.y * dragAmount.y
                )
                if (initialDistance == 0f) {
                    initialDistance = currentDistance
                } else if (initialDistance > 0f) {
                    val scale = currentDistance / initialDistance
                    onPinch(scale)
                    initialDistance = currentDistance
                }
            },
        )
    }

    /**
     * Detects horizontal swipes only (left/right).
     * Useful for track skipping gestures.
     *
     * @param onSwipeLeft Called when swiped left.
     * @param onSwipeRight Called when swiped right.
     * @param threshold Minimum horizontal drag distance.
     */
    fun Modifier.detectHorizontalSwipe(
        onSwipeLeft: (() -> Unit)? = null,
        onSwipeRight: (() -> Unit)? = null,
        threshold: Float = 80f,
    ): Modifier = this.pointerInput(Unit) {
        var swipeFired = false
        detectDragGestures(
            onDragEnd = { swipeFired = false },
            onDragCancel = { swipeFired = false },
            onDrag = { change, dragAmount ->
                change.consume()
                if (swipeFired) return@detectDragGestures
                if (kotlin.math.abs(dragAmount.x) > threshold &&
                    kotlin.math.abs(dragAmount.x) > kotlin.math.abs(dragAmount.y) * 2f
                ) {
                    swipeFired = true
                    if (dragAmount.x > 0) onSwipeRight?.invoke() else onSwipeLeft?.invoke()
                }
            },
        )
    }

    /**
     * Detects vertical swipes only (up/down).
     * Useful for seek bar or volume gestures.
     *
     * @param onSwipeUp Called when swiped up.
     * @param onSwipeDown Called when swiped down.
     * @param threshold Minimum vertical drag distance.
     */
    fun Modifier.detectVerticalSwipe(
        onSwipeUp: (() -> Unit)? = null,
        onSwipeDown: (() -> Unit)? = null,
        threshold: Float = 80f,
    ): Modifier = this.pointerInput(Unit) {
        var swipeFired = false
        detectDragGestures(
            onDragEnd = { swipeFired = false },
            onDragCancel = { swipeFired = false },
            onDrag = { change, dragAmount ->
                change.consume()
                if (swipeFired) return@detectDragGestures
                if (kotlin.math.abs(dragAmount.y) > threshold &&
                    kotlin.math.abs(dragAmount.y) > kotlin.math.abs(dragAmount.x) * 2f
                ) {
                    swipeFired = true
                    if (dragAmount.y > 0) onSwipeDown?.invoke() else onSwipeUp?.invoke()
                }
            },
        )
    }
}
