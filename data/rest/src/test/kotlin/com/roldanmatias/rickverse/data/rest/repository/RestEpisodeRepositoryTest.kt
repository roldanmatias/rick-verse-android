package com.roldanmatias.rickverse.data.rest.repository

import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.network.NetworkJson
import com.roldanmatias.rickverse.core.network.OkHttpClientFactory
import com.roldanmatias.rickverse.core.testing.Fixtures
import com.roldanmatias.rickverse.core.testing.MockWebServerRule
import com.roldanmatias.rickverse.core.testing.TestDispatcherProvider
import com.roldanmatias.rickverse.data.rest.FakeBackend
import com.roldanmatias.rickverse.data.rest.api.CharacterApi
import com.roldanmatias.rickverse.data.rest.api.EpisodeApi
import com.roldanmatias.rickverse.data.rest.di.createRetrofit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class RestEpisodeRepositoryTest {
    @get:Rule
    val web = MockWebServerRule()

    private val repository by lazy {
        val retrofit =
            createRetrofit(web.baseUrl("/api/").toString(), OkHttpClientFactory.create(), NetworkJson.default)
        RestEpisodeRepository(
            retrofit.create(EpisodeApi::class.java),
            retrofit.create(CharacterApi::class.java),
            TestDispatcherProvider(Dispatchers.IO),
        )
    }

    @Test
    fun `episodes page maps and exposes the next page`() =
        runBlocking {
            web.enqueueJson(Fixtures.load("episodes_page1.json"))

            val page = (repository.episodes(1) as DomainResult.Success).value

            assertEquals(20, page.items.size)
            assertEquals("Pilot", page.items.first().name)
            assertEquals(2, page.nextPage)
        }

    @Test
    fun `season and episode number come from the code`() =
        runBlocking {
            web.enqueueJson(Fixtures.load("episodes_page1.json"))

            val episodes = (repository.episodes(1) as DomainResult.Success).value.items

            // Page 1 holds S01E01..S01E11 and S02E01..S02E09.
            assertEquals(1 to 1, episodes.first().season to episodes.first().number)
            assertEquals(2 to 9, episodes.last().season to episodes.last().number)
        }

    @Test
    fun `detail is the episode plus its cast in exactly 2 requests`() =
        runBlocking {
            web.server.dispatcher = FakeBackend()

            val detail = (repository.detail(1) as DomainResult.Success).value

            assertEquals("Pilot", detail.episode.name)
            assertEquals(LocalDate.of(2013, 12, 2), detail.episode.airDate)
            assertEquals(19, detail.cast.size)
            assertEquals(2, web.server.requestCount)
        }

    @Test
    fun `unknown episode is NotFound`() =
        runBlocking {
            web.server.dispatcher = FakeBackend()

            assertEquals(DomainResult.Failure(DomainError.NotFound), repository.detail(9_999))
        }
}
