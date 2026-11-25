package com.roldanmatias.rickverse.feature.settings

import app.cash.turbine.test
import com.roldanmatias.rickverse.core.domain.model.CachePolicy
import com.roldanmatias.rickverse.core.domain.model.DataSource
import com.roldanmatias.rickverse.core.testing.FakeSettingsRepository
import com.roldanmatias.rickverse.core.testing.MainDispatcherRule
import com.roldanmatias.rickverse.feature.settings.presentation.SettingsUiState
import com.roldanmatias.rickverse.feature.settings.presentation.SettingsViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `selections are written to the repository and reflected in state`() = runTest(mainRule.dispatcher) {
        val settings = FakeSettingsRepository()
        val viewModel = SettingsViewModel(settings)

        viewModel.uiState.test {
            assertEquals(SettingsUiState(), awaitItem())
            viewModel.onDataSourceSelected(DataSource.GraphQl)
            assertEquals(SettingsUiState(dataSource = DataSource.GraphQl), awaitItem())
            viewModel.onCachePolicySelected(CachePolicy.NetworkOnly)
            assertEquals(SettingsUiState(DataSource.GraphQl, CachePolicy.NetworkOnly), awaitItem())
        }
    }
}
