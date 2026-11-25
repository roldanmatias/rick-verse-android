package com.roldanmatias.rickverse.data.rest.repository

import com.roldanmatias.rickverse.core.common.DispatcherProvider
import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.error.runCatchingDomain
import com.roldanmatias.rickverse.core.domain.model.CastFilter
import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.domain.model.CastMemberDetail
import com.roldanmatias.rickverse.core.domain.model.Page
import com.roldanmatias.rickverse.core.domain.repository.CastRepository
import com.roldanmatias.rickverse.data.rest.api.CharacterApi
import com.roldanmatias.rickverse.data.rest.mapper.toDomain
import com.roldanmatias.rickverse.data.rest.mapper.toDomainError
import com.roldanmatias.rickverse.data.rest.mapper.toPage
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.HttpURLConnection
import javax.inject.Inject

internal class RestCastRepository
    @Inject
    constructor(
        private val characterApi: CharacterApi,
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

        // Implemented in Phase 5.
        override suspend fun detail(id: Int): DomainResult<CastMemberDetail> =
            DomainResult.Failure(DomainError.Unknown("Not implemented yet"))
    }
