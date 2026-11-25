package com.roldanmatias.rickverse.core.network.interceptor

import com.roldanmatias.rickverse.core.network.NetworkEvent
import com.roldanmatias.rickverse.core.network.NetworkLog
import com.roldanmatias.rickverse.core.network.NetworkSource
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Writes exactly one [NetworkEvent] per **logical** call to [log], including time spent in retries, so install it
 * as an *application* interceptor outside [RetryInterceptor]. The same instance works for REST and for Apollo
 * because both run on the shared `OkHttpClient`.
 *
 * GraphQL is told apart by the `/graphql` path, and labelled with the `X-APOLLO-OPERATION-NAME` header that Apollo
 * Kotlin adds to every operation.
 */
public class MetricsInterceptor(
    private val log: NetworkLog,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val nanoTime: () -> Long = System::nanoTime,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startNanos = nanoTime()
        val response =
            try {
                chain.proceed(request)
            } catch (failure: IOException) {
                record(request, status = 0, startNanos = startNanos, bytes = -1, fromCache = false)
                throw failure
            }
        val fromCache = response.cacheResponse != null && response.networkResponse == null
        record(request, response.code, startNanos, response.body.contentLength(), fromCache)
        return response
    }

    private fun record(request: Request, status: Int, startNanos: Long, bytes: Long, fromCache: Boolean) {
        val isGraphQl = request.url.encodedPath.endsWith("/graphql")
        log.record(
            NetworkEvent(
                id = log.nextId(),
                timestampMillis = currentTimeMillis(),
                source = if (isGraphQl) NetworkSource.GraphQl else NetworkSource.Rest,
                label =
                    if (isGraphQl) {
                        request.header(APOLLO_OPERATION_NAME) ?: "GraphQL"
                    } else {
                        "${request.method} ${request.url.encodedPath}${request.url.query?.let { "?$it" }.orEmpty()}"
                    },
                status = status,
                durationMs = TimeUnit.NANOSECONDS.toMillis(nanoTime() - startNanos),
                bytes = bytes,
                servedFromCache = fromCache,
            ),
        )
    }

    public companion object {
        public const val APOLLO_OPERATION_NAME: String = "X-APOLLO-OPERATION-NAME"
    }
}
