package com.roldanmatias.rickverse.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class InstantIso8601SerializerTest {
    @Serializable
    private data class Holder(
        @Serializable(with = InstantIso8601Serializer::class) val created: Instant,
    )

    @Test
    fun `parses fractional seconds as the API sends them`() {
        val holder = NetworkJson.default.decodeFromString<Holder>("""{"created":"2017-11-04T18:48:46.250Z"}""")

        assertEquals(Instant.parse("2017-11-04T18:48:46.250Z"), holder.created)
    }

    @Test
    fun `parses whole seconds too`() {
        val holder = NetworkJson.default.decodeFromString<Holder>("""{"created":"2017-11-04T18:48:46Z"}""")

        assertEquals(Instant.parse("2017-11-04T18:48:46Z"), holder.created)
    }
}
