package com.roldanmatias.rickverse.core.ui.pagination

import com.roldanmatias.rickverse.core.domain.error.DomainError

/**
 * State of the "load the next page" part of a paginated list, shared by every list screen.
 * Plain Kotlin on purpose: ViewModels in `presentation` packages use it and must not import Compose.
 */
public sealed interface PaginationState {
    /** Ready to load the next page when asked. */
    public data object Idle : PaginationState

    /** A page is being fetched. */
    public data object Loading : PaginationState

    /** The last attempt failed; the items already loaded are kept. */
    public data class Failed(
        val error: DomainError,
    ) : PaginationState

    /** There is nothing more to load. */
    public data object Exhausted : PaginationState
}
