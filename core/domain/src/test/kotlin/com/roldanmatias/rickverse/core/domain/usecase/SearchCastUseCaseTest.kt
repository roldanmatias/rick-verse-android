package com.roldanmatias.rickverse.core.domain.usecase

import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.CastFilter
import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.domain.model.Page
import com.roldanmatias.rickverse.core.testing.FakeCastRepository
import com.roldanmatias.rickverse.core.testing.TestData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

class SearchCastUseCaseTest {
    private val repository = FakeCastRepository()
    private val useCase = SearchCastUseCase(repository)

    @Test
    fun `status passes through untouched`() =
        runTest {
            useCase(page = 1, query = null, status = CastMember.Status.Dead)

            assertEquals(CastFilter(name = null, status = CastMember.Status.Dead), repository.castCalls.single().filter)
        }

    @Test
    fun `page number is forwarded`() =
        runTest {
            useCase(page = 7)

            assertEquals(7, repository.castCalls.single().page)
        }

    @Test
    fun `failure reaches the caller unchanged`() =
        runTest {
            repository.castHandler = { _, _ -> DomainResult.Failure(DomainError.Offline) }

            val result = useCase(page = 1)

            assertEquals(DomainResult.Failure(DomainError.Offline), result)
        }

    @Test
    fun `success reaches the caller unchanged`() =
        runTest {
            val page = Page(items = listOf(TestData.castMember()), nextPage = 2)
            repository.castHandler = { _, _ -> DomainResult.Success(page) }

            assertEquals(DomainResult.Success(page), useCase(page = 1))
        }
}

@RunWith(Parameterized::class)
class SearchCastUseCaseTrimmingTest(private val input: String?, private val expectedName: String?) {
    @Test
    fun `query is normalised before reaching the repository`() =
        runTest {
            val repository = FakeCastRepository()

            SearchCastUseCase(repository)(page = 1, query = input)

            assertEquals(
                expectedName,
                repository.castCalls
                    .single()
                    .filter.name,
            )
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "\"{0}\" -> \"{1}\"")
        fun cases(): List<Array<String?>> =
            listOf(
                arrayOf(null, null),
                arrayOf("", null),
                arrayOf("   ", null),
                arrayOf("\t\n", null),
                arrayOf(" rick ", "rick"),
                arrayOf("rick", "rick"),
                arrayOf("  Rick  Sanchez ", "Rick  Sanchez"),
            )
    }
}
