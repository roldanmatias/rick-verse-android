package com.roldanmatias.rickverse.data

import com.roldanmatias.rickverse.core.domain.di.GraphQl
import com.roldanmatias.rickverse.core.domain.di.Rest
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.CastFilter
import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.domain.model.CastMemberDetail
import com.roldanmatias.rickverse.core.domain.model.DataSource
import com.roldanmatias.rickverse.core.domain.model.Episode
import com.roldanmatias.rickverse.core.domain.model.EpisodeDetail
import com.roldanmatias.rickverse.core.domain.model.Page
import com.roldanmatias.rickverse.core.domain.repository.CastRepository
import com.roldanmatias.rickverse.core.domain.repository.EpisodeRepository
import com.roldanmatias.rickverse.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What features inject as a plain `CastRepository`. It reads the current data source **at call time** and delegates,
 * so ViewModels never know a switch happened (and a call already in flight finishes against the source it started on).
 */
@Singleton
internal class SwitchingCastRepository @Inject constructor(
    @Rest private val rest: CastRepository,
    @GraphQl private val graphQl: CastRepository,
    private val settings: SettingsRepository,
) : CastRepository {
    override suspend fun cast(page: Int, filter: CastFilter): DomainResult<Page<CastMember>> =
        current().cast(page, filter)

    override suspend fun detail(id: Int): DomainResult<CastMemberDetail> = current().detail(id)

    override fun detailUpdates(id: Int): Flow<DomainResult<CastMemberDetail>> =
        flow { emitAll(current().detailUpdates(id)) }

    private suspend fun current() = if (settings.dataSource.first() == DataSource.GraphQl) graphQl else rest
}

@Singleton
internal class SwitchingEpisodeRepository @Inject constructor(
    @Rest private val rest: EpisodeRepository,
    @GraphQl private val graphQl: EpisodeRepository,
    private val settings: SettingsRepository,
) : EpisodeRepository {
    override suspend fun episodes(page: Int): DomainResult<Page<Episode>> = current().episodes(page)

    override suspend fun detail(id: Int): DomainResult<EpisodeDetail> = current().detail(id)

    private suspend fun current() = if (settings.dataSource.first() == DataSource.GraphQl) graphQl else rest
}
