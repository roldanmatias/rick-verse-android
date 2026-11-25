package com.roldanmatias.rickverse.data

import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.CastFilter
import com.roldanmatias.rickverse.core.domain.model.DataSource
import com.roldanmatias.rickverse.core.domain.model.Page
import com.roldanmatias.rickverse.core.testing.FakeCastRepository
import com.roldanmatias.rickverse.core.testing.FakeEpisodeRepository
import com.roldanmatias.rickverse.core.testing.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SwitchingRepositoriesTest {
    private val rest = FakeCastRepository()
    private val graphQl = FakeCastRepository()
    private val settings = FakeSettingsRepository()

    @Test
    fun `cast calls follow the data source flag`() = runTest {
        val repository = SwitchingCastRepository(rest, graphQl, settings)

        repository.cast(1, CastFilter.None)
        settings.setDataSource(DataSource.GraphQl)
        repository.cast(2, CastFilter.None)
        repository.detail(7)
        settings.setDataSource(DataSource.Rest)
        repository.detail(8)

        assertEquals(listOf(1), rest.castCalls.map { it.page })
        assertEquals(listOf(8), rest.detailCalls)
        assertEquals(listOf(2), graphQl.castCalls.map { it.page })
        assertEquals(listOf(7), graphQl.detailCalls)
    }

    @Test
    fun `episode calls follow the data source flag and results pass through untouched`() = runTest {
        val restEpisodes = FakeEpisodeRepository()
        val graphQlEpisodes = FakeEpisodeRepository()
        val expected = DomainResult.Success(Page(emptyList<com.roldanmatias.rickverse.core.domain.model.Episode>(), 9))
        graphQlEpisodes.episodesHandler = { expected }
        val repository = SwitchingEpisodeRepository(restEpisodes, graphQlEpisodes, settings)

        repository.episodes(1)
        settings.setDataSource(DataSource.GraphQl)

        assertEquals(expected, repository.episodes(2))
        assertEquals(listOf(1), restEpisodes.episodeCalls)
        assertEquals(listOf(2), graphQlEpisodes.episodeCalls)
    }
}
