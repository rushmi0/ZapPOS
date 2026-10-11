/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.components.product

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.siamdev.zappos.ui.components.menu.DefaultProductCategories
import org.siamdev.zappos.ui.screens.product.entry.MasterEntryViewModel
import org.siamdev.zappos.ui.screens.product.entry.EntryType
import org.siamdev.zappos.utils.formatPrice

@Composable
internal fun ItemInfoCard(state: MasterEntryViewModel.State) {
    val catEntry = DefaultProductCategories.find { it.id == state.detail.category }
    val categoryDisplay = run {
        val cat = catEntry?.name ?: state.detail.category.ifBlank { null } ?: return@run null
        val sub = catEntry?.subCategories?.find { it.id == state.detail.subCategory }?.name
        if (sub != null) "$cat · $sub" else cat
    }
    val priceValue = state.pricing.price.toDoubleOrNull() ?: 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).padding(10.dp)) {
            Text(
                "ITEM INFO",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            val pairs = buildList<Pair<String, String>> {
                state.productId?.let { add("Product ID" to it) }
                if (categoryDisplay != null) add("Category" to categoryDisplay)
                add("Price" to "${priceValue.formatPrice()} / ${state.pricing.unit}")
                if (state.pricing.openPrice && state.entryType == EntryType.GOODS) add("Pricing" to "Open / variable price")
                if (!state.pricing.chargeVat) add("VAT" to "Not charged")
                if (state.showCostPrice) {
                    val cost = state.pricing.costPrice.toDoubleOrNull() ?: 0.0
                    if (cost > 0) add("Cost price" to "฿ ${cost.formatPrice()} / ${state.pricing.unit}")
                }

                when (state.entryType) {
                    EntryType.GOODS -> {
                        if (state.inventory.trackStock) {
                            val qty = state.inventory.openingStock.toIntOrNull() ?: 0
                            val cap = state.inventory.maxCapacity.toIntOrNull() ?: 0
                            if (qty > 0 || cap > 0) add("Stock" to "$qty / $cap ${state.pricing.unit}")
                            val alert = state.inventory.lowStockAlert.toIntOrNull() ?: 0
                            if (alert > 0) add("Low stock alert" to "$alert ${state.pricing.unit}")
                        }
                        if (state.inventory.supplier.isNotBlank()) add("Supplier" to state.inventory.supplier)
                    }

                    EntryType.SERVICE -> {
                        val chargedByLabel = listOf("person", "session", "hour")
                            .getOrElse(state.pricing.chargedBy) { "hour" }
                        add("Charged per" to chargedByLabel)
                        val cap = state.service.capacity.toIntOrNull() ?: 0
                        if (cap > 0) add("Capacity" to "$cap people")
                        val dur = state.service.duration.toIntOrNull() ?: 0
                        if (dur > 0) add("Duration" to "$dur min / session")
                        add("Hours" to "${state.service.opens} – ${state.service.closes}")
                        if (state.service.activeDays.isNotEmpty()) {
                            val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            add(
                                "Active days" to state.service.activeDays.sorted()
                                    .joinToString(", ") { dayLabels.getOrElse(it) { "?" } })
                        }
                        if (state.service.instructor.isNotBlank()) add("Instructor" to state.service.instructor)
                        if (state.service.requiresBooking) add("Booking" to "Required")
                    }

                    EntryType.RENTAL -> {
                        val units = state.rental.unitsCount.toIntOrNull() ?: 0
                        if (units > 0) add("Units available" to "$units")
                        val dur = state.pricing.bookingDuration.toIntOrNull() ?: 0
                        if (dur > 0) add("Slot duration" to "$dur min")
                        val minBook = state.pricing.minBooking.toIntOrNull() ?: 0
                        if (minBook > 1) add("Min. booking" to "$minBook slot(s)")
                        add("Hours" to "${state.rental.opens} – ${state.rental.closes}")
                        val buf = state.rental.buffer.toIntOrNull() ?: 0
                        if (buf > 0) add("Buffer" to "$buf min")
                        val dep = state.rental.depositAmount.toDoubleOrNull() ?: 0.0
                        if (dep > 0) add("Deposit" to "฿ ${dep.formatPrice()}")
                        if (state.rental.requiresBooking) add("Booking" to "Required")
                    }
                }
            }

            pairs.forEachIndexed { i, (label, value) ->
                if (i > 0) HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            if (state.detail.description.isNotBlank()) {
                if (pairs.isNotEmpty()) HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Text(
                    "Description",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    state.detail.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemInfoCardPreview() {
    val state = MasterEntryViewModel.State(
        detail = MasterEntryViewModel.State.Detail(
            name = "Matcha Latte",
            category = "beverages",
            subCategory = "tea_coffee",
            description = "Premium Japanese matcha with creamy milk.",
        ),
        pricing = MasterEntryViewModel.State.Pricing(price = "85", unit = "cup"),
        inventory = MasterEntryViewModel.State.Inventory(trackStock = true, openingStock = "50", maxCapacity = "200"),
    )
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            ItemInfoCard(state = state)
        }
    }
}
