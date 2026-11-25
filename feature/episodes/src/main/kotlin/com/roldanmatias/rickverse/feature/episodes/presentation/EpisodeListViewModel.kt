package com.roldanmatias.rickverse.feature.episodes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.Episode
import com.roldanmatias.rickverse.core.domain.usecase.GetEpisodesUseCase
import com.roldanmatias.rickverse.core.ui.pagination.PaginationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class EpisodeListUiState(
    val loading: Boolean = true,
    val error: DomainError? = null,
    val items: List<Episode> = emptyList(),
    val pagination: PaginationState = PaginationState.Idle,
    val nextPage: Int? = null,
)

/** Paginated episodes, grouped by season in the UI. One load runs at a time (`inFlight`). */
@HiltViewModel
internal class EpisodeListViewModel @Inject constructor(private val getEpisodes: GetEpisodesUseCase) : ViewModel() {
    private val mutableState = MutableStateFlow(EpisodeListUiState())
    val uiState: StateFlow<EpisodeListUiState> = mutableState.asStateFlow()
    private var inFlight: Job? = null

    init {
        loadFirst()
    }

    fun retry() {
        mutableState.value = EpisodeListUiState()
        loadFirst()
    }

    fun loadMore() {
        val state = mutableState.value
        val page = state.nextPage ?: return
        if (inFlight?.isActive == true || state.pagination is PaginationState.Loading) return
        mutableState.update { it.copy(pagination = PaginationState.Loading) }
        inFlight = viewModelScope.launch {
            when (val result = getEpisodes(page)) {
                is DomainResult.Success -> mutableState.update {
                    it.copy(
                        items = (it.items + result.value.items).distinctBy(Episode::id),
                        nextPage = result.value.nextPage,
                        pagination = paginationFor(result.value.nextPage),
                    )
                }

                is DomainResult.Failure -> mutableState.update {
                    it.copy(pagination = PaginationState.Failed(result.error))
                }
            }
        }
    }

    private fun loadFirst() {
        inFlight = viewModelScope.launch {
            mutableState.value = when (val result = getEpisodes(1)) {
                is DomainResult.Success -> EpisodeListUiState(
                    loading = false,
                    items = result.value.items,
                    nextPage = result.value.nextPage,
                    pagination = paginationFor(result.value.nextPage),
                )

                is DomainResult.Failure -> EpisodeListUiState(loading = false, error = result.error)
            }
        }
    }

    private fun paginationFor(nextPage: Int?) =
        if (nextPage == null) PaginationState.Exhausted else PaginationState.Idle
}
