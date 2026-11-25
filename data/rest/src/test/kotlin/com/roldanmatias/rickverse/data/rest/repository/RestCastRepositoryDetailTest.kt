package com.roldanmatias.rickverse.data.rest.repository

import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.network.NetworkJson
import com.roldanmatias.rickverse.core.network.OkHttpClientFactory
import com.roldanmatias.rickverse.core.testing.MockWebServerRule
import com.roldanmatias.rickverse.core.testing.TestDispatcherProvider
import com.roldanmatias.rickverse.data.rest.FakeBackend
import com.roldanmatias.rickverse.data.rest.api.CharacterApi
import com.roldanmatias.rickverse.data.rest.api.EpisodeApi
import com.roldanmatias.rickverse.data.rest.di.createRetrofit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RestCastRepositoryDetailTest {
    @get:Rule
    val web = MockWebServerRule()

    private val backend = FakeBackend(latencyMillis = 15)

    private val repository by lazy {
        web.server.dispatcher = backend
        val retrofit =
            createRetrofit(web.baseUrl("/api/").toString(), OkHttpClientFactory.create(), NetworkJson.default)
        RestCastRepository(
            retrofit.create(CharacterApi::class.java),
            retrofit.create(EpisodeApi::class.java),
            TestDispatcherProvider(Dispatchers.IO),
        )
    }

    private suspend fun rickDetail() = (repository.detail(1) as DomainResult.Success).value

    @Test
    fun `detail maps the character and all of their episodes in order`() =
        runBlocking {
            val detail = rickDetail()

            assertEquals("Rick Sanchez", detail.member.name)
            assertEquals("Earth (C-137)", detail.originName)
            assertEquals((1..51).toList(), detail.episodes.map { it.id })
            assertEquals("S01E01", detail.episodes.first().code)
        }

    @Test
    fun `naive detail costs 52 requests (the N+1 problem)`() =
        runBlocking {
            rickDetail()

            assertEquals(1 + 51, web.server.requestCount)
        }

    @Test
    fun `concurrency is capped so the server is not flooded`() =
        runBlocking {
            rickDetail()

            assertTrue("peak was ${backend.peakConcurrency}", backend.peakConcurrency in 2..6)
        }

    @Test
    fun `unknown character is NotFound`() =
        runBlocking {
            assertEquals(DomainResult.Failure(DomainError.NotFound), repository.detail(9_999))
        }
}
