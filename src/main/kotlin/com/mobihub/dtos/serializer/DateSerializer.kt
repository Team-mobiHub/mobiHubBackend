package com.mobihub.dtos.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Serializer for [Instant].
 *
 * This class is used to serialize and deserialize the [Instant] object.
 *
 * @property formatter The DateTimeFormatter object
 * @property descriptor The SerialDescriptor object
 *
 * @author Team-Mobihub
 */
object InstantSerializer : KSerializer<Instant> {
    private val formatter = DateTimeFormatter.ISO_INSTANT

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    /**
     * Serialize the [Instant] to [String].
     *
     * @param encoder The encoder to serialize the [Instant] object
     * @param value The [Instant] object
     */
    override fun serialize(encoder: Encoder, value: Instant) {
        val dateString = formatter.format(value)
        encoder.encodeString(dateString)
    }

    /**
     * Deserialize the string to [Instant].
     *
     * @param decoder The decoder
     * @return Instant The [Instant] object
     */
    override fun deserialize(decoder: Decoder): Instant {
        val dateString = decoder.decodeString()
        return Instant.parse(dateString)
    }
}