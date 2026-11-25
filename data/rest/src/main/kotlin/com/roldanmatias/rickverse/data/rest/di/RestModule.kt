package com.roldanmatias.rickverse.data.rest.di

import com.roldanmatias.rickverse.core.domain.di.Rest
import com.roldanmatias.rickverse.core.domain.repository.CastRepository
import com.roldanmatias.rickverse.core.domain.repository.EpisodeRepository
import com.roldanmatias.rickverse.data.rest.api.CharacterApi
import com.roldanmatias.rickverse.data.rest.api.EpisodeApi
import com.roldanmatias.rickverse.data.rest.repository.RestCastRepository
import com.roldanmatias.rickverse.data.rest.repository.RestEpisodeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

internal const val REST_BASE_URL = "https://rickandmortyapi.com/api/"

/** Builds the Retrofit instance. Extracted so tests can point it at a `MockWebServer`. */
internal fun createRetrofit(baseUrl: String, client: OkHttpClient, json: Json): Retrofit =
    Retrofit
        .Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

@Module
@InstallIn(SingletonComponent::class)
internal object RestProvidesModule {
    @Provides
    @Singleton
    fun retrofit(client: OkHttpClient, json: Json): Retrofit = createRetrofit(REST_BASE_URL, client, json)

    @Provides
    @Singleton
    fun characterApi(retrofit: Retrofit): CharacterApi = retrofit.create(CharacterApi::class.java)

    @Provides
    @Singleton
    fun episodeApi(retrofit: Retrofit): EpisodeApi = retrofit.create(EpisodeApi::class.java)
}

/** Exposes the REST repositories under the `@Rest` qualifier; `:app` chooses between `@Rest` and `@GraphQl`. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class RestBindingsModule {
    @Binds
    @Singleton
    @Rest
    abstract fun castRepository(impl: RestCastRepository): CastRepository

    @Binds
    @Singleton
    @Rest
    abstract fun episodeRepository(impl: RestEpisodeRepository): EpisodeRepository
}
