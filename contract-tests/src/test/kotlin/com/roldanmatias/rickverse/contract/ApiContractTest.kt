package com.roldanmatias.rickverse.contract

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The assumptions our DTOs and quirk handling rely on, checked against the real API (nightly CI, never on PRs).
 * If one fails, the app's mapping is about to break: fix the DTO or the quirk handling, not the test.
 */
internal class ApiContractTest : LiveApiTest("RUN_CONTRACT_TESTS") {
    private val client = liveClient()

    @Test
    fun `character page has the fields the DTO decodes`() {
        val root = client.get("$REST/character?page=1").json().jsonObject

        assertTrue(
            "info.next must be a URL string",
            root.getValue("info").jsonObject.getValue("next").jsonPrimitive.content.contains("page=2"),
        )
        val first = root.getValue("results").jsonArray.first().jsonObject
        listOf("id", "name", "status", "species", "image", "gender", "origin", "location", "episode").forEach {
            assertTrue("character.$it is missing", it in first)
        }
        assertTrue(first.getValue("episode").jsonArray.first().jsonPrimitive.content.contains("/episode/"))
    }

    @Test
    fun `a search with no match is still HTTP 404`() {
        client.get("$REST/character/?name=zzzzzzzz").use { assertEquals(404, it.code) }
    }

    @Test
    fun `batch endpoint returns an object for one id and an array for several`() {
        assertTrue(client.get("$REST/character/1").json() is JsonObject)
        assertTrue(client.get("$REST/character/1,2").json() is JsonArray)
        assertTrue(client.get("$REST/episode/1").json() is JsonObject)
        assertTrue(client.get("$REST/episode/1,2,3").json() is JsonArray)
    }

    @Test
    fun `episode has code and air date in the formats the mapper parses`() {
        val episode = client.get("$REST/episode/1").json().jsonObject
        assertTrue(Regex("""S\d+E\d+""").matches(episode.getValue("episode").jsonPrimitive.content))
        assertTrue(Regex("""[A-Z][a-z]+ \d{1,2}, \d{4}""").matches(episode.getValue("air_date").jsonPrimitive.content))
    }

    @Test
    fun `GraphQL CastPage query answers with string ids and an integer next page`() {
        val query = "query(\$page:Int){characters(page:\$page){info{next} results{id name status species image}}}"
        val data = client.graphql(query, """{"page":1}""").json().jsonObject.getValue("data").jsonObject
        val characters = data.getValue("characters").jsonObject

        assertEquals("2", characters.getValue("info").jsonObject.getValue("next").jsonPrimitive.content)
        val id = characters.getValue("results").jsonArray.first().jsonObject.getValue("id").jsonPrimitive
        assertTrue("GraphQL ids are strings", id.isString)
    }

    @Test
    fun `GraphQL answers an unknown character with null data, not an HTTP error`() {
        val response = client.graphql("{character(id:\"99999\"){id}}")
        assertEquals(200, response.code)
        assertTrue(response.json().jsonObject.getValue("data").jsonObject.getValue("character").toString() == "null")
    }
}
