package com.roldanmatias.rickverse.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roldanmatias.rickverse.core.domain.model.CachePolicy
import com.roldanmatias.rickverse.core.domain.model.DataSource
import com.roldanmatias.rickverse.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class SettingsUiState(
    val dataSource: DataSource = DataSource.Rest,
    val cachePolicy: CachePolicy = CachePolicy.CacheFirst,
)

@HiltViewModel
internal class SettingsViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        combine(settings.dataSource, settings.cachePolicy, ::SettingsUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState())

    fun onDataSourceSelected(value: DataSource) {
        viewModelScope.launch { settings.setDataSource(value) }
    }

    fun onCachePolicySelected(value: CachePolicy) {
        viewModelScope.launch { settings.setCachePolicy(value) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
