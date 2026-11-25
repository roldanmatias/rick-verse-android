package com.roldanmatias.rickverse.data.rest

import com.roldanmatias.rickverse.core.testing.Fixtures
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.RecordedRequest
import java.util.concurrent.atomic.AtomicInteger

/**
 * A MockWebServer dispatcher that behaves like the real API using **real captured fixtures**:
 * `/character/{id}`, `/episode/{id}` and the batch forms with comma-separated ids, including the single-id quirk
 * (an object instead of an array). Tracks the maximum number of requests in flight.
 */
internal class FakeBackend(
    private val latencyMillis: Long = 0,
) : Dispatcher() {
    private val episodes: Map<Int, JsonObject> =
        Json
            .parseToJsonElement(Fixtures.load("episodes_1_to_51.json"))
            .jsonArray
            .map { it.jsonObject }
            .associateBy { it.getValue("id").jsonPrimitive.int }
    private val characters: Map<Int, JsonObject> =
        Json
            .parseToJsonElement(Fixtures.load("characters_of_episode_1.json"))
            .jsonArray
            .map { it.jsonObject }
            .associateBy { it.getValue("id").jsonPrimitive.int }
    private val rick = Fixtures.load("character_1.json")
    private val inFlight = AtomicInteger()
    private val maxInFlight = AtomicInteger()

    val peakConcurrency: Int get() = maxInFlight.get()

    override fun dispatch(request: RecordedRequest): MockResponse {
        val current = inFlight.incrementAndGet()
        maxInFlight.updateAndGet { maxOf(it, current) }
        try {
            if (latencyMillis > 0) Thread.sleep(latencyMillis)
            return respond(request.url.encodedPath)
        } finally {
            inFlight.decrementAndGet()
        }
    }

    private fun respond(path: String): MockResponse {
        val segments = path.removePrefix("/api/").split('/')
        val kind = segments[0]
        val idPart = segments.getOrNull(1)
        val ids = idPart?.split(',')?.mapNotNull { it.toIntOrNull() }.orEmpty()
        return when {
            kind == "character" && idPart == "1" -> {
                json(rick)
            }

            kind == "character" && ids.isNotEmpty() -> {
                batch(ids, characters)
            }

            kind == "episode" && ids.isNotEmpty() -> {
                batch(ids, episodes)
            }

            else -> {
                MockResponse
                    .Builder()
                    .code(404)
                    .body("""{"error":"There is nothing here"}""")
                    .build()
            }
        }
    }

    /** One id -> a bare object, several -> an array: exactly like the real API. */
    private fun batch(ids: List<Int>, source: Map<Int, JsonObject>): MockResponse {
        val found = ids.mapNotNull(source::get)
        if (found.isEmpty()) {
            return MockResponse
                .Builder()
                .code(
                    404,
                ).body("""{"error":"There is nothing here"}""")
                .build()
        }
        return json(if (ids.size == 1) found.single().toString() else JsonArray(found).toString())
    }

    private fun json(body: String) =
        MockResponse
            .Builder()
            .code(200)
            .addHeader("Content-Type", "application/json")
            .body(body)
            .build()
}
