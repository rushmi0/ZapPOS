/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.screens.product.entry.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.siamdev.zappos.ui.components.common.ChipRow
import org.siamdev.zappos.ui.components.imagepicker.ImagePickerBox
import org.siamdev.zappos.ui.components.common.EntryField
import org.siamdev.zappos.ui.components.common.SectionCard
import org.siamdev.zappos.ui.components.common.ToggleItem
import org.siamdev.zappos.ui.screens.product.entry.EntryType
import org.siamdev.zappos.ui.screens.product.entry.EntryFormEdit
import org.siamdev.zappos.ui.screens.product.entry.MasterEntryViewModel
import org.siamdev.zappos.ui.screens.product.entry.PreviewEntryForm

private data class TypeConfig(
    val sectionTitle: String,
    val sectionSubtitle: String,
    val namePlaceholder: String,
    val categoryPlaceholder: String,
    val descPlaceholder: String,
    val chips: List<String>,
)

private fun typeConfigFor(type: EntryType): TypeConfig =
    when (type) {
        EntryType.GOODS ->
            TypeConfig(
                sectionTitle = "Product details",
                sectionSubtitle = "The essentials shown on your menu",
                namePlaceholder = "e.g. Green Tea Latte",
                categoryPlaceholder = "e.g. Beverage",
                descPlaceholder = "Ingredients, size, what's included...",
                chips = listOf("Beverage", "Food", "Snack", "Dessert", "Retail"),
            )

        EntryType.SERVICE ->
            TypeConfig(
                sectionTitle = "Service details",
                sectionSubtitle = "Service name, category and availability",
                namePlaceholder = "e.g. Room Cleaning",
                categoryPlaceholder = "e.g. Cleaning",
                descPlaceholder = "What the service includes, how long, what to prepare...",
                chips = listOf("Cleaning", "Repair", "Consultation", "Training", "Beauty"),
            )

        EntryType.RENTAL ->
            TypeConfig(
                sectionTitle = "Rental details",
                sectionSubtitle = "Rental item name, category and availability",
                namePlaceholder = "e.g. Tennis Court",
                categoryPlaceholder = "e.g. Equipment",
                descPlaceholder = "What's included, rental terms, how to use...",
                chips = listOf("Equipment", "Vehicle", "Room", "Sports", "Event"),
            )
    }

@Composable
internal fun ProductDetailsSection(form: MasterEntryViewModel.State, onEdit: EntryFormEdit) {
    val config = typeConfigFor(form.entryType)

    SectionCard(
        icon = Icons.Default.Apps,
        title = config.sectionTitle,
        subtitle = config.sectionSubtitle,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            if (maxWidth >= 580.dp) {
                DetailsDesktop(form, onEdit, config)
            } else {
                DetailsMobile(form, onEdit, config)
            }
        }
    }
}

@Composable
private fun DetailsDesktop(form: MasterEntryViewModel.State, onEdit: EntryFormEdit, config: TypeConfig) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {

            ImagePickerBox(
                modifier = Modifier
                    .width(220.dp)
                    .height(300.dp)
            )

            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                EntryField(
                    value = form.detail.name,
                    onValueChange = { onEdit { copy(detail = detail.copy(name = it)) } },
                    label = "Name",
                    placeholder = config.namePlaceholder,
                    required = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EntryField(
                        value = form.detail.category,
                        onValueChange = { onEdit { withCategory(it) } },
                        label = "Category",
                        placeholder = config.categoryPlaceholder,
                        required = true,
                        modifier = Modifier.weight(1f),
                    )
                    EntryField(
                        value = form.detail.subCategory ?: "",
                        onValueChange = { onEdit { copy(detail = detail.copy(subCategory = it.ifEmpty { null })) } },
                        label = "Sub-category",
                        optional = false,
                        modifier = Modifier.weight(1f),
                    )
                }
                ChipRow(
                    items = config.chips,
                    selected = form.detail.category,
                    onSelect = { onEdit { withCategory(it) } },
                )
                EntryField(
                    value = form.detail.description,
                    onValueChange = { onEdit { copy(detail = detail.copy(description = it)) } },
                    label = "Description",
                    optional = true,
                    placeholder = config.descPlaceholder,
                    singleLine = false,
                    minLines = 3,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(),
        ) {
            ToggleItem(
                icon = Icons.Default.Visibility,
                label = "Available",
                subtitle = "Shown on the menu and orderable right now",
                checked = form.detail.isAvailable,
                onCheckedChange = { onEdit { copy(detail = detail.copy(isAvailable = it)) } },
            )
            ToggleItem(
                icon = Icons.Default.Star,
                label = "Recommended",
                subtitle = "Featured with a ★ on the menu",
                checked = form.detail.isRecommended,
                onCheckedChange = { onEdit { copy(detail = detail.copy(isRecommended = it)) } },
            )
        }
    }
}


// Mobile: full-width image at top, then each field stacked individually, then toggles.
@Composable
private fun DetailsMobile(form: MasterEntryViewModel.State, onEdit: EntryFormEdit, config: TypeConfig) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        ImagePickerBox(
            modifier = Modifier.fillMaxWidth().height(270.dp),
        )
        EntryField(
            value = form.detail.name,
            onValueChange = { onEdit { copy(detail = detail.copy(name = it)) } },
            label = "Name",
            placeholder = config.namePlaceholder,
            required = true,
        )
        EntryField(
            value = form.detail.category,
            onValueChange = { onEdit { withCategory(it) } },
            label = "Category",
            placeholder = config.categoryPlaceholder,
            required = true,
        )
        EntryField(
            value = form.detail.subCategory ?: "",
            onValueChange = { onEdit { copy(detail = detail.copy(subCategory = it.ifEmpty { null })) } },
            label = "Sub-category",
            placeholder = "optional",
            optional = true,
        )
        ChipRow(
            items = config.chips,
            selected = form.detail.category,
            onSelect = { onEdit { withCategory(it) } },
        )
        EntryField(
            value = form.detail.description,
            onValueChange = { onEdit { copy(detail = detail.copy(description = it)) } },
            label = "Description",
            optional = true,
            placeholder = config.descPlaceholder,
            singleLine = false,
            minLines = 2,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ToggleItem(
            icon = Icons.Default.Visibility,
            label = "Available",
            subtitle = "Shown on the menu and orderable right now",
            checked = form.detail.isAvailable,
            onCheckedChange = { onEdit { copy(detail = detail.copy(isAvailable = it)) } },
        )
        ToggleItem(
            icon = Icons.Default.Star,
            label = "Recommended",
            subtitle = "Featured with a ★ on the menu",
            checked = form.detail.isRecommended,
            onCheckedChange = { onEdit { copy(detail = detail.copy(isRecommended = it)) } },
        )
    }
}

@Preview(showBackground = true, widthDp = 411, name = "Mobile · Goods")
@Composable
private fun PreviewMobileGoods() {
    MaterialTheme {
        PreviewEntryForm { form, onEdit ->
            ProductDetailsSection(form, onEdit)
        }
    }
}

@Preview(showBackground = true, widthDp = 411, name = "Mobile · Service")
@Composable
private fun PreviewMobileService() {
    MaterialTheme {
        PreviewEntryForm(MasterEntryViewModel.State().withEntryType(EntryType.SERVICE)) { form, onEdit ->
            ProductDetailsSection(form, onEdit)
        }
    }
}

@Preview(showBackground = true, widthDp = 411, name = "Mobile · Rental")
@Composable
private fun PreviewMobileRental() {
    MaterialTheme {
        PreviewEntryForm(MasterEntryViewModel.State().withEntryType(EntryType.RENTAL)) { form, onEdit ->
            ProductDetailsSection(form, onEdit)
        }
    }
}

@Preview(showBackground = true, widthDp = 600, name = "Tablet · with Product ID")
@Composable
private fun PreviewTabletWithId() {
    MaterialTheme {
        PreviewEntryForm(MasterEntryViewModel.State(productId = "PRD-0042")) { form, onEdit ->
            ProductDetailsSection(form, onEdit)
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, name = "Desktop · with Product ID")
@Composable
private fun PreviewDesktopWithId() {
    MaterialTheme {
        PreviewEntryForm(MasterEntryViewModel.State(productId = "PRD-0042")) { form, onEdit ->
            ProductDetailsSection(form, onEdit)
        }
    }
}
