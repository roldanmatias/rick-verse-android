package com.roldanmatias.rickverse.core.domain.usecase

import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.CastMemberDetail
import com.roldanmatias.rickverse.core.domain.repository.CastRepository
import javax.inject.Inject

/** Loads one cast member with their episodes. */
public class GetCastMemberDetailUseCase
    @Inject
    constructor(
        private val repository: CastRepository,
    ) {
        public suspend operator fun invoke(id: Int): DomainResult<CastMemberDetail> = repository.detail(id)
    }
