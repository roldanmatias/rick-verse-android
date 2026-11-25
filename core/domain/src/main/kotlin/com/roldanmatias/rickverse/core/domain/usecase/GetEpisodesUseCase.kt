package com.roldanmatias.rickverse.core.domain.usecase

import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.Episode
import com.roldanmatias.rickverse.core.domain.model.Page
import com.roldanmatias.rickverse.core.domain.repository.EpisodeRepository
import javax.inject.Inject

/** Loads one page of episodes. */
public class GetEpisodesUseCase
    @Inject
    constructor(
        private val repository: EpisodeRepository,
    ) {
        public suspend operator fun invoke(page: Int): DomainResult<Page<Episode>> = repository.episodes(page)
    }
