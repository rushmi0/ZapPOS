/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.components.product

import org.siamdev.zappos.ui.components.menu.MenuItemsContent
import org.siamdev.zappos.ui.components.common.ViewModeToggle
import org.siamdev.zappos.ui.components.menu.MenuViewMode
import org.siamdev.zappos.ui.components.menu.SearchFilter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.siamdev.zappos.ui.screens.sale.MenuItem

@Composable
fun ProductPanel(
    items: List<MenuItem>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onAddItem: (Int) -> Unit,
    onReduceItem: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(MenuViewMode.LIST) }
    var searchQuery by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf<String?>(null) }

    val categories = remember(items) {
        items.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val filteredItems by remember(items) {
        derivedStateOf {
            items.filter { item ->
                (categoryFilter == null || item.category == categoryFilter) &&
                        (searchQuery.isBlank() || item.name.contains(
                            searchQuery,
                            ignoreCase = true
                        ))
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
        ) {
            SearchFilter(
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                categories = categories,
                selectedCategory = categoryFilter,
                onCategorySelect = { categoryFilter = it },
                trailingContent = {
                    ViewModeToggle(
                        options = listOf(Icons.AutoMirrored.Filled.ViewList, Icons.Default.GridView),
                        selectedIndex = viewMode.ordinal,
                        onSelect = { viewMode = MenuViewMode.entries[it] }
                    )
                }
            )

            Box(modifier = Modifier.fillMaxSize()) {
                MenuItemsContent(
                    items = filteredItems,
                    viewMode = viewMode,
                    isLoading = isLoading,
                    onRefresh = onRefresh,
                    onAddItem = onAddItem,
                    onReduceItem = onReduceItem,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private val previewItems = listOf(
    MenuItem(
        1,
        "",
        "Mocha",
        "70.00",
        "17,500",
        "coffee",
        isRecommended = true,
        count = 2u
    ),
    MenuItem(2, "", "Latte", "70.00", "17,500", "coffee"),
    MenuItem(
        3,
        "",
        "Matcha Latte",
        "100.00",
        "26,000",
        "matcha",
        isRecommended = true,
        count = 1u
    ),
    MenuItem(4, "", "Thai Tea", "60.00", "15,000", "tea"),
    MenuItem(5, "", "Espresso", "50.00", "12,500", "coffee"),
    MenuItem(6, "", "Americano", "60.00", "15,000", "coffee")
)

@Preview(showBackground = true, widthDp = 700, heightDp = 500)
@Composable
fun ProductPanelListPreview() {
    MaterialTheme {
        ProductPanel(
            items = previewItems,
            isLoading = false,
            onRefresh = {},
            onAddItem = {},
            onReduceItem = {},
            modifier = Modifier.fillMaxSize().padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 500)
@Composable
fun ProductPanelGridPreview() {
    MaterialTheme {
        ProductPanel(
            items = previewItems,
            isLoading = false,
            onRefresh = {},
            onAddItem = {},
            onReduceItem = {},
            modifier = Modifier.fillMaxSize().padding(16.dp)
        )
    }
}

@Preview(showBackground = true, widthDp = 700, heightDp = 500)
@Composable
fun ProductPanelLoadingPreview() {
    MaterialTheme {
        ProductPanel(
            items = emptyList(),
            isLoading = true,
            onRefresh = {},
            onAddItem = {},
            onReduceItem = {},
            modifier = Modifier.fillMaxSize().padding(16.dp)
        )
    }
}