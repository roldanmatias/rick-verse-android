package com.roldanmatias.rickverse.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roldanmatias.rickverse.core.network.NetworkEvent
import com.roldanmatias.rickverse.core.network.NetworkLog
import com.roldanmatias.rickverse.core.network.NetworkSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** `null` source means "All". */
internal data class NetworkLogUiState(
    val filter: NetworkSource? = null,
    val events: List<NetworkEvent> = emptyList(),
    val totalRequests: Int = 0,
    val cacheHits: Int = 0,
)

@HiltViewModel
internal class NetworkLogViewModel @Inject constructor(private val log: NetworkLog) : ViewModel() {
    private val filter = MutableStateFlow<NetworkSource?>(null)

    val uiState: StateFlow<NetworkLogUiState> =
        combine(log.events, filter) { events, selected ->
            val visible = events.filter { selected == null || it.source == selected }
            NetworkLogUiState(
                filter = selected,
                events = visible,
                totalRequests = visible.count { !it.servedFromCache },
                cacheHits = visible.count { it.servedFromCache },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), NetworkLogUiState())

    fun onFilterSelected(source: NetworkSource?) {
        filter.value = source
    }

    fun onClear() = log.clear()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
