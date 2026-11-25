package com.roldanmatias.rickverse.data.rest.mapper

import com.roldanmatias.rickverse.core.domain.model.CastMember
import com.roldanmatias.rickverse.data.rest.dto.CharacterDto
import com.roldanmatias.rickverse.data.rest.dto.PageDto
import com.roldanmatias.rickverse.data.rest.fixture
import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterMapperTest {
    @Test
    fun `maps a real character fixture`() {
        val member = fixture<CharacterDto>("character_1.json").toDomain()

        assertEquals(
            CastMember(
                id = 1,
                name = "Rick Sanchez",
                status = CastMember.Status.Alive,
                species = "Human",
                imageUrl = "https://rickandmortyapi.com/api/character/avatar/1.jpeg",
            ),
            member,
        )
    }

    @Test
    fun `maps a whole real page`() {
        val page = fixture<PageDto<CharacterDto>>("characters_page1.json").toPage { it.toDomain() }

        assertEquals(20, page.items.size)
        assertEquals(2, page.nextPage)
    }

    @Test
    fun `unknown status string becomes Unknown instead of crashing`() {
        val zombie = fixture<CharacterDto>("character_1.json").copy(status = "Zombie")

        assertEquals(CastMember.Status.Unknown, zombie.toDomain().status)
    }

    @Test
    fun `status matching ignores case`() {
        assertEquals(CastMember.Status.Dead, "dead".toStatus())
        assertEquals(CastMember.Status.Unknown, "unknown".toStatus())
    }

    @Test
    fun `detail carries gender, origin, location and the given episodes`() {
        val detail = fixture<CharacterDto>("character_1.json").toDetail(episodes = emptyList())

        assertEquals("Male", detail.gender)
        assertEquals("Earth (C-137)", detail.originName)
        assertEquals("Citadel of Ricks", detail.locationName)
    }

    @Test
    fun `extracts episode ids from the episode urls`() {
        val ids = fixture<CharacterDto>("character_1.json").episodeIds()

        assertEquals(51, ids.size)
        assertEquals(listOf(1, 2, 3), ids.take(3))
    }
}
