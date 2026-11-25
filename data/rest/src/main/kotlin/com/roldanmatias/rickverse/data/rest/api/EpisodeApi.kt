package com.roldanmatias.rickverse.data.rest.api

import com.roldanmatias.rickverse.data.rest.dto.EpisodeDto
import com.roldanmatias.rickverse.data.rest.dto.OneOrMany
import com.roldanmatias.rickverse.data.rest.dto.PageDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface EpisodeApi {
    @GET("episode")
    suspend fun episodes(
        @Query("page") page: Int,
    ): PageDto<EpisodeDto>

    @GET("episode/{id}")
    suspend fun episode(
        @Path("id") id: Int,
    ): EpisodeDto

    /** The batch endpoint: one request for many episodes. [ids] is a comma-separated list. */
    @GET("episode/{ids}")
    suspend fun episodes(
        @Path("ids", encoded = true) ids: String,
    ): OneOrMany<EpisodeDto>
}
