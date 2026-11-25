package com.roldanmatias.rickverse.data.graphql.client

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.network.okHttpClient
import com.apollographql.cache.normalized.api.TypePolicy
import com.apollographql.cache.normalized.memory.MemoryCacheFactory
import com.apollographql.cache.normalized.normalizedCache
import okhttp3.OkHttpClient

internal const val GRAPHQL_URL = "https://rickandmortyapi.com/graphql"
private const val CACHE_BYTES = 10 * 1024 * 1024

/**
 * Builds Apollo **over the app's shared OkHttpClient**, so the default-headers, retry, metrics and logging
 * interceptors that cover REST cover GraphQL too. The normalized cache (in memory for now) stores one record
 * per `Character`/`Episode`/`Location` thanks to the key fields in `extra.graphqls`.
 */
internal fun createApolloClient(serverUrl: String, okHttp: OkHttpClient): ApolloClient =
    ApolloClient
        .Builder()
        .serverUrl(serverUrl)
        .okHttpClient(okHttp)
        .normalizedCache(
            normalizedCacheFactory = MemoryCacheFactory(maxSizeBytes = CACHE_BYTES),
            // Mirrors `extra.graphqls`: the same entities are one record whichever query reached them.
            typePolicies = mapOf(
                "Character" to TypePolicy(keyFields = listOf("id")),
                "Episode" to TypePolicy(keyFields = listOf("id")),
                "Location" to TypePolicy(keyFields = listOf("id")),
            ),
            fieldPolicies = emptyMap(),
        )
        .build()
