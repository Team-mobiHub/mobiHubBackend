package com.mobihub

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        val existingConfig = ApplicationConfig("application-test.yaml")

        environment {
            config = existingConfig // Use the existing config
        }

        client.get("/healthCheck").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
}
