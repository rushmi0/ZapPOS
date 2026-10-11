/*
 * MIT License
 * Copyright (c) 2025 SiamDevTeam
 */
package org.siamdev.zappos.ui.components.nav

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.OrbitContainer
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import org.siamdev.zappos.ui.UiContainerSettings

/** Drawer selection, shared by every screen's drawer so it survives navigation. */
class NavigationViewModel :
    ViewModel(),
    OrbitContainerHost<NavigationViewModel.State, NavigationViewModel.State, Nothing> {

    data class State(
        val activeNavId: String = "home",
        val activeSectionId: String? = "SALES"
    )

    override val container: OrbitContainer<State, State, Nothing> =
        orbitContainer(State(), buildSettings = UiContainerSettings)

    fun setActiveNav(id: String) = intent { reduce { state.copy(activeNavId = id) } }

    fun setFilter(sectionId: String?) = intent { reduce { state.copy(activeSectionId = sectionId) } }
}
