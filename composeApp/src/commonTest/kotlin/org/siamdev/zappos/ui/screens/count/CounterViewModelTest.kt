/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.screens.count

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun plusMinusReset() = runTest {
        CounterViewModel().testWithInternalState(this) {
            containerHost.plus()
            expectInternalState { copy(count = 1) }
            containerHost.plus()
            expectInternalState { copy(count = 2) }
            containerHost.minus()
            expectInternalState { copy(count = 1) }
            containerHost.reset()
            expectInternalState { copy(count = 0) }
        }
    }
}
