/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.screens.product.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import org.siamdev.zappos.ui.components.common.SegmentedTabBar
import org.siamdev.zappos.ui.components.common.WorkspaceHeader
import org.siamdev.zappos.ui.components.menu.DefaultProductCategories
import org.siamdev.zappos.ui.components.product.ProductHeader
import org.siamdev.zappos.ui.screens.product.entry.sections.AdvancedSection
import org.siamdev.zappos.ui.screens.product.entry.sections.EntryActionBar
import org.siamdev.zappos.ui.screens.product.entry.sections.InventorySection
import org.siamdev.zappos.ui.screens.product.entry.sections.OptionsSection
import org.siamdev.zappos.ui.screens.product.entry.sections.PricingSection
import org.siamdev.zappos.ui.screens.product.entry.sections.ProductDetailsSection
import org.siamdev.zappos.ui.screens.product.entry.sections.ResourcesBookingSection
import org.siamdev.zappos.ui.screens.product.entry.sections.ScheduleCapacitySection

@Composable
fun MasterEntryScreen(
    productId: String? = null,
    onNavigateBack: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onSave: () -> Unit = {},
    showBackButton: Boolean = false,
) {
    val vm = viewModel { MasterEntryViewModel(productId) }
    val form by vm.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    vm.collectSideEffect { effect ->
        when (effect) {
            is MasterEntryViewModel.SideEffect.SaveSuccess -> onSave()
            is MasterEntryViewModel.SideEffect.NavigateBack -> onNavigateBack()
            is MasterEntryViewModel.SideEffect.Error -> snackbarHostState.showSnackbar(effect.message)
        }
    }

    val onEdit: EntryFormEdit = vm::edit

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            WorkspaceHeader(
                title = "Product Master",
                subtitle = if (form.isEditMode) "Edit product · master data" else "New product · master data",
                onSegmentClick = onOpenDrawer,
                onNavigateBack = if (showBackButton) onNavigateBack else null,
            )

            if (!form.isEditMode) {
                SegmentedTabBar(
                    tabs = entryTabs,
                    selectedIndex = form.entryType.ordinal,
                    onTabSelect = { onEdit { withEntryType(EntryType.entries[it]) } },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 8.dp),
                )
            }

            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                if (maxWidth >= 750.dp) {
                    EntryDesktopLayout(form, onEdit)
                } else {
                    EntryMobileLayout(form, onEdit)
                }
            }

            EntryActionBar(
                isFormValid = form.isFormValid && !form.isSaving,
                isEditMode = form.isEditMode,
                onSave = { vm.save() },
                onDiscard = { vm.discard() },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(bottom = 72.dp),
        )
    }
}

@Composable
private fun EntryMobileLayout(form: MasterEntryViewModel.State, onEdit: EntryFormEdit) {
    val tab = entryTabs[form.entryType.ordinal]
    val catEntry =
        remember(form.detail.category) { DefaultProductCategories.find { it.id == form.detail.category } }
    val categoryName = catEntry?.name ?: form.detail.category.ifBlank { tab.label }
    val subName = catEntry?.subCategories?.find { it.id == form.detail.subCategory }?.name
    val catIcon = catEntry?.icon ?: tab.icon ?: Icons.Default.ShoppingBag

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (form.detail.name.isNotBlank()) {
            item {
                ProductHeader(
                    name = form.detail.name,
                    categoryName = categoryName,
                    subName = subName,
                    catIcon = catIcon,
                )
            }
        }

        item { ProductDetailsSection(form, onEdit) }
        item { PricingSection(form, onEdit) }

        when (form.entryType) {
            EntryType.GOODS -> {
                item { InventorySection(form, onEdit) }
                item { OptionsSection(form, onEdit) }
            }

            EntryType.SERVICE -> {
                item { ScheduleCapacitySection(form, onEdit) }
                item { OptionsSection(form, onEdit) }
            }

            EntryType.RENTAL -> item { ResourcesBookingSection(form, onEdit) }
        }

        item { AdvancedSection(form, onEdit) }
    }
}

@Composable
private fun EntryDesktopLayout(form: MasterEntryViewModel.State, onEdit: EntryFormEdit) {
    val tab = entryTabs[form.entryType.ordinal]
    val catEntry =
        remember(form.detail.category) { DefaultProductCategories.find { it.id == form.detail.category } }
    val categoryName = catEntry?.name ?: form.detail.category.ifBlank { tab.label }
    val subName = catEntry?.subCategories?.find { it.id == form.detail.subCategory }?.name
    val catIcon = catEntry?.icon ?: tab.icon ?: Icons.Default.ShoppingBag

    Column(modifier = Modifier.fillMaxSize()) {
        if (form.detail.name.isNotBlank()) {
            ProductHeader(
                name = form.detail.name,
                categoryName = categoryName,
                subName = subName,
                catIcon = catIcon,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Row(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { ProductDetailsSection(form, onEdit) }
                item { PricingSection(form, onEdit) }
            }

            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (form.entryType) {
                    EntryType.GOODS -> {
                        item { InventorySection(form, onEdit) }
                        item { OptionsSection(form, onEdit) }
                    }

                    EntryType.SERVICE -> {
                        item { ScheduleCapacitySection(form, onEdit) }
                        item { OptionsSection(form, onEdit) }
                    }

                    EntryType.RENTAL -> {
                        item { ResourcesBookingSection(form, onEdit) }
                    }
                }
                item { AdvancedSection(form, onEdit) }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891, name = "Mobile · Create")
@Composable
private fun MasterEntryMobilePreview() {
    MaterialTheme { MasterEntryScreen() }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 891, name = "Mobile · Edit")
@Composable
private fun MasterEntryMobileEditPreview() {
    MaterialTheme { MasterEntryScreen(productId = "1") }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800, name = "Desktop · Create")
@Composable
private fun MasterEntryDesktopPreview() {
    MaterialTheme { MasterEntryScreen() }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800, name = "Desktop · Edit")
@Composable
private fun MasterEntryDesktopEditPreview() {
    MaterialTheme { MasterEntryScreen(productId = "1") }
}