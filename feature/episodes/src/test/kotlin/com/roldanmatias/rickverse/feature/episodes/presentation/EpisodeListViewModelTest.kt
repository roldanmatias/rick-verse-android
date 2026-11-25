package com.roldanmatias.rickverse.feature.episodes.presentation

import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.Page
import com.roldanmatias.rickverse.core.domain.usecase.GetEpisodesUseCase
import com.roldanmatias.rickverse.core.testing.FakeEpisodeRepository
import com.roldanmatias.rickverse.core.testing.MainDispatcherRule
import com.roldanmatias.rickverse.core.testing.TestData
import com.roldanmatias.rickverse.core.ui.pagination.PaginationState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EpisodeListViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeEpisodeRepository()

    private fun twoPages() {
        repository.episodesHandler = { page ->
            val first = (page - 1) * 3 + 1
            DomainResult.Success(
                Page((first until first + 3).map { TestData.episode(id = it) }, if (page < 2) 2 else null),
            )
        }
    }

    private fun viewModel() = EpisodeListViewModel(GetEpisodesUseCase(repository))

    @Test
    fun `first page loads and exposes the next page`() = runTest(mainRule.dispatcher) {
        twoPages()
        val viewModel = viewModel()
        assertEquals(true, viewModel.uiState.value.loading)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(1, 2, 3), state.items.map { it.id })
        assertEquals(2, state.nextPage)
        assertEquals(PaginationState.Idle, state.pagination)
    }

    @Test
    fun `loadMore appends and ends at the last page`() = runTest(mainRule.dispatcher) {
        twoPages()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(listOf(1, 2, 3, 4, 5, 6), viewModel.uiState.value.items.map { it.id })
        assertEquals(PaginationState.Exhausted, viewModel.uiState.value.pagination)
        viewModel.loadMore() // nothing left: no extra call
        advanceUntilIdle()
        assertEquals(listOf(1, 2), repository.episodeCalls)
    }

    @Test
    fun `repeated loadMore while loading fetches the page once`() = runTest(mainRule.dispatcher) {
        twoPages()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.loadMore()
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(listOf(1, 2), repository.episodeCalls)
    }

    @Test
    fun `first page failure shows the error and retry recovers`() = runTest(mainRule.dispatcher) {
        repository.episodesHandler = { DomainResult.Failure(DomainError.Timeout) }
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(DomainError.Timeout, viewModel.uiState.value.error)

        twoPages()
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.error)
        assertEquals(3, viewModel.uiState.value.items.size)
    }

    @Test
    fun `next page failure keeps items and can be retried`() = runTest(mainRule.dispatcher) {
        twoPages()
        val viewModel = viewModel()
        advanceUntilIdle()
        repository.episodesHandler = { DomainResult.Failure(DomainError.Offline) }

        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(PaginationState.Failed(DomainError.Offline), viewModel.uiState.value.pagination)
        assertEquals(3, viewModel.uiState.value.items.size)
        assertEquals(2, viewModel.uiState.value.nextPage)
    }
}
