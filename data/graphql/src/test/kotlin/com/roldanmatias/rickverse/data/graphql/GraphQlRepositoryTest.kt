package com.roldanmatias.rickverse.data.graphql

import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.CachePolicy
import com.roldanmatias.rickverse.core.domain.model.CastFilter
import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.network.OkHttpClientFactory
import com.roldanmatias.rickverse.core.testing.FakeSettingsRepository
import com.roldanmatias.rickverse.core.testing.Fixtures
import com.roldanmatias.rickverse.core.testing.MockWebServerRule
import com.roldanmatias.rickverse.data.graphql.client.ApolloGraphQlClient
import com.roldanmatias.rickverse.data.graphql.client.createApolloClient
import com.roldanmatias.rickverse.data.graphql.repository.GraphQlCastRepository
import com.roldanmatias.rickverse.data.graphql.repository.GraphQlEpisodeRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Real Apollo parsing and a real `ApolloClient`, against a MockWebServer serving captured responses. */
class GraphQlRepositoryTest {
    @get:Rule
    val web = MockWebServerRule()

    private val settings = FakeSettingsRepository(initialCachePolicy = CachePolicy.NetworkOnly)

    private val client by lazy {
        ApolloGraphQlClient(createApolloClient(web.baseUrl("/graphql").toString(), OkHttpClientFactory.create()))
    }
    private val casts by lazy { GraphQlCastRepository(client, settings) }
    private val episodes by lazy { GraphQlEpisodeRepository(client, settings) }

    private fun <T> DomainResult<T>.value(): T = (this as DomainResult.Success).value

    private fun error(result: DomainResult<*>): DomainError = (result as DomainResult.Failure).error

    @Test
    fun `cast page maps items and next page`() = runBlocking {
        web.enqueueJson(Fixtures.load("cast_page1.json"))

        val page = casts.cast(1, CastFilter.None).value()

        assertEquals(20, page.items.size)
        assertEquals("Rick Sanchez", page.items.first().name)
        assertEquals(CastMember.Status.Alive, page.items.first().status)
        assertEquals(2, page.nextPage)
    }

    @Test
    fun `filter is sent as variables and an empty search is an empty page, not an error`() = runBlocking {
        web.enqueueJson(
            """{"data":{"characters":{"__typename":"Characters","info":{"__typename":"Info","next":null},
              "results":[]}}}""",
        )

        val page = casts.cast(1, CastFilter(name = "zzzz", status = CastMember.Status.Dead)).value()

        assertTrue(page.items.isEmpty())
        assertEquals(null, page.nextPage)
        val body = web.server.takeRequest().body!!.utf8()
        assertTrue(body, body.contains("\"zzzz\"") && body.contains("\"dead\""))
    }

    @Test
    fun `detail is one request with every episode`() = runBlocking {
        web.enqueueJson(Fixtures.load("cast_detail_1.json"))

        val detail = casts.detail(1).value()

        assertEquals("Earth (C-137)", detail.originName)
        assertEquals(51, detail.episodes.size)
        assertEquals(1, web.server.requestCount)
    }

    @Test
    fun `null character is NotFound even though HTTP is 200`() = runBlocking {
        web.enqueueJson("""{"data":{"character":null}}""")

        assertEquals(DomainError.NotFound, error(casts.detail(9_999)))
    }

    @Test
    fun `an element with a null name is skipped, the rest of the page survives`() = runBlocking {
        web.enqueueJson(
            """{"data":{"characters":{"__typename":"Characters","info":{"__typename":"Info","next":3},"results":[
              {"__typename":"Character","id":"1","name":null,"status":"Alive","species":"Human","image":"a"},
              {"__typename":"Character","id":"x","name":"Bad id","status":"Alive","species":"Human","image":"a"},
              {"__typename":"Character","id":"2","name":"Morty","status":"weird","species":"Human","image":"a"}]}}}""",
        )

        val page = casts.cast(2, CastFilter.None).value()

        assertEquals(listOf("Morty"), page.items.map { it.name })
        assertEquals(CastMember.Status.Unknown, page.items.single().status) // unknown status never crashes
        assertEquals(3, page.nextPage)
    }

    @Test
    fun `a single item with a null name is InvalidData`() = runBlocking {
        web.enqueueJson(
            """{"data":{"character":{"__typename":"Character","id":"1","name":null,"status":null,"species":null,
              "image":null,"gender":null,"origin":null,"location":null,"episode":[]}}}""",
        )

        assertTrue(error(casts.detail(1)) is DomainError.InvalidData)
    }

    @Test
    fun `partial data with errors still returns the data`() = runBlocking {
        web.enqueueJson(
            """{"data":{"characters":{"__typename":"Characters","info":{"__typename":"Info","next":null},"results":[
              {"__typename":"Character","id":"1","name":"Rick","status":"Alive","species":"Human","image":"a"},null]}},
              "errors":[{"message":"Something failed for one item"}]}""",
        )

        assertEquals(1, casts.cast(1, CastFilter.None).value().items.size)
    }

    @Test
    fun `errors without data is a server error`() = runBlocking {
        web.enqueueJson("""{"errors":[{"message":"boom"}]}""")

        assertEquals(DomainError.Server(200), error(casts.cast(1, CastFilter.None)))
    }

    @Test
    fun `http 500 is a server error`() = runBlocking {
        web.enqueueCode(500)

        assertEquals(DomainError.Server(500), error(casts.cast(1, CastFilter.None)))
    }

    @Test
    fun `unreachable server is offline`() = runBlocking {
        web.server.close()

        assertEquals(DomainError.Offline, error(casts.cast(1, CastFilter.None)))
    }

    @Test
    fun `CacheFirst serves the second identical call from the normalized cache`() = runBlocking {
        val cached = FakeSettingsRepository(initialCachePolicy = CachePolicy.CacheFirst)
        val repository = GraphQlCastRepository(client, cached)
        web.enqueueJson(Fixtures.load("cast_detail_1.json"))

        repository.detail(1).value()
        repository.detail(1).value()

        assertEquals(1, web.server.requestCount)
    }

    @Test
    fun `episode page and detail`() = runBlocking {
        web.enqueueJson(Fixtures.load("episode_page1.json"))
        web.enqueueJson(Fixtures.load("episode_detail_1.json"))

        val page = episodes.episodes(1).value()
        val detail = episodes.detail(1).value()

        assertEquals(2, page.nextPage)
        assertEquals("S01E01", page.items.first().code)
        assertEquals("Pilot", detail.episode.name)
        assertEquals(19, detail.cast.size)
    }
}
