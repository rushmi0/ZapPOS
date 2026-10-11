/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.screens.count

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainer
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import org.siamdev.zappos.ui.UiContainerSettings

class CounterViewModel : ViewModel(), OrbitContainerHost<CounterViewModel.State, CounterViewModel.State, Nothing> {

    data class State(val count: Int = 0)

    override val container: OrbitContainer<State, State, Nothing> =
        orbitContainer(State(), buildSettings = UiContainerSettings)

    fun plus() = intent { reduce { state.copy(count = state.count + 1) } }

    fun minus() = intent { reduce { state.copy(count = state.count - 1) } }

    fun reset() = intent { reduce { State() } }
}
