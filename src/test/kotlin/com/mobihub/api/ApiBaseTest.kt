package com.mobihub.api

import com.mobihub.dtos.serializer.InstantSerializer
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.time.Instant

/**
 * Base class for API tests.
 * Provides a method to test the application with a specific configuration.
 *
 * @constructor Creates a new instance of [ApiBaseTest].
 */
open class ApiBaseTest {
    protected val serializer = Json {
        serializersModule = SerializersModule { contextual(Instant::class, InstantSerializer) }
    }

    protected fun testApplicationWithConfig(test: suspend ApplicationTestBuilder.() -> Unit) {
        testApplication {
            environment {
                config = ApplicationConfig("application-test.yaml")
            }
            test()
        }
    }
}