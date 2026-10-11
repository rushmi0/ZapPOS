/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui

import kotlinx.coroutines.Dispatchers
import org.orbitmvi.orbit.SettingsBuilder

/**
 * Orbit settings shared by every screen ViewModel.
 *
 * The event loop runs on `Dispatchers.Main.immediate` – the same dispatcher `viewModelScope` used
 * before Orbit – so a `reduce` triggered from a UI callback is applied synchronously on the main
 * thread. Text fields depend on this: an asynchronous round-trip drops keystrokes, and Orbit's
 * `blockingIntent` is not available on js/wasmJs.
 *
 * Unit tests must install a test Main dispatcher (`Dispatchers.setMain`) because the container
 * resolves this dispatcher when it is created; orbit-test then replaces it with its own.
 */
internal val UiContainerSettings: SettingsBuilder.() -> Unit = {
    eventLoopDispatcher = { Dispatchers.Main.immediate }
}
