package com.roldanmatias.rickverse.data.rest.repository

import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.CastFilter
import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.network.NetworkJson
import com.roldanmatias.rickverse.core.network.OkHttpClientFactory
import com.roldanmatias.rickverse.core.testing.Fixtures
import com.roldanmatias.rickverse.core.testing.MockWebServerRule
import com.roldanmatias.rickverse.core.testing.TestDispatcherProvider
import com.roldanmatias.rickverse.data.rest.api.CharacterApi
import com.roldanmatias.rickverse.data.rest.api.EpisodeApi
import com.roldanmatias.rickverse.data.rest.di.createRetrofit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RestCastRepositoryTest {
    @get:Rule
    val web = MockWebServerRule()

    private val repository by lazy {
        val retrofit =
            createRetrofit(web.baseUrl("/api/").toString(), OkHttpClientFactory.create(), NetworkJson.default)
        RestCastRepository(
            retrofit.create(CharacterApi::class.java),
            retrofit.create(EpisodeApi::class.java),
            TestDispatcherProvider(Dispatchers.IO),
        )
    }

    @Test
    fun `maps a real page and extracts the next page number`() =
        runBlocking {
            web.enqueueJson(Fixtures.load("characters_page1.json"))

            val page = (repository.cast(1, CastFilter.None) as DomainResult.Success).value

            assertEquals(20, page.items.size)
            assertEquals("Rick Sanchez", page.items.first().name)
            assertEquals(2, page.nextPage)
        }

    @Test
    fun `last page has no next page`() =
        runBlocking {
            web.enqueueJson(Fixtures.load("characters_page_last.json"))

            val page = (repository.cast(42, CastFilter.None) as DomainResult.Success).value

            assertNull(page.nextPage)
            assertTrue(!page.hasMore)
        }

    @Test
    fun `404 for an empty search is an empty page, not NotFound`() =
        runBlocking {
            web.enqueueJson(Fixtures.load("characters_empty_search_404.json"), code = 404)

            val result = repository.cast(1, CastFilter(name = "zzzzzz"))

            assertTrue(result is DomainResult.Success)
            val page = (result as DomainResult.Success).value
            assertTrue(page.items.isEmpty())
            assertTrue(!page.hasMore)
        }

    @Test
    fun `sends page, trimmed name and lowercase status`() =
        runBlocking {
            web.enqueueJson(Fixtures.load("characters_page1.json"))

            repository.cast(3, CastFilter(name = "rick", status = CastMember.Status.Alive))

            val url = web.server.takeRequest().url
            assertEquals("/api/character", url.encodedPath)
            assertEquals("3", url.queryParameter("page"))
            assertEquals("rick", url.queryParameter("name"))
            assertEquals("alive", url.queryParameter("status"))
        }

    @Test
    fun `omits name and status when there is no filter`() =
        runBlocking {
            web.enqueueJson(Fixtures.load("characters_page1.json"))

            repository.cast(1, CastFilter.None)

            val url = web.server.takeRequest().url
            assertNull(url.queryParameter("name"))
            assertNull(url.queryParameter("status"))
        }

    @Test
    fun `500 maps to Server`() =
        runBlocking {
            web.enqueueCode(500)

            assertEquals(DomainResult.Failure(DomainError.Server(500)), repository.cast(1, CastFilter.None))
        }

    @Test
    fun `non-404 client error is not hidden as an empty page`() =
        runBlocking {
            web.enqueueCode(400)

            assertEquals(DomainResult.Failure(DomainError.Unknown("HTTP 400")), repository.cast(1, CastFilter.None))
        }

    @Test
    fun `malformed JSON maps to InvalidData`() =
        runBlocking {
            web.enqueueJson("""{"info": {""")

            val result = repository.cast(1, CastFilter.None)

            assertTrue("was $result", (result as DomainResult.Failure).error is DomainError.InvalidData)
        }

    @Test
    fun `server that is gone maps to Offline`() =
        runBlocking {
            web.server.close()

            assertEquals(DomainResult.Failure(DomainError.Offline), repository.cast(1, CastFilter.None))
        }

    @Test
    fun `cancellation propagates and does not become a Failure`() =
        runBlocking {
            web.enqueueDelayed(delayMillis = 3_000)
            var result: DomainResult<*>? = null

            val job = launch(Dispatchers.IO) { result = repository.cast(1, CastFilter.None) }
            delay(200)
            job.cancelAndJoin()

            assertTrue(job.isCancelled)
            assertNull("no result may be produced for a cancelled call", result)
        }
}
