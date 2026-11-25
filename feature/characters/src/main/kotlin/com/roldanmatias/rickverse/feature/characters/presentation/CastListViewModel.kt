package com.roldanmatias.rickverse.feature.characters.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.domain.model.Page
import com.roldanmatias.rickverse.core.domain.usecase.SearchCastUseCase
import com.roldanmatias.rickverse.core.ui.pagination.PaginationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Cast list: debounced search, status filter, pagination, pull-to-refresh.
 *
 * How the pieces answer the classic questions:
 * * **Stale responses**: all loading for one set of criteria happens inside one `channelFlow` under
 *   `flatMapLatest`. A new query/filter/refresh cancels the previous one, *including its in-flight next-page
 *   request*, so an old answer can never overwrite or append to a newer list. Even a fake that ignores
 *   cancellation cannot do harm: `send` on a cancelled channel throws.
 * * **Cancellation is silent**: `CancellationException` is never turned into a `Failed` state.
 * * **State survives rotation and process death**: `query` and `statusFilter` live in [SavedStateHandle].
 *   Items are reloaded.
 * * **`stateIn(WhileSubscribed(5_000))`**: the pipeline runs only while the UI collects, and the 5 s grace keeps it
 *   alive across a configuration change so rotation does not restart the request.
 */
@HiltViewModel
internal class CastListViewModel
    @Inject
    constructor(
        private val searchCast: SearchCastUseCase,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val query: StateFlow<String> = savedStateHandle.getStateFlow(KEY_QUERY, "")
        private val statusName: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_STATUS, null)
        private val status: Flow<CastMember.Status?> =
            statusName.map { name -> CastMember.Status.entries.find { it.name == name } }

        private val refreshTick = MutableStateFlow(0)

        /** Page numbers the UI asked to load. Carrying the page makes duplicate requests harmless (see [load]). */
        private val loadMoreRequests =
            MutableSharedFlow<Int>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

        private var lastCriteria: Criteria? = null
        private var lastContent: Content = Content()

        val uiState: StateFlow<CastListUiState> =
            combine(content(), query, status) { content, query, status ->
                CastListUiState(
                    phase = content.phase,
                    items = content.items,
                    pagination = content.pagination,
                    query = query,
                    statusFilter = status,
                    isRefreshing = content.isRefreshing,
                    nextPage = content.nextPage,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = CastListUiState(query = query.value),
            )

        fun onIntent(intent: CastListIntent) {
            when (intent) {
                is CastListIntent.QueryChanged -> {
                    savedStateHandle[KEY_QUERY] = intent.text
                }

                is CastListIntent.StatusSelected -> {
                    savedStateHandle[KEY_STATUS] = intent.status?.name
                }

                CastListIntent.Refresh, CastListIntent.Retry -> {
                    refreshTick.update { it + 1 }
                }

                is CastListIntent.ItemAppeared -> {
                    loadMoreIfNeeded(intent.id)
                }

                CastListIntent.RetryNextPage -> {
                    val state = uiState.value
                    if (state.pagination is PaginationState.Failed) state.nextPage?.let(loadMoreRequests::tryEmit)
                }
            }
        }

        private fun loadMoreIfNeeded(visibleId: Int) {
            val state = uiState.value
            val nextPage = state.nextPage ?: return
            val index = state.items.indexOfFirst { it.id == visibleId }
            val nearEnd = index >= state.items.size - PREFETCH_DISTANCE
            if (index >= 0 && nearEnd && state.pagination == PaginationState.Idle) loadMoreRequests.tryEmit(nextPage)
        }

        // --- loading pipeline ---------------------------------------------------------------------------------

        @OptIn(FlowPreview::class)
        private fun criteria(): Flow<Criteria> {
            // The first value is immediate (no 400 ms wait on open); later keystrokes are debounced.
            val debouncedQuery =
                merge(query.take(1), query.drop(1).debounce(DEBOUNCE_MILLIS))
                    .map { it.trim() }
                    .distinctUntilChanged()
            // The status filter and refresh bypass the debounce: they are discrete taps, not typing.
            return combine(debouncedQuery, status, refreshTick, ::Criteria)
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun content(): Flow<Content> =
            criteria().flatMapLatest { criteria -> load(criteria) }.onEach { lastContent = it }

        private fun load(criteria: Criteria): Flow<Content> =
            channelFlow {
                val previous = lastCriteria
                lastCriteria = criteria
                val isRefresh =
                    previous != null &&
                        previous.query == criteria.query && previous.status == criteria.status &&
                        previous.refreshTick != criteria.refreshTick
                val keptItems = if (isRefresh) lastContent.items else emptyList()

                // Refresh keeps the current list visible until page 1 arrives; a new search shows the skeleton.
                send(
                    if (keptItems.isNotEmpty()) lastContent.copy(isRefreshing = true) else Content(),
                )

                val first = searchCast(page = FIRST_PAGE, query = criteria.query, status = criteria.status)
                when (val result = first) {
                    is DomainResult.Failure -> {
                        send(Content(phase = CastListUiState.Phase.Failed(result.error), items = keptItems))
                    }

                    is DomainResult.Success -> {
                        var current = result.value.toContent(emptyList())
                        send(current)
                        // Serve "load page N" while these criteria are current (flatMapLatest cancels us).
                        loadMoreRequests.collect { requestedPage ->
                            // Any page but the next one is a duplicate (ItemAppeared fired twice) or stale.
                            if (requestedPage != current.nextPage) return@collect
                            send(current.copy(pagination = PaginationState.Loading))
                            current = fetchNext(current, requestedPage, criteria)
                            send(current)
                        }
                    }
                }
            }

        private suspend fun fetchNext(current: Content, page: Int, criteria: Criteria): Content =
            when (val result = searchCast(page, criteria.query, criteria.status)) {
                is DomainResult.Success -> result.value.toContent(current.items)

                // Items are kept and nextPage unchanged, so a retry asks for the same page.
                is DomainResult.Failure -> current.copy(pagination = PaginationState.Failed(result.error))
            }

        private fun Page<CastMember>.toContent(existing: List<CastMember>): Content {
            // De-duplicate by id: pages can overlap when the backend data changes between requests.
            val merged = (existing + items).distinctBy { it.id }
            return Content(
                phase = if (merged.isEmpty()) CastListUiState.Phase.Empty else CastListUiState.Phase.Loaded,
                items = merged,
                pagination = if (nextPage == null) PaginationState.Exhausted else PaginationState.Idle,
                nextPage = nextPage,
            )
        }

        private data class Criteria(
            val query: String,
            val status: CastMember.Status?,
            val refreshTick: Int,
        )

        private data class Content(
            val phase: CastListUiState.Phase = CastListUiState.Phase.Loading,
            val items: List<CastMember> = emptyList(),
            val pagination: PaginationState = PaginationState.Idle,
            val isRefreshing: Boolean = false,
            val nextPage: Int? = null,
        )

        private companion object {
            const val KEY_QUERY = "query"
            const val KEY_STATUS = "status"
            const val FIRST_PAGE = 1
            const val DEBOUNCE_MILLIS = 400L
            const val STOP_TIMEOUT_MILLIS = 5_000L
            const val PREFETCH_DISTANCE = 5
        }
    }
