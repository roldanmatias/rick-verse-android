package com.roldanmatias.rickverse.data.rest.api

import com.roldanmatias.rickverse.data.rest.dto.CharacterDto
import com.roldanmatias.rickverse.data.rest.dto.OneOrMany
import com.roldanmatias.rickverse.data.rest.dto.PageDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface CharacterApi {
    /** A `null` [name] or [status] is omitted from the query by Retrofit. */
    @GET("character")
    suspend fun characters(
        @Query("page") page: Int,
        @Query("name") name: String? = null,
        @Query("status") status: String? = null,
    ): PageDto<CharacterDto>

    @GET("character/{id}")
    suspend fun character(
        @Path("id") id: Int,
    ): CharacterDto

    /** [ids] is a comma-separated list; `encoded` keeps the commas literal. */
    @GET("character/{ids}")
    suspend fun characters(
        @Path("ids", encoded = true) ids: String,
    ): OneOrMany<CharacterDto>
}
