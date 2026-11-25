package com.roldanmatias.rickverse.feature.characters.presentation

import androidx.lifecycle.SavedStateHandle
import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.usecase.GetCastMemberDetailUseCase
import com.roldanmatias.rickverse.core.testing.FakeCastRepository
import com.roldanmatias.rickverse.core.testing.MainDispatcherRule
import com.roldanmatias.rickverse.core.testing.TestData
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test

class CastDetailViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = FakeCastRepository()

    private fun viewModel() =
        CastDetailViewModel(GetCastMemberDetailUseCase(repository), SavedStateHandle(mapOf("id" to 7)))

    @Test
    fun `loads the detail for the id in the saved state`() = runTest(mainRule.dispatcher) {
        val detail = TestData.castMemberDetail(member = TestData.castMember(id = 7))
        repository.detailHandler = { DomainResult.Success(detail) }

        val viewModel = viewModel()
        assertEquals(CastDetailUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        assertEquals(CastDetailUiState.Loaded(detail), viewModel.uiState.value)
        assertEquals(listOf(7), repository.detailCalls)
    }

    @Test
    fun `failure is shown and retry recovers`() = runTest(mainRule.dispatcher) {
        repository.detailHandler = { DomainResult.Failure(DomainError.Offline) }
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(CastDetailUiState.Failed(DomainError.Offline), viewModel.uiState.value)

        val detail = TestData.castMemberDetail()
        repository.detailHandler = { DomainResult.Success(detail) }
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(CastDetailUiState.Loaded(detail), viewModel.uiState.value)
    }

    @Test
    fun `missing id argument fails fast`() {
        assertThrows(IllegalStateException::class.java) {
            CastDetailViewModel(GetCastMemberDetailUseCase(repository), SavedStateHandle())
        }
    }
}
