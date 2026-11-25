package com.roldanmatias.rickverse.core.network

import com.roldanmatias.rickverse.core.network.interceptor.DefaultHeadersInterceptor
import com.roldanmatias.rickverse.core.testing.MockWebServerRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException

class RetrofitIntegrationTest {
    @get:Rule
    val web = MockWebServerRule()

    private val api by lazy {
        testApi(
            web.baseUrl(),
            OkHttpClientFactory.create(applicationInterceptors = listOf(DefaultHeadersInterceptor())),
        )
    }

    @Test
    fun `builds path and percent-encodes the query`() =
        runBlocking {
            web.enqueueJson("""{"results":[]}""")

            api.search(name = "Rick Sanchez", page = 2)

            val request = web.server.takeRequest()
            assertEquals("/character", request.url.encodedPath)
            assertEquals("name=Rick%20Sanchez&page=2", request.url.encodedQuery)
        }

    @Test
    fun `sends the default headers`() =
        runBlocking {
            web.enqueueJson("""{"id":1,"name":"Rick"}""")

            api.character(1)

            val headers = web.server.takeRequest().headers
            assertEquals("application/json", headers["Accept"])
            assertEquals(DefaultHeadersInterceptor.DEFAULT_USER_AGENT, headers["User-Agent"])
        }

    @Test
    fun `200 is decoded and unknown keys are ignored`() =
        runBlocking {
            web.enqueueJson("""{"id":1,"name":"Rick","somethingNew":true}""")

            assertEquals(TestCharacter(1, "Rick"), api.character(1))
        }

    @Test
    fun `404 maps to Http 404 with its body`() =
        runBlocking {
            web.enqueueJson("""{"error":"There is nothing here"}""", code = 404)

            val error = captureError { api.character(999) }

            assertEquals(NetworkError.Http(404, """{"error":"There is nothing here"}"""), error)
        }

    @Test
    fun `malformed JSON maps to Decoding`() =
        runBlocking {
            web.enqueueJson("""{"id": "not-a-number""")

            val error = captureError { api.character(1) }

            assertTrue("expected Decoding but was $error", error is NetworkError.Decoding)
        }

    @Test
    fun `server that is gone maps to Offline`() =
        runBlocking {
            web.server.close()

            val error = captureError { api.character(1) }

            assertEquals(NetworkError.Offline, error)
        }

    private suspend fun captureError(block: suspend () -> Unit): NetworkError {
        try {
            block()
        } catch (expected: HttpException) {
            return expected.toNetworkError()
        } catch (expected: java.io.IOException) {
            return expected.toNetworkError()
        } catch (expected: kotlinx.serialization.SerializationException) {
            return expected.toNetworkError()
        }
        fail("expected a failure")
        error("unreachable")
    }
}
