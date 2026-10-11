/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.screens.sale

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.orbitmvi.orbit.test.testWithInternalState
import org.siamdev.zappos.ui.testMenuItems
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MainMenuViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(fetch: suspend () -> List<MenuItem>) =
        MainMenuViewModel(fetchItems = fetch, fetchThumbnails = {})

    private val loaded = MainMenuViewModel.State().withItems(testMenuItems)

    @Test
    fun onCreate_showsLoadingThenCatalogue() = runTest {
        viewModel { delay(2_000); testMenuItems }.testWithInternalState(this) {
            runOnCreate()

            assertTrue(awaitInternalState().isLoading)
            val state = awaitInternalState()
            assertFalse(state.isLoading)
            assertEquals(testMenuItems, state.catalog.items)
            assertEquals("0.00", state.order.totalFiat)
        }
    }

    @Test
    fun loadFailure_stopsLoadingAndReportsError() = runTest {
        viewModel { delay(10); error("offline") }.testWithInternalState(this) {
            runOnCreate()

            assertTrue(awaitInternalState().isLoading)
            assertFalse(awaitInternalState().isLoading)
            expectSideEffect(MainMenuViewModel.SideEffect.Error("offline"))
        }
    }

    @Test
    fun reload_cancelsInFlightLoadSoStaleResultIsNeverApplied() = runTest {
        var calls = 0
        val stale = listOf(MenuItem(99, "", "Stale", "1.00", "1"))
        val vm = viewModel {
            calls++
            if (calls == 1) {
                delay(5_000); stale
            } else {
                delay(10); testMenuItems
            }
        }
        vm.testWithInternalState(this) {
            runOnCreate()
            assertTrue(awaitInternalState().isLoading)

            containerHost.reloadProductsData()
            assertEquals(testMenuItems, awaitInternalState().catalog.items)

            advanceUntilIdle()
            expectNoItems()
        }
        assertEquals(2, calls)
    }

    @Test
    fun cartOperations_updateSelectionAndTotals() = runTest {
        viewModel { testMenuItems }.testWithInternalState(this, initialState = loaded) {
            containerHost.addItem(1)
            awaitInternalState().let {
                assertEquals(listOf(1), it.order.selectedKeys)
                assertEquals("70.00", it.order.totalFiat)
                assertEquals("17,500.00", it.order.totalSat)
            }

            containerHost.addItem(3)
            awaitInternalState().let {
                assertEquals(listOf(1, 3), it.order.selectedKeys)
                assertEquals("170.00", it.order.totalFiat)
            }

            containerHost.setItemCount(3, 3u)
            assertEquals("370.00", awaitInternalState().order.totalFiat)

            containerHost.reduceItem(1)
            awaitInternalState().let {
                assertEquals(listOf(3), it.order.selectedKeys)
                assertEquals(listOf(it.catalog.items[2]), it.selectedItems)
            }
        }
    }

    @Test
    fun reduceItem_neverGoesBelowZero() = runTest {
        viewModel { testMenuItems }.testWithInternalState(this, initialState = loaded) {
            containerHost.reduceItem(1).join()
            expectNoItems()
        }
    }

    @Test
    fun clearAllItems_resetsCountsAndOrder() = runTest {
        val withCart = loaded.withItemCount(2) { 4u }
        viewModel { testMenuItems }.testWithInternalState(this, initialState = withCart) {
            containerHost.clearAllItems()

            val state = awaitInternalState()
            assertTrue(state.catalog.items.all { it.count == 0u })
            assertEquals(MainMenuViewModel.State.Order(), state.order)
        }
    }
}
