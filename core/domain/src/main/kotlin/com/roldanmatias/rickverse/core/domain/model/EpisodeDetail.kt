package com.roldanmatias.rickverse.core.domain.model

/** An [Episode] and the characters that appear in it. */
public data class EpisodeDetail(val episode: Episode, val cast: List<CastMember>)
