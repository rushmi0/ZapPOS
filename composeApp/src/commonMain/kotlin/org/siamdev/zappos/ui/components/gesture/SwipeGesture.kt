/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.components.gesture

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Fires a callback when the user swipes across this element further than [threshold].
 *
 * Pass only the directions you need; `null` directions are ignored.
 * Horizontal-only or vertical-only listeners leave the other axis free,
 * so e.g. `onSwipeRight` on a screen with a `LazyColumn` does not block vertical scrolling.
 * Listening on both axes uses a free drag and picks the dominant axis on release.
 *
 * ```
 * Modifier.swipe(onSwipeRight = onNavigateBack)
 * ```
 */
fun Modifier.swipe(
    enabled: Boolean = true,
    threshold: Dp = 40.dp,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    onSwipeUp: (() -> Unit)? = null,
    onSwipeDown: (() -> Unit)? = null,
): Modifier = composed {
    val left by rememberUpdatedState(onSwipeLeft)
    val right by rememberUpdatedState(onSwipeRight)
    val up by rememberUpdatedState(onSwipeUp)
    val down by rememberUpdatedState(onSwipeDown)

    val horizontal = onSwipeLeft != null || onSwipeRight != null
    val vertical = onSwipeUp != null || onSwipeDown != null

    pointerInput(enabled, threshold, horizontal, vertical) {
        if (!enabled || (!horizontal && !vertical)) return@pointerInput

        val thresholdPx = threshold.toPx()
        var dx = 0f
        var dy = 0f
        val reset = { dx = 0f; dy = 0f }

        val dispatch = {
            val isHorizontal = when {
                horizontal && vertical -> abs(dx) >= abs(dy)
                else -> horizontal
            }
            if (isHorizontal) {
                when {
                    dx > thresholdPx -> right?.invoke()
                    dx < -thresholdPx -> left?.invoke()
                }
            } else {
                when {
                    dy > thresholdPx -> down?.invoke()
                    dy < -thresholdPx -> up?.invoke()
                }
            }
            reset()
        }

        when {
            horizontal && vertical -> detectDragGestures(
                onDragStart = { reset() },
                onDrag = { change, amount ->
                    change.consume()
                    dx += amount.x
                    dy += amount.y
                },
                onDragEnd = dispatch,
                onDragCancel = reset,
            )

            horizontal -> detectHorizontalDragGestures(
                onDragStart = { reset() },
                onHorizontalDrag = { change, amount ->
                    change.consume()
                    dx += amount
                },
                onDragEnd = dispatch,
                onDragCancel = reset,
            )

            else -> detectVerticalDragGestures(
                onDragStart = { reset() },
                onVerticalDrag = { change, amount ->
                    change.consume()
                    dy += amount
                },
                onDragEnd = dispatch,
                onDragCancel = reset,
            )
        }
    }
}
