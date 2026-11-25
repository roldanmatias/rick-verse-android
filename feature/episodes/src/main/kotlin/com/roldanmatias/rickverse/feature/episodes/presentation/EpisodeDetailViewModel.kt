package com.roldanmatias.rickverse.feature.episodes.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.EpisodeDetail
import com.roldanmatias.rickverse.core.domain.usecase.GetEpisodeDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal sealed interface EpisodeDetailUiState {
    data object Loading : EpisodeDetailUiState

    data class Loaded(val detail: EpisodeDetail) : EpisodeDetailUiState

    data class Failed(val error: DomainError) : EpisodeDetailUiState
}

@HiltViewModel
internal class EpisodeDetailViewModel @Inject constructor(
    private val getDetail: GetEpisodeDetailUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val id: Int = checkNotNull(savedStateHandle["id"]) { "EpisodeDetail requires an id argument" }
    private val mutableState = MutableStateFlow<EpisodeDetailUiState>(EpisodeDetailUiState.Loading)
    val uiState: StateFlow<EpisodeDetailUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        mutableState.value = EpisodeDetailUiState.Loading
        load()
    }

    private fun load() {
        viewModelScope.launch {
            mutableState.value = when (val result = getDetail(id)) {
                is DomainResult.Success -> EpisodeDetailUiState.Loaded(result.value)
                is DomainResult.Failure -> EpisodeDetailUiState.Failed(result.error)
            }
        }
    }
}
