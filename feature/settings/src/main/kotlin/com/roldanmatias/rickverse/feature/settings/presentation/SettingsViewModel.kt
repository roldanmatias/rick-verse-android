package com.roldanmatias.rickverse.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roldanmatias.rickverse.core.domain.model.CachePolicy
import com.roldanmatias.rickverse.core.domain.model.DataSource
import com.roldanmatias.rickverse.core.domain.repository.CacheMaintenance
import com.roldanmatias.rickverse.core.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class SettingsUiState(
    val dataSource: DataSource = DataSource.Rest,
    val cachePolicy: CachePolicy = CachePolicy.CacheFirst,
    val cacheCleared: Boolean = false,
)

@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val cache: CacheMaintenance,
) : ViewModel() {
    // "Cache cleared" is state the UI acknowledges (not a one-off event), so it survives rotation.
    private val mutableCacheCleared = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> =
        combine(settings.dataSource, settings.cachePolicy, mutableCacheCleared, ::SettingsUiState)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState())

    fun onDataSourceSelected(value: DataSource) {
        viewModelScope.launch { settings.setDataSource(value) }
    }

    fun onCachePolicySelected(value: CachePolicy) {
        viewModelScope.launch { settings.setCachePolicy(value) }
    }

    fun onClearCache() {
        viewModelScope.launch {
            cache.clearCache()
            mutableCacheCleared.value = true
        }
    }

    fun onCacheClearedShown() {
        mutableCacheCleared.value = false
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
