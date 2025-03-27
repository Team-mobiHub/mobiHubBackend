package com.mobihub.api

import com.mobihub.dtos.AuthResponseDTO
import com.mobihub.dtos.UserDTO
import com.mobihub.dtos.serializer.InstantSerializer
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.utils.EmptyContent.contentType
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.serializersModuleOf
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.Instant
import kotlin.test.*

/**
 * Tests for the User API.
 *
 * These tests are run using the Ktor test engine.
 */
class UserApiTest : ApiBaseTest() {

    @BeforeTest
    fun setup() {
        DataBaseHelper.generateSampleData()
    }

    // region Register tests
    @Test
    fun registerValidUserTest() = testApplicationWithConfig {
        client.post("/user") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"testNew","email":"testNew@example.com","password":"a1B2c3D4e5#"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val actualResponse: UserDTO = assertDoesNotThrow { serializer.decodeFromString<UserDTO>(bodyAsText()) }
            assertNotNull(actualResponse)
            actualResponse.apply {
                assertEquals("testNew", name)
                assertEquals("testNew@example.com", email)
                assertNotNull(id)
            }
        }
    }

    // endregion

    // region update user tests
    @Test
    fun updateValidUserTest() = testApplicationWithConfig {
        val token = LoginHelper.login(1)

        client.put("/user") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody(
                serializer.encodeToString(
                    UserDTO(
                        1, "testUpdate", "testUpdate@example.com", null, "", true, false, emptyList()
                    )
                )
            )
        }.apply {
            val actualResponse: UserDTO = assertDoesNotThrow { serializer.decodeFromString<UserDTO>(bodyAsText()) }
            assertNotNull(actualResponse)
            actualResponse.apply {
                assertEquals("testUpdate", name)
                assertEquals("testUpdate@example.com", email)
            }
        }
    }

    // endregion

    // region get user test
    @Test
    fun getUserByValidIdTest() = testApplicationWithConfig {
        client.get("/user/1") {
            contentType(ContentType.Application.Json)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val actualResponse: UserDTO = assertDoesNotThrow { serializer.decodeFromString<UserDTO>(bodyAsText()) }
            assertNotNull(actualResponse)
            actualResponse.apply {
                assertEquals("captain_crunch", name)
            }
        }
    }
    // endregion

    // region change password tests
    @Test
    fun changePasswordValidTests() = testApplicationWithConfig {
        val token = LoginHelper.login(1)

        client.put("/auth/change-password") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""{"oldPassword":"Password?","newPassword":"a1B2c3D4e5#"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun changePasswordNotLoggedInTest() = testApplicationWithConfig {
        client.put("/auth/change-password") {
            contentType(ContentType.Application.Json)
            setBody("""{"oldPassword":"Password?","newPassword":"a1B2c3D4e5#"}""")
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun changePasswordInvalidOldPasswordTest() = testApplicationWithConfig {
        val token = LoginHelper.login(1)

        client.put("/auth/change-password") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
            setBody("""{"oldPassword":"invalid","newPassword":"12345"}""")
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }
    // endregion

    // region delete user tests
    @Test
    fun deleteUserValidTests() = testApplicationWithConfig {
        val token = LoginHelper.login(1)

        client.delete("/user/1") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun deleteNotLoggedInTest() = testApplicationWithConfig {
        client.delete("/user/1") {
            contentType(ContentType.Application.Json)
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }
    // endregion

    // region Login tests
    @Test
    fun loginValidUserTest() = testApplicationWithConfig {
        client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user1@example.com","password":"Password?"}""")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
            val actualResponse: AuthResponseDTO =
                assertDoesNotThrow { serializer.decodeFromString<AuthResponseDTO>(bodyAsText()) }
            assertNotNull(actualResponse)
            actualResponse.apply {
                assertNotNull(token)
                assertNotNull(expiresAt)
                assert(expiresAt.isAfter(Instant.now()))
            }
        }
    }

    @Test
    fun loginNotExistingUser() = testApplicationWithConfig {
        client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"notExisting@example.com","password":"Password?"}""")
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun loginInvalidPassword() = testApplicationWithConfig {
        client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user1@example.com","password":"invalid"}""")
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    // endregion

    // region Logout tests
    @Test
    fun logoutUserValidTest() = testApplicationWithConfig {
        val token = LoginHelper.login(1)

        client.post("/auth/logout") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        // Check if the token is invalid
        client.post("/favorite/10") {
            header("Authorization", "Bearer $token")
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }

    @Test
    fun logoutNotLoggedInTest() = testApplicationWithConfig {
        client.post("/auth/logout") {
            contentType(ContentType.Application.Json)
        }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }
    // endregion

    // region reset password by email tests
    // Can not be tested here because it sends an email
    // endregion

    // region get user by email
    @Test
    fun getUserByValidEmailTest() = testApplicationWithConfig {
        val token = LoginHelper.login(1)

        client.get("/user/email/user1@example.com") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $token")
        }.apply {
            val actualResponse: UserDTO = assertDoesNotThrow { serializer.decodeFromString<UserDTO>(bodyAsText()) }
            assertNotNull(actualResponse)
            actualResponse.apply {
                assertEquals("captain_crunch", name)
            }
        }
    }
    // endregion
}
