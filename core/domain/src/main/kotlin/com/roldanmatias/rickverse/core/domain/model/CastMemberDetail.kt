package com.roldanmatias.rickverse.core.domain.model

/** A [CastMember] together with the extra fields only the detail screen needs and the episodes they appear in. */
public data class CastMemberDetail(
    val member: CastMember,
    val gender: String,
    val originName: String,
    val locationName: String,
    val episodes: List<Episode>,
    /** `true` while this is a locally cached copy (offline, or before the fresh one arrives). */
    val fromCache: Boolean = false,
)
