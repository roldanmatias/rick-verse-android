package com.roldanmatias.rickverse.di

import com.roldanmatias.rickverse.core.domain.di.Rest
import com.roldanmatias.rickverse.core.domain.repository.CastRepository
import com.roldanmatias.rickverse.core.domain.repository.EpisodeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * The composition root's one decision: which implementation the unqualified repositories resolve to.
 * Features inject `CastRepository` and never learn whether REST or GraphQL answers. Phase 7 replaces these
 * bindings with a switching repository that delegates to `@Rest` or `@GraphQl` at runtime.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds
    abstract fun castRepository(
        @Rest impl: CastRepository,
    ): CastRepository

    @Binds
    abstract fun episodeRepository(
        @Rest impl: EpisodeRepository,
    ): EpisodeRepository
}
