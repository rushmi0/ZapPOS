/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.screens.product.entry.sections

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.siamdev.zappos.ui.components.common.EntryChip
import org.siamdev.zappos.ui.components.common.EntryField
import org.siamdev.zappos.ui.components.common.LabeledChipRow
import org.siamdev.zappos.ui.components.common.NumberUnitField
import org.siamdev.zappos.ui.components.common.SectionCard
import org.siamdev.zappos.ui.components.common.ToggleItem
import org.siamdev.zappos.ui.components.picker.TimeRangeRow
import org.siamdev.zappos.ui.screens.product.entry.EntryType
import org.siamdev.zappos.ui.screens.product.entry.EntryFormEdit
import org.siamdev.zappos.ui.screens.product.entry.MasterEntryViewModel
import org.siamdev.zappos.ui.screens.product.entry.PreviewEntryForm

private val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
internal fun ScheduleCapacitySection(form: MasterEntryViewModel.State, onEdit: EntryFormEdit) {
    SectionCard(
        icon = Icons.Default.DateRange,
        title = "Schedule & capacity",
        subtitle = "When the service runs and how many it fits",
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberUnitField(
                value = form.service.capacity,
                onValueChange = { onEdit { copy(service = service.copy(capacity = it)) } },
                label = "Capacity",
                unitLabel = "people",
                modifier = Modifier.weight(1f),
            )
            NumberUnitField(
                value = form.service.duration,
                onValueChange = { onEdit { copy(service = service.copy(duration = it)) } },
                label = "Duration",
                unitLabel = "min",
                modifier = Modifier.weight(1f),
            )
        }

        TimeRangeRow(
            opensValue = form.service.opens,
            closesValue = form.service.closes,
            onOpensChange = { onEdit { copy(service = service.copy(opens = it)) } },
            onClosesChange = { onEdit { copy(service = service.copy(closes = it)) } },
            enableManualInput = false,
        )

        LabeledChipRow(label = "Active days") {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                dayLabels.forEachIndexed { index, dayLabel ->
                    EntryChip(
                        selected = index in form.service.activeDays,
                        onClick = {
                            onEdit {
                                val days = service.activeDays
                                copy(service = service.copy(activeDays = if (index in days) days - index else days + index))
                            }
                        },
                        label = dayLabel,
                    )
                }
            }
        }

        EntryField(
            value = form.service.instructor,
            onValueChange = { onEdit { copy(service = service.copy(instructor = it)) } },
            label = "Instructor / host",
            placeholder = "Name of the person leading the session",
        )

        ToggleItem(
            icon = Icons.Default.Event,
            label = "Requires booking",
            subtitle = "Customers must book ahead — not walk-in",
            checked = form.service.requiresBooking,
            onCheckedChange = { onEdit { copy(service = service.copy(requiresBooking = it)) } },
        )
    }
}

@Preview(showBackground = true, widthDp = 411)
@Composable
private fun ScheduleCapacitySectionPreview() {
    MaterialTheme {
        PreviewEntryForm(MasterEntryViewModel.State().withEntryType(EntryType.SERVICE)) { form, onEdit ->
            ScheduleCapacitySection(form, onEdit)
        }
    }
}