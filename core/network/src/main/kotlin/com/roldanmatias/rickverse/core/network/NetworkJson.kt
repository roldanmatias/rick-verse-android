package com.roldanmatias.rickverse.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.time.Instant

/** The shared [Json] configuration. Each setting is a decision worth being able to explain. */
public object NetworkJson {
    public val default: Json =
        Json {
            // The backend may add fields at any time; a new field must never break an old app.
            ignoreUnknownKeys = true
            // A null/unknown enum value falls back to the property default instead of failing the whole page.
            coerceInputValues = true
            // Do not write null fields (smaller payloads); missing and null are treated alike when reading.
            explicitNulls = false
        }
}

/**
 * ISO-8601 instants such as `2017-11-04T18:48:46.250Z` (the API's `created`, with fractional seconds).
 * `Instant.parse` accepts both with and without a fraction, so one serializer covers every shape.
 */
public object InstantIso8601Serializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }
}
