/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.screens.sale.checkout

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainer
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import org.siamdev.zappos.ui.UiContainerSettings
import org.siamdev.zappos.ui.screens.sale.MenuItem

enum class PaymentMethod(val label: String) {
    NFC_LIGHTNING("NFC Lightning Card"),
    BITCOIN_LIGHTNING("Bitcoin Lightning"),
    PROMPT_PAY("Prompt Pay"),
    CASH("Cash")
}

enum class CheckoutStep {
    ORDER, SELECT_PAYMENT, CASH_CALCULATOR, PROCESSING, SUCCESS
}

data class CheckoutItem(
    val name: String,
    val count: UInt,
    val priceBaht: String,
    val priceSat: String
)

fun MenuItem.toCheckoutItem() = CheckoutItem(name = name, count = count, priceBaht = priceBaht, priceSat = priceSat)

/** The checkout wizard: order review → payment method → cash / processing → success. */
class CheckoutViewModel :
    ViewModel(),
    OrbitContainerHost<CheckoutViewModel.State, CheckoutViewModel.State, Nothing> {

    data class State(
        val step: CheckoutStep = CheckoutStep.ORDER,
        val order: Order = Order(),
        val payment: Payment = Payment()
    ) {
        data class Order(
            val items: List<CheckoutItem> = emptyList(),
            val totalFiat: String = "0.00",
            val totalSat: String = "0",
            val taxPercent: Float = 7f,
            val grandTotalFiat: String = "0.00",
            val grandTotalSat: String = "0"
        )

        data class Payment(
            val selectedMethod: PaymentMethod? = null,
            val receivedAmount: String = "",
            val changeAmount: Double = 0.0,
            val isChangeValid: Boolean = false
        )

        fun withOrder(items: List<CheckoutItem>, totalFiat: String, totalSat: String): State =
            copy(order = order.copy(items = items, totalFiat = totalFiat, totalSat = totalSat)).recomputed()

        fun withReceivedAmount(amount: String): State =
            copy(payment = payment.copy(receivedAmount = amount)).recomputed()

        /** Re-derives grand totals and change from the order, tax rate and received cash. */
        fun recomputed(): State {
            val subtotalFiat = order.totalFiat.replace(",", "").toDoubleOrNull() ?: 0.0
            val subtotalSat = order.totalSat.replace(",", "").toDoubleOrNull() ?: 0.0
            val tax = order.taxPercent / 100f
            val received = payment.receivedAmount.replace(",", "").toDoubleOrNull() ?: 0.0
            val change = (received - subtotalFiat).coerceAtLeast(0.0)
            return copy(
                order = order.copy(
                    grandTotalFiat = formatDouble(subtotalFiat * (1 + tax)),
                    grandTotalSat = formatDouble(subtotalSat * (1 + tax))
                ),
                payment = payment.copy(
                    changeAmount = change,
                    isChangeValid = received >= subtotalFiat
                )
            )
        }
    }

    override val container: OrbitContainer<State, State, Nothing> =
        orbitContainer(State(), buildSettings = UiContainerSettings)

    fun syncFromMenu(items: List<CheckoutItem>, fiat: String, sat: String) = intent {
        reduce { state.withOrder(items, fiat, sat) }
    }

    fun setTax(percent: Float) = intent {
        reduce { state.copy(order = state.order.copy(taxPercent = percent)).recomputed() }
    }

    fun openSelectPayment() = intent { reduce { state.copy(step = CheckoutStep.SELECT_PAYMENT) } }

    fun backToOrder() = intent { reduce { state.copy(step = CheckoutStep.ORDER) } }

    fun backToSelectPayment() = intent { reduce { state.copy(step = CheckoutStep.SELECT_PAYMENT) } }

    fun selectMethod(method: PaymentMethod) = intent {
        reduce { state.copy(payment = state.payment.copy(selectedMethod = method)) }
    }

    fun confirmPayment() = intent {
        reduce {
            when (state.payment.selectedMethod) {
                null -> state
                PaymentMethod.CASH -> state.copy(step = CheckoutStep.CASH_CALCULATOR)
                else -> state.copy(step = CheckoutStep.PROCESSING)
            }
        }
    }

    fun appendCashDigit(digit: String) = intent {
        reduce {
            val current = state.payment.receivedAmount
            when {
                digit == "." && current.contains(".") -> state
                digit != "." && (current == "0" || current.isEmpty()) -> state.withReceivedAmount(digit)
                else -> state.withReceivedAmount(current + digit)
            }
        }
    }

    fun deleteCashDigit() = intent {
        reduce { state.withReceivedAmount(state.payment.receivedAmount.dropLast(1)) }
    }

    fun clearReceivedAmount() = intent { reduce { state.withReceivedAmount("") } }

    fun appendQuickAmount(amount: String) = intent {
        val add = amount.toDoubleOrNull() ?: return@intent
        reduce {
            val current = state.payment.receivedAmount.replace(",", "").toDoubleOrNull() ?: 0.0
            val result = current + add
            val next = if (result == result.toLong().toDouble()) {
                result.toLong().toString()
            } else {
                val intPart = result.toLong()
                val decPart = ((result - intPart) * 100).toLong()
                "$intPart.${decPart.toString().padStart(2, '0')}"
            }
            state.withReceivedAmount(next)
        }
    }

    fun confirmCash() = intent {
        reduce { if (state.payment.isChangeValid) state.copy(step = CheckoutStep.PROCESSING) else state }
    }

    fun confirmProcessing() = intent { reduce { state.copy(step = CheckoutStep.SUCCESS) } }

    fun reset() = intent {
        reduce {
            state.copy(
                step = CheckoutStep.ORDER,
                payment = state.payment.copy(selectedMethod = null, receivedAmount = "")
            ).recomputed()
        }
    }
}

internal fun formatDouble(value: Double): String {
    val intPart = value.toLong()
    val decPart = ((value - intPart) * 100).toInt()
    val intStr = intPart.toString().reversed().chunked(3).joinToString(",").reversed()
    return "$intStr.${decPart.toString().padStart(2, '0')}"
}