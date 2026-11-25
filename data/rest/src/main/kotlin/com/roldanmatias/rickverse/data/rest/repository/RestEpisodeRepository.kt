package com.roldanmatias.rickverse.data.rest.repository

import com.roldanmatias.rickverse.core.common.DispatcherProvider
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.error.runCatchingDomain
import com.roldanmatias.rickverse.core.domain.model.Episode
import com.roldanmatias.rickverse.core.domain.model.EpisodeDetail
import com.roldanmatias.rickverse.core.domain.model.Page
import com.roldanmatias.rickverse.core.domain.repository.EpisodeRepository
import com.roldanmatias.rickverse.data.rest.api.CharacterApi
import com.roldanmatias.rickverse.data.rest.api.EpisodeApi
import com.roldanmatias.rickverse.data.rest.mapper.characterIds
import com.roldanmatias.rickverse.data.rest.mapper.toDomain
import com.roldanmatias.rickverse.data.rest.mapper.toDomainError
import com.roldanmatias.rickverse.data.rest.mapper.toPage
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class RestEpisodeRepository @Inject constructor(
    private val episodeApi: EpisodeApi,
    private val characterApi: CharacterApi,
    private val dispatchers: DispatcherProvider,
) : EpisodeRepository {
    override suspend fun episodes(page: Int): DomainResult<Page<Episode>> =
        runCatchingDomain(mapError = Throwable::toDomainError) {
            withContext(dispatchers.io) { episodeApi.episodes(page).toPage { it.toDomain() } }
        }

    /** The episode plus its characters through the batch endpoint: **2 requests**, however large the cast. */
    override suspend fun detail(id: Int): DomainResult<EpisodeDetail> =
        runCatchingDomain(mapError = Throwable::toDomainError) {
            withContext(dispatchers.io) {
                val episode = episodeApi.episode(id)
                val characterIds = episode.characterIds()
                val cast = if (characterIds.isEmpty()) {
                    emptyList()
                } else {
                    characterApi.characters(characterIds.joinToString(",")).items.map { it.toDomain() }
                }
                EpisodeDetail(episode = episode.toDomain(), cast = cast)
            }
        }
}
