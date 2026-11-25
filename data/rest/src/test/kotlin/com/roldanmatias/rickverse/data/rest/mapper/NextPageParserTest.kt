package com.roldanmatias.rickverse.data.rest.mapper

import com.roldanmatias.rickverse.data.rest.dto.CharacterDto
import com.roldanmatias.rickverse.data.rest.dto.PageDto
import com.roldanmatias.rickverse.data.rest.fixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextPageParserTest {
    @Test
    fun `extracts the page query parameter`() {
        assertEquals(2, parseNextPage("https://rickandmortyapi.com/api/character?page=2"))
        assertEquals(7, parseNextPage("https://rickandmortyapi.com/api/character?name=rick&page=7&status=alive"))
    }

    @Test
    fun `null and garbage give null`() {
        assertNull(parseNextPage(null))
        assertNull(parseNextPage("not a url"))
        assertNull(parseNextPage("https://rickandmortyapi.com/api/character"))
    }

    @Test
    fun `last real page has no next page`() {
        val page = fixture<PageDto<CharacterDto>>("characters_page_last.json").toPage { it.toDomain() }

        assertNull(page.nextPage)
    }

    @Test
    fun `trailing id`() {
        assertEquals(28, "https://rickandmortyapi.com/api/episode/28".trailingId())
        assertNull("https://rickandmortyapi.com/api/episode/".trailingId())
    }
}
