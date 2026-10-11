/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.screens.splash

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun navigatesAwayAfterSplashDelayOncePreloadIsDone() = runTest {
        val vm = SplashViewModel(preloadImages = { onProgress ->
            onProgress(0.5f)
            delay(100)
            onProgress(1f)
        })
        vm.testWithInternalState(this) {
            runOnCreate()

            assertEquals(0.5f, awaitInternalState().preloadProgress)
            assertEquals(1f, awaitInternalState().preloadProgress)
            assertTrue(awaitInternalState().isDone)
            assertEquals(3_000, currentTime)
            assertTrue(awaitInternalState().isReady)
            expectSideEffect(SplashViewModel.SideEffect.NavigateAway)
        }
    }

    @Test
    fun slowPreload_doesNotHoldTheSplashPastTheTimeout() = runTest {
        val vm = SplashViewModel(preloadImages = { delay(60_000) })
        vm.testWithInternalState(this) {
            runOnCreate()

            assertTrue(awaitInternalState().isDone)
            assertTrue(awaitInternalState().isReady)
            assertEquals(8_000, currentTime) // 3 s splash + 5 s preload timeout
            expectSideEffect(SplashViewModel.SideEffect.NavigateAway)

            // The preload itself keeps running in the background.
            cancelAndIgnoreRemainingItems()
        }
    }
}
