package com.roldanmatias.rickverse.core.domain.usecase

import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.CastFilter
import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.domain.model.Page
import com.roldanmatias.rickverse.core.domain.repository.CastRepository
import javax.inject.Inject

/**
 * Searches the cast. Owns the one business rule of the search box: user input is trimmed, and blank input means
 * "no name filter". Keeping it here means every screen and both data sources get identical behaviour.
 */
public class SearchCastUseCase
    @Inject
    constructor(
        private val repository: CastRepository,
    ) {
        public suspend operator fun invoke(
            page: Int,
            query: String? = null,
            status: CastMember.Status? = null,
        ): DomainResult<Page<CastMember>> =
            repository.cast(page = page, filter = CastFilter(name = query.normalized(), status = status))

        private fun String?.normalized(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
    }
