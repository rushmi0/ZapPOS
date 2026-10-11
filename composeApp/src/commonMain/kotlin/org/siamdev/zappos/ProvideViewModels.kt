/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.viewmodel.compose.viewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.siamdev.zappos.ui.components.nav.NavigationViewModel
import org.siamdev.zappos.ui.screens.count.CounterViewModel
import org.siamdev.zappos.ui.screens.sale.MainMenuViewModel
import org.siamdev.zappos.ui.screens.sale.checkout.CheckoutViewModel
import org.siamdev.zappos.ui.screens.setting.SettingViewModel

/**
 * Read-only app settings (currency, theme…) for any composable. Collected once in
 * [ProvideViewModels]; the default lets previews render without a ViewModel.
 */
val LocalSettingState = compositionLocalOf { SettingViewModel.State() }

/** For screens that change settings. Read state from [LocalSettingState] instead. */
val LocalSettingVM = staticCompositionLocalOf<SettingViewModel> {
    error("Missing ProvideViewModels in composition tree")
}

val LocalMenuVM = staticCompositionLocalOf<MainMenuViewModel> {
    error("Missing ProvideViewModels in composition tree")
}

val LocalCheckoutVM = staticCompositionLocalOf<CheckoutViewModel> {
    error("Missing ProvideViewModels in composition tree")
}

val LocalCounterVM = staticCompositionLocalOf<CounterViewModel> {
    error("Missing ProvideViewModels in composition tree")
}

val LocalNavigationVM = staticCompositionLocalOf<NavigationViewModel> {
    error("Missing ProvideViewModels in composition tree")
}

/**
 * App-scoped ViewModels: their state outlives individual back-stack entries (the cart is shared
 * by Menu → Confirm → Checkout, the drawer selection by every screen).
 */
@Composable
fun ProvideViewModels(content: @Composable () -> Unit) {
    val settingVM = viewModel { SettingViewModel() }
    val menuVM = viewModel { MainMenuViewModel() }
    val checkoutVM = viewModel { CheckoutViewModel() }
    val counterVM = viewModel { CounterViewModel() }
    val navigationVM = viewModel { NavigationViewModel() }

    val settingState by settingVM.collectAsState()

    CompositionLocalProvider(
        LocalSettingState provides settingState,
        LocalSettingVM provides settingVM,
        LocalMenuVM provides menuVM,
        LocalCheckoutVM provides checkoutVM,
        LocalCounterVM provides counterVM,
        LocalNavigationVM provides navigationVM,
        content = content
    )
}
