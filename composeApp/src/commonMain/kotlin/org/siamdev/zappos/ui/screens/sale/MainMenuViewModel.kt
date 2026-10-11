/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.screens.sale

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import org.siamdev.zappos.cache.ImagePreloader
import org.siamdev.zappos.cache.ThumbnailSection
import org.orbitmvi.orbit.OrbitContainer
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import org.siamdev.zappos.ui.UiContainerSettings
import org.siamdev.zappos.ui.components.menu.MenuViewMode
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "MainMenu"

@Serializable
data class MenuItem(
    val id: Int,
    val imageUrl: String,
    val name: String,
    val priceBaht: String,
    val priceSat: String,
    val category: String = "",
    val isRecommended: Boolean = false,
    val isAvailable: Boolean = true,
    @Transient val count: UInt = 0u
)

internal val menuJson = Json { ignoreUnknownKeys = true }

const val MOCK_MENU_JSON = """
[
  { "id": 1,  "imageUrl": "https://images.pexels.com/photos/350478/pexels-photo-350478.jpeg",     "name": "Mocha",             "priceBaht": "70.00",  "priceSat": "17,500", "category": "coffee", "isRecommended": true,  "isAvailable": true  },
  { "id": 2,  "imageUrl": "https://images.pexels.com/photos/17486832/pexels-photo-17486832.jpeg", "name": "Latte",             "priceBaht": "70.00",  "priceSat": "17,500", "category": "coffee", "isRecommended": false, "isAvailable": true  },
  { "id": 3,  "imageUrl": "https://images.pexels.com/photos/2611811/pexels-photo-2611811.jpeg",   "name": "Matcha Latte",      "priceBaht": "100.00", "priceSat": "26,000", "category": "matcha", "isRecommended": true,  "isAvailable": true  },
  { "id": 4,  "imageUrl": "https://images.pexels.com/photos/18635175/pexels-photo-18635175.jpeg", "name": "Matcha Coffee",     "priceBaht": "100.00", "priceSat": "26,000", "category": "matcha", "isRecommended": false, "isAvailable": true  },
  { "id": 5,  "imageUrl": "https://images.pexels.com/photos/302899/pexels-photo-302899.jpeg",     "name": "Espresso",          "priceBaht": "50.00",  "priceSat": "12,500", "category": "coffee", "isRecommended": false, "isAvailable": true  },
  { "id": 6,  "imageUrl": "https://images.pexels.com/photos/414555/pexels-photo-414555.jpeg",     "name": "Americano",         "priceBaht": "60.00",  "priceSat": "15,000", "category": "coffee", "isRecommended": false, "isAvailable": true  },
  { "id": 7,  "imageUrl": "https://images.pexels.com/photos/585750/pexels-photo-585750.jpeg",     "name": "Cappuccino",        "priceBaht": "75.00",  "priceSat": "18,750", "category": "coffee", "isRecommended": true,  "isAvailable": true  },
  { "id": 8,  "imageUrl": "https://images.pexels.com/photos/312418/pexels-photo-312418.jpeg",     "name": "Flat White",        "priceBaht": "80.00",  "priceSat": "20,000", "category": "coffee", "isRecommended": false, "isAvailable": true  },
  { "id": 9,  "imageUrl": "https://images.pexels.com/photos/2103949/pexels-photo-2103949.jpeg",   "name": "Caramel Macchiato", "priceBaht": "90.00",  "priceSat": "22,500", "category": "coffee", "isRecommended": false, "isAvailable": true  },
  { "id": 10, "imageUrl": "https://images.pexels.com/photos/302902/pexels-photo-302902.jpeg",     "name": "Iced Coffee",       "priceBaht": "65.00",  "priceSat": "16,250", "category": "coffee", "isRecommended": false, "isAvailable": true  },
  { "id": 11, "imageUrl": "https://images.pexels.com/photos/2907301/pexels-photo-2907301.jpeg",   "name": "Thai Tea",          "priceBaht": "60.00",  "priceSat": "15,000", "category": "tea",    "isRecommended": true,  "isAvailable": true  },
  { "id": 12, "imageUrl": "https://images.pexels.com/photos/1337825/pexels-photo-1337825.jpeg",   "name": "Green Tea",         "priceBaht": "55.00",  "priceSat": "13,750", "category": "tea",    "isRecommended": false, "isAvailable": true  },
  { "id": 13, "imageUrl": "https://images.pexels.com/photos/374885/pexels-photo-374885.jpeg",     "name": "Hot Chocolate",     "priceBaht": "85.00",  "priceSat": "21,250", "category": "other",  "isRecommended": false, "isAvailable": true  },
  { "id": 14, "imageUrl": "https://images.pexels.com/photos/416656/pexels-photo-416656.jpeg",     "name": "Milk",              "priceBaht": "50.00",  "priceSat": "12,500", "category": "other",  "isRecommended": false, "isAvailable": false }
]
"""

val SaleOrderSteps = listOf("Confirm", "Checkout", "Payment", "Successful")

fun loadMenuItems(): List<MenuItem> =
    menuJson.decodeFromString<List<MenuItem>>(MOCK_MENU_JSON)


/** Mock product fetch: stands in for the network call until a product API exists. */
private suspend fun fetchMenuItems(): List<MenuItem> {
    delay(2000.milliseconds)
    return loadMenuItems()
}

/**
 * The menu catalogue and the cart built from it. App-scoped: Menu, Confirm Order and Checkout all
 * read the same cart.
 */
class MainMenuViewModel(
    private val fetchItems: suspend () -> List<MenuItem> = ::fetchMenuItems,
    private val fetchThumbnails: suspend () -> Unit = ::fetchMissingThumbnails,
) : ViewModel(), OrbitContainerHost<MainMenuViewModel.State, MainMenuViewModel.State, MainMenuViewModel.SideEffect> {

    data class State(
        val isLoading: Boolean = false,
        val viewMode: MenuViewMode = MenuViewMode.LIST,
        val catalog: Catalog = Catalog(),
        val order: Order = Order()
    ) {
        data class Catalog(val items: List<MenuItem> = emptyList())
        data class Order(
            val selectedKeys: List<Int> = emptyList(),
            val totalFiat: String = "0.00",
            val totalSat: String = "0"
        )

        val selectedItems: List<MenuItem> get() = catalog.items.filter { it.count > 0u }

        /** Replaces the catalogue and rebuilds the order (selection + totals) from it. */
        fun withItems(items: List<MenuItem>): State {
            val selected = items.filter { it.count > 0u }
            return copy(
                catalog = catalog.copy(items = items),
                order = Order(
                    selectedKeys = selected.map { it.id },
                    totalFiat = formatNumber(items.sumOf { it.priceBaht.toDouble() * it.count.toDouble() }),
                    totalSat = formatNumber(items.sumOf { it.priceSatValue * it.count.toDouble() })
                )
            )
        }

        fun withItemCount(id: Int, count: (UInt) -> UInt): State =
            withItems(catalog.items.map { if (it.id == id) it.copy(count = count(it.count)) else it })
    }

    sealed interface SideEffect {
        data class Error(val message: String) : SideEffect
    }

    override val container: OrbitContainer<State, State, SideEffect> =
        orbitContainer(State(), buildSettings = UiContainerSettings) { loadProducts() }

    // Only touched on the main thread (the container's event loop runs there).
    private var loadJob: Job? = null

    /**
     * Starts a catalogue load, cancelling one still in flight so a stale response can never
     * overwrite a newer one.
     */
    private fun loadProducts(reset: Boolean = false) {
        loadJob?.cancel()
        loadJob = intent {
            reduce {
                if (reset) State(viewMode = state.viewMode, isLoading = true)
                else state.copy(isLoading = true)
            }
            println("[$TAG] Fetching product list from network...")
            val loaded = try {
                fetchItems()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                reduce { state.copy(isLoading = false) }
                postSideEffect(SideEffect.Error(e.message ?: "Could not load products"))
                return@intent
            }
            reduce { state.copy(isLoading = false).withItems(loaded) }
            println("[$TAG] Loaded ${loaded.size} products")
            fetchThumbnails()
        }
    }

    fun reloadProductsData() = loadProducts(reset = true)

    fun setViewMode(mode: MenuViewMode) = intent { reduce { state.copy(viewMode = mode) } }

    fun addItem(id: Int) = intent { reduce { state.withItemCount(id) { it + 1u } } }

    fun reduceItem(id: Int) = intent {
        reduce { state.withItemCount(id) { if (it > 0u) it - 1u else it } }
    }

    fun setItemCount(id: Int, count: UInt) = intent { reduce { state.withItemCount(id) { count } } }

    fun clearAllItems() = intent {
        reduce { state.withItems(state.catalog.items.map { it.copy(count = 0u) }).copy(order = State.Order()) }
    }
}

private suspend fun fetchMissingThumbnails() {
    println("[$TAG] Checking thumbnails for missing entries...")
    ImagePreloader.preloadMenuItems(
        section = ThumbnailSection.PRODUCTS,
        onProgress = { progress ->
            val pct = (progress * 100).toInt()
            if (pct % 25 == 0) println("[$TAG] Thumbnail fetch progress: $pct%")
        }
    )
    println("[$TAG] Thumbnail fetch complete")
}

private val MenuItem.priceSatValue: Double get() = priceSat.replace(",", "").toDoubleOrNull() ?: 0.0

private fun formatNumber(value: Double): String {
    val intPart = value.toLong()
    val decPart = ((value - intPart) * 100).toInt()
    val intStr = intPart.toString().reversed().chunked(3).joinToString(",").reversed()
    return "$intStr.${decPart.toString().padStart(2, '0')}"
}
