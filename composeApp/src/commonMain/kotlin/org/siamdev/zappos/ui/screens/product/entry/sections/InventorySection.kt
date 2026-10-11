/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.screens.product.entry.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.siamdev.zappos.ui.components.common.EntryField
import org.siamdev.zappos.ui.components.common.NumberUnitField
import org.siamdev.zappos.ui.components.common.SectionCard
import org.siamdev.zappos.ui.components.common.ToggleItem
import org.siamdev.zappos.ui.screens.product.entry.EntryFormEdit
import org.siamdev.zappos.ui.screens.product.entry.MasterEntryViewModel
import org.siamdev.zappos.ui.screens.product.entry.PreviewEntryForm

@Composable
internal fun InventorySection(form: MasterEntryViewModel.State, onEdit: EntryFormEdit) {
    val sellUnit = form.pricing.unit.ifEmpty { "unit" }

    SectionCard(
        icon = Icons.Default.Notifications,
        title = "Stock notification",
        subtitle = "Tap to enable quantity tracking",
        expanded = form.inventory.trackStock,
        onExpandChange = { onEdit { copy(inventory = inventory.copy(trackStock = it)) } },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberUnitField(
                value = form.inventory.openingStock,
                onValueChange = { onEdit { copy(inventory = inventory.copy(openingStock = it)) } },
                label = "Opening stock",
                unitLabel = sellUnit,
                modifier = Modifier.weight(1f),
            )
            NumberUnitField(
                value = form.inventory.maxCapacity,
                onValueChange = { onEdit { copy(inventory = inventory.copy(maxCapacity = it)) } },
                label = "Max capacity",
                unitLabel = sellUnit,
                modifier = Modifier.weight(1f),
            )
        }

        NumberUnitField(
            value = form.inventory.lowStockAlert,
            onValueChange = { onEdit { copy(inventory = inventory.copy(lowStockAlert = it)) } },
            label = "Low stock alert",
            unitLabel = sellUnit,
        )

        EntryField(
            value = form.inventory.supplier,
            onValueChange = { onEdit { copy(inventory = inventory.copy(supplier = it)) } },
            label = "Supplier / vendor",
            placeholder = "Name or company",
        )

        ToggleItem(
            icon = if (form.inventory.trackActive) Icons.Filled.CheckCircle else Icons.Outlined.Cancel,
            label = "Expiry alerts",
            subtitle = if (form.inventory.trackActive) "Active" else "Inactive",
            checked = form.inventory.trackActive,
            onCheckedChange = { onEdit { copy(inventory = inventory.copy(trackActive = it)) } },
        )
    }
}

@Preview(showBackground = true, widthDp = 411, name = "Inventory · tracking on")
@Composable
private fun InventorySectionPreview() {
    MaterialTheme {
        PreviewEntryForm { form, onEdit ->
            InventorySection(form, onEdit)
        }
    }
}

@Preview(showBackground = true, widthDp = 411, name = "Inventory · tracking off")
@Composable
private fun InventorySectionNoTrackPreview() {
    MaterialTheme {
        PreviewEntryForm(MasterEntryViewModel.State().run { copy(inventory = inventory.copy(trackStock = false)) }) { form, onEdit ->
            InventorySection(form, onEdit)
        }
    }
}