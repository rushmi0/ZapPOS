/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.screens.splash

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.orbitmvi.orbit.OrbitContainer
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import org.siamdev.zappos.cache.ImagePreloader
import org.siamdev.zappos.cache.ThumbnailSection
import org.siamdev.zappos.ui.UiContainerSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val TAG = "SplashVM"

class SplashViewModel(
    private val splashDuration: Duration = 3000.milliseconds,
    private val preloadTimeout: Duration = 5.seconds,
    private val preloadImages: suspend (onProgress: (Float) -> Unit) -> Unit = { onProgress ->
        ImagePreloader.preloadMenuItems(section = ThumbnailSection.PRODUCTS, onProgress = onProgress)
    },
) : ViewModel(), OrbitContainerHost<SplashViewModel.State, SplashViewModel.State, SplashViewModel.SideEffect> {

    data class State(
        val isReady: Boolean = false,
        val isDone: Boolean = false,
        val preloadProgress: Float = 0f
    )

    sealed interface SideEffect {
        data object NavigateAway : SideEffect
    }

    // onCreate runs once, when the splash UI first subscribes.
    override val container: OrbitContainer<State, State, SideEffect> =
        orbitContainer(State(), buildSettings = UiContainerSettings) { runSplash() }

    private suspend fun Syntax<State, SideEffect>.runSplash() = coroutineScope {
        println("[$TAG] Splash started")
        val delayJob = launch {
            delay(splashDuration)
            reduce { state.copy(isDone = true) }
            println("[$TAG] Splash delay done")
        }
        val preloadJob = launch {
            println("[$TAG] Preload begin")
            preloadImages { progress ->
                intent { reduce { state.copy(preloadProgress = progress) } }
                val pct = (progress * 100).toInt()
                if (pct % 20 == 0) println("[$TAG] Preload $pct%")
            }
            println("[$TAG] Preload end")
        }

        delayJob.join()
        println("[$TAG] Splash delay done – waiting for preload (max $preloadTimeout)")
        // A slow preload must not hold the splash; it keeps running in the background.
        withTimeoutOrNull(preloadTimeout) { preloadJob.join() }

        println("[$TAG] Ready – navigating")
        reduce { state.copy(isReady = true) }
        postSideEffect(SideEffect.NavigateAway)
    }
}
