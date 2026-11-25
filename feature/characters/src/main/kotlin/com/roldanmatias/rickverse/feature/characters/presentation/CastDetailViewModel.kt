package com.roldanmatias.rickverse.feature.characters.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.CastMemberDetail
import com.roldanmatias.rickverse.core.domain.usecase.GetCastMemberDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

internal sealed interface CastDetailUiState {
    data object Loading : CastDetailUiState

    data class Loaded(val detail: CastMemberDetail) : CastDetailUiState

    data class Failed(val error: DomainError) : CastDetailUiState
}

/** The character id arrives in [SavedStateHandle] under `"id"`: Navigation puts typed-route arguments there. */
@HiltViewModel
internal class CastDetailViewModel @Inject constructor(
    private val getDetail: GetCastMemberDetailUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val id: Int = checkNotNull(savedStateHandle["id"]) { "CastDetail requires an id argument" }
    private val mutableState = MutableStateFlow<CastDetailUiState>(CastDetailUiState.Loading)
    val uiState: StateFlow<CastDetailUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun retry() {
        mutableState.value = CastDetailUiState.Loading
        load()
    }

    private fun load() {
        viewModelScope.launch {
            mutableState.value = when (val result = getDetail(id)) {
                is DomainResult.Success -> CastDetailUiState.Loaded(result.value)
                is DomainResult.Failure -> CastDetailUiState.Failed(result.error)
            }
        }
    }
}
