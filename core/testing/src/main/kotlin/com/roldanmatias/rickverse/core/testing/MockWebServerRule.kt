package com.roldanmatias.rickverse.core.testing

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.HttpUrl
import org.junit.rules.ExternalResource
import java.util.concurrent.TimeUnit

/**
 * A fresh [MockWebServer] per test (`@get:Rule`), so no state is shared between tests: this is the JVM
 * counterpart of Swift's `URLProtocolStub`, with real sockets and recorded requests.
 */
public class MockWebServerRule : ExternalResource() {
    public val server: MockWebServer = MockWebServer()

    override fun before() {
        server.start()
    }

    override fun after() {
        server.close()
    }

    /** Base URL ending in `/`, suitable for Retrofit. */
    public fun baseUrl(path: String = "/"): HttpUrl = server.url(path)

    /** Queue a JSON response. */
    public fun enqueueJson(body: String, code: Int = 200, headers: Map<String, String> = emptyMap()) {
        server.enqueue(
            MockResponse
                .Builder()
                .code(code)
                .addHeader("Content-Type", "application/json")
                .apply { headers.forEach { (name, value) -> addHeader(name, value) } }
                .body(body)
                .build(),
        )
    }

    /** Queue a response whose headers are delayed, to provoke a read timeout. */
    public fun enqueueDelayed(delayMillis: Long, body: String = "{}", code: Int = 200) {
        server.enqueue(
            MockResponse
                .Builder()
                .code(code)
                .addHeader("Content-Type", "application/json")
                .headersDelay(delayMillis, TimeUnit.MILLISECONDS)
                .body(body)
                .build(),
        )
    }

    /** Queue a bodiless response with [code]. */
    public fun enqueueCode(code: Int, headers: Map<String, String> = emptyMap()) {
        server.enqueue(
            MockResponse
                .Builder()
                .code(code)
                .apply { headers.forEach { (name, value) -> addHeader(name, value) } }
                .build(),
        )
    }
}
