package com.roldanmatias.rickverse.data.rest.dto

import com.roldanmatias.rickverse.data.rest.fixture
import org.junit.Assert.assertEquals
import org.junit.Test

class OneOrManyTest {
    @Test
    fun `decodes an array`() {
        assertEquals(3, fixture<OneOrMany<EpisodeDto>>("episodes_1_2_3.json").items.size)
    }

    @Test
    fun `decodes a lone object as a list of one`() {
        val items = fixture<OneOrMany<EpisodeDto>>("episode_1.json").items

        assertEquals(1, items.size)
        assertEquals("Pilot", items.single().name)
    }

    @Test
    fun `works for characters too`() {
        assertEquals(2, fixture<OneOrMany<CharacterDto>>("characters_1_2.json").items.size)
        assertEquals(1, fixture<OneOrMany<CharacterDto>>("character_1.json").items.size)
    }
}
