package com.roldanmatias.rickverse.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Receives log lines. `:app` supplies `Log.d` in debug and a no-op in release, so this module stays JVM-only. */
public fun interface HttpLogger {
    public fun log(message: String)

    public companion object {
        public val None: HttpLogger = HttpLogger { }
    }
}

/**
 * Logs method, URL, status and duration. Designed as a **network** interceptor: it sees every hop
 * (redirects, retries) exactly as sent on the wire.
 *
 * Bodies are never logged, and sensitive headers are redacted, so a release build cannot leak tokens.
 */
public class LoggingInterceptor(
    private val logger: HttpLogger,
    private val logHeaders: Boolean = false,
    private val redactedHeaders: Set<String> = DEFAULT_REDACTED,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        logger.log("--> ${request.method} ${request.url}")
        if (logHeaders) logHeaders(request.headers)
        val startNanos = System.nanoTime()
        val response =
            try {
                chain.proceed(request)
            } catch (failure: IOException) {
                logger.log("<-- FAILED ${request.url}: ${failure.javaClass.simpleName}")
                throw failure
            }
        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)
        logger.log("<-- ${response.code} ${request.url} (${tookMs}ms)")
        if (logHeaders) logHeaders(response.headers)
        return response
    }

    private fun logHeaders(headers: okhttp3.Headers) {
        for (index in 0 until headers.size) {
            val name = headers.name(index)
            val value = if (redactedHeaders.any { it.equals(name, ignoreCase = true) }) "██" else headers.value(index)
            logger.log("    $name: $value")
        }
    }

    public companion object {
        public val DEFAULT_REDACTED: Set<String> = setOf("Authorization", "Cookie", "Set-Cookie", "Proxy-Authorization")
    }
}
