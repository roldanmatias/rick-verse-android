package com.roldanmatias.rickverse.data.rest.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mirrors the REST JSON exactly (`air_date` is snake_case, `episode` is the `S01E01` code). */
@Serializable
internal data class EpisodeDto(
    val id: Int,
    val name: String,
    @SerialName("air_date") val airDate: String = "",
    val episode: String,
    val characters: List<String> = emptyList(),
)
