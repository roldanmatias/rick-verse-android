package com.roldanmatias.rickverse.core.domain.model

/**
 * Search criteria for the cast list. A `null` field means "do not filter by this".
 * Normalisation (trimming, blank to `null`) is done once, in `SearchCastUseCase`.
 */
public data class CastFilter(val name: String? = null, val status: CastMember.Status? = null) {
    public companion object {
        /** No filtering at all. */
        public val None: CastFilter = CastFilter()
    }
}
