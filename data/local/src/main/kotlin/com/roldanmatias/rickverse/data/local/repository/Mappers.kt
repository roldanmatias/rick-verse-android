package com.roldanmatias.rickverse.data.local.repository

import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.domain.model.Episode
import com.roldanmatias.rickverse.data.local.db.CastMemberEntity
import com.roldanmatias.rickverse.data.local.db.EpisodeEntity
import java.time.LocalDate

internal fun CastMember.toEntity(now: Long) =
    CastMemberEntity(id, name, status.name, species, imageUrl, cachedAt = now)

internal fun CastMemberEntity.toDomain() =
    CastMember(
        id = id,
        name = name,
        // An unknown stored value must never crash a cache read.
        status = CastMember.Status.entries.firstOrNull { it.name == status } ?: CastMember.Status.Unknown,
        species = species,
        imageUrl = imageUrl,
    )

internal fun Episode.toEntity() = EpisodeEntity(id, name, code, season, number, airDate?.toString())

internal fun EpisodeEntity.toDomain() =
    Episode(id, name, code, season, number, airDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() })
