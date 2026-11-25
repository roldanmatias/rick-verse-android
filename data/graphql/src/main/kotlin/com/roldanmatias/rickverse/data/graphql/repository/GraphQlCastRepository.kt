package com.roldanmatias.rickverse.data.graphql.repository

import com.apollographql.apollo.api.Optional
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.error.runCatchingDomain
import com.roldanmatias.rickverse.core.domain.model.CachePolicy
import com.roldanmatias.rickverse.core.domain.model.CastFilter
import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.domain.model.CastMemberDetail
import com.roldanmatias.rickverse.core.domain.model.Page
import com.roldanmatias.rickverse.core.domain.repository.CastRepository
import com.roldanmatias.rickverse.core.domain.repository.SettingsRepository
import com.roldanmatias.rickverse.data.graphql.client.FetchPolicy
import com.roldanmatias.rickverse.data.graphql.client.GraphQlClient
import com.roldanmatias.rickverse.data.graphql.generated.CastMemberDetailQuery
import com.roldanmatias.rickverse.data.graphql.generated.CastPageQuery
import com.roldanmatias.rickverse.data.graphql.mapper.InvalidDataException
import com.roldanmatias.rickverse.data.graphql.mapper.NotFoundException
import com.roldanmatias.rickverse.data.graphql.mapper.toDomainError
import com.roldanmatias.rickverse.data.graphql.mapper.toDomainOrNull
import kotlinx.coroutines.flow.first
import javax.inject.Inject

internal class GraphQlCastRepository @Inject constructor(
    private val client: GraphQlClient,
    private val settings: SettingsRepository,
) : CastRepository {
    /** An empty search is an empty `results` array here (REST answers 404): no special case needed. */
    override suspend fun cast(page: Int, filter: CastFilter): DomainResult<Page<CastMember>> =
        runCatchingDomain(mapError = Throwable::toDomainError) {
            val query = CastPageQuery(
                page = Optional.present(page),
                name = Optional.presentIfNotNull(filter.name),
                status = Optional.presentIfNotNull(filter.status?.name?.lowercase()),
            )
            val characters = client.query(query, currentPolicy()).characters
            Page(
                items = characters?.results.orEmpty().mapNotNull { it?.castMemberBasic?.toDomainOrNull() },
                nextPage = characters?.info?.next,
            )
        }

    /** One request: the character and all their episodes in a single selection set. */
    override suspend fun detail(id: Int): DomainResult<CastMemberDetail> =
        runCatchingDomain(mapError = Throwable::toDomainError) {
            val character = client.query(CastMemberDetailQuery(id = id.toString()), currentPolicy()).character
                ?: throw NotFoundException
            val member = character.castMemberBasic.toDomainOrNull()
                ?: throw InvalidDataException("Character $id is missing required fields")
            CastMemberDetail(
                member = member,
                gender = character.gender.orEmpty(),
                originName = character.origin?.name.orEmpty(),
                locationName = character.location?.name.orEmpty(),
                episodes = character.episode.mapNotNull { it?.episodeBasic?.toDomainOrNull() },
            )
        }

    private suspend fun currentPolicy(): FetchPolicy = settings.cachePolicy.first().toFetchPolicy()
}

internal fun CachePolicy.toFetchPolicy(): FetchPolicy =
    when (this) {
        CachePolicy.CacheFirst -> FetchPolicy.CacheFirst
        CachePolicy.NetworkFirst -> FetchPolicy.NetworkFirst
        CachePolicy.NetworkOnly -> FetchPolicy.NetworkOnly
        CachePolicy.CacheAndNetwork -> FetchPolicy.CacheAndNetwork
    }
