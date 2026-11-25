package com.roldanmatias.rickverse.core.network.interceptor

import com.roldanmatias.rickverse.core.network.NetworkLog
import com.roldanmatias.rickverse.core.network.NetworkSource
import com.roldanmatias.rickverse.core.testing.MockWebServerRule
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MetricsInterceptorTest {
    @get:Rule
    val web = MockWebServerRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val log = NetworkLog()

    private fun client(cache: Cache? = null) =
        OkHttpClient
            .Builder()
            .cache(cache)
            .addInterceptor(MetricsInterceptor(log))
            .build()

    @Test
    fun `records one REST event per call with method, path and query`() {
        web.enqueueJson("""{"ok":true}""")

        client().newCall(Request.Builder().url(web.baseUrl("/episode/1,2,3?page=2")).build()).execute().close()

        val event = log.events.value.single()
        assertEquals(NetworkSource.Rest, event.source)
        assertEquals("GET /episode/1,2,3?page=2", event.label)
        assertEquals(200, event.status)
        assertFalse(event.servedFromCache)
    }

    @Test
    fun `labels GraphQL calls with the Apollo operation name`() {
        web.enqueueJson("""{"data":{}}""")
        val request =
            Request
                .Builder()
                .url(web.baseUrl("/graphql"))
                .header(MetricsInterceptor.APOLLO_OPERATION_NAME, "CastPage")
                .post("{}".toRequestBody())
                .build()

        client().newCall(request).execute().close()

        val event = log.events.value.single()
        assertEquals(NetworkSource.GraphQl, event.source)
        assertEquals("CastPage", event.label)
    }

    @Test
    fun `records a failed call with status 0 and rethrows`() {
        web.server.close()

        val failure =
            runCatching {
                client().newCall(Request.Builder().url(web.baseUrl("/x")).build()).execute()
            }

        assertTrue(failure.isFailure)
        assertEquals(
            0,
            log.events.value
                .single()
                .status,
        )
    }

    @Test
    fun `marks responses served from the HTTP cache`() {
        web.enqueueJson("""{"n":1}""", headers = mapOf("Cache-Control" to "max-age=60"))
        val client = client(Cache(tmp.newFolder(), 1_000_000))
        val request = Request.Builder().url(web.baseUrl("/cached")).build()

        client.newCall(request).execute().use { it.body.string() }
        client.newCall(request).execute().use { it.body.string() }

        assertEquals(1, web.server.requestCount)
        val (second, first) = log.events.value
        assertTrue(second.servedFromCache)
        assertFalse(first.servedFromCache)
    }
}
