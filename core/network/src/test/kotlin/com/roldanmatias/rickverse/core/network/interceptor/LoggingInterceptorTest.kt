package com.roldanmatias.rickverse.core.network.interceptor

import com.roldanmatias.rickverse.core.testing.MockWebServerRule
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoggingInterceptorTest {
    @get:Rule
    val web = MockWebServerRule()

    private val lines = mutableListOf<String>()

    private fun client(logHeaders: Boolean) =
        OkHttpClient
            .Builder()
            .addNetworkInterceptor(LoggingInterceptor({ lines += it }, logHeaders = logHeaders))
            .build()

    @Test
    fun `logs method, URL and status`() {
        web.enqueueJson("""{"secret":"body"}""")

        client(logHeaders = false).newCall(Request.Builder().url(web.baseUrl("/a")).build()).execute().close()

        assertTrue(lines.any { it.startsWith("--> GET") && it.endsWith("/a") })
        assertTrue(lines.any { it.startsWith("<-- 200") })
    }

    @Test
    fun `never logs the body`() {
        web.enqueueJson("""{"secret":"body"}""")

        client(logHeaders = true).newCall(Request.Builder().url(web.baseUrl("/a")).build()).execute().use {
            it.body.string()
        }

        assertFalse(lines.any { it.contains("secret") })
    }

    @Test
    fun `redacts sensitive headers`() {
        web.enqueueJson("{}")
        val request =
            Request
                .Builder()
                .url(web.baseUrl("/a"))
                .header("Authorization", "Bearer abc123")
                .build()

        client(logHeaders = true).newCall(request).execute().close()

        assertFalse(lines.any { it.contains("abc123") })
        assertTrue(lines.any { it.contains("Authorization") })
    }
}
