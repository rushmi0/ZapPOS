/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.screens.product.entry

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CancellationException
import org.orbitmvi.orbit.OrbitContainer
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import org.siamdev.zappos.data.source.local.LocalSources
import org.siamdev.zappos.data.source.local.contract.ProductSource
import org.siamdev.zappos.data.source.local.model.InventoryConfig
import org.siamdev.zappos.data.source.local.model.OptionGroupModel
import org.siamdev.zappos.data.source.local.model.OptionItemModel
import org.siamdev.zappos.data.source.local.model.ProductModel
import org.siamdev.zappos.data.source.local.model.RentalConfig
import org.siamdev.zappos.data.source.local.model.ServiceConfig
import org.siamdev.zappos.utils.DateTimeUtils
import org.siamdev.zappos.ui.UiContainerSettings
import org.siamdev.zappos.utils.TimeValue

/**
 * Create / edit form for a product master record. One instance per back-stack entry; the
 * product (if any) is loaded once in onCreate, so returning to the screen never discards edits.
 */
class MasterEntryViewModel(
    private val productId: String? = null,
    private val productSource: () -> ProductSource? = { LocalSources.product },
) : ViewModel(), OrbitContainerHost<MasterEntryViewModel.State, MasterEntryViewModel.State, MasterEntryViewModel.SideEffect> {

    enum class Mode { CREATE, EDIT }

    data class State(
        val mode: Mode = Mode.CREATE,
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,
        val entryType: EntryType = EntryType.GOODS,
        val productId: String? = null,
        val showCostPrice: Boolean = false,
        val advancedExpanded: Boolean = false,
        val detail: Detail = Detail(),
        val pricing: Pricing = Pricing(),
        val inventory: Inventory = Inventory(),
        val service: Service = Service(),
        val rental: Rental = Rental(),
        val options: List<OptionGroup> = emptyList(),
        val advanced: Advanced = Advanced(),
    ) {
        data class Detail(
            val name: String = "",
            val category: String = "",
            val subCategory: String? = null,
            val description: String = "",
            val isAvailable: Boolean = true,
            val isRecommended: Boolean = false,
        )

        data class Pricing(
            val price: String = "",
            val unit: String = "piece",
            val chargeVat: Boolean = true,
            val costPrice: String = "",
            val openPrice: Boolean = false,
            val chargedBy: Int = 2,
            val bookingDuration: String = "60",
            val minBooking: String = "1",
        )

        data class Inventory(
            val trackStock: Boolean = true,
            val openingStock: String = "0",
            val maxCapacity: String = "0",
            val lowStockAlert: String = "0",
            val supplier: String = "",
            val trackActive: Boolean = false,
        )

        data class Service(
            val capacity: String = "12",
            val duration: String = "60",
            val opens: TimeValue = TimeValue(9, 0),
            val closes: TimeValue = TimeValue(18, 0),
            val activeDays: Set<Int> = setOf(0, 1, 2, 3, 4),
            val instructor: String = "",
            val requiresBooking: Boolean = false,
        )

        data class Rental(
            val unitsCount: String = "2",
            val buffer: String = "0",
            val opens: TimeValue = TimeValue(8, 0),
            val closes: TimeValue = TimeValue(22, 0),
            val depositAmount: String = "0.00",
            val requiresBooking: Boolean = true,
        )

        data class Advanced(
            val sku: String = "",
            val barcode: String = "",
            val sendOrderTo: String = "None",
            val displayOrder: String = "0",
        )

        val isEditMode: Boolean get() = mode == Mode.EDIT
        val isFormValid: Boolean get() = detail.name.isNotBlank() && pricing.price.isNotBlank()
        val unitOptions: List<String> get() = unitOptionsFor(entryType)

        /** Switching type resets the unit to that type's default. */
        fun withEntryType(type: EntryType): State =
            copy(entryType = type, pricing = pricing.copy(unit = defaultUnitFor(type)))

        /** Sub-categories belong to a category, so changing the category clears it. */
        fun withCategory(category: String): State =
            copy(detail = detail.copy(category = category, subCategory = null))
    }

    sealed interface SideEffect {
        data object SaveSuccess : SideEffect
        data class Error(val message: String) : SideEffect
        data object NavigateBack : SideEffect
    }

    override val container: OrbitContainer<State, State, SideEffect> = orbitContainer(
        initialState = if (productId == null) State()
        else State(mode = Mode.EDIT, productId = productId, isLoading = true),
        buildSettings = UiContainerSettings,
        onCreate = { if (productId != null) load(productId) }
    )

    private suspend fun Syntax<State, SideEffect>.load(id: String) {
        try {
            val product = productSource()?.getById(id)
            reduce { if (product != null) state.loadFrom(product) else state.copy(isLoading = false) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reduce { state.copy(isLoading = false) }
            postSideEffect(SideEffect.Error(e.message ?: "Could not load product"))
        }
    }

    /** Applies a field edit. Runs synchronously on the main thread (see UiContainerSettings). */
    fun edit(transform: State.() -> State) = intent { reduce { state.transform() } }

    fun save() = intent {
        if (!state.isFormValid || state.isSaving) return@intent
        reduce { state.copy(isSaving = true) }
        val result = try {
            productSource()?.save(state.toProductModel()) ?: Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
        reduce { state.copy(isSaving = false) }
        result.fold(
            onSuccess = { postSideEffect(SideEffect.SaveSuccess) },
            onFailure = { postSideEffect(SideEffect.Error(it.message ?: "Save failed")) },
        )
    }

    fun discard() = intent { postSideEffect(SideEffect.NavigateBack) }
}

/** Applies a form edit; the section composables take one of these instead of the ViewModel. */
internal typealias EntryFormEdit = (MasterEntryViewModel.State.() -> MasterEntryViewModel.State) -> Unit

internal fun defaultUnitFor(type: EntryType): String = when (type) {
    EntryType.GOODS -> "piece"
    EntryType.SERVICE -> "hour"
    EntryType.RENTAL -> "hour"
}

internal fun unitOptionsFor(type: EntryType): List<String> = when (type) {
    EntryType.GOODS -> listOf("cup", "plate", "bowl", "piece", "skewer", "bottle", "pack", "kg", "box")
    EntryType.SERVICE -> listOf("session", "person", "hour", "course", "class", "month")
    EntryType.RENTAL -> listOf("hour", "court", "field", "table", "room", "day")
}

private fun MasterEntryViewModel.State.loadFrom(product: ProductModel): MasterEntryViewModel.State {
    val entryType = when (product.kind) {
        "1001" -> EntryType.SERVICE
        "1002" -> EntryType.RENTAL
        else -> EntryType.GOODS
    }
    val inv = product.inventory
    val svc = product.service
    val rnt = product.rental
    return copy(
        mode = MasterEntryViewModel.Mode.EDIT,
        isLoading = false,
        entryType = entryType,
        productId = product.id,
        showCostPrice = product.costPrice != null,
        detail = detail.copy(
            name = product.name,
            category = product.catId,
            description = product.description ?: "",
            isAvailable = product.isAvailable,
            isRecommended = product.isRecommended,
        ),
        pricing = pricing.copy(
            price = product.price.toLong().toString(),
            unit = product.unit,
            chargeVat = product.chargeVat,
            openPrice = product.openPrice,
            costPrice = product.costPrice?.toLong()?.toString() ?: "",
            chargedBy = svc?.chargedBy?.toIntOrNull() ?: rnt?.let { 2 } ?: 2,
            bookingDuration = svc?.durationMin?.toString() ?: rnt?.slotDurationMin?.toString()
            ?: "60",
            minBooking = rnt?.minBooking?.toString() ?: "1",
        ),
        inventory = if (inv != null) inventory.copy(
            trackStock = inv.trackStock,
            maxCapacity = inv.maxCapacity.toLong().toString(),
            lowStockAlert = inv.lowStockAlert.toLong().toString(),
            supplier = inv.supplierName ?: "",
            trackActive = inv.trackExpiry,
        ) else inventory,
        service = if (svc != null) service.copy(
            capacity = svc.capacity.toString(),
            duration = svc.durationMin.toString(),
            opens = TimeValue(svc.opensHh, svc.opensMm),
            closes = TimeValue(svc.closesHh, svc.closesMm),
            activeDays = svc.activeDays,
            instructor = svc.instructor ?: "",
            requiresBooking = svc.requiresBooking,
        ) else service,
        rental = if (rnt != null) rental.copy(
            unitsCount = rnt.unitsCount.toString(),
            buffer = rnt.bufferMin.toString(),
            opens = TimeValue(rnt.opensHh, rnt.opensMm),
            closes = TimeValue(rnt.closesHh, rnt.closesMm),
            depositAmount = rnt.depositAmt.toString(),
            requiresBooking = rnt.requiresBooking,
        ) else rental,
        advanced = advanced.copy(
            sku = product.sku ?: "",
            barcode = product.barcode ?: "",
            sendOrderTo = product.sendOrderTo ?: "None",
            displayOrder = product.displayOrder.toString(),
        ),
        options = product.optionGroups.map { group ->
            OptionGroup(
                id = group.id,
                name = group.name,
                pickMode = if (group.pickMode == "02") PickMode.MANY else PickMode.ONE,
                required = group.required,
                items = group.items.map {
                    OptionItem(
                        id = it.id,
                        name = it.name,
                        priceModifier = it.priceModifier.toInt()
                    )
                },
            )
        },
    )
}

internal fun MasterEntryViewModel.State.toProductModel(): ProductModel {
    val kind = when (entryType) {
        EntryType.GOODS -> "1000"
        EntryType.SERVICE -> "1001"
        EntryType.RENTAL -> "1002"
    }
    val inv = inventory
    val svc = service
    val rnt = rental
    return ProductModel(
        id = productId ?: "prd-${DateTimeUtils.nowEpochMillis()}",
        kind = kind,
        catId = detail.category,
        taxId = "",
        name = detail.name,
        description = detail.description.ifBlank { null },
        unit = pricing.unit,
        price = pricing.price.toDoubleOrNull() ?: 0.0,
        costPrice = if (showCostPrice) pricing.costPrice.toDoubleOrNull() else null,
        chargeVat = pricing.chargeVat,
        openPrice = pricing.openPrice,
        sku = advanced.sku.ifBlank { null },
        barcode = advanced.barcode.ifBlank { null },
        sendOrderTo = advanced.sendOrderTo.takeIf { it != "None" },
        displayOrder = advanced.displayOrder.toIntOrNull() ?: 0,
        isAvailable = detail.isAvailable,
        isRecommended = detail.isRecommended,
        isActive = true,
        inventory = if (entryType == EntryType.GOODS) InventoryConfig(
            trackStock = inv.trackStock,
            maxCapacity = inv.maxCapacity.toDoubleOrNull() ?: 0.0,
            lowStockAlert = inv.lowStockAlert.toDoubleOrNull() ?: 0.0,
            supplierName = inv.supplier.ifBlank { null },
            trackExpiry = inv.trackActive,
        ) else null,
        service = if (entryType == EntryType.SERVICE) ServiceConfig(
            chargedBy = pricing.chargedBy.toString().padStart(2, '0'),
            capacity = svc.capacity.toIntOrNull() ?: 12,
            durationMin = svc.duration.toIntOrNull() ?: 60,
            opensHh = svc.opens.hour,
            opensMm = svc.opens.minute,
            closesHh = svc.closes.hour,
            closesMm = svc.closes.minute,
            activeDays = svc.activeDays,
            instructor = svc.instructor.ifBlank { null },
            requiresBooking = svc.requiresBooking,
        ) else null,
        rental = if (entryType == EntryType.RENTAL) RentalConfig(
            unitsCount = rnt.unitsCount.toIntOrNull() ?: 2,
            slotDurationMin = pricing.bookingDuration.toIntOrNull() ?: 60,
            minBooking = pricing.minBooking.toIntOrNull() ?: 1,
            bufferMin = rnt.buffer.toIntOrNull() ?: 0,
            opensHh = rnt.opens.hour,
            opensMm = rnt.opens.minute,
            closesHh = rnt.closes.hour,
            closesMm = rnt.closes.minute,
            depositAmt = rnt.depositAmount.toDoubleOrNull() ?: 0.0,
            requiresBooking = rnt.requiresBooking,
        ) else null,
        media = emptyList(),
        optionGroups = options.mapIndexed { index, group ->
            OptionGroupModel(
                id = group.id,
                name = group.name,
                pickMode = if (group.pickMode == PickMode.MANY) "02" else "01",
                required = group.required,
                sortOrder = index,
                items = group.items.mapIndexed { itemIndex, item ->
                    OptionItemModel(
                        id = item.id,
                        name = item.name,
                        priceModifier = item.priceModifier.toDouble(),
                        sortOrder = itemIndex,
                    )
                },
            )
        },
    )
}