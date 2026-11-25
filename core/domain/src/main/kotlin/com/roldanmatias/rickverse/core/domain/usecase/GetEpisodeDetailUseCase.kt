package com.roldanmatias.rickverse.core.domain.usecase

import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.EpisodeDetail
import com.roldanmatias.rickverse.core.domain.repository.EpisodeRepository
import javax.inject.Inject

/** Loads one episode with its characters. */
public class GetEpisodeDetailUseCase @Inject constructor(private val repository: EpisodeRepository) {
    public suspend operator fun invoke(id: Int): DomainResult<EpisodeDetail> = repository.detail(id)
}
