package com.roldanmatias.rickverse.core.network

import okhttp3.Authenticator
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Timeouts and cache for the shared client. The numbers are deliberate:
 *
 * * connect 10 s: a phone on a bad network should fail fast and let retry/UI react,
 * * read/write 20 s: the API is small; a stalled body is a failure, not something to wait on,
 * * call 30 s: hard ceiling for one logical request including redirects (not retries, those are separate calls).
 *
 * @property cache HTTP disk cache. It needs a directory, so `:app` builds it from `Context` and passes it in:
 *   core modules never touch `Context`.
 */
public data class NetworkConfig(
    val connectTimeoutSeconds: Long = 10,
    val readTimeoutSeconds: Long = 20,
    val writeTimeoutSeconds: Long = 20,
    val callTimeoutSeconds: Long = 30,
    val cache: Cache? = null,
)

/** The only place that configures OkHttp. Everything else supplies interceptors. */
public object OkHttpClientFactory {
    /**
     * @param applicationInterceptors see the *logical* request once; used for headers, retry and metrics.
     * @param networkInterceptors see every hop (redirects, retries) on the wire; used for logging.
     */
    public fun create(
        config: NetworkConfig = NetworkConfig(),
        applicationInterceptors: List<Interceptor> = emptyList(),
        networkInterceptors: List<Interceptor> = emptyList(),
        authenticator: Authenticator? = null,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(config.writeTimeoutSeconds, TimeUnit.SECONDS)
            .callTimeout(config.callTimeoutSeconds, TimeUnit.SECONDS)
            .cache(config.cache)
            // OkHttp already retries connection-level failures; RetryInterceptor handles HTTP-level retry.
            .retryOnConnectionFailure(true)
            .apply {
                applicationInterceptors.forEach(::addInterceptor)
                networkInterceptors.forEach(::addNetworkInterceptor)
                authenticator?.let(::authenticator)
            }.build()
}
