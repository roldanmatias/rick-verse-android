package com.roldanmatias.rickverse.core.network.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/** Adds `Authorization: Bearer <token>` when a token exists. Blocking on an OkHttp thread is fine. */
public class BearerTokenInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenStore.accessToken() }
        val request =
            chain
                .request()
                .newBuilder()
                .apply { if (token != null) header(AUTHORIZATION, "Bearer $token") }
                .build()
        return chain.proceed(request)
    }

    internal companion object {
        const val AUTHORIZATION = "Authorization"
    }
}
