package com.roldanmatias.rickverse.data.graphql.di

import com.apollographql.apollo.ApolloClient
import com.roldanmatias.rickverse.core.domain.di.GraphQl
import com.roldanmatias.rickverse.core.domain.repository.CastRepository
import com.roldanmatias.rickverse.core.domain.repository.EpisodeRepository
import com.roldanmatias.rickverse.data.graphql.client.ApolloGraphQlClient
import com.roldanmatias.rickverse.data.graphql.client.GRAPHQL_URL
import com.roldanmatias.rickverse.data.graphql.client.GraphQlClient
import com.roldanmatias.rickverse.data.graphql.client.createApolloClient
import com.roldanmatias.rickverse.data.graphql.repository.GraphQlCastRepository
import com.roldanmatias.rickverse.data.graphql.repository.GraphQlEpisodeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object GraphQlProvidesModule {
    @Provides
    @Singleton
    fun apolloClient(okHttp: OkHttpClient): ApolloClient = createApolloClient(GRAPHQL_URL, okHttp)
}

/** Exposes the GraphQL repositories under `@GraphQl`; everything else in this module stays `internal`. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class GraphQlBindingsModule {
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
