/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.screens.setting

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CancellationException
import org.orbitmvi.orbit.OrbitContainer
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer
import org.siamdev.zappos.data.source.local.LocalSources
import org.siamdev.zappos.data.source.local.contract.SettingSource
import org.siamdev.zappos.data.source.local.model.CurrencyItem
import org.siamdev.zappos.data.source.local.model.FontItem
import org.siamdev.zappos.data.source.local.model.ThemeItem
import org.siamdev.zappos.ui.UiContainerSettings

/**
 * App-wide settings (theme, font, currency, accent colour).
 *
 * The source is looked up on every use rather than captured, because the web targets register
 * [LocalSources] asynchronously after the UI has started.
 */
class SettingViewModel(
    private val source: () -> SettingSource? = { LocalSources.setting },
) : ViewModel(), OrbitContainerHost<SettingViewModel.State, SettingViewModel.State, SettingViewModel.SideEffect> {

    data class State(
        val isLoading: Boolean = false,
        val theme: Theme = Theme(),
        val font: Font = Font(),
        val currency: Currency = Currency(),
        val appearance: Appearance = Appearance()
    ) {
        data class Theme(
            val options: List<ThemeItem> = emptyList(),
            val active: ThemeItem? = null
        )

        data class Font(
            val options: List<FontItem> = emptyList(),
            val active: FontItem? = null
        )

        data class Currency(
            val options: List<CurrencyItem> = emptyList(),
            val primary: CurrencyItem? = null,
            val secondary: CurrencyItem? = null,
            val showSecondary: Boolean = false
        ) {
            val primaryCode: String get() = primary?.code ?: "THB"
            val secondaryCode: String get() = secondary?.code ?: "SATS"
        }

        data class Appearance(val accentColor: String? = null)
    }

    sealed interface SideEffect {
        data class Error(val message: String) : SideEffect
    }

    override val container: OrbitContainer<State, State, SideEffect> =
        orbitContainer(State(), buildSettings = UiContainerSettings) { loadAll() }

    private suspend fun Syntax<State, SideEffect>.loadAll() {
        val src = source() ?: return

        reduce { state.copy(isLoading = true) }
        try {
            seedDefaults(src)
            val loaded = State(
                theme = State.Theme(options = src.getThemes(), active = src.getActiveTheme()),
                font = State.Font(options = src.getFonts(), active = src.getActiveFont()),
                currency = State.Currency(
                    options = src.getCurrencies(),
                    primary = src.getPrimaryCurrency(),
                    secondary = src.getSecondaryCurrency(),
                    showSecondary = src.getShowSecondaryCurrency()
                ),
                appearance = State.Appearance(accentColor = src.getAccentColor())
            )
            reduce { loaded }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reduce { state.copy(isLoading = false) }
            postSideEffect(SideEffect.Error(e.message ?: "Could not load settings"))
        }
    }

    private suspend fun seedDefaults(src: SettingSource) {
        if (src.getThemes().isEmpty()) {
            src.seedTheme(id = "theme-system", name = "System", mode = "SYSTEM", isDefault = true)
            src.seedTheme(id = "theme-light", name = "Light", mode = "LIGHT", isDefault = false)
            src.seedTheme(id = "theme-dark", name = "Dark", mode = "DARK", isDefault = false)
        }

        val existingSizes = src.getFonts().map { it.size.toInt() }.toSet()
        for (size in 12..20) {
            if (size !in existingSizes) {
                src.seedFont(id = "font-size-$size", name = "$size", size = size.toDouble())
            }
        }

        if (src.getCurrencies().isEmpty()) {
            src.seedCurrency(id = "ccy-thb", code = "THB", name = "Thai Baht", symbol = "฿")
            src.seedCurrency(id = "ccy-usd", code = "USD", name = "US Dollar", symbol = "$")
            src.seedCurrency(id = "ccy-btc", code = "BTC", name = "Bitcoin", symbol = "₿")
            src.seedCurrency(id = "ccy-sats", code = "SATS", name = "Satoshi", symbol = "⚡")
        }

        src.initSettings()

        if (src.getActiveTheme() == null) {
            src.getThemes().find { it.isDefault }?.let { src.setActiveTheme(it.id) }
        }

        if (src.getActiveFont() == null) {
            src.getFonts().find { it.id == "font-default" }?.let { src.setActiveFont(it.id) }
        }

        if (src.getPrimaryCurrency() == null) {
            src.getCurrencies().find { it.code == "THB" }?.let { src.setPrimaryCurrency(it.id) }
        }
    }

    fun selectTheme(themeId: String) = intent {
        val previous = state.theme.active
        reduce { state.copy(theme = state.theme.copy(active = state.theme.options.find { it.id == themeId })) }
        persist(
            write = { setActiveTheme(themeId) },
            isStillApplied = { theme.active?.id == themeId },
            rollback = { copy(theme = theme.copy(active = previous)) },
        )
    }

    fun selectFont(fontId: String) = intent {
        val previous = state.font.active
        reduce { state.copy(font = state.font.copy(active = state.font.options.find { it.id == fontId })) }
        persist(
            write = { setActiveFont(fontId) },
            isStillApplied = { font.active?.id == fontId },
            rollback = { copy(font = font.copy(active = previous)) },
        )
    }

    fun selectPrimaryCurrency(currencyId: String) = intent {
        val previous = state.currency.primary
        reduce {
            state.copy(currency = state.currency.copy(primary = state.currency.options.find { it.id == currencyId }))
        }
        persist(
            write = { setPrimaryCurrency(currencyId) },
            isStillApplied = { currency.primary?.id == currencyId },
            rollback = { copy(currency = currency.copy(primary = previous)) },
        )
    }

    fun selectSecondaryCurrency(currencyId: String) = intent {
        val previous = state.currency.secondary
        reduce {
            state.copy(currency = state.currency.copy(secondary = state.currency.options.find { it.id == currencyId }))
        }
        persist(
            write = { setSecondaryCurrency(currencyId) },
            isStillApplied = { currency.secondary?.id == currencyId },
            rollback = { copy(currency = currency.copy(secondary = previous)) },
        )
    }

    fun toggleSecondaryCurrency(show: Boolean) = intent {
        val previous = state.currency.showSecondary
        reduce { state.copy(currency = state.currency.copy(showSecondary = show)) }
        persist(
            write = { setShowSecondaryCurrency(show) },
            isStillApplied = { currency.showSecondary == show },
            rollback = { copy(currency = currency.copy(showSecondary = previous)) },
        )
    }

    fun selectAccentColor(hex: String) = intent {
        val previous = state.appearance.accentColor
        reduce { state.copy(appearance = state.appearance.copy(accentColor = hex)) }
        persist(
            write = { setAccentColor(hex) },
            isStillApplied = { appearance.accentColor == hex },
            rollback = { copy(appearance = appearance.copy(accentColor = previous)) },
        )
    }

    /**
     * Writes an optimistic change through to the source. On failure the change is rolled back –
     * but only while it is still the applied value, so a late failure never clobbers a newer
     * selection – and the error is reported to the UI.
     */
    private suspend fun Syntax<State, SideEffect>.persist(
        write: suspend SettingSource.() -> Unit,
        isStillApplied: State.() -> Boolean,
        rollback: State.() -> State,
    ) {
        val src = source() ?: return
        try {
            src.write()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reduce { if (state.isStillApplied()) state.rollback() else state }
            postSideEffect(SideEffect.Error(e.message ?: "Operation failed"))
        }
    }
}
