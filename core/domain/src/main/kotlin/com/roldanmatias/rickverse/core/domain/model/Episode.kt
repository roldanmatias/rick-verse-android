package com.roldanmatias.rickverse.core.domain.model

import java.time.LocalDate

/**
 * An episode of the show.
 *
 * @property code the production code such as `"S01E01"`; [season] and [number] are parsed from it.
 * @property airDate `null` when the backend value is missing or cannot be parsed.
 */
public data class Episode(
    val id: Int,
    val name: String,
    val code: String,
    val season: Int,
    val number: Int,
    val airDate: LocalDate?,
)
