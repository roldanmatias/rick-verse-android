package com.roldanmatias.rickverse.feature.characters.presentation

import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.ui.pagination.PaginationState

/** What the cast list screen shows. Pure data: the screen is a function of this. */
internal data class CastListUiState(
    val phase: Phase = Phase.Loading,
    val items: List<CastMember> = emptyList(),
    val pagination: PaginationState = PaginationState.Idle,
    /** What is in the search box right now (updates on every keystroke; the request itself is debounced). */
    val query: String = "",
    val statusFilter: CastMember.Status? = null,
    /** `true` while a pull-to-refresh is in flight. The old [items] stay visible. */
    val isRefreshing: Boolean = false,
    /** The page number to request next, or `null` when there is none. */
    val nextPage: Int? = null,
    /** The list is a saved copy because the network failed: the UI shows a non-blocking banner. */
    val fromCache: Boolean = false,
) {
    sealed interface Phase {
        /** First page is loading and there is nothing to show yet. */
        data object Loading : Phase

        data object Loaded : Phase

        /** The first page arrived and is empty. */
        data object Empty : Phase

        /** The first page failed. With non-empty items, a refresh failed and the list is kept. */
        data class Failed(val error: DomainError) : Phase
    }
}

internal sealed interface CastListIntent {
    data class QueryChanged(val text: String) : CastListIntent

    data class StatusSelected(val status: CastMember.Status?) : CastListIntent

    data object Refresh : CastListIntent

    data object Retry : CastListIntent

    /** The row with this id became the last visible one. May trigger loading the next page. */
    data class ItemAppeared(val id: Int) : CastListIntent

    data object RetryNextPage : CastListIntent
}
