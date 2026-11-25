package com.roldanmatias.rickverse.feature.characters.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.CastFilter
import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.domain.model.Page
import com.roldanmatias.rickverse.core.domain.usecase.SearchCastUseCase
import com.roldanmatias.rickverse.core.testing.FakeCastRepository
import com.roldanmatias.rickverse.core.testing.MainDispatcherRule
import com.roldanmatias.rickverse.core.testing.TestData
import com.roldanmatias.rickverse.core.ui.pagination.PaginationState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CastListViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeCastRepository()

    /** 3 pages of 20: ids 1..20, 21..40, 41..60. */
    private fun threePages() {
        repository.castHandler = { page, _ ->
            DomainResult.Success(
                Page(
                    items = TestData.castMembers(count = 20, from = (page - 1) * 20 + 1),
                    nextPage = if (page < 3) page + 1 else null,
                ),
            )
        }
    }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        CastListViewModel(SearchCastUseCase(repository), handle)

    /** Keeps `uiState` hot (the pipeline only runs while subscribed) and returns the viewmodel. */
    private fun TestScope.started(viewModel: CastListViewModel): CastListViewModel {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }
        return viewModel
    }

    @Test
    fun `init goes from Loading to Loaded`() =
        runTest(mainRule.dispatcher) {
            threePages()

            viewModel().uiState.test {
                assertEquals(CastListUiState.Phase.Loading, awaitItem().phase)
                val loaded = awaitItem()
                assertEquals(CastListUiState.Phase.Loaded, loaded.phase)
                assertEquals(20, loaded.items.size)
                assertEquals(PaginationState.Idle, loaded.pagination)
            }
        }

    @Test
    fun `empty result shows Empty`() =
        runTest(mainRule.dispatcher) {
            repository.castHandler = { _, _ -> DomainResult.Success(Page.empty()) }

            viewModel().uiState.test {
                awaitItem()
                assertEquals(CastListUiState.Phase.Empty, awaitItem().phase)
            }
        }

    @Test
    fun `failure shows Failed with the domain error`() =
        runTest(mainRule.dispatcher) {
            repository.castHandler = { _, _ -> DomainResult.Failure(DomainError.Offline) }

            viewModel().uiState.test {
                awaitItem()
                assertEquals(CastListUiState.Phase.Failed(DomainError.Offline), awaitItem().phase)
            }
        }

    @Test
    fun `retry after failure loads again`() =
        runTest(mainRule.dispatcher) {
            repository.castHandler = { _, _ -> DomainResult.Failure(DomainError.Offline) }
            val vm = started(viewModel())
            advanceUntilIdle()
            threePages()

            vm.onIntent(CastListIntent.Retry)
            advanceUntilIdle()

            assertEquals(CastListUiState.Phase.Loaded, vm.uiState.value.phase)
        }

    @Test
    fun `cancellation never becomes an error state`() =
        runTest(mainRule.dispatcher) {
            repository.castHandler = { _, _ -> throw CancellationException("cancelled") }
            val vm = started(viewModel())

            advanceUntilIdle()

            assertTrue(
                "state must not be Failed: ${vm.uiState.value.phase}",
                vm.uiState.value.phase !is CastListUiState.Phase.Failed,
            )
        }

    @Test
    fun `typing r, ri, ric, rick within 400 ms triggers exactly one search`() =
        runTest(mainRule.dispatcher) {
            threePages()
            val vm = started(viewModel())
            advanceUntilIdle()
            repository.castCalls.size.let { assertEquals(1, it) } // the initial, empty query

            listOf("r", "ri", "ric", "rick").forEach {
                vm.onIntent(CastListIntent.QueryChanged(it))
                advanceTimeBy(100)
            }
            advanceUntilIdle()

            val searches = repository.castCalls.drop(1)
            assertEquals(1, searches.size)
            assertEquals("rick", searches.single().filter.name)
            assertEquals("rick", vm.uiState.value.query)
        }

    @Test
    fun `search box updates immediately even though the request is debounced`() =
        runTest(mainRule.dispatcher) {
            threePages()
            val vm = started(viewModel())
            advanceUntilIdle()

            vm.onIntent(CastListIntent.QueryChanged("ri"))
            advanceTimeBy(10)

            assertEquals("ri", vm.uiState.value.query)
            assertEquals(1, repository.castCalls.size)
        }

    @Test
    fun `stale response cannot overwrite the newer search, even if the slow call ignores cancellation`() =
        runTest(mainRule.dispatcher) {
            repository.castHandler = { _, filter ->
                when (filter.name) {
                    "ri" -> {
                        withContext(NonCancellable) {
                            delay(2_000) // slower than "rick", and deaf to cancellation
                            DomainResult.Success(Page(listOf(TestData.castMember(1, name = "STALE ri")), null))
                        }
                    }

                    "rick" -> {
                        DomainResult.Success(Page(listOf(TestData.castMember(2, name = "Rick")), null))
                    }

                    else -> {
                        DomainResult.Success(Page.empty())
                    }
                }
            }
            val vm = started(viewModel())
            advanceUntilIdle()

            vm.onIntent(CastListIntent.QueryChanged("ri"))
            advanceTimeBy(500) // "ri" request is now in flight
            vm.onIntent(CastListIntent.QueryChanged("rick"))
            advanceUntilIdle()

            assertEquals(
                listOf("Rick"),
                vm.uiState.value.items
                    .map { it.name },
            )
        }

    @Test
    fun `status filter bypasses the debounce and reaches the repository`() =
        runTest(mainRule.dispatcher) {
            threePages()
            val vm = started(viewModel())
            advanceUntilIdle()

            vm.onIntent(CastListIntent.StatusSelected(CastMember.Status.Dead))
            advanceTimeBy(1) // far less than the 400 ms debounce

            assertEquals(CastFilter(name = null, status = CastMember.Status.Dead), repository.castCalls.last().filter)
            assertEquals(CastMember.Status.Dead, vm.uiState.value.statusFilter)
        }

    @Test
    fun `last item appearing loads page 2 once even if sent twice`() =
        runTest(mainRule.dispatcher) {
            threePages()
            val vm = started(viewModel())
            advanceUntilIdle()

            vm.onIntent(CastListIntent.ItemAppeared(20))
            vm.onIntent(CastListIntent.ItemAppeared(20))
            advanceUntilIdle()

            assertEquals(listOf(1, 2), repository.castCalls.map { it.page })
            assertEquals(40, vm.uiState.value.items.size)
        }

    @Test
    fun `an item far from the end does not trigger a load`() =
        runTest(mainRule.dispatcher) {
            threePages()
            val vm = started(viewModel())
            advanceUntilIdle()

            vm.onIntent(CastListIntent.ItemAppeared(3))
            advanceUntilIdle()

            assertEquals(listOf(1), repository.castCalls.map { it.page })
        }

    @Test
    fun `page 2 failure keeps page 1 items and retry loads it`() =
        runTest(mainRule.dispatcher) {
            threePages()
            val vm = started(viewModel())
            advanceUntilIdle()
            val ok = repository.castHandler
            repository.castHandler = { page, filter ->
                if (page == 2) DomainResult.Failure(DomainError.Timeout) else ok(page, filter)
            }

            vm.onIntent(CastListIntent.ItemAppeared(20))
            advanceUntilIdle()

            assertEquals(20, vm.uiState.value.items.size)
            assertEquals(PaginationState.Failed(DomainError.Timeout), vm.uiState.value.pagination)

            repository.castHandler = ok
            vm.onIntent(CastListIntent.RetryNextPage)
            advanceUntilIdle()

            assertEquals(40, vm.uiState.value.items.size)
            assertEquals(PaginationState.Idle, vm.uiState.value.pagination)
        }

    @Test
    fun `last page is Exhausted`() =
        runTest(mainRule.dispatcher) {
            repository.castHandler = { _, _ -> DomainResult.Success(Page(TestData.castMembers(3), nextPage = null)) }
            val vm = started(viewModel())

            advanceUntilIdle()

            assertEquals(PaginationState.Exhausted, vm.uiState.value.pagination)
        }

    @Test
    fun `overlapping pages are de-duplicated by id`() =
        runTest(mainRule.dispatcher) {
            repository.castHandler = { page, _ ->
                DomainResult.Success(
                    Page(
                        TestData.castMembers(count = 20, from = (page - 1) * 10 + 1),
                        if (page <
                            2
                        ) {
                            2
                        } else {
                            null
                        },
                    ),
                )
            }
            val vm = started(viewModel())
            advanceUntilIdle()

            vm.onIntent(CastListIntent.ItemAppeared(20))
            advanceUntilIdle()

            assertEquals(
                (1..30).toList(),
                vm.uiState.value.items
                    .map { it.id },
            )
        }

    @Test
    fun `refresh keeps the current items visible until page 1 arrives`() =
        runTest(mainRule.dispatcher) {
            threePages()
            val vm = started(viewModel())
            advanceUntilIdle()
            val fast = repository.castHandler
            repository.castHandler = { page, filter ->
                delay(1_000)
                fast(page, filter)
            }

            vm.onIntent(CastListIntent.Refresh)
            advanceTimeBy(500)

            val during = vm.uiState.value
            assertEquals(20, during.items.size)
            assertTrue(during.isRefreshing)

            advanceUntilIdle()
            assertTrue(!vm.uiState.value.isRefreshing)
        }

    @Test
    fun `failed refresh keeps the list`() =
        runTest(mainRule.dispatcher) {
            threePages()
            val vm = started(viewModel())
            advanceUntilIdle()
            repository.castHandler = { _, _ -> DomainResult.Failure(DomainError.Offline) }

            vm.onIntent(CastListIntent.Refresh)
            advanceUntilIdle()

            assertEquals(20, vm.uiState.value.items.size)
            assertEquals(CastListUiState.Phase.Failed(DomainError.Offline), vm.uiState.value.phase)
        }

    @Test
    fun `query and status are restored from SavedStateHandle`() =
        runTest(mainRule.dispatcher) {
            threePages()
            val handle = SavedStateHandle(mapOf("query" to "morty", "status" to "Dead"))

            val vm = started(viewModel(handle))
            advanceUntilIdle()

            val call = repository.castCalls.first()
            assertEquals(CastFilter(name = "morty", status = CastMember.Status.Dead), call.filter)
            assertEquals("morty", vm.uiState.first().query)
            assertEquals(CastMember.Status.Dead, vm.uiState.value.statusFilter)
        }
}
