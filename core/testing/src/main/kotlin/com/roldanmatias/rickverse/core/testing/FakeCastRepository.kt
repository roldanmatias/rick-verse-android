package com.roldanmatias.rickverse.core.testing

import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.CastFilter
import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.domain.model.CastMemberDetail
import com.roldanmatias.rickverse.core.domain.model.Page
import com.roldanmatias.rickverse.core.domain.repository.CastRepository
import java.util.Collections

/**
 * A scripted fake that also **records** every call. Tests assert on state (what was asked, what came back)
 * instead of verifying mock interactions, so they survive refactoring.
 *
 * Script behaviour by assigning [castHandler] / [detailHandler]; handlers are `suspend`, so a test can
 * `delay()` inside one to simulate a slow response (virtual time makes that free).
 */
public class FakeCastRepository : CastRepository {
    public data class CastCall(val page: Int, val filter: CastFilter)

    private val recordedCastCalls = Collections.synchronizedList(mutableListOf<CastCall>())
    private val recordedDetailCalls = Collections.synchronizedList(mutableListOf<Int>())

    public val castCalls: List<CastCall> get() = recordedCastCalls.toList()
    public val detailCalls: List<Int> get() = recordedDetailCalls.toList()

    public var castHandler: suspend (page: Int, filter: CastFilter) -> DomainResult<Page<CastMember>> =
        { _, _ -> DomainResult.Success(Page.empty()) }

    public var detailHandler: suspend (id: Int) -> DomainResult<CastMemberDetail> =
        { DomainResult.Success(TestData.castMemberDetail(member = TestData.castMember(id = it))) }

    override suspend fun cast(page: Int, filter: CastFilter): DomainResult<Page<CastMember>> {
        recordedCastCalls += CastCall(page, filter)
        return castHandler(page, filter)
    }

    override suspend fun detail(id: Int): DomainResult<CastMemberDetail> {
        recordedDetailCalls += id
        return detailHandler(id)
    }
}
