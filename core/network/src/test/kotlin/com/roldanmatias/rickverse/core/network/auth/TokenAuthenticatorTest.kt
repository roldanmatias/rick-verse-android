package com.roldanmatias.rickverse.core.network.auth

import com.roldanmatias.rickverse.core.testing.MockWebServerRule
import kotlinx.coroutines.delay
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class TokenAuthenticatorTest {
    @get:Rule
    val web = MockWebServerRule()

    private class FakeTokenStore(
        initial: String?,
    ) : TokenStore {
        @Volatile private var token = initial

        override suspend fun accessToken(): String? = token

        override suspend fun save(accessToken: String) {
            token = accessToken
        }
    }

    private val refreshCount = AtomicInteger()

    private fun client(store: TokenStore, refresher: TokenRefresher) =
        OkHttpClient
            .Builder()
            .addInterceptor(BearerTokenInterceptor(store))
            .authenticator(TokenAuthenticator(store, refresher))
            .build()

    private fun serverAcceptingOnly(token: String) {
        web.server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val ok = request.headers["Authorization"] == "Bearer $token"
                    return MockResponse.Builder().code(if (ok) 200 else 401).build()
                }
            }
    }

    @Test
    fun `five concurrent 401s trigger exactly one refresh`() {
        serverAcceptingOnly("new")
        val store = FakeTokenStore("old")
        val refresher =
            TokenRefresher {
                refreshCount.incrementAndGet()
                delay(150) // widen the race window
                "new"
            }
        val client = client(store, refresher)
        val pool = Executors.newFixedThreadPool(5)

        val codes =
            (1..5)
                .map {
                    pool.submit<Int> {
                        client
                            .newCall(
                                Request.Builder().url(web.baseUrl("/x")).build(),
                            ).execute()
                            .use { it.code }
                    }
                }.map { it.get() }
        pool.shutdown()

        assertEquals(List(5) { 200 }, codes)
        assertEquals(1, refreshCount.get())
    }

    @Test
    fun `gives up when the refresh cannot produce a token`() {
        serverAcceptingOnly("new")

        val response =
            client(FakeTokenStore("old"), TokenRefresher { null })
                .newCall(Request.Builder().url(web.baseUrl("/x")).build())
                .execute()

        assertEquals(401, response.code)
        assertNull(response.priorResponse)
    }

    @Test
    fun `does not loop when the new token is also rejected`() {
        serverAcceptingOnly("never")

        val response =
            client(FakeTokenStore("old"), TokenRefresher { "new" })
                .newCall(Request.Builder().url(web.baseUrl("/x")).build())
                .execute()

        assertEquals(401, response.code)
        assertEquals(2, web.server.requestCount)
    }
}
