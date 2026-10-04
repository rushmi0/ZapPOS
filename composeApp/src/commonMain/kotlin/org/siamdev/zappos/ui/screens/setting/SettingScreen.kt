/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.screens.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import org.siamdev.zappos.LocalSettingVM
import androidx.compose.runtime.CompositionLocalProvider
import org.siamdev.zappos.ui.components.menu.SearchFilter
import org.siamdev.zappos.ui.screens.setting.SettingSurfaceImpl
import org.siamdev.zappos.ui.components.common.WorkspaceHeader
import org.siamdev.zappos.ui.components.gesture.SwipeAnimatedContent
import org.siamdev.zappos.ui.screens.setting.appearance.AppearanceSettingContent
import org.siamdev.zappos.ui.screens.setting.currency.CurrencySettingContent

enum class SettingGroup(val title: String) {
    GENERAL("General"),
    APPEARANCE("Appearance"),
    NETWORK("Network & Data"),
    ACCOUNT("Account")
}

enum class SettingInfo(val title: String, val subtitle: String? = null) {
    ABOUT("About", "App information"), CURRENCY("Preferred Currency"),
    LANGUAGE("Display Language", "English"), APPEARANCE("Appearance"),
    LOCK("Lock Screen"), NOTIFICATION("Notifications"),
    DATA("Data and Storage"), RELAY("Relay"),
    NWC("NWC", "Nostr Wallet Connect"),
    ACCOUNT_KEYS("Account Keys", "Private key & mnemonic"),
    SIGN_OUT("Sign out")
}

data class SettingItemData(
    val destination: SettingInfo,
    val group: SettingGroup,
    val icon: ImageVector
)

@Composable
fun SettingScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateTo: (SettingInfo) -> Unit = {},
    onLogout: () -> Unit = {},
    initialSelected: SettingInfo? = null,
) {
    val setting = LocalSettingVM.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        setting.errors.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var search by remember { mutableStateOf("") }
    val allItems = remember { getSettingItems() }
    val filteredItems = remember(search) {
        if (search.isBlank()) allItems
        else allItems.filter { it.destination.title.contains(search, true) }
    }
    var selected by remember { mutableStateOf(initialSelected) }

    // Items with an inline detail pane open in place; the rest keep their existing routing.
    val onItemClick: (SettingInfo) -> Unit = {
        when {
            it == SettingInfo.SIGN_OUT -> onLogout()
            it.hasDetail() -> selected = it
            else -> onNavigateTo(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { _ ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            if (maxWidth >= 600.dp) {
                DesktopLayout(
                    search = search,
                    onSearchChange = { search = it },
                    allItems = allItems,
                    filteredItems = filteredItems,
                    selected = selected,
                    onItemClick = onItemClick,
                    onNavigateBack = onNavigateBack,
                )
            } else {
                MobileLayout(
                    search = search,
                    onSearchChange = { search = it },
                    allItems = allItems,
                    filteredItems = filteredItems,
                    selected = selected,
                    onItemClick = onItemClick,
                    onBack = { selected = null },
                    onNavigateBack = onNavigateBack,
                )
            }
        }
    }
}

/** Settings that render inline in the detail pane instead of navigating away. */
private fun SettingInfo.hasDetail(): Boolean =
    this == SettingInfo.APPEARANCE || this == SettingInfo.CURRENCY

@Composable
private fun SettingDetailContent(info: SettingInfo, modifier: Modifier = Modifier) {
    when (info) {
        SettingInfo.APPEARANCE -> AppearanceSettingContent(modifier)
        SettingInfo.CURRENCY -> CurrencySettingContent(modifier)
        else -> Unit
    }
}

/** Single-column layout that slides between the list and detail. Used on screens < 600 dp wide. */
@Composable
private fun MobileLayout(
    search: String, onSearchChange: (String) -> Unit,
    allItems: List<SettingItemData>, filteredItems: List<SettingItemData>,
    selected: SettingInfo?,
    onItemClick: (SettingInfo) -> Unit,
    onBack: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    // Swipe left → right goes back, same as the header back button.
    SwipeAnimatedContent(
        targetState = selected,
        depth = { if (it == null) 0 else 1 },
        onSwipeBack = { if (it != null) onBack() else onNavigateBack() },
    ) { current ->
        Column(Modifier.fillMaxSize()) {
            if (current != null) {
                WorkspaceHeader(
                    title = current.title,
                    subtitle = "Settings · ${current.title.lowercase()}",
                    onNavigateBack = onBack,
                )
                SettingDetailContent(current, Modifier.weight(1f))
            } else {
                WorkspaceHeader(
                    title = "Settings",
                    subtitle = "App · preferences",
                    onNavigateBack = onNavigateBack,
                )
                SearchFilter(
                    searchQuery = search,
                    onSearchChange = onSearchChange,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                SettingList(
                    search = search,
                    allItems = allItems,
                    filteredItems = filteredItems,
                    selected = null,
                    onNavigate = onItemClick,
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
                )
            }
        }
    }
}

/** List on the left, selected setting on the right. Used on screens >= 600 dp wide. */
@Composable
private fun DesktopLayout(
    search: String, onSearchChange: (String) -> Unit,
    allItems: List<SettingItemData>, filteredItems: List<SettingItemData>,
    selected: SettingInfo?,
    onItemClick: (SettingInfo) -> Unit,
    onNavigateBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        WorkspaceHeader(
            title = "Settings",
            subtitle = "App · preferences",
            onNavigateBack = onNavigateBack
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                SearchFilter(
                    searchQuery = search,
                    onSearchChange = onSearchChange,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                SettingList(
                    search = search,
                    allItems = allItems,
                    filteredItems = filteredItems,
                    selected = selected,
                    onNavigate = onItemClick,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (selected != null) {
                    SettingDetailContent(selected)
                } else {
                    EmptyDetailState()
                }
            }
        }
    }
}

@Composable
private fun EmptyDetailState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.TouchApp,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Select a setting to view details",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            )
        }
    }
}

@Composable
private fun SettingList(
    search: String,
    allItems: List<SettingItemData>,
    filteredItems: List<SettingItemData>,
    selected: SettingInfo?,
    onNavigate: (SettingInfo) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (search.isBlank()) {
            SettingGroup.entries.forEach { group ->
                val groupItems = allItems.filter { it.group == group }
                if (groupItems.isNotEmpty()) {
                    item {
                        Text(
                            group.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(groupItems) { item ->
                        SettingItemRow(
                            item,
                            isSelected = item.destination == selected,
                            onClick = { onNavigate(item.destination) },
                        )
                    }
                }
            }
        } else {
            items(filteredItems) { item ->
                SettingItemRow(
                    item,
                    isSelected = item.destination == selected,
                    onClick = { onNavigate(item.destination) },
                )
            }
        }
    }
}

@Composable
private fun SettingItemRow(
    item: SettingItemData,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    val isLogout = item.destination == SettingInfo.SIGN_OUT
    val tintColor = if (isLogout) Color(0xFFE53935) else MaterialTheme.colorScheme.primary
    val bgColor =
        if (isLogout) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.destination.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isLogout) tintColor else MaterialTheme.colorScheme.onSurface
            )
            item.destination.subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (!isLogout) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

private fun getSettingItems() = listOf(
    SettingItemData(SettingInfo.ABOUT, SettingGroup.GENERAL, Icons.Default.Info),
    SettingItemData(SettingInfo.CURRENCY, SettingGroup.GENERAL, Icons.Default.CurrencyExchange),
    SettingItemData(SettingInfo.LANGUAGE, SettingGroup.GENERAL, Icons.Default.Language),
    SettingItemData(SettingInfo.APPEARANCE, SettingGroup.APPEARANCE, Icons.Default.Palette),
    SettingItemData(SettingInfo.LOCK, SettingGroup.APPEARANCE, Icons.Default.Lock),
    SettingItemData(SettingInfo.NOTIFICATION, SettingGroup.APPEARANCE, Icons.Default.Notifications),
    SettingItemData(SettingInfo.DATA, SettingGroup.NETWORK, Icons.Default.Storage),
    SettingItemData(SettingInfo.RELAY, SettingGroup.NETWORK, Icons.Default.Cloud),
    SettingItemData(SettingInfo.NWC, SettingGroup.NETWORK, Icons.Default.Link),
    SettingItemData(SettingInfo.ACCOUNT_KEYS, SettingGroup.ACCOUNT, Icons.Default.Key),
    SettingItemData(SettingInfo.SIGN_OUT, SettingGroup.ACCOUNT, Icons.AutoMirrored.Filled.Logout)
)


@Preview(showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun SettingScreenMobilePreview() {
    MaterialTheme {
        CompositionLocalProvider(LocalSettingVM provides SettingSurfaceImpl(SettingViewModel())) {
            SettingScreen()
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun SettingScreenDesktopPreview() {
    MaterialTheme {
        CompositionLocalProvider(LocalSettingVM provides SettingSurfaceImpl(SettingViewModel())) {
            SettingScreen()
        }
    }
}
@Preview(showBackground = true, widthDp = 411, heightDp = 891, name = "Mobile · Detail – Appearance")
@Composable
private fun SettingScreenMobileDetailPreview() {
    MaterialTheme {
        CompositionLocalProvider(LocalSettingVM provides SettingSurfaceImpl(SettingViewModel())) {
            SettingScreen(initialSelected = SettingInfo.APPEARANCE)
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800, name = "Desktop · With Currency Selection")
@Composable
private fun SettingScreenDesktopDetailPreview() {
    MaterialTheme {
        CompositionLocalProvider(LocalSettingVM provides SettingSurfaceImpl(SettingViewModel())) {
            SettingScreen(initialSelected = SettingInfo.CURRENCY)
        }
    }
}
