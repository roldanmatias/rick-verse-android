package com.roldanmatias.rickverse.core.domain.repository

import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.CastFilter
import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.domain.model.CastMemberDetail
import com.roldanmatias.rickverse.core.domain.model.Page
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Source of cast members. Implemented by the REST and GraphQL data modules; features only see this interface. */
public interface CastRepository {
    /**
     * One page of cast members matching [filter]. An empty search is a [DomainResult.Success] with an empty
     * [Page], never a `NotFound`.
     *
     * @param page 1-based page number.
     */
    public suspend fun cast(page: Int, filter: CastFilter): DomainResult<Page<CastMember>>

    /** A single cast member with their episodes, or `Failure(NotFound)` when [id] does not exist. */
    public suspend fun detail(id: Int): DomainResult<CastMemberDetail>

    /**
     * The detail as a stream: **stale-while-revalidate**. A backend with a cache may emit the cached value first and
     * the fresh one later; the default emits [detail] once (REST). Collectors must treat every emission as the
     * latest truth, and a failure after a success must not discard what is already on screen.
     */
    public fun detailUpdates(id: Int): Flow<DomainResult<CastMemberDetail>> = flow { emit(detail(id)) }
}
