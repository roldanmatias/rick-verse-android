package com.roldanmatias.rickverse.di

import com.roldanmatias.rickverse.core.domain.di.Remote
import com.roldanmatias.rickverse.core.domain.repository.CastRepository
import com.roldanmatias.rickverse.core.domain.repository.EpisodeRepository
import com.roldanmatias.rickverse.data.SwitchingCastRepository
import com.roldanmatias.rickverse.data.SwitchingEpisodeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * The composition root's one decision: `@Remote` repositories resolve to the switching ones, which delegate to
 * `@Rest` or `@GraphQl` according to Settings. Features inject `CastRepository` and never learn which answers.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    // `@Remote` = whichever network backend is active. The offline-first cache (:data:local) wraps it and is what
    // features get as the unqualified CastRepository.
    @Binds
    @Remote
    abstract fun castRepository(impl: SwitchingCastRepository): CastRepository

    @Binds
    abstract fun episodeRepository(impl: SwitchingEpisodeRepository): EpisodeRepository
}
