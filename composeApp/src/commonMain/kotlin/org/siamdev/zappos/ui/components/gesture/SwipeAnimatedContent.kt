/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.components.gesture

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

private const val PageDurationMs = 320

/**
 * [AnimatedContent] with an interactive swipe-back gesture.
 *
 * Pages slide in from the right when [depth] grows and slide out to the right when it shrinks.
 * Dragging a page to the right moves it with the finger; releasing past [backThreshold]
 * (fraction of the width) or flinging faster than [flingVelocity] calls [onSwipeBack],
 * and the page keeps sliding out from where the finger left it. A short drag springs back.
 *
 * Only horizontal drags are captured, so vertical scrolling inside pages still works.
 *
 * ```
 * SwipeAnimatedContent(
 *     targetState = selected,
 *     depth = { if (it == null) 0 else 1 },
 *     canSwipeBack = { it != null },
 *     onSwipeBack = { selected = null },
 * ) { current -> ... }
 * ```
 */
@Composable
fun <S> SwipeAnimatedContent(
    targetState: S,
    depth: (S) -> Int,
    onSwipeBack: (S) -> Unit,
    modifier: Modifier = Modifier,
    canSwipeBack: (S) -> Boolean = { true },
    backThreshold: Float = 0.35f,
    flingVelocity: Dp = 600.dp,
    content: @Composable (S) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            val forward = depth(targetState) >= depth(initialState)
            val slide = tween<IntOffset>(PageDurationMs, easing = FastOutSlowInEasing)
            val fade = tween<Float>(PageDurationMs, easing = FastOutSlowInEasing)
            if (forward) {
                (slideInHorizontally(slide) { it } + fadeIn(fade)) togetherWith
                        (slideOutHorizontally(slide) { -it / 4 } + fadeOut(fade))
            } else {
                (slideInHorizontally(slide) { -it / 4 } + fadeIn(fade)) togetherWith
                        (slideOutHorizontally(slide) { it } + fadeOut(fade))
            }.apply {
                // The deeper page always draws on top, both when pushing and when popping.
                targetContentZIndex = if (forward) 1f else -1f
            }
        },
        label = "SwipeAnimatedContent",
    ) { page ->
        SwipeBackPage(
            enabled = canSwipeBack(page),
            backThreshold = backThreshold,
            flingVelocity = flingVelocity,
            onBack = { onSwipeBack(page) },
        ) {
            content(page)
        }
    }
}

/** One page that follows a rightward drag and either commits [onBack] or springs back. */
@Composable
private fun SwipeBackPage(
    enabled: Boolean,
    backThreshold: Float,
    flingVelocity: Dp,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val currentOnBack by rememberUpdatedState(onBack)
    val flingPx = with(LocalDensity.current) { flingVelocity.toPx() }
    var width by remember { mutableIntStateOf(0) }

    // Per-page offset: kept on the exiting page so its slide-out continues from the finger.
    var dragX by remember { mutableFloatStateOf(0f) }

    val dragState = rememberDraggableState { delta ->
        dragX = (dragX + delta).coerceIn(0f, width.toFloat())
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { width = it.width }
            .draggable(
                state = dragState,
                orientation = Orientation.Horizontal,
                enabled = enabled,
                onDragStopped = { velocity ->
                    val passed = width > 0 && dragX > width * backThreshold
                    if (passed || velocity > flingPx) {
                        currentOnBack()
                    } else {
                        animate(
                            initialValue = dragX,
                            targetValue = 0f,
                            initialVelocity = velocity,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        ) { value, _ -> dragX = value }
                    }
                },
            )
            .graphicsLayer {
                translationX = dragX
                if (width > 0) alpha = 1f - (dragX / width) * 0.25f
            }
    ) {
        content()
    }
}
