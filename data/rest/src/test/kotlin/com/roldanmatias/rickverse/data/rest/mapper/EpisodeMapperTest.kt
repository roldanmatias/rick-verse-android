package com.roldanmatias.rickverse.data.rest.mapper

import com.roldanmatias.rickverse.data.rest.dto.EpisodeDto
import com.roldanmatias.rickverse.data.rest.dto.OneOrMany
import com.roldanmatias.rickverse.data.rest.fixture
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class EpisodeMapperTest {
    private val original = Locale.getDefault()

    @Before
    fun spanishDevice() {
        // The guide's quirk: a device set to Spanish must still parse "December".
        Locale.setDefault(Locale.forLanguageTag("es-AR"))
    }

    @After
    fun restore() = Locale.setDefault(original)

    @Test
    fun `maps a real episode fixture and parses the English air date on a Spanish device`() {
        val episode = fixture<EpisodeDto>("episode_1.json").toDomain()

        assertEquals("Pilot", episode.name)
        assertEquals("S01E01", episode.code)
        assertEquals(1, episode.season)
        assertEquals(1, episode.number)
        assertEquals(LocalDate.of(2013, 12, 2), episode.airDate)
    }

    @Test
    fun `parses season and number from the code`() {
        val episode = fixture<EpisodeDto>("episode_1.json").copy(episode = "S02E10").toDomain()

        assertEquals(2, episode.season)
        assertEquals(10, episode.number)
    }

    @Test
    fun `an unparsable air date becomes null, not a failure`() {
        val episode = fixture<EpisodeDto>("episode_1.json").copy(airDate = "someday").toDomain()

        assertNull(episode.airDate)
    }

    @Test
    fun `a malformed code is a mapping failure`() {
        assertThrows(MappingException::class.java) {
            fixture<EpisodeDto>("episode_1.json").copy(episode = "pilot").toDomain()
        }
    }

    @Test
    fun `batch of three preserves order`() {
        val episodes = fixture<OneOrMany<EpisodeDto>>("episodes_1_2_3.json").items.map { it.toDomain() }

        assertEquals(listOf(1, 2, 3), episodes.map { it.id })
    }

    @Test
    fun `extracts character ids from the episode`() {
        val ids = fixture<EpisodeDto>("episode_1.json").characterIds()

        assertEquals(listOf(1, 2, 35), ids.take(3))
    }
}
