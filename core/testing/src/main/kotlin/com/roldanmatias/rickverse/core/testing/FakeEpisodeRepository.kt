package com.roldanmatias.rickverse.core.testing

import com.roldanmatias.rickverse.core.domain.error.DomainResult
import com.roldanmatias.rickverse.core.domain.model.Episode
import com.roldanmatias.rickverse.core.domain.model.EpisodeDetail
import com.roldanmatias.rickverse.core.domain.model.Page
import com.roldanmatias.rickverse.core.domain.repository.EpisodeRepository
import java.util.Collections

/** Scripted, call-recording fake of [EpisodeRepository]. See [FakeCastRepository]. */
public class FakeEpisodeRepository : EpisodeRepository {
    private val recordedEpisodeCalls = Collections.synchronizedList(mutableListOf<Int>())
    private val recordedDetailCalls = Collections.synchronizedList(mutableListOf<Int>())

    public val episodeCalls: List<Int> get() = recordedEpisodeCalls.toList()
    public val detailCalls: List<Int> get() = recordedDetailCalls.toList()

    public var episodesHandler: suspend (page: Int) -> DomainResult<Page<Episode>> =
        { DomainResult.Success(Page.empty()) }

    public var detailHandler: suspend (id: Int) -> DomainResult<EpisodeDetail> =
        { DomainResult.Success(TestData.episodeDetail(episode = TestData.episode(id = it))) }

    override suspend fun episodes(page: Int): DomainResult<Page<Episode>> {
        recordedEpisodeCalls += page
        return episodesHandler(page)
    }

    override suspend fun detail(id: Int): DomainResult<EpisodeDetail> {
        recordedDetailCalls += id
        return detailHandler(id)
    }
}
