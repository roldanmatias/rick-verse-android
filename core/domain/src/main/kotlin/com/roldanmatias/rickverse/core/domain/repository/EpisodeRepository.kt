package com.roldanmatias.rickverse.core.domain.repository

import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.Episode
import com.roldanmatias.rickverse.core.domain.model.EpisodeDetail
import com.roldanmatias.rickverse.core.domain.model.Page

/** Source of episodes. Implemented by the REST and GraphQL data modules. */
public interface EpisodeRepository {
    /** One page of episodes. @param page 1-based page number. */
    public suspend fun episodes(page: Int): DomainResult<Page<Episode>>

    /** An episode with the characters that appear in it, or `Failure(NotFound)` when [id] does not exist. */
    public suspend fun detail(id: Int): DomainResult<EpisodeDetail>
}
