/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.screens.setting

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.orbitmvi.orbit.test.testWithInternalState
import org.siamdev.zappos.data.source.local.model.ThemeItem
import org.siamdev.zappos.ui.FakeSettingSource
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val system = ThemeItem("theme-system", "System", "SYSTEM", isDefault = true)
    private val light = ThemeItem("theme-light", "Light", "LIGHT", isDefault = false)
    private val dark = ThemeItem("theme-dark", "Dark", "DARK", isDefault = false)

    private fun sourceWithThemes() = FakeSettingSource().apply { themes += listOf(system, light, dark) }

    private val themedState = SettingViewModel.State(
        theme = SettingViewModel.State.Theme(options = listOf(system, light, dark), active = system)
    )

    @Test
    fun onCreate_seedsDefaultsAndLoadsSettings() = runTest {
        val source = FakeSettingSource()
        SettingViewModel { source }.testWithInternalState(this) {
            runOnCreate()

            assertTrue(awaitInternalState().isLoading)
            val state = awaitInternalState()
            assertFalse(state.isLoading)
            assertEquals(listOf("theme-system", "theme-light", "theme-dark"), state.theme.options.map { it.id })
            assertEquals("theme-system", state.theme.active?.id)
            assertEquals((12..20).map { "font-size-$it" }, state.font.options.map { it.id })
            assertEquals("THB", state.currency.primaryCode)
            assertEquals("SATS", state.currency.secondaryCode) // fallback while unset
        }
    }

    @Test
    fun onCreate_withoutSource_keepsDefaults() = runTest {
        SettingViewModel { null }.testWithInternalState(this) {
            runOnCreate().join()
            expectNoItems()
        }
    }

    @Test
    fun selectTheme_appliesOptimisticallyAndPersists() = runTest {
        val source = sourceWithThemes()
        SettingViewModel { source }.testWithInternalState(this, initialState = themedState) {
            containerHost.selectTheme(dark.id).join()

            assertEquals(dark, awaitInternalState().theme.active)
            assertEquals(dark.id, source.activeThemeId)
        }
    }

    @Test
    fun selectTheme_writeFailure_rollsBackAndReportsError() = runTest {
        val source = sourceWithThemes().apply { failingThemeWrites[dark.id] = IllegalStateException("disk full") }
        SettingViewModel { source }.testWithInternalState(this, initialState = themedState) {
            containerHost.selectTheme(dark.id)

            assertEquals(dark, awaitInternalState().theme.active)
            assertEquals(system, awaitInternalState().theme.active)
            expectSideEffect(SettingViewModel.SideEffect.Error("disk full"))
        }
    }

    @Test
    fun lateWriteFailure_doesNotRollBackANewerSelection() = runTest {
        val darkWrite = CompletableDeferred<Unit>()
        val source = sourceWithThemes().apply {
            themeWriteGates[dark.id] = darkWrite
            failingThemeWrites[dark.id] = IllegalStateException("disk full")
        }
        SettingViewModel { source }.testWithInternalState(this, initialState = themedState) {
            containerHost.selectTheme(dark.id)
            assertEquals(dark, awaitInternalState().theme.active)

            containerHost.selectTheme(light.id)
            assertEquals(light, awaitInternalState().theme.active)

            darkWrite.complete(Unit)
            // The failed dark write is reported, but the user's newer choice (light) stays.
            expectSideEffect(SettingViewModel.SideEffect.Error("disk full"))
        }
        assertEquals(light.id, source.activeThemeId)
    }

    @Test
    fun toggleSecondaryCurrency_persists() = runTest {
        val source = FakeSettingSource()
        SettingViewModel { source }.testWithInternalState(this) {
            containerHost.toggleSecondaryCurrency(true).join()

            assertTrue(awaitInternalState().currency.showSecondary)
            assertTrue(source.showSecondary)
        }
    }
}
