package com.roldanmatias.rickverse.core.testing

import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.core.domain.model.CastMemberDetail
import com.roldanmatias.rickverse.core.domain.model.Episode
import com.roldanmatias.rickverse.core.domain.model.EpisodeDetail
import java.time.LocalDate

/** Builders with sensible defaults so a test only spells out what it cares about. */
public object TestData {
    public fun castMember(
        id: Int = 1,
        name: String = "Rick Sanchez",
        status: CastMember.Status = CastMember.Status.Alive,
        species: String = "Human",
        imageUrl: String = "https://rickandmortyapi.com/api/character/avatar/$id.jpeg",
    ): CastMember = CastMember(id, name, status, species, imageUrl)

    public fun episode(
        id: Int = 1,
        name: String = "Pilot",
        season: Int = 1,
        number: Int = id,
        airDate: LocalDate? = LocalDate.of(2013, 12, 2),
    ): Episode =
        Episode(
            id = id,
            name = name,
            code = "S%02dE%02d".format(season, number),
            season = season,
            number = number,
            airDate = airDate,
        )

    public fun castMemberDetail(
        member: CastMember = castMember(),
        episodes: List<Episode> = listOf(episode()),
    ): CastMemberDetail =
        CastMemberDetail(
            member = member,
            gender = "Male",
            originName = "Earth (C-137)",
            locationName = "Citadel of Ricks",
            episodes = episodes,
        )

    public fun episodeDetail(
        episode: Episode = episode(),
        cast: List<CastMember> = listOf(castMember()),
    ): EpisodeDetail = EpisodeDetail(episode, cast)

    /** `count` distinct members with ids `from..from+count-1`. */
    public fun castMembers(count: Int, from: Int = 1): List<CastMember> =
        (from until from + count).map { castMember(id = it, name = "Character $it") }
}
