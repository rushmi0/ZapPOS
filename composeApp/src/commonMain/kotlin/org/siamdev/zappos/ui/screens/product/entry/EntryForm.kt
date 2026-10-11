/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.screens.product.entry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.*
import org.siamdev.zappos.data.source.EventKind
import org.siamdev.zappos.data.source.MasterEvent
import org.siamdev.zappos.ui.components.common.TabItem
import org.siamdev.zappos.utils.DateTimeUtils

enum class EntryType {
    GOODS,
    SERVICE,
    RENTAL
}

enum class PickMode {
    ONE,
    MANY
}

data class OptionItem(
    val id: String = DateTimeUtils.nowEpochMillis().toString(),
    val name: String = "",
    val priceModifier: Int = 0,
)

data class OptionGroup(
    val id: String = DateTimeUtils.nowEpochMillis().toString(),
    val name: String = "Size",
    val pickMode: PickMode = PickMode.ONE,
    val required: Boolean = false,
    val items: List<OptionItem> = emptyList(),
)

internal val entryTabs: List<TabItem> =
    listOf(
        TabItem("Goods", Icons.Default.Inventory),
        TabItem("Service", Icons.Default.Person),
        TabItem("Rental", Icons.Default.Schedule),
    )

/** Read-only form view of a stored product, used by the product list's detail panel. */
internal fun MasterEvent.toEntryForm(): MasterEntryViewModel.State {
    val base = MasterEntryViewModel.State()
    // Local vals required: member properties with custom getters cannot be smart-cast
    val costPriceVal = costPrice
    val stockQtyVal = stockQty
    return base.copy(
        productId = id,
        entryType = when (kind) {
            EventKind.SERVICE -> EntryType.SERVICE
            EventKind.RENTAL -> EntryType.RENTAL
            else -> EntryType.GOODS
        },
        showCostPrice = costPriceVal != null,
        detail = base.detail.copy(
            name = name,
            category = category,
            subCategory = subCategory,
            description = description,
            isAvailable = isAvailable,
            isRecommended = isRecommended,
        ),
        pricing = base.pricing.copy(
            price = price.toLong().toString(),
            unit = unit,
            chargeVat = chargeVat,
            openPrice = openPrice,
            costPrice = costPriceVal?.toLong()?.toString() ?: base.pricing.costPrice,
        ),
        inventory = base.inventory.copy(
            trackStock = stockQtyVal != null,
            openingStock = stockQtyVal?.toString() ?: base.inventory.openingStock,
            maxCapacity = stockMax?.toString() ?: base.inventory.maxCapacity,
            lowStockAlert = lowStockAlert?.toString() ?: base.inventory.lowStockAlert,
            supplier = supplier,
        ),
        advanced = base.advanced.copy(sku = sku),
        options = optionGroups.map { group ->
            OptionGroup(
                name = group.name,
                pickMode = if (group.multiSelect) PickMode.MANY else PickMode.ONE,
                required = group.required,
                items = group.items.map { OptionItem(name = it.name, priceModifier = it.priceModifier) },
            )
        },
    )
}

/** Holds form state locally so section previews stay interactive without a ViewModel. */
@Composable
internal fun PreviewEntryForm(
    initial: MasterEntryViewModel.State = MasterEntryViewModel.State(),
    content: @Composable (form: MasterEntryViewModel.State, onEdit: EntryFormEdit) -> Unit,
) {
    var form by remember { mutableStateOf(initial) }
    content(form) { transform -> form = form.transform() }
}
