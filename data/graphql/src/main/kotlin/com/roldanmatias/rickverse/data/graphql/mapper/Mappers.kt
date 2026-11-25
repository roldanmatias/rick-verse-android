package com.roldanmatias.rickverse.data.graphql.mapper

import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.domain.model.Episode
import com.roldanmatias.rickverse.data.graphql.generated.fragment.CastMemberBasic
import com.roldanmatias.rickverse.data.graphql.generated.fragment.EpisodeBasic
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val CODE_PATTERN = Regex("""S(\d+)E(\d+)""", RegexOption.IGNORE_CASE)
private val AIR_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)

/**
 * Resilient policy (ADR 0006): every field in this schema is nullable, so an element that cannot be mapped
 * (null/non-numeric id, null name) yields `null` and the caller drops it with `mapNotNull`. One bad element must not
 * blank a whole page. A *single-item* screen has nothing to drop, so there a `null` becomes `InvalidData`.
 */
internal fun CastMemberBasic.toDomainOrNull(): CastMember? {
    val numericId = id?.toIntOrNull()
    val displayName = name?.takeIf { it.isNotBlank() }
    if (numericId == null || displayName == null) return null
    return CastMember(
        id = numericId,
        name = displayName,
        status = status.toStatus(),
        species = species.orEmpty(),
        imageUrl = image.orEmpty(),
    )
}

internal fun EpisodeBasic.toDomainOrNull(): Episode? {
    val numericId = id?.toIntOrNull()
    val displayName = name?.takeIf { it.isNotBlank() }
    val match = episode?.let { CODE_PATTERN.matchEntire(it.trim()) }
    if (numericId == null || displayName == null || match == null) return null
    return Episode(
        id = numericId,
        name = displayName,
        code = episode.orEmpty(),
        season = match.groupValues[1].toInt(),
        number = match.groupValues[2].toInt(),
        airDate = air_date?.toAirDate(),
    )
}

internal fun String?.toStatus(): CastMember.Status =
    CastMember.Status.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: CastMember.Status.Unknown

private fun String.toAirDate(): LocalDate? =
    try {
        LocalDate.parse(trim(), AIR_DATE_FORMAT)
    } catch (_: DateTimeParseException) {
        null
    }
