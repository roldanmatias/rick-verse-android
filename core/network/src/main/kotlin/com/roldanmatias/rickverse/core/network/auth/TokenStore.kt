package com.roldanmatias.rickverse.core.network.auth

/**
 * Exercise only: the Rick and Morty API has no authentication. This shows the pattern interviewers ask about:
 * attach a bearer token, and on 401 refresh **once** (single-flight) and retry.
 */
public interface TokenStore {
    public suspend fun accessToken(): String?

    public suspend fun save(accessToken: String)
}

/** Obtains a fresh access token (calls the auth backend). Returns `null` when the user must sign in again. */
public fun interface TokenRefresher {
    public suspend fun refresh(): String?
}
