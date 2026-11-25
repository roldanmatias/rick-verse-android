package com.roldanmatias.rickverse.data.rest.mapper

import com.roldanmatias.rickverse.core.domain.model.Episode
import com.roldanmatias.rickverse.data.rest.dto.EpisodeDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val CODE_PATTERN = Regex("""S(\d+)E(\d+)""", RegexOption.IGNORE_CASE)

/**
 * `air_date` looks like `December 2, 2013`. The locale is pinned to English: with the device default, month names
 * would not parse on a phone set to Spanish.
 */
private val AIR_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)

internal fun EpisodeDto.toDomain(): Episode {
    val match =
        CODE_PATTERN.matchEntire(episode.trim())
            ?: throw MappingException("Malformed episode code '$episode' for episode $id")
    return Episode(
        id = id,
        name = name,
        code = episode,
        season = match.groupValues[1].toInt(),
        number = match.groupValues[2].toInt(),
        airDate = airDate.toAirDate(),
    )
}

private fun String.toAirDate(): LocalDate? =
    try {
        LocalDate.parse(trim(), AIR_DATE_FORMAT)
    } catch (_: DateTimeParseException) {
        null // a missing or odd date must not fail the whole episode
    }

/** The numeric ids of the characters in this episode, from URLs like `…/character/35`. */
internal fun EpisodeDto.characterIds(): List<Int> = characters.mapNotNull { it.trailingId() }
