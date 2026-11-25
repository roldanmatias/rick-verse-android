package com.roldanmatias.rickverse.data.rest.repository

import com.roldanmatias.rickverse.core.common.DispatcherProvider
import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.error.runCatchingDomain
import com.roldanmatias.rickverse.core.domain.model.Episode
import com.roldanmatias.rickverse.core.domain.model.EpisodeDetail
import com.roldanmatias.rickverse.core.domain.model.Page
import com.roldanmatias.rickverse.core.domain.repository.EpisodeRepository
import com.roldanmatias.rickverse.data.rest.api.EpisodeApi
import com.roldanmatias.rickverse.data.rest.mapper.toDomain
import com.roldanmatias.rickverse.data.rest.mapper.toDomainError
import com.roldanmatias.rickverse.data.rest.mapper.toPage
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class RestEpisodeRepository
    @Inject
    constructor(
        private val episodeApi: EpisodeApi,
        private val dispatchers: DispatcherProvider,
    ) : EpisodeRepository {
        override suspend fun episodes(page: Int): DomainResult<Page<Episode>> =
            runCatchingDomain(mapError = Throwable::toDomainError) {
                withContext(dispatchers.io) { episodeApi.episodes(page).toPage { it.toDomain() } }
            }

        // Implemented in Phase 5.
        override suspend fun detail(id: Int): DomainResult<EpisodeDetail> =
            DomainResult.Failure(DomainError.Unknown("Not implemented yet"))
    }
