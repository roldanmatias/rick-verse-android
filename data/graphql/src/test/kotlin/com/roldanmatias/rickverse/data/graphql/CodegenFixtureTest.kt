package com.roldanmatias.rickverse.data.graphql

import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.parseResponse
import com.roldanmatias.rickverse.core.testing.Fixtures
import com.roldanmatias.rickverse.data.graphql.generated.CastMemberDetailQuery
import com.roldanmatias.rickverse.data.graphql.generated.CastPageQuery
import com.roldanmatias.rickverse.data.graphql.generated.EpisodeDetailQuery
import com.roldanmatias.rickverse.data.graphql.generated.EpisodePageQuery
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Proves codegen + serialization end to end: real captured responses parse into the generated `Data` classes. */
class CodegenFixtureTest {
    @Test
    fun `cast page response parses into CastPageQuery Data`() {
        val response = CastPageQuery(page = Optional.present(1)).parseResponse(reader("cast_page1.json"))

        assertNull(response.errors)
        val characters = response.dataOrThrow().characters!!
        assertEquals(2, characters.info?.next)
        assertEquals(20, characters.results?.size)
        val rick = characters.results!!.first()!!.castMemberBasic
        assertEquals("1", rick.id) // ID stays a String; mappers convert
        assertEquals("Rick Sanchez", rick.name)
    }

    @Test
    fun `character detail carries the episodes in the same single response`() {
        val response = CastMemberDetailQuery(id = "1").parseResponse(reader("cast_detail_1.json"))

        val character = response.dataOrThrow().character!!
        assertEquals("Earth (C-137)", character.origin?.name)
        assertEquals(51, character.episode.size)
        assertEquals("S01E01", character.episode.first()!!.episodeBasic.episode)
    }

    @Test
    fun `episode detail and pages parse`() {
        val detail = EpisodeDetailQuery(id = "1").parseResponse(reader("episode_detail_1.json"))
        assertEquals(19, detail.dataOrThrow().episode!!.characters.size)

        val page = EpisodePageQuery(page = Optional.present(1)).parseResponse(reader("episode_page1.json"))
        assertNotNull(page.dataOrThrow().episodes!!.results)
        assertEquals(2, page.dataOrThrow().episodes!!.info?.next)
    }

    private fun reader(name: String) = Buffer().writeUtf8(Fixtures.load(name)).jsonReader()
}
