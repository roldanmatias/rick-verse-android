package com.roldanmatias.rickverse.core.domain.usecase

import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.testing.FakeCastRepository
import com.roldanmatias.rickverse.core.testing.FakeEpisodeRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PassThroughUseCasesTest {
    @Test
    fun `cast detail use case forwards the id and the failure`() =
        runTest {
            val repository = FakeCastRepository()
            repository.detailHandler = { DomainResult.Failure(DomainError.NotFound) }

            val result = GetCastMemberDetailUseCase(repository)(42)

            assertEquals(listOf(42), repository.detailCalls)
            assertEquals(DomainResult.Failure(DomainError.NotFound), result)
        }

    @Test
    fun `episodes use case forwards the page`() =
        runTest {
            val repository = FakeEpisodeRepository()

            GetEpisodesUseCase(repository)(3)

            assertEquals(listOf(3), repository.episodeCalls)
        }

    @Test
    fun `episode detail use case forwards the id`() =
        runTest {
            val repository = FakeEpisodeRepository()

            GetEpisodeDetailUseCase(repository)(9)

            assertEquals(listOf(9), repository.detailCalls)
        }
}
