package com.roldanmatias.rickverse.data.rest.repository

import com.roldanmatias.rickverse.core.common.DispatcherProvider
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.error.runCatchingDomain
import com.roldanmatias.rickverse.core.domain.model.CastFilter
import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.domain.model.CastMemberDetail
import com.roldanmatias.rickverse.core.domain.model.Page
import com.roldanmatias.rickverse.core.domain.repository.CastRepository
import com.roldanmatias.rickverse.data.rest.api.CharacterApi
import com.roldanmatias.rickverse.data.rest.api.EpisodeApi
import com.roldanmatias.rickverse.data.rest.mapper.episodeIds
import com.roldanmatias.rickverse.data.rest.mapper.toDetail
import com.roldanmatias.rickverse.data.rest.mapper.toDomain
import com.roldanmatias.rickverse.data.rest.mapper.toDomainError
import com.roldanmatias.rickverse.data.rest.mapper.toPage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.HttpURLConnection
import javax.inject.Inject

internal class RestCastRepository
    @Inject
    constructor(
        private val characterApi: CharacterApi,
        private val episodeApi: EpisodeApi,
        private val dispatchers: DispatcherProvider,
    ) : CastRepository {
        override suspend fun cast(page: Int, filter: CastFilter): DomainResult<Page<CastMember>> =
            runCatchingDomain(mapError = Throwable::toDomainError) {
                withContext(dispatchers.io) {
                    try {
                        characterApi
                            .characters(page = page, name = filter.name, status = filter.status?.name?.lowercase())
                            .toPage { it.toDomain() }
                    } catch (notFound: HttpException) {
                        // Quirk: REST answers 404 for a search with no matches.
                        // For a list that means "empty", not "not found".
                        if (notFound.code() == HttpURLConnection.HTTP_NOT_FOUND) Page.empty() else throw notFound
                    }
                }
            }

        /**
         * **Version A (naive, concurrent).** `GET /character/{id}`, then one `GET /episode/{n}` per episode: for Rick
         * that is 1 + 51 = **52 requests**. Running them concurrently with `async` makes it faster, but it is still 52
         * requests, so a [Semaphore] caps how many are in flight to avoid flooding the server.
         * Superseded by the batch endpoint in the next commit; kept in git history for the benchmark.
         */
        override suspend fun detail(id: Int): DomainResult<CastMemberDetail> =
            runCatchingDomain(mapError = Throwable::toDomainError) {
                withContext(dispatchers.io) {
                    val character = characterApi.character(id)
                    val gate = Semaphore(MAX_CONCURRENT_REQUESTS)
                    val episodes =
                        coroutineScope {
                            character
                                .episodeIds()
                                .map { episodeId ->
                                    async { gate.withPermit { episodeApi.episode(episodeId).toDomain() } }
                                }.awaitAll() // awaitAll preserves the order of the ids
                        }
                    character.toDetail(episodes)
                }
            }

        private companion object {
            const val MAX_CONCURRENT_REQUESTS = 6
        }
    }
