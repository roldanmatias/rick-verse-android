package com.roldanmatias.rickverse.data.graphql.repository

import com.apollographql.apollo.api.Optional
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.error.runCatchingDomain
import com.roldanmatias.rickverse.core.domain.model.Episode
import com.roldanmatias.rickverse.core.domain.model.EpisodeDetail
import com.roldanmatias.rickverse.core.domain.model.Page
import com.roldanmatias.rickverse.core.domain.repository.EpisodeRepository
import com.roldanmatias.rickverse.core.domain.repository.SettingsRepository
import com.roldanmatias.rickverse.data.graphql.client.GraphQlClient
import com.roldanmatias.rickverse.data.graphql.generated.EpisodeDetailQuery
import com.roldanmatias.rickverse.data.graphql.generated.EpisodePageQuery
import com.roldanmatias.rickverse.data.graphql.mapper.InvalidDataException
import com.roldanmatias.rickverse.data.graphql.mapper.NotFoundException
import com.roldanmatias.rickverse.data.graphql.mapper.toDomainError
import com.roldanmatias.rickverse.data.graphql.mapper.toDomainOrNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class GraphQlEpisodeRepository @Inject constructor(
    private val client: GraphQlClient,
    private val settings: SettingsRepository,
) : EpisodeRepository {
    override suspend fun episodes(page: Int): DomainResult<Page<Episode>> =
        runCatchingDomain(mapError = Throwable::toDomainError) {
            val episodes = client.query(EpisodePageQuery(Optional.present(page)), policy()).episodes
            Page(
                items = episodes?.results.orEmpty().mapNotNull { it?.episodeBasic?.toDomainOrNull() },
                nextPage = episodes?.info?.next,
            )
        }

    override suspend fun detail(id: Int): DomainResult<EpisodeDetail> =
        runCatchingDomain(mapError = Throwable::toDomainError) {
            val episode = client.query(EpisodeDetailQuery(id.toString()), policy()).episode
                ?: throw NotFoundException
            EpisodeDetail(
                episode = episode.episodeBasic.toDomainOrNull()
                    ?: throw InvalidDataException("Episode $id is missing required fields"),
                cast = episode.characters.mapNotNull { it?.castMemberBasic?.toDomainOrNull() },
            )
        }

    private suspend fun policy() = settings.cachePolicy.first().toFetchPolicy()
}
