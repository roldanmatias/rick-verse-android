package com.roldanmatias.rickverse.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/** Adds `Accept` and `User-Agent` to every request. Headers already set by the caller win. */
public class DefaultHeadersInterceptor(private val userAgent: String = DEFAULT_USER_AGENT) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val request =
            original
                .newBuilder()
                .apply {
                    if (original.header(ACCEPT) == null) header(ACCEPT, "application/json")
                    if (original.header(USER_AGENT) == null) header(USER_AGENT, userAgent)
                }.build()
        return chain.proceed(request)
    }

    public companion object {
        public const val DEFAULT_USER_AGENT: String = "RickVerse/1.0 (Android)"
        private const val ACCEPT = "Accept"
        private const val USER_AGENT = "User-Agent"
    }
}
