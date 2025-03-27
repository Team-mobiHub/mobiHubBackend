package com.mobihub.api

import com.mobihub.dtos.CommentDTO
import com.mobihub.dtos.RatingDTO
import com.mobihub.dtos.TrafficModelDTO
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.*

class InteractionControllerTest: ApiBaseTest() {

    @BeforeTest
    fun setUp(){
        DataBaseHelper.generateSampleData();
    }

    @Test
    fun addCommentSuccessfully() = testApplicationWithConfig{
        val userId = 1;
        val trafficModelId = 1;
        val content = "This is a new comment on the traffic model."

        val comment = """{
            "id": null,
            "trafficModelId": $trafficModelId,
            "userId": $userId,
            "content": "$content"
        }
        """.trimIndent()

        var response: CommentDTO
        val token = LoginHelper.login(userId)

        client.post("/comment") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
            contentType(ContentType.Application.Json)
            setBody(comment)
        }.apply {
            assertEquals(HttpStatusCode.OK, status)

            response =
                assertDoesNotThrow { serializer.decodeFromString<CommentDTO>(bodyAsText()) }

            assertEquals(userId, response.userId)
            assertEquals(content, response.content)
        }
    }

    @Test
    fun addCommentFailure() = testApplicationWithConfig{
        val userId = 1;
        val trafficModelId = 2;
        val content = ""

        val comment = """{
            "id": null,
            "trafficModelId": $trafficModelId,
            "userId": $userId,
            "content": "$content"
        }
        """.trimIndent()

        val token = LoginHelper.login(userId)

        client.post("/comment") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
            contentType(ContentType.Application.Json)
            setBody(comment)
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun updateComment() = testApplicationWithConfig{
        val commentId = 1;
        val userId = 1;
        val trafficModelId = 2
        val content = "This is a new comment on the traffic model."

        val updatedComment = """{
            "id": $commentId,
            "trafficModelId": $trafficModelId,
            "userId": $userId,
            "content": "$content"
        }
        """.trimIndent()

        val token = LoginHelper.login(userId)

        val response: CommentDTO

        client.put("/comment/$commentId") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
            contentType(ContentType.Application.Json)
            setBody(updatedComment)
        }. apply {
            assertEquals(HttpStatusCode.OK, status)

            response =
                assertDoesNotThrow { serializer.decodeFromString<CommentDTO>(bodyAsText()) }

            assertEquals(userId, response.userId)
            assertEquals(content, response.content)
        }
    }

    @Test
    fun updateCommentToEmpty() = testApplicationWithConfig{
        val commentId = 1;
        val userId = 1;
        val trafficModelId = 2
        val content = ""

        val updatedComment = """{
            "id": $commentId,
            "trafficModelId": $trafficModelId,
            "userId": $userId,
            "content": "$content"
        }
        """.trimIndent()

        val token = LoginHelper.login(userId)

        client.put("/comment/$commentId") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
            contentType(ContentType.Application.Json)
            setBody(updatedComment)
        }. apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun updateNonexistentComment() = testApplicationWithConfig{
        val commentId = 20;
        val userId = 1;
        val trafficModelId = 2
        val content = "Comment here"

        val updatedComment = """{
            "id": $commentId,
            "trafficModelId": $trafficModelId,
            "userId": $userId,
            "content": "$content"
        }
        """.trimIndent()

        val token = LoginHelper.login(userId)

        client.put("/comment/$commentId") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
            contentType(ContentType.Application.Json)
            setBody(updatedComment)
        }. apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun deleteComment() = testApplicationWithConfig{
        val commentId = 1;
        val userId = 1;

        val token = LoginHelper.login(userId)

        client.delete("/comment/$commentId") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }

    }

    @Test
    fun deleteNonExistingComment() = testApplicationWithConfig{
        val commentId = 9;
        val userId = 1;

        val token = LoginHelper.login(userId)

        client.delete("/comment/$commentId") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun addRatingSuccessfully() = testApplicationWithConfig{
        val userId = 1;
        val trafficModelId = 11;
        val rating = 5;

        val token = LoginHelper.login(userId)
        var response: RatingDTO

        client.post("/rating/$trafficModelId/$rating") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }.apply {
            assertEquals(HttpStatusCode.OK, status)

            response =
                assertDoesNotThrow { serializer.decodeFromString<RatingDTO>(bodyAsText()) }

            assertEquals(trafficModelId, response.trafficModelId)
            assertEquals(rating, response.usersRating)
        }
    }

    @Test
    fun addInvalidRating() = testApplicationWithConfig{
        val userId = 1;
        val trafficModelId = 11;
        val rating = 4.3;

        val token = LoginHelper.login(userId)

        client.post("/rating/$trafficModelId/$rating") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun addRatingToInvalidModel() = testApplicationWithConfig{
        val userId = 1;
        val trafficModelId = 20;
        val rating = 4;

        val token = LoginHelper.login(userId)

        client.post("/rating/$trafficModelId/$rating") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun updateRatingSuccessfully() = testApplicationWithConfig{
        val userId = 1;
        val trafficModelId = 10;
        val rating = 4;

        val token = LoginHelper.login(userId)
        var response: RatingDTO

        client.put("/rating/$trafficModelId/$rating") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }.apply {
            assertEquals(HttpStatusCode.OK, status)

            response =
                assertDoesNotThrow { serializer.decodeFromString<RatingDTO>(bodyAsText()) }

            assertEquals(trafficModelId, response.trafficModelId)
            assertEquals(rating, response.usersRating)
        }
    }


    @Test
    fun updateInvalidRating() = testApplicationWithConfig{
        val userId = 1;
        val trafficModelId = 10;
        val rating = 1.1;

        val token = LoginHelper.login(userId)

        client.put("/rating/$trafficModelId/$rating") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun updateRatingToInvalidModel() = testApplicationWithConfig{
        val userId = 1;
        val trafficModelId = 20;
        val rating = 1.1;

        val token = LoginHelper.login(userId)

        client.put("/rating/$trafficModelId/$rating") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }


    @Test
    fun deleteRating() = testApplicationWithConfig{
        val userId = 1;
        val trafficModelId = 1;

        val token = LoginHelper.login(userId)

        client.delete("/rating/$userId/$trafficModelId") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun deleteNonexistentRating() = testApplicationWithConfig{
        val userId = 1;
        val trafficModelId = 20;

        val token = LoginHelper.login(userId)

        client.delete("/rating/$userId/$trafficModelId") {
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun addToFavorite() = testApplicationWithConfig{
        val userId = 1;
        val trafficModelId = 3;

        val token = LoginHelper.login(userId)

        client.post("/favorite/$trafficModelId"){
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun addToFavoriteFailure() = testApplicationWithConfig{
        val userId = 1;
        val trafficModelId = 20;

        val token = LoginHelper.login(userId)

        client.post("/favorite/$trafficModelId"){
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }.apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun removeFavorite() = testApplicationWithConfig{
        val userId = 1;
        val trafficModelId = 1;

        val token = LoginHelper.login(userId)

        client.delete("/favorite/$trafficModelId"){
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun getFavoritesOfUser() = testApplicationWithConfig{
        val userId = 1;
        val favorites = listOf(1,2)


        val token = LoginHelper.login(userId)
        val response:List<TrafficModelDTO>

        client.get("/favorite"){
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }.apply {
            assertEquals(HttpStatusCode.OK, status)

            response =
                assertDoesNotThrow { serializer.decodeFromString<List<TrafficModelDTO>>(bodyAsText()) }

            response.forEachIndexed {index,value ->
                assertEquals(favorites[index], value.id)
            }
        }
    }
}