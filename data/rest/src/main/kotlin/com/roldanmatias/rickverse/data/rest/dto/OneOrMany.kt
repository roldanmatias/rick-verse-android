package com.roldanmatias.rickverse.data.rest.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder

/**
 * The batch endpoints (`/episode/1,2,3`) return an **array**, but with a single id (`/episode/1`) they return one
 * **object**. This wrapper accepts both shapes, so callers never special-case a batch of one.
 */
@Serializable(with = OneOrManySerializer::class)
internal class OneOrMany<T>(
    val items: List<T>,
)

internal class OneOrManySerializer<T>(
    elementSerializer: KSerializer<T>,
) : KSerializer<OneOrMany<T>> {
    private val listSerializer = ListSerializer(elementSerializer)

    override val descriptor: SerialDescriptor = listSerializer.descriptor

    override fun deserialize(decoder: Decoder): OneOrMany<T> {
        val jsonDecoder = decoder as? JsonDecoder ?: error("OneOrMany can only be decoded from JSON")
        val element = jsonDecoder.decodeJsonElement()
        val array = element as? JsonArray ?: JsonArray(listOf(element))
        return OneOrMany(jsonDecoder.json.decodeFromJsonElement(listSerializer, array))
    }

    override fun serialize(encoder: Encoder, value: OneOrMany<T>) {
        encoder.encodeSerializableValue(listSerializer, value.items)
    }
}
