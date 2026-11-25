package com.roldanmatias.rickverse.core.network.auth

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * OkHttp calls this on a 401. N concurrent requests may all fail at once with the same expired token, but the
 * refresh must happen **once**: the [Mutex] makes it single-flight, and the "has the token already changed?"
 * check lets the losers reuse the winner's token instead of refreshing again. It is the Android equivalent of a
 * Swift `actor`. Blocking with `runBlocking` is legal here: `authenticate` runs on an OkHttp worker thread.
 */
public class TokenAuthenticator(private val tokenStore: TokenStore, private val refresher: TokenRefresher) :
    Authenticator {
    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Already retried once with a fresh token and still 401: give up instead of looping.
        if (response.priorResponse != null) return null
        val failedToken = response.request.header(BearerTokenInterceptor.AUTHORIZATION)?.removePrefix("Bearer ")

        val newToken =
            runBlocking {
                mutex.withLock {
                    val current = tokenStore.accessToken()
                    if (current != null && current != failedToken) {
                        current // another request already refreshed
                    } else {
                        refresher.refresh()?.also { tokenStore.save(it) }
                    }
                }
            } ?: return null

        return response.request
            .newBuilder()
            .header(BearerTokenInterceptor.AUTHORIZATION, "Bearer $newToken")
            .build()
    }
}
