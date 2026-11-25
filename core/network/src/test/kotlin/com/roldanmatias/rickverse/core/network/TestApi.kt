package com.roldanmatias.rickverse.core.network

import kotlinx.serialization.Serializable
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class TestCharacter(val id: Int, val name: String)

@Serializable
data class TestPage(val results: List<TestCharacter>)

interface TestApi {
    @GET("character")
    suspend fun search(
        @Query("name") name: String,
        @Query("page") page: Int = 1,
    ): TestPage

    @GET("character/{id}")
    suspend fun character(
        @Path("id") id: Int,
    ): TestCharacter
}

fun testApi(baseUrl: HttpUrl, client: OkHttpClient): TestApi =
    Retrofit
        .Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(NetworkJson.default.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(TestApi::class.java)
