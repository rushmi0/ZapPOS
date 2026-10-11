/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.screens.sale.checkout

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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val items = listOf(CheckoutItem("Mocha", 2u, "70.00", "17,500"))
    private val orderState = CheckoutViewModel.State().withOrder(items, totalFiat = "140.00", totalSat = "35,000")

    @Test
    fun syncFromMenu_appliesDefaultTaxToGrandTotals() = runTest {
        CheckoutViewModel().testWithInternalState(this) {
            containerHost.syncFromMenu(items, fiat = "140.00", sat = "35,000")

            val state = awaitInternalState()
            assertEquals(items, state.order.items)
            assertEquals("149.80", state.order.grandTotalFiat)
            assertEquals("37,450.00", state.order.grandTotalSat)
        }
    }

    @Test
    fun setTax_recomputesGrandTotals() = runTest {
        CheckoutViewModel().testWithInternalState(this, initialState = orderState) {
            containerHost.setTax(0f)

            val state = awaitInternalState()
            assertEquals(0f, state.order.taxPercent)
            assertEquals("140.00", state.order.grandTotalFiat)
        }
    }

    @Test
    fun confirmPayment_withoutMethod_staysOnCurrentStep() = runTest {
        CheckoutViewModel().testWithInternalState(this, initialState = orderState) {
            containerHost.confirmPayment().join()
            expectNoItems()
        }
    }

    @Test
    fun confirmPayment_routesCashToCalculatorAndOthersToProcessing() = runTest {
        CheckoutViewModel().testWithInternalState(this, initialState = orderState) {
            containerHost.selectMethod(PaymentMethod.CASH)
            assertEquals(PaymentMethod.CASH, awaitInternalState().payment.selectedMethod)
            containerHost.confirmPayment()
            assertEquals(CheckoutStep.CASH_CALCULATOR, awaitInternalState().step)

            containerHost.selectMethod(PaymentMethod.PROMPT_PAY)
            awaitInternalState()
            containerHost.confirmPayment()
            assertEquals(CheckoutStep.PROCESSING, awaitInternalState().step)
        }
    }

    @Test
    fun cashDigits_replaceLeadingZeroAndRejectSecondDecimalPoint() = runTest {
        CheckoutViewModel().testWithInternalState(this, initialState = orderState) {
            containerHost.appendCashDigit("0")
            assertEquals("0", awaitInternalState().payment.receivedAmount)
            containerHost.appendCashDigit("5")
            assertEquals("5", awaitInternalState().payment.receivedAmount)
            containerHost.appendCashDigit(".")
            assertEquals("5.", awaitInternalState().payment.receivedAmount)
            containerHost.appendCashDigit(".").join()
            expectNoItems()
            containerHost.deleteCashDigit()
            assertEquals("5", awaitInternalState().payment.receivedAmount)
        }
    }

    @Test
    fun confirmCash_onlyProceedsOnceReceivedCoversTotal() = runTest {
        CheckoutViewModel().testWithInternalState(this, initialState = orderState) {
            containerHost.appendQuickAmount("100")
            val short = awaitInternalState()
            assertEquals("100", short.payment.receivedAmount)
            assertFalse(short.payment.isChangeValid)

            containerHost.confirmCash().join()
            expectNoItems()

            containerHost.appendQuickAmount("50")
            val enough = awaitInternalState()
            assertTrue(enough.payment.isChangeValid)
            assertEquals(10.0, enough.payment.changeAmount)

            containerHost.confirmCash()
            assertEquals(CheckoutStep.PROCESSING, awaitInternalState().step)
        }
    }

    @Test
    fun reset_returnsToOrderStepAndClearsPayment() = runTest {
        val inProgress = orderState.withReceivedAmount("200").copy(
            step = CheckoutStep.SUCCESS,
            payment = orderState.withReceivedAmount("200").payment.copy(selectedMethod = PaymentMethod.CASH)
        )
        CheckoutViewModel().testWithInternalState(this, initialState = inProgress) {
            containerHost.reset()

            val state = awaitInternalState()
            assertEquals(CheckoutStep.ORDER, state.step)
            assertEquals(null, state.payment.selectedMethod)
            assertEquals("", state.payment.receivedAmount)
            assertEquals(items, state.order.items)
        }
    }
}
