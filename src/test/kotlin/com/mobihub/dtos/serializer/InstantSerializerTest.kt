package com.mobihub.dtos.serializer

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test class for the `InstantSerializer`.
 * This class contains unit tests to verify the serialization and deserialization of `Instant` objects using the `InstantSerializer`.
 */
class InstantSerializerTest {

    /**
     * A configured instance of `Json` with a `SerializersModule` that includes the `InstantSerializer`.
     * This is used to serialize and deserialize `Instant` objects in the tests.
     */
    private val json = Json {
        serializersModule = SerializersModule {
            contextual(InstantSerializer)
        }
    }

    /**
     * Tests the serialization of an `Instant` object to a JSON string.
     * Verifies that the serialized JSON string matches the expected format.
     */
    @Test
    fun testSerialize() {
        val instant = Instant.parse("2023-10-31T10:30:00Z")
        val jsonString = json.encodeToString(InstantSerializer, instant)
        assertEquals("\"2023-10-31T10:30:00Z\"", jsonString)
    }

    /**
     * Tests the deserialization of a JSON string to an `Instant` object.
     * Verifies that the deserialized `Instant` matches the expected value.
     */
    @Test
    fun testDeserialize() {
        val jsonString = "\"2023-10-31T10:30:00Z\""
        val instant = json.decodeFromString(InstantSerializer, jsonString)
        assertEquals(Instant.parse("2023-10-31T10:30:00Z"), instant)
    }
}