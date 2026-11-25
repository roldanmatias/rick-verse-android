package com.roldanmatias.rickverse

import com.roldanmatias.rickverse.core.domain.di.Remote
import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.Page
import com.roldanmatias.rickverse.core.domain.repository.CastRepository
import com.roldanmatias.rickverse.core.domain.repository.EpisodeRepository
import com.roldanmatias.rickverse.core.testing.FakeCastRepository
import com.roldanmatias.rickverse.core.testing.FakeEpisodeRepository
import com.roldanmatias.rickverse.core.testing.TestData
import com.roldanmatias.rickverse.di.RepositoryModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Replaces the production [RepositoryModule] so the end-to-end test is hermetic: no network, no backend choice,
 * deterministic data. The rest of the graph (ViewModels, navigation, DataStore, theme) is the real one.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [RepositoryModule::class])
internal object FakeRepositoryModule {
    @Provides
    @Singleton
    @Remote
    fun castRepository(): CastRepository =
        FakeCastRepository().apply {
            castHandler = { _, _ ->
                DomainResult.Success(
                    Page(
                        listOf(
                            TestData.castMember(id = 1, name = "Rick Sanchez"),
                            TestData.castMember(id = 2, name = "Morty Smith"),
                        ),
                        nextPage = null,
                    ),
                )
            }
            detailHandler = { id ->
                DomainResult.Success(
                    TestData.castMemberDetail(member = TestData.castMember(id = id, name = "Rick Sanchez")),
                )
            }
        }

    @Provides
    @Singleton
    fun episodeRepository(): EpisodeRepository = FakeEpisodeRepository()
}
