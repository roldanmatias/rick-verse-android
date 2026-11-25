package com.roldanmatias.rickverse.data.local.repository

import com.roldanmatias.rickverse.core.domain.di.Remote
import com.roldanmatias.rickverse.core.domain.error.DomainError
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.CastFilter
import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.domain.model.CastMemberDetail
import com.roldanmatias.rickverse.core.domain.model.Page
import com.roldanmatias.rickverse.core.domain.repository.CastRepository
import com.roldanmatias.rickverse.data.local.db.CastDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Decorator: adds an on-device cache in front of whichever remote repository is active (ADR 0010).
 *
 * * **Writes**: every successful remote answer is saved to Room.
 * * **Lists**: network first; if the network is unreachable the cached page is served with `fromCache = true`, so
 *   the UI shows a banner instead of an error screen. A cache miss keeps the original error.
 * * **Detail**: [detailUpdates] reads Room first (instant, offline-capable), then refreshes and saves.
 *
 * Only transient failures fall back to the cache. `NotFound` and `InvalidData` are answers, not outages.
 */
internal class OfflineFirstCastRepository @Inject constructor(
    @Remote private val remote: CastRepository,
    private val dao: CastDao,
) : CastRepository {
    override suspend fun cast(page: Int, filter: CastFilter): DomainResult<Page<CastMember>> {
        val result = remote.cast(page, filter)
        if (result is DomainResult.Success) {
            runCatchingStorage { dao.saveList(result.value.items.map { it.toEntity(System.currentTimeMillis()) }) }
            return result
        }
        val error = (result as DomainResult.Failure).error
        return if (error.isTransient()) cachedPage(page, filter) ?: result else result
    }

    override suspend fun detail(id: Int): DomainResult<CastMemberDetail> {
        val result = remote.detail(id)
        if (result is DomainResult.Success) {
            runCatchingStorage { save(result.value) }
            return result
        }
        val error = (result as DomainResult.Failure).error
        return if (error.isTransient()) cachedDetail(id)?.let { DomainResult.Success(it) } ?: result else result
    }

    override fun detailUpdates(id: Int): Flow<DomainResult<CastMemberDetail>> =
        flow {
            val cached = cachedDetail(id)
            if (cached != null) emit(DomainResult.Success(cached))
            when (val fresh = remote.detail(id)) {
                is DomainResult.Success -> {
                    runCatchingStorage { save(fresh.value) }
                    emit(fresh)
                }

                // Keep showing the cached copy (already emitted, still flagged fromCache); only fail if there is none.
                is DomainResult.Failure -> if (cached == null) emit(fresh)
            }
        }

    private suspend fun cachedPage(page: Int, filter: CastFilter): DomainResult<Page<CastMember>>? {
        val rows = runCatchingStorage {
            dao.page(
                name = filter.name?.escapeLike(),
                status = filter.status?.name,
                limit = PAGE_SIZE + 1, // one extra row tells us whether another page exists
                offset = (page - 1) * PAGE_SIZE,
            )
        }.orEmpty()
        if (rows.isEmpty()) return null
        val items = rows.take(PAGE_SIZE).map { it.toDomain() }
        val next = if (rows.size > PAGE_SIZE) page + 1 else null
        return DomainResult.Success(Page(items, next, fromCache = true))
    }

    private suspend fun cachedDetail(id: Int): CastMemberDetail? {
        val member = runCatchingStorage { dao.member(id) } ?: return null
        // `gender == null` means only a list row exists: the detail was never cached, so there is nothing to show.
        val gender = member.gender ?: return null
        return CastMemberDetail(
            member = member.toDomain(),
            gender = gender,
            originName = member.originName.orEmpty(),
            locationName = member.locationName.orEmpty(),
            episodes = dao.episodesOf(id).map { it.toDomain() },
            fromCache = true,
        )
    }

    private suspend fun save(detail: CastMemberDetail) {
        val entity = detail.member.toEntity(System.currentTimeMillis()).copy(
            gender = detail.gender,
            originName = detail.originName,
            locationName = detail.locationName,
        )
        dao.saveDetail(entity, detail.episodes.map { it.toEntity() })
    }

    private fun DomainError.isTransient() =
        this is DomainError.Offline || this is DomainError.Timeout || this is DomainError.Server ||
            this is DomainError.Unknown

    /** A broken cache must never break the app: storage failures degrade to "no cache". */
    private inline fun <T> runCatchingStorage(block: () -> T): T? =
        try {
            block()
        } catch (@Suppress("TooGenericExceptionCaught") failure: Exception) {
            if (failure is kotlinx.coroutines.CancellationException) throw failure
            null
        }

    private fun String.escapeLike() = replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    private companion object {
        const val PAGE_SIZE = 20
    }
}
