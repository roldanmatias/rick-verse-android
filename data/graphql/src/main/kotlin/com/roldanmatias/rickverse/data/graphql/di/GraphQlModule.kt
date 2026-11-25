package com.roldanmatias.rickverse.data.graphql.di

import android.content.Context
import com.apollographql.apollo.ApolloClient
import com.apollographql.cache.normalized.memory.MemoryCacheFactory
import com.apollographql.cache.normalized.sql.SqlNormalizedCacheFactory
import com.roldanmatias.rickverse.core.domain.di.GraphQl
import com.roldanmatias.rickverse.core.domain.repository.CacheMaintenance
import com.roldanmatias.rickverse.core.domain.repository.CastRepository
import com.roldanmatias.rickverse.core.domain.repository.EpisodeRepository
import com.roldanmatias.rickverse.core.network.NetworkLog
import com.roldanmatias.rickverse.data.graphql.client.ApolloGraphQlClient
import com.roldanmatias.rickverse.data.graphql.client.GRAPHQL_URL
import com.roldanmatias.rickverse.data.graphql.client.GraphQlClient
import com.roldanmatias.rickverse.data.graphql.client.createApolloClient
import com.roldanmatias.rickverse.data.graphql.repository.ApolloCacheMaintenance
import com.roldanmatias.rickverse.data.graphql.repository.GraphQlCastRepository
import com.roldanmatias.rickverse.data.graphql.repository.GraphQlEpisodeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

private const val MEMORY_CACHE_BYTES = 2 * 1024 * 1024

@Module
@InstallIn(SingletonComponent::class)
internal object GraphQlProvidesModule {
    /**
     * Two-level normalized cache: a small in-memory LRU in front of SQLite, so a cold start (even in airplane mode)
     * still has the characters you looked at. Eviction is size-based (memory LRU); invalidation is manual
     * ("Clear GraphQL cache" in Settings). Time-based expiry (`maxAge`) is not configured: documented in notes.
     */
    @Provides
    @Singleton
    fun apolloClient(@ApplicationContext context: Context, okHttp: OkHttpClient, log: NetworkLog): ApolloClient =
        createApolloClient(
            serverUrl = GRAPHQL_URL,
            okHttp = okHttp,
            cacheFactory = MemoryCacheFactory(maxSizeBytes = MEMORY_CACHE_BYTES)
                .chain(SqlNormalizedCacheFactory(context, "apollo_cache.db")),
            log = log,
        )
}

/** Exposes the GraphQL repositories under `@GraphQl`; everything else in this module stays `internal`. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class GraphQlBindingsModule {
    @Binds
    abstract fun cacheMaintenance(impl: ApolloCacheMaintenance): CacheMaintenance

    @Binds
    abstract fun client(impl: ApolloGraphQlClient): GraphQlClient

    @Binds
    @Singleton
    @GraphQl
    abstract fun castRepository(impl: GraphQlCastRepository): CastRepository

    @Binds
    @Singleton
    @GraphQl
    abstract fun episodeRepository(impl: GraphQlEpisodeRepository): EpisodeRepository
}
