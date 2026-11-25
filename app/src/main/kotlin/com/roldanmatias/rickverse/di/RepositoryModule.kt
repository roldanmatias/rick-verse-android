package com.roldanmatias.rickverse.di

import com.roldanmatias.rickverse.core.domain.repository.CastRepository
import com.roldanmatias.rickverse.core.domain.repository.EpisodeRepository
import com.roldanmatias.rickverse.data.SwitchingCastRepository
import com.roldanmatias.rickverse.data.SwitchingEpisodeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * The composition root's one decision: unqualified repositories resolve to the switching ones, which delegate to
 * `@Rest` or `@GraphQl` according to Settings. Features inject `CastRepository` and never learn which answers.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds
    abstract fun castRepository(impl: SwitchingCastRepository): CastRepository

    @Binds
    abstract fun episodeRepository(impl: SwitchingEpisodeRepository): EpisodeRepository
}
