/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui

import kotlinx.coroutines.CompletableDeferred
import org.siamdev.zappos.data.source.local.contract.ProductSource
import org.siamdev.zappos.data.source.local.contract.SettingSource
import org.siamdev.zappos.data.source.local.model.CurrencyItem
import org.siamdev.zappos.data.source.local.model.FontItem
import org.siamdev.zappos.data.source.local.model.InventoryConfig
import org.siamdev.zappos.data.source.local.model.ProductModel
import org.siamdev.zappos.data.source.local.model.ThemeItem
import org.siamdev.zappos.ui.screens.sale.MenuItem

internal val testMenuItems = listOf(
    MenuItem(1, "", "Mocha", "70.00", "17,500", "coffee"),
    MenuItem(2, "", "Latte", "70.00", "17,500", "coffee"),
    MenuItem(3, "", "Matcha Latte", "100.00", "26,000", "matcha"),
)

/** In-memory [SettingSource]. Writes can be made to fail or to wait for a signal. */
internal class FakeSettingSource : SettingSource {
    val themes = mutableListOf<ThemeItem>()
    val fonts = mutableListOf<FontItem>()
    val currencies = mutableListOf<CurrencyItem>()
    var activeThemeId: String? = null
    var activeFontId: String? = null
    var primaryCurrencyId: String? = null
    var secondaryCurrencyId: String? = null
    var showSecondary = false
    var accentColor: String? = null

    /** Per theme id: a write that throws this error, optionally after the gate opens. */
    val failingThemeWrites = mutableMapOf<String, Exception>()
    val themeWriteGates = mutableMapOf<String, CompletableDeferred<Unit>>()

    override suspend fun getThemes() = themes.toList()
    override suspend fun getActiveTheme() = themes.find { it.id == activeThemeId }
    override suspend fun setActiveTheme(themeId: String) {
        themeWriteGates[themeId]?.await()
        failingThemeWrites[themeId]?.let { throw it }
        activeThemeId = themeId
    }

    override suspend fun seedTheme(id: String, name: String, mode: String, isDefault: Boolean) {
        themes += ThemeItem(id, name, mode, isDefault)
    }

    override suspend fun getFonts() = fonts.toList()
    override suspend fun getActiveFont() = fonts.find { it.id == activeFontId }
    override suspend fun setActiveFont(fontId: String) {
        activeFontId = fontId
    }

    override suspend fun seedFont(id: String, name: String, size: Double) {
        fonts += FontItem(id, name, size)
    }

    override suspend fun getCurrencies() = currencies.toList()
    override suspend fun getPrimaryCurrency() = currencies.find { it.id == primaryCurrencyId }
    override suspend fun getSecondaryCurrency() = currencies.find { it.id == secondaryCurrencyId }
    override suspend fun setPrimaryCurrency(currencyId: String) {
        primaryCurrencyId = currencyId
    }

    override suspend fun setSecondaryCurrency(currencyId: String) {
        secondaryCurrencyId = currencyId
    }

    override suspend fun getShowSecondaryCurrency() = showSecondary
    override suspend fun setShowSecondaryCurrency(show: Boolean) {
        showSecondary = show
    }

    override suspend fun seedCurrency(id: String, code: String, name: String, symbol: String) {
        currencies += CurrencyItem(id, code, name, symbol)
    }

    override suspend fun getAccentColor() = accentColor
    override suspend fun setAccentColor(hex: String) {
        accentColor = hex
    }

    override suspend fun initSettings() = Unit
}

/** In-memory [ProductSource] recording saves. */
internal class FakeProductSource(
    private val products: Map<String, ProductModel> = emptyMap(),
    var saveResult: Result<Unit> = Result.success(Unit),
    var getError: Exception? = null,
) : ProductSource {
    val saved = mutableListOf<ProductModel>()
    var saveGate: CompletableDeferred<Unit>? = null

    override suspend fun getAll() = products.values.toList()
    override suspend fun getById(id: String): ProductModel? {
        getError?.let { throw it }
        return products[id]
    }

    override suspend fun save(model: ProductModel): Result<Unit> {
        saveGate?.await()
        saved += model
        return saveResult
    }

    override suspend fun setAvailability(id: String, available: Boolean) = Result.success(Unit)
    override suspend fun delete(id: String) = Result.success(Unit)
}

internal fun testProduct(id: String = "prd-1") = ProductModel(
    id = id,
    kind = "1000",
    catId = "beverages",
    taxId = "",
    name = "Green Tea",
    description = "Hot",
    unit = "cup",
    price = 55.0,
    costPrice = 20.0,
    chargeVat = true,
    openPrice = false,
    sku = "GT-01",
    barcode = null,
    sendOrderTo = null,
    displayOrder = 3,
    isAvailable = true,
    isRecommended = false,
    isActive = true,
    inventory = InventoryConfig(
        trackStock = true,
        maxCapacity = 100.0,
        lowStockAlert = 5.0,
        supplierName = "Leaf Co",
        trackExpiry = false,
    ),
    service = null,
    rental = null,
    media = emptyList(),
    optionGroups = emptyList(),
)
