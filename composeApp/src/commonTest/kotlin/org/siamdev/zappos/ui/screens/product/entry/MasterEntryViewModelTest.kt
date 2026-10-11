/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.screens.product.entry

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.orbitmvi.orbit.test.testWithInternalState
import org.siamdev.zappos.ui.FakeProductSource
import org.siamdev.zappos.ui.testProduct
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MasterEntryViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val validForm = MasterEntryViewModel.State(
        detail = MasterEntryViewModel.State.Detail(name = "Iced Tea", category = "beverages"),
        pricing = MasterEntryViewModel.State.Pricing(price = "45", unit = "cup"),
    )

    @Test
    fun createMode_startsWithEmptyForm() = runTest {
        MasterEntryViewModel(productId = null) { FakeProductSource() }.testWithInternalState(this) {
            runOnCreate().join()
            expectNoItems()
            assertFalse(containerHost.container.stateFlow.value.isEditMode)
        }
    }

    @Test
    fun editMode_loadsProductOnce() = runTest {
        val source = FakeProductSource(products = mapOf("prd-1" to testProduct()))
        MasterEntryViewModel(productId = "prd-1") { source }.testWithInternalState(this) {
            runOnCreate()

            val form = awaitInternalState()
            assertFalse(form.isLoading)
            assertTrue(form.isEditMode)
            assertEquals("Green Tea", form.detail.name)
            assertEquals("55", form.pricing.price)
            assertEquals("cup", form.pricing.unit)
            assertTrue(form.showCostPrice)
            assertEquals("20", form.pricing.costPrice)
            assertEquals("Leaf Co", form.inventory.supplier)
            assertEquals("GT-01", form.advanced.sku)
        }
    }

    @Test
    fun editMode_loadFailure_stopsLoadingAndReportsError() = runTest {
        val source = FakeProductSource(getError = IllegalStateException("db locked"))
        MasterEntryViewModel(productId = "prd-1") { source }.testWithInternalState(this) {
            runOnCreate()

            assertFalse(awaitInternalState().isLoading)
            expectSideEffect(MasterEntryViewModel.SideEffect.Error("db locked"))
        }
    }

    @Test
    fun formRules_entryTypeSetsDefaultUnitAndCategoryClearsSubCategory() {
        val service = MasterEntryViewModel.State().withEntryType(EntryType.SERVICE)
        assertEquals("hour", service.pricing.unit)
        assertEquals(unitOptionsFor(EntryType.SERVICE), service.unitOptions)

        val withSub = validForm.copy(detail = validForm.detail.copy(subCategory = "tea"))
        val recategorised = withSub.withCategory("food")
        assertEquals("food", recategorised.detail.category)
        assertNull(recategorised.detail.subCategory)

        assertTrue(validForm.isFormValid)
        assertFalse(validForm.copy(pricing = validForm.pricing.copy(price = "")).isFormValid)
    }

    @Test
    fun edit_appliesFieldChange() = runTest {
        MasterEntryViewModel { FakeProductSource() }.testWithInternalState(this) {
            containerHost.edit { copy(detail = detail.copy(name = "Thai Tea")) }
            assertEquals("Thai Tea", awaitInternalState().detail.name)
        }
    }

    @Test
    fun save_invalidForm_doesNothing() = runTest {
        val source = FakeProductSource()
        MasterEntryViewModel { source }.testWithInternalState(this) {
            containerHost.save().join()
            expectNoItems()
        }
        assertTrue(source.saved.isEmpty())
    }

    @Test
    fun save_success_persistsOnceAndNavigates() = runTest {
        val gate = CompletableDeferred<Unit>()
        val source = FakeProductSource().apply { saveGate = gate }
        MasterEntryViewModel { source }.testWithInternalState(this, initialState = validForm) {
            containerHost.save()
            assertTrue(awaitInternalState().isSaving)

            // A second tap while saving is ignored – no duplicate product.
            containerHost.save().join()

            gate.complete(Unit)
            assertFalse(awaitInternalState().isSaving)
            expectSideEffect(MasterEntryViewModel.SideEffect.SaveSuccess)
        }
        assertEquals(1, source.saved.size)
        assertEquals("Iced Tea", source.saved.single().name)
        assertEquals(45.0, source.saved.single().price)
    }

    @Test
    fun save_failure_reportsError() = runTest {
        val gate = CompletableDeferred<Unit>()
        val source = FakeProductSource(saveResult = Result.failure(IllegalStateException("duplicate sku")))
            .apply { saveGate = gate }
        MasterEntryViewModel { source }.testWithInternalState(this, initialState = validForm) {
            containerHost.save()
            assertTrue(awaitInternalState().isSaving)
            gate.complete(Unit)
            assertFalse(awaitInternalState().isSaving)
            expectSideEffect(MasterEntryViewModel.SideEffect.Error("duplicate sku"))
        }
    }

    @Test
    fun discard_navigatesBack() = runTest {
        MasterEntryViewModel { FakeProductSource() }.testWithInternalState(this) {
            containerHost.discard()
            expectSideEffect(MasterEntryViewModel.SideEffect.NavigateBack)
        }
    }
}
