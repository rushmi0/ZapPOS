/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

/**
 * Lowers the release threshold of the swipe-back gesture for [content].
 *
 * The platform decides whether a released swipe goes back (iOS: still moving right or past 30% of
 * the width; Android: the system). This wrapper relays the platform's gesture to [content] through
 * its own dispatcher and turns a cancel into a completion once the swipe has reached
 * [threshold], unless the finger was moving back toward the edge.
 */
@Composable
fun SwipeBackThreshold(
    enabled: Boolean = true,
    threshold: Float = 0.15f,
    content: @Composable () -> Unit
) {
    val parent = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    if (!enabled || parent == null) {
        content()
        return
    }

    val relay = remember { ThresholdBackRelay(threshold) }

    DisposableEffect(parent, relay) {
        parent.addHandler(relay.handler)
        onDispose { relay.handler.remove() }
    }
    DisposableEffect(relay) {
        onDispose { relay.dispatcher.dispose() }
    }

    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides relay) {
        content()
    }
}

private class ThresholdBackRelay(
    private val threshold: Float
) : NavigationEventDispatcherOwner {

    override val navigationEventDispatcher: NavigationEventDispatcher = NavigationEventDispatcher()
    val dispatcher: NavigationEventDispatcher get() = navigationEventDispatcher

    private var lastProgress = 0f
    private var prevProgress = 0f

    /** Feeds the relayed gesture into the inner dispatcher. */
    private val input = RelayInput()

    private inner class RelayInput : NavigationEventInput() {
        override fun onHasEnabledHandlersChanged(hasEnabledHandlers: Boolean) {
            // Only intercept back while something inside can handle it,
            // so the platform fallback (e.g. closing the app) still works on the root screen.
            handler.isBackEnabled = hasEnabledHandlers
        }

        fun started(event: NavigationEvent) = dispatchOnBackStarted(event)
        fun progressed(event: NavigationEvent) = dispatchOnBackProgressed(event)
        fun completed() = dispatchOnBackCompleted()
        fun cancelled() = dispatchOnBackCancelled()
    }

    /** Receives the platform's gesture from the parent dispatcher. */
    val handler: NavigationEventHandler<NavigationEventInfo> = object : NavigationEventHandler<NavigationEventInfo>(
        initialInfo = NavigationEventInfo.None,
        isBackEnabled = false
    ) {
        override fun onBackStarted(event: NavigationEvent) {
            lastProgress = event.progress
            prevProgress = event.progress
            input.started(event)
        }

        override fun onBackProgressed(event: NavigationEvent) {
            prevProgress = lastProgress
            lastProgress = event.progress
            input.progressed(event)
        }

        override fun onBackCompleted() {
            input.completed()
        }

        override fun onBackCancelled() {
            val movingBack = lastProgress < prevProgress
            if (lastProgress >= threshold && !movingBack) input.completed() else input.cancelled()
        }
    }

    init {
        navigationEventDispatcher.addInput(input)
    }
}
