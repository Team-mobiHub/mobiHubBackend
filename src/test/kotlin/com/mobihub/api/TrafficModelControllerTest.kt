package com.mobihub.api

import com.mobihub.dtos.CreateTrafficModelResponseDTO
import com.mobihub.dtos.SearchResultDTO
import com.mobihub.dtos.TrafficModelDTO
import com.mobihub.model.Framework
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.File
import kotlin.test.*

/**
 * API Tests for the TrafficModelController.
 *
 * @author Team-MobiHub
 */
class TrafficModelControllerTest : ApiBaseTest() {

    @BeforeTest
    fun setUp() {
        DataBaseHelper.generateSampleData()
    }


    @Test
    fun testCreateTrafficModel() = testApplicationWithConfig {
        val modelId = 12
        val userId = 1

        val body = """
            {
                "id": null,
                "name": "test",
                "description": "A detailed simulation of urban traffic flow.",
                "ownerUserId": 1,
                "ownerTeamId": null,
                "isVisibilityPublic": true,
                "dataSourceUrl": "https://example.com/traffic-data",
                "characteristics": [],
                "framework": "PTV_VISSIM",
                "region": "Berlin",
                "coordinates": "52.5200,13.4050",
                "hasZipFileChanged": true,
                "changedImages": [{"fileName": "tmb1.png", "status": "ADDED"}, {"fileName": "tmb2.png", "status": "ADDED"}]
            }
        """.trimIndent()

        val createResponse: CreateTrafficModelResponseDTO
        val loginToken = LoginHelper.login(userId)

        client.post("/trafficModel") {
            contentType(ContentType.Application.Json)
            setBody(body)
            headers {
                append(HttpHeaders.Authorization, "Bearer $loginToken")
            }
        }.apply {
            assertEquals(HttpStatusCode.OK, status)

            createResponse =
                assertDoesNotThrow { Json.decodeFromString<CreateTrafficModelResponseDTO>(bodyAsText()) }
            assertNotNull(createResponse)
            assertEquals(modelId, createResponse.id)
            assertNotNull(createResponse.zipFileToken)
            assertTrue { createResponse.zipFileToken.isNotBlank() }
            assertTrue { createResponse.imageTokens.isNotEmpty() }
            assertTrue { createResponse.imageTokens.all { it.isNotBlank() } }
            assertEquals(2, createResponse.imageTokens.size)
        }

        client.post("/trafficModel/$modelId/uploadZip/${createResponse.zipFileToken}") {
            setBody(MultiPartFormDataContent(formData {
                append(
                    "file",
                    File("src/test/resources/testfiles/mobihub-zip-file.zip").readBytes(),
                    Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"file.zip\"")
                    })
            }))
            headers {
                append(HttpHeaders.Authorization, "Bearer $loginToken")
            }
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
        }

        for (imageToken in createResponse.imageTokens) {
            client.post("/trafficModel/$modelId/uploadImage/${imageToken}") {
                setBody(MultiPartFormDataContent(formData {
                    append(
                        "file",
                        File(
                            "src/test/resources/testfiles/mobiHubImages/tmb${
                                createResponse.imageTokens.indexOf(
                                    imageToken
                                ) + 1
                            }.png"
                        ).readBytes(),
                        Headers.build {
                            append(
                                HttpHeaders.ContentDisposition,
                                "filename=\"tmb${createResponse.imageTokens.indexOf(imageToken) + 1}.png\""
                            )
                        })
                }))
                headers {
                    append(HttpHeaders.Authorization, "Bearer $loginToken")
                }
            }.apply {
                assertEquals(HttpStatusCode.Created, status)
            }
        }

        val updateBody = """
            {
                "id": $modelId,
                "name": "Updated Model $modelId",
                "description": "This model has been updated",
                "ownerUserId": $userId,
                "ownerTeamId": null,
                "isVisibilityPublic": true,
                "dataSourceUrl": "https://example.com/traffic-data",
                "characteristics": [],
                "framework": "PTV_VISSIM",
                "region": "Berlin",
                "coordinates": "52.5200,13.4050",
                "hasZipFileChanged": true,
                "changedImages": [{"fileName": "tmb3.png", "status": "ADDED"}, {"fileName": "${createResponse.imageTokens[1]}.png", "status": "REMOVED"}]
            }
        """.trimIndent()

        var trafficModelToUpdate: TrafficModelDTO?

        client.get("/trafficModel/$modelId").apply {
            assertEquals(HttpStatusCode.OK, status)
            trafficModelToUpdate = serializer.decodeFromString(bodyAsText())
        }

        val updateResponse: CreateTrafficModelResponseDTO

        client.put("/trafficModel/$modelId") {
            contentType(ContentType.Application.Json)
            setBody(updateBody)
            headers {
                append(HttpHeaders.Authorization, "Bearer $loginToken")
            }
        }.apply {
            assertEquals(HttpStatusCode.OK, status)

            updateResponse = assertDoesNotThrow {
                serializer.decodeFromString<CreateTrafficModelResponseDTO>(bodyAsText())
            }
            assertNotNull(updateResponse)
            assertEquals(updateResponse.id, modelId)
            assertNotNull(updateResponse.zipFileToken)
            assertNotEquals(trafficModelToUpdate!!.zipFileToken, updateResponse.zipFileToken)
            assertTrue { updateResponse.isVisibilityPublic }

            assertTrue { updateResponse.imageTokens.isNotEmpty() }
            assertTrue { updateResponse.imageTokens.all { it.isNotBlank() } }
            assertEquals(1, updateResponse.imageTokens.size)
        }

        client.post("/trafficModel/$modelId/uploadZip/${updateResponse.zipFileToken}") {
            setBody(MultiPartFormDataContent(formData {
                append(
                    "file",
                    File("src/test/resources/testfiles/mobihub-changed-zip-file.zip").readBytes(),
                    Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"changedFile.zip\"")
                    })
            }))
            headers {
                append(HttpHeaders.Authorization, "Bearer $loginToken")
            }
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
        }

        client.post("/trafficModel/$modelId/uploadImage/${updateResponse.imageTokens[0]}") {
            setBody(MultiPartFormDataContent(formData {
                append(
                    "file",
                    File(
                        "src/test/resources/testfiles/mobiHubImages/tmb3.png"
                    ).readBytes(),
                    Headers.build {
                        append(
                            HttpHeaders.ContentDisposition,
                            "filename=\"tmb3.png\""
                        )
                    })
            }))
            headers {
                append(HttpHeaders.Authorization, "Bearer $loginToken")
            }
        }.apply {
            assertEquals(HttpStatusCode.Created, status)
        }

        client.get("trafficModel/${modelId}").apply {
            assertEquals(HttpStatusCode.OK, status)
            val updatedModel = assertDoesNotThrow { serializer.decodeFromString<TrafficModelDTO>(bodyAsText()) }
            assertNotNull(updatedModel)
            assertEquals(modelId, updatedModel.id)
            assertEquals("Updated Model $modelId", updatedModel.name)
            assertEquals("This model has been updated", updatedModel.description)
            assertEquals("https://example.com/traffic-data", updatedModel.dataSourceUrl)
            assertEquals(Framework.PTV_VISSIM, updatedModel.framework)
            assertEquals("Berlin", updatedModel.region)
            assertEquals("52.5200,13.4050", updatedModel.coordinates)
            assertTrue { updatedModel.isVisibilityPublic }

            assertTrue { updatedModel.imageURLs.isNotEmpty() }
            assertTrue { updatedModel.imageURLs.all { it.isNotBlank() } }
            assertEquals(2, updatedModel.imageURLs.size)
        }
    }

    @Test
    fun getTrafficModelByIdValidId() = testApplicationWithConfig {
        val modelId = 4

        client.get("/trafficModel/$modelId").apply {
            assertEquals(HttpStatusCode.OK, status)

            val actualResponse = assertDoesNotThrow { serializer.decodeFromString<TrafficModelDTO>(bodyAsText()) }

            assertNotNull(actualResponse)
            assertEquals(modelId, actualResponse.id)
        }
    }

    @Test
    fun getTrafficModelByIdInvalidId() = testApplicationWithConfig {
        val modelId = 999

        client.get("/trafficModel/$modelId").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun getForOwnerNotLoggedIn() = testApplicationWithConfig {

        val ownerType = "USER"
        val ownerId = 2

        client.get("/trafficModel/$ownerType/$ownerId").apply {
            assertEquals(HttpStatusCode.OK, status)

            val actualResponse =
                assertDoesNotThrow { serializer.decodeFromString<List<TrafficModelDTO>>(bodyAsText()) }

            assertNotNull(actualResponse)
            assertTrue { actualResponse.isNotEmpty() }

            println(actualResponse.map { it.id })

            assertTrue { actualResponse.map { it.id }.containsAll(listOf(8, 11)) }
            assertEquals(2, actualResponse.size)
        }
    }

    @Test
    fun getForOwnerInvalidOwnerType() = testApplicationWithConfig {

        val ownerType = "INVALID"
        val ownerId = 2

        client.get("/trafficModel/$ownerType/$ownerId").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun getForOwnerLoggedIn() = testApplicationWithConfig {

        val ownerType = "USER"
        val ownerId = 2

        client.get("/trafficModel/$ownerType/$ownerId") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${LoginHelper.login(2)}")
            }
        }.apply {
            assertEquals(HttpStatusCode.OK, status)

            val actualResponse =
                assertDoesNotThrow { serializer.decodeFromString<List<TrafficModelDTO>>(bodyAsText()) }

            assertNotNull(actualResponse)
            assertTrue { actualResponse.isNotEmpty() }

            println(actualResponse.map { it.id })

            assertTrue { actualResponse.map { it.id }.containsAll(listOf(2, 8, 10, 11)) }
            assertEquals(4, actualResponse.size)
        }
    }

    @Test
    fun updateTrafficModel() = testApplicationWithConfig {

        val modelId = 1
        val userId = 1

        val body = """
            {
                "id": $modelId,
                "name": "Updated Model $modelId",
                "description": "This model has been updated",
                "ownerUserId": $userId,
                "ownerTeamId": null,
                "isVisibilityPublic": true,
                "dataSourceUrl": "https://example.com/traffic-data",
                "characteristics": [],
                "framework": "PTV_VISSIM",
                "region": "Berlin",
                "coordinates": "52.5200,13.4050",
                "hasZipFileChanged": false,
                "changedImages": []
            }
        """.trimIndent()

        var trafficModelToUpdate: TrafficModelDTO?

        client.get("/trafficModel/$modelId").apply {
            assertEquals(HttpStatusCode.OK, status)
            trafficModelToUpdate = serializer.decodeFromString(bodyAsText())
        }

        client.put("/trafficModel/$modelId") {
            contentType(ContentType.Application.Json)
            setBody(body)
            headers {
                append(HttpHeaders.Authorization, "Bearer ${LoginHelper.login(1)}")
            }
        }.apply {
            assertEquals(HttpStatusCode.OK, status)

            val actualResponse: CreateTrafficModelResponseDTO = assertDoesNotThrow {
                serializer.decodeFromString<CreateTrafficModelResponseDTO>(bodyAsText())
            }

            assertNotNull(actualResponse)
            assertEquals(actualResponse.id, modelId)
            assertEquals(actualResponse.zipFileToken, trafficModelToUpdate!!.zipFileToken)
            assertTrue { actualResponse.isVisibilityPublic }
        }
    }

    @Test
    fun searchModelNotExisting() = testApplicationWithConfig {
        val body = """
            {
                "page": 1,
                "pageSize": 20,
                "name": "I do not exist",
                "authorName": null,
                "region": null,
                "modelLevels": [],
                "modelMethods": [],
                "frameworks": []
            }
        """.trimIndent()

        client.post("/trafficModel/search") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)

            val actualResponse = assertDoesNotThrow { serializer.decodeFromString<SearchResultDTO>(bodyAsText()) }

            assertNotNull(actualResponse)
            assertTrue { actualResponse.searchResult.isEmpty() }
            assertEquals(0, actualResponse.totalCount)
        }
    }

    @Test
    fun searchModelExisting() = testApplicationWithConfig {
        val requests = listOf(
            Triple(
                """
            {
                "page": 0,
                "pageSize": 4,
                "name": null,
                "authorName": null,
                "region": null,
                "modelLevels": [],
                "modelMethods": [],
                "frameworks": []
            }
        """.trimIndent(), listOf(1, 3, 4, 5), 8
            ), Triple(
                """
            {
                "page": 0,
                "pageSize": 20,
                "name": null,
                "authorName": null,
                "region": null,
                "modelLevels": [],
                "modelMethods": [],
                "frameworks": []
            }
        """.trimIndent(), listOf(6, 8, 9, 11), 8
            ), Triple(
                """
            {
                "page": 0,
                "pageSize": 20,
                "name": "stAu",
                "authorName": null,
                "region": null,
                "modelLevels": [],
                "modelMethods": [],
                "frameworks": []
            }
            """, listOf(1), 1
            ), Triple(
                """
            {
                "page": 0,
                "pageSize": 20,
                "name": "Stau",
                "authorName": "crunch",
                "region": null,
                "modelLevels": [],
                "modelMethods": [],
                "frameworks": []
            }
        """.trimIndent(), listOf(1), 1
            ), Triple(
                """
            {
                "page": 0,
                "pageSize": 20,
                "name": null,
                "authorName": null,
                "region": "Ber",
                "modelLevels": [],
                "modelMethods": [],
                "frameworks": []
            }
        """.trimIndent(), listOf(1), 1
            ), Triple(
                """
            {
                "page": 0,
                "pageSize": 20,
                "name": null,
                "authorName": null,
                "region": null,
                "modelLevels": ["CHOICE_OF_WORKPLACE"],
                "modelMethods": [],
                "frameworks": []
            }
        """.trimIndent(), listOf(5), 1
            ), Triple(
                """
            {
                "page": 0,
                "pageSize": 20,
                "name": null,
                "authorName": null,
                "region": null,
                "modelLevels": [],
                "modelMethods": ["WIEDEMANN_99"],
                "frameworks": []
            }
        """.trimIndent(), listOf(8), 1
            ), Triple(
                """
            {
                "page": 0,
                "pageSize": 20,
                "name": null,
                "authorName": null,
                "region": null,
                "modelLevels": [],
                "modelMethods": [],
                "frameworks": ["PTV_VISUM"]
            }
        """.trimIndent(), listOf(3, 8), 2
            ), Triple(
                """
            {
                "page": 0,
                "pageSize": 20,
                "name": null,
                "authorName": null,
                "region": null,
                "modelLevels": ["CHOICE_OF_WORKPLACE", "CAR_OWNER"],
                "modelMethods": ["MATRIX_MATCHING","NESTED_LOGIT"],
                "frameworks": []
            }
        """.trimIndent(), listOf(5), 1
            ), Triple(
                """
            {
                "page": 0,
                "pageSize": 20,
                "name": null,
                "authorName": null,
                "region": null,
                "modelLevels": ["CHOICE_OF_WORKPLACE", "CAR_OWNER"],
                "modelMethods": [],
                "frameworks": ["VISUM_VISSIM_HYBRID"]
            }
        """.trimIndent(), listOf(5), 1
            )

        )

        for (request in requests) {
            client.post("/trafficModel/search") {
                contentType(ContentType.Application.Json)
                setBody(request.first)
            }.apply {
                assertEquals(HttpStatusCode.OK, status)

                val actualResponse =
                    assertDoesNotThrow { serializer.decodeFromString<SearchResultDTO>(bodyAsText()) }
                assertNotNull(actualResponse)
                assertTrue { actualResponse.searchResult.isNotEmpty() }
                assertEquals(request.third.toLong(), actualResponse.totalCount)
            }
        }
    }

    @Test
    fun downloadZipFile() = testApplicationWithConfig {
        val modelId = 10

        client.get("/trafficModel/$modelId/download").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertTrue { bodyAsText().isNotBlank() }
            assertTrue { bodyAsText().matches("http://[A-Za-z0-9.-]+:[0-9]+/s/[A-Za-z0-9]+/download/[a-f0-9-]+\\.zip".toRegex()) }
        }
    }

    @Test
    fun deleteTrafficModel() = testApplicationWithConfig {

        val modelId = 1

        client.delete("/trafficModel/$modelId") {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${LoginHelper.login(2)}")
            }
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

        client.get("/trafficModel/$modelId").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }
}