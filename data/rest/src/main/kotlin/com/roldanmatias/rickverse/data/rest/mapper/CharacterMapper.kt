package com.roldanmatias.rickverse.data.rest.mapper

import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.domain.model.CastMemberDetail
import com.roldanmatias.rickverse.core.domain.model.Episode
import com.roldanmatias.rickverse.data.rest.dto.CharacterDto

/** An unknown status string never crashes: it becomes [CastMember.Status.Unknown]. */
internal fun String.toStatus(): CastMember.Status =
    CastMember.Status.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: CastMember.Status.Unknown

internal fun CharacterDto.toDomain(): CastMember =
    CastMember(id = id, name = name, status = status.toStatus(), species = species, imageUrl = image)

internal fun CharacterDto.toDetail(episodes: List<Episode>): CastMemberDetail =
    CastMemberDetail(
        member = toDomain(),
        gender = gender,
        originName = origin.name,
        locationName = location.name,
        episodes = episodes,
    )

/** The numeric ids of the episodes this character appears in, from URLs like `…/episode/28`. */
internal fun CharacterDto.episodeIds(): List<Int> = episode.mapNotNull { it.trailingId() }
