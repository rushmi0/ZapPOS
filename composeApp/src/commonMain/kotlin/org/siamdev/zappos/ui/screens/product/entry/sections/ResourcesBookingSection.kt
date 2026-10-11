/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.screens.product.entry.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.siamdev.zappos.ui.components.common.EntryField
import org.siamdev.zappos.ui.components.common.NumberUnitField
import org.siamdev.zappos.ui.components.common.SectionCard
import org.siamdev.zappos.ui.components.common.ToggleItem
import org.siamdev.zappos.ui.components.picker.TimeRangeRow
import org.siamdev.zappos.ui.screens.product.entry.EntryType
import org.siamdev.zappos.ui.screens.product.entry.EntryFormEdit
import org.siamdev.zappos.ui.screens.product.entry.MasterEntryViewModel
import org.siamdev.zappos.ui.screens.product.entry.PreviewEntryForm

@Composable
internal fun ResourcesBookingSection(form: MasterEntryViewModel.State, onEdit: EntryFormEdit) {
    SectionCard(
        icon = Icons.Default.Business,
        title = "Resources & booking",
        subtitle = "Units available and operating window",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberUnitField(
                value = form.rental.unitsCount,
                onValueChange = { onEdit { copy(rental = rental.copy(unitsCount = it)) } },
                label = "Units available",
                unitLabel = form.pricing.unit.ifEmpty { "unit" },
                modifier = Modifier.weight(1f),
            )
            NumberUnitField(
                value = form.rental.buffer,
                onValueChange = { onEdit { copy(rental = rental.copy(buffer = it)) } },
                label = "Buffer between slots",
                unitLabel = "min",
                modifier = Modifier.weight(1f),
            )
        }

        TimeRangeRow(
            opensValue = form.rental.opens,
            closesValue = form.rental.closes,
            onOpensChange = { onEdit { copy(rental = rental.copy(opens = it)) } },
            onClosesChange = { onEdit { copy(rental = rental.copy(closes = it)) } },
        )

        EntryField(
            value = form.rental.depositAmount,
            onValueChange = { onEdit { copy(rental = rental.copy(depositAmount = it.filter { c -> c.isDigit() || c == '.' })) } },
            label = "Deposit",
            placeholder = "0.00",
            keyboardType = KeyboardType.Decimal,
            prefix = { Text("฿") },
        )

        ToggleItem(
            icon = Icons.Default.Event,
            label = "Requires booking",
            subtitle = "Customers must reserve a slot in advance",
            checked = form.rental.requiresBooking,
            onCheckedChange = { onEdit { copy(rental = rental.copy(requiresBooking = it)) } },
        )
    }
}

@Preview(showBackground = true, widthDp = 411)
@Composable
private fun ResourcesBookingSectionPreview() {
    MaterialTheme {
        PreviewEntryForm(MasterEntryViewModel.State().withEntryType(EntryType.RENTAL)) { form, onEdit ->
            ResourcesBookingSection(form, onEdit)
        }
    }
}