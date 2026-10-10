/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.screens.product.goods.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.siamdev.zappos.LocalSettingVM
import org.siamdev.zappos.ui.components.common.MaterialButton
import org.siamdev.zappos.ui.components.menu.DefaultProductCategories
import org.siamdev.zappos.ui.components.menu.SearchFilter
import org.siamdev.zappos.ui.components.product.ProductListItem
import org.siamdev.zappos.data.source.MasterEvent
import org.siamdev.zappos.ui.screens.product.goods.sampleProducts
import org.siamdev.zappos.ui.screens.setting.SettingSurfaceImpl
import org.siamdev.zappos.ui.screens.setting.SettingViewModel
import org.siamdev.zappos.theme.ThemeMode
import org.siamdev.zappos.theme.ZapposTheme

/** Scrollable list of products with search and category filter controls. */
@Composable
internal fun ProductListPane(
    products: List<MasterEvent>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    onNewProduct: (() -> Unit)? = null,
) {
    var searchQuery by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf<String?>(null) }

    val categoryIds =
        remember(products) {
            DefaultProductCategories
                .map { it.id }
                .filter { id -> products.any { p -> p.category == id } }
        }

    val filtered =
        remember(products, searchQuery, categoryFilter) {
            products.filter { p ->
                (categoryFilter == null || p.category == categoryFilter) &&
                        (searchQuery.isBlank() || p.name.contains(searchQuery, ignoreCase = true))
            }
        }

    Column(modifier = modifier) {
        if (onNewProduct != null) {
            MaterialButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp),
                text = "New Product",
                iconStart = Icons.Default.Add,
                onClick = onNewProduct,
            )
        }
        SearchFilter(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            categories = categoryIds,
            selectedCategory = categoryFilter,
            onCategorySelect = { categoryFilter = it },
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp),
        )

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No products found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                items(filtered, key = { it.id }) { product ->
                    ProductListItem(
                        product = product,
                        isSelected = product.id == selectedId,
                        onClick = { onSelect(product.id) },
                        modifier = Modifier.padding(vertical = 3.dp),
                    )
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 700,
    name = "ProductListPane · Empty selection"
)
@Composable
private fun ProductListPanePreview() {
    ZapposTheme(themeMode = ThemeMode.LIGHT) {
        CompositionLocalProvider(LocalSettingVM provides SettingSurfaceImpl(SettingViewModel())) {
            ProductListPane(
                products = sampleProducts(),
                selectedId = null,
                onSelect = {},
            )
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 700,
    name = "ProductListPane · With selection"
)
@Composable
private fun ProductListPaneSelectedPreview() {
    ZapposTheme(themeMode = ThemeMode.LIGHT) {
        CompositionLocalProvider(LocalSettingVM provides SettingSurfaceImpl(SettingViewModel())) {
            val products = sampleProducts()
            ProductListPane(
                products = products,
                selectedId = products.first().id,
                onSelect = {},
            )
        }
    }
}