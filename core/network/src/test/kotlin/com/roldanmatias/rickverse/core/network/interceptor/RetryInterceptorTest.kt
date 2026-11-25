package com.roldanmatias.rickverse.core.network.interceptor

import com.roldanmatias.rickverse.core.network.OkHttpClientFactory
import com.roldanmatias.rickverse.core.network.testApi
import com.roldanmatias.rickverse.core.testing.MockWebServerRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class RetryInterceptorTest {
    @get:Rule
    val web = MockWebServerRule()

    private val sleeps = mutableListOf<Long>()
    private val sleeper = Sleeper { sleeps += it }

    private fun client(
        retry: RetryInterceptor = RetryInterceptor(sleeper = sleeper, random = Random(seed = 1)),
        readTimeoutMillis: Long = 5_000,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
            .addInterceptor(retry)
            .build()

    private fun get() = Request.Builder().url(web.baseUrl("/x")).build()

    @Test
    fun `503, 503, 200 succeeds after exactly three requests`() {
        web.enqueueCode(503)
        web.enqueueCode(503)
        web.enqueueJson("{}")

        val response = client().newCall(get()).execute()

        assertEquals(200, response.code)
        assertEquals(3, web.server.requestCount)
        assertEquals(2, sleeps.size)
    }

    @Test
    fun `gives up after maxAttempts and returns the last response`() {
        repeat(3) { web.enqueueCode(500) }

        val response = client().newCall(get()).execute()

        assertEquals(500, response.code)
        assertEquals(3, web.server.requestCount)
    }

    @Test
    fun `a 400 is not retried`() {
        web.enqueueCode(400)

        val response = client().newCall(get()).execute()

        assertEquals(400, response.code)
        assertEquals(1, web.server.requestCount)
        assertTrue(sleeps.isEmpty())
    }

    @Test
    fun `a 404 is not retried`() {
        web.enqueueCode(404)

        client().newCall(get()).execute()

        assertEquals(1, web.server.requestCount)
    }

    @Test
    fun `a POST is not retried`() {
        web.enqueueCode(503)
        val post =
            Request
                .Builder()
                .url(web.baseUrl("/x"))
                .post("{}".toRequestBody())
                .build()

        val response = client().newCall(post).execute()

        assertEquals(503, response.code)
        assertEquals(1, web.server.requestCount)
    }

    @Test
    fun `a POST explicitly marked safe is retried and the marker is not sent`() {
        web.enqueueCode(503)
        web.enqueueJson("{}")
        val post =
            Request
                .Builder()
                .url(web.baseUrl("/graphql"))
                .header(RetryInterceptor.RETRY_SAFE_HEADER, "true")
                .post("{}".toRequestBody())
                .build()

        val response = client().newCall(post).execute()

        assertEquals(200, response.code)
        assertEquals(2, web.server.requestCount)
        assertNull(web.server.takeRequest().headers[RetryInterceptor.RETRY_SAFE_HEADER])
    }

    @Test
    fun `Retry-After wins over the computed backoff`() {
        web.enqueueCode(429, headers = mapOf("Retry-After" to "2"))
        web.enqueueJson("{}")

        client().newCall(get()).execute()

        assertEquals(listOf(2_000L), sleeps)
    }

    @Test
    fun `Retry-After is capped`() {
        web.enqueueCode(503, headers = mapOf("Retry-After" to "3600"))
        web.enqueueJson("{}")

        client(RetryInterceptor(sleeper = sleeper, maxRetryAfterMillis = 10_000)).newCall(get()).execute()

        assertEquals(listOf(10_000L), sleeps)
    }

    @Test
    fun `backoff uses full jitter within the exponential bound`() {
        repeat(2) { web.enqueueCode(503) }
        web.enqueueJson("{}")

        client(RetryInterceptor(baseDelayMillis = 500, sleeper = sleeper, random = Random(seed = 42)))
            .newCall(get())
            .execute()

        assertEquals(2, sleeps.size)
        assertTrue("first delay within 0..500: ${sleeps[0]}", sleeps[0] in 0..500)
        assertTrue("second delay within 0..1000: ${sleeps[1]}", sleeps[1] in 0..1_000)
    }

    @Test
    fun `a timeout is retried`() {
        web.enqueueDelayed(delayMillis = 1_000)
        web.enqueueJson("{}")

        val response = client(readTimeoutMillis = 150).newCall(get()).execute()

        assertEquals(200, response.code)
        assertEquals(2, web.server.requestCount)
    }

    @Test
    fun `cancelling mid-request neither retries nor surfaces an error`() =
        runBlocking {
            web.enqueueDelayed(delayMillis = 3_000)
            val api = testApi(web.baseUrl(), client())

            val job = launch(Dispatchers.IO) { api.character(1) }
            delay(200)
            job.cancelAndJoin()

            assertTrue(job.isCancelled)
            assertEquals(1, web.server.requestCount)
            assertTrue("no retry sleep expected", sleeps.isEmpty())
            assertFalse(job.isActive)
        }

    @Test
    fun `factory wires interceptors on the client`() {
        val client = OkHttpClientFactory.create(applicationInterceptors = listOf(RetryInterceptor()))

        assertEquals(1, client.interceptors.size)
        assertEquals(30_000, client.callTimeoutMillis)
    }
}
