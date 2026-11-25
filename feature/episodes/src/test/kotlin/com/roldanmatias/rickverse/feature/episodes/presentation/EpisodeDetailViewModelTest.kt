package com.roldanmatias.rickverse.feature.episodes.presentation

import androidx.lifecycle.SavedStateHandle
import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.usecase.GetEpisodeDetailUseCase
import com.roldanmatias.rickverse.core.testing.FakeEpisodeRepository
import com.roldanmatias.rickverse.core.testing.MainDispatcherRule
import com.roldanmatias.rickverse.core.testing.TestData
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EpisodeDetailViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeEpisodeRepository()

    private fun viewModel() =
        EpisodeDetailViewModel(GetEpisodeDetailUseCase(repository), SavedStateHandle(mapOf("id" to 4)))

    @Test
    fun `loads the episode for the id in the saved state`() = runTest(mainRule.dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        val loaded = viewModel.uiState.value as EpisodeDetailUiState.Loaded
        assertEquals(4, loaded.detail.episode.id)
        assertEquals(listOf(4), repository.detailCalls)
    }

    @Test
    fun `failure then retry`() = runTest(mainRule.dispatcher) {
        repository.detailHandler = { DomainResult.Failure(DomainError.NotFound) }
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(EpisodeDetailUiState.Failed(DomainError.NotFound), viewModel.uiState.value)

        val detail = TestData.episodeDetail()
        repository.detailHandler = { DomainResult.Success(detail) }
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(EpisodeDetailUiState.Loaded(detail), viewModel.uiState.value)
    }
}
