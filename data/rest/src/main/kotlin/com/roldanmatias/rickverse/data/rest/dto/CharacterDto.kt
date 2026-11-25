package com.roldanmatias.rickverse.data.rest.dto

import kotlinx.serialization.Serializable

/** Mirrors the REST JSON exactly. Never leaves this module; mappers turn it into a domain model. */
@Serializable
internal data class CharacterDto(
    val id: Int,
    val name: String,
    val status: String,
    val species: String,
    val type: String = "",
    val gender: String = "",
    val origin: LocationRefDto,
    val location: LocationRefDto,
    val image: String,
    val episode: List<String> = emptyList(),
)

@Serializable
internal data class LocationRefDto(
    val name: String,
    val url: String = "",
)
