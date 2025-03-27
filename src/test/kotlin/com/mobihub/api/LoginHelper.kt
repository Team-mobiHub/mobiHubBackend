package com.mobihub.api

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Helper class to log in a user.
 *
 * Author: Team-MobiHub
 */
object LoginHelper {
    /**
     * Logs in a user and returns the token.
     *
     * @return The token.
     */
    fun login(userId: Int): String {
        var token: String? = null

        testApplication {
            val existingConfig = ApplicationConfig("application-test.yaml")

            environment {
                config = existingConfig // Use the existing config
            }

            client.post("/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                        "email": "user${userId}@example.com"
                        "password": "Password?"
                    }
                """.trimIndent()
                )
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                token = bodyAsText().substringAfter("\"token\":\"").substringBefore("\"")
                assertNotNull(token)
                token?.let {
                    assertTrue { it.isNotBlank() }
                }
            }
        }

        return token!!
    }
}
