package com.roldanmatias.rickverse.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/** Blocks the calling thread. Injected so tests run instantly and can assert the requested delays. */
public fun interface Sleeper {
    public fun sleep(millis: Long)

    public companion object {
        public val Thread: Sleeper = Sleeper { java.lang.Thread.sleep(it) }
    }
}

/**
 * HTTP-level retry with exponential backoff and **full jitter**. Install as an *application* interceptor
 * (it must see the logical request and be allowed to call `proceed` more than once).
 *
 * Rules:
 * * Only idempotent methods (`GET`, `HEAD`, `PUT`, `DELETE`) are retried. A `POST` is not, unless the caller
 *   marks it safe with [RETRY_SAFE_HEADER] (GraphQL *queries* are POSTs but read-only). The header is stripped
 *   before the request is sent.
 * * Retries on 408, 429, 5xx and timeouts. A 400 or 404 is a caller bug or a legitimate answer: never retried.
 * * `Retry-After` (seconds or HTTP date) wins over the computed backoff, capped at [maxRetryAfterMillis].
 * * Delay is `random(0..base * 2^attempt)`: jitter spreads clients out so a recovering server is not hit by a
 *   thundering herd.
 * * Stops immediately when the call is cancelled.
 *
 * OkHttp's own `retryOnConnectionFailure` already handles connection-level failures; this is for HTTP-level ones.
 * Interceptors run on OkHttp threads, where blocking (`Thread.sleep`) is legal; the sleeper is still injected
 * so tests are instant and can assert the delays.
 */
public class RetryInterceptor(
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val baseDelayMillis: Long = DEFAULT_BASE_DELAY_MILLIS,
    private val sleeper: Sleeper = Sleeper.Thread,
    private val random: Random = Random.Default,
    private val maxRetryAfterMillis: Long = DEFAULT_MAX_RETRY_AFTER_MILLIS,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : Interceptor {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be >= 1" }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val retrySafe = original.header(RETRY_SAFE_HEADER) != null
        val request = if (retrySafe) original.newBuilder().removeHeader(RETRY_SAFE_HEADER).build() else original
        val idempotent = retrySafe || request.isIdempotent()

        var attempt = 0
        while (true) {
            if (chain.call().isCanceled()) throw IOException("Canceled")
            val isLastAttempt = attempt >= maxAttempts - 1
            try {
                val response = chain.proceed(request)
                if (!idempotent || isLastAttempt || !response.isRetryable()) return response
                val delay = retryAfterMillis(response) ?: backoffMillis(attempt)
                response.close()
                sleeper.sleep(delay)
            } catch (failure: InterruptedIOException) {
                // Timeouts only. "Canceled" is a plain IOException and is never retried.
                if (!idempotent || isLastAttempt || chain.call().isCanceled()) throw failure
                sleeper.sleep(backoffMillis(attempt))
            }
            attempt++
        }
    }

    private fun backoffMillis(attempt: Int): Long = random.nextLong(0, (baseDelayMillis shl attempt) + 1)

    private fun retryAfterMillis(response: Response): Long? {
        val raw = response.header("Retry-After") ?: return null
        val millis =
            raw.trim().toLongOrNull()?.let { TimeUnit.SECONDS.toMillis(it) }
                ?: response.headers.getDate("Retry-After")?.let { it.time - currentTimeMillis() }
        return millis?.coerceIn(0, maxRetryAfterMillis)
    }

    private fun Request.isIdempotent(): Boolean = method in IDEMPOTENT_METHODS

    private fun Response.isRetryable(): Boolean =
        code == HTTP_REQUEST_TIMEOUT || code == HTTP_TOO_MANY_REQUESTS || code in HTTP_SERVER_ERRORS

    public companion object {
        /** Marks a non-idempotent method as safe to retry (read-only GraphQL queries). Stripped before sending. */
        public const val RETRY_SAFE_HEADER: String = "X-Retry-Safe"

        private const val DEFAULT_MAX_ATTEMPTS = 3
        private const val DEFAULT_BASE_DELAY_MILLIS = 500L
        private const val DEFAULT_MAX_RETRY_AFTER_MILLIS = 30_000L
        private const val HTTP_REQUEST_TIMEOUT = 408
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private val HTTP_SERVER_ERRORS = 500..599
        private val IDEMPOTENT_METHODS = setOf("GET", "HEAD", "PUT", "DELETE")
    }
}
