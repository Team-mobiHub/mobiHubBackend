package com.mobihub.servicesTest

import com.mobihub.dtos.CreateCommentDTO
import com.mobihub.model.*
import com.mobihub.repositories.*
import com.mobihub.services.InteractionService
import com.mobihub.model.TrafficModel
import com.mobihub.exceptions.DataWithIdentifierNotFoundException
import com.mobihub.exceptions.UnauthorizedException
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import org.junit.BeforeClass
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.*
import java.time.Instant


class TestInteractionService {


    companion object {
        private lateinit var trafficModel: TrafficModel
        private lateinit var user: User
        private lateinit var interactionService: InteractionService
        private lateinit var userRepository: UserRepository
        private lateinit var trafficModelRepository: TrafficModelRepository
        private lateinit var commentRepository: CommentRepository
        private lateinit var ratingRepository: RatingRepository
        private lateinit var favouriteRepository: FavouriteRepository
        private lateinit var environment: ApplicationEnvironment

        @BeforeClass
        @JvmStatic
        fun setupClass() {

            // Initialize the repositories
            userRepository = mock()
            trafficModelRepository = mock()
            commentRepository = mock()
            ratingRepository = mock()
            favouriteRepository = mock()

            // Initialize the environment
            environment = applicationEnvironment {
                config = ApplicationConfig("application.yaml")
            }

            // Initialize the service
            interactionService = InteractionService(
                ratingRepository = ratingRepository,
                favouriteRepository = favouriteRepository,
                commentRepository = commentRepository,
                userRepository = userRepository,
                trafficModelRepository = trafficModelRepository,
                environment = environment
            )

            user = User(
                _id = UserId(1),
                _name = "testuser",
                _email = "test@example.com",
                _profilePictureProvider = { null },
                _trafficModelProvider = { emptyList() },
                isEmailVerified = true,
                passwordHash = "hashedPassword",
                isAdmin = false,
                teamsProvider = { emptyList() }
            )

            trafficModel = TrafficModel(
                id = TrafficModelId(1),
                name = "Test Model",
                description = "Test Description",
                isVisibilityPublic = true,
                dataSourceUrl = "https://example.com",
                location = Location(Region("Test Region"), Coordinates("0.0, 0.0")),
                framework = Framework.SATURN,
                zipFileToken = UUID.randomUUID(),
                isZipFileUploaded = true,
                methodLevelPairProvider = { emptyList() },
                authorProvider = { user },
                markdownFileUrlProvider = { null },
                favoritesProvider = { emptyList() },
                imagesProvider = { emptyList() },
                averageRatingProvider = { 0.0 },
                ratingsProvider = { emptyList() },
                commentsProvider = { emptyList() }
            )

            whenever(userRepository.getById(UserId(1))).thenReturn(user)
            whenever(trafficModelRepository.getById(TrafficModelId(1))).thenReturn(trafficModel)
        }
    }

    @BeforeTest
    fun setup() {
        whenever(userRepository.getById(UserId(1))).thenReturn(user)
        whenever(trafficModelRepository.getById(TrafficModelId(1))).thenReturn(trafficModel)
    }

    @Test
    fun test_Create_Comment() {
        val createCommentDTO = CreateCommentDTO(
            id = null,
            trafficModelId = 1,
            userId = 1,
            content = "This is a test comment"
        )

        whenever(commentRepository.addComment(any())).thenAnswer { invocation ->
            val comment = invocation.arguments[0] as Comment
            comment.copy(id = CommentId(1))
        }

        val commentDTO = interactionService.addComment(createCommentDTO)

        assertNotNull(commentDTO.id)
        assertEquals(createCommentDTO.content, commentDTO.content)
        assertEquals(createCommentDTO.userId, commentDTO.userId)
        assertEquals(createCommentDTO.trafficModelId, commentDTO.trafficModelId)
    }

    @Test
    fun test_Create_Comment_Fails() {
        val createCommentDTO = CreateCommentDTO(
            id = 1,
            trafficModelId = 999,
            userId = 1,
            content = "This is a test comment"
        )
        val createCommentDTO2 = CreateCommentDTO(
            id = 1,
            trafficModelId = 1,
            userId = 999,
            content = "This is a test comment"
        )

        whenever(commentRepository.addComment(any())).thenAnswer { invocation ->
            val comment = invocation.arguments[0] as Comment
            comment.copy(id = CommentId(1))
        }

        val exception = assertFailsWith<IllegalArgumentException> {
            interactionService.addComment(createCommentDTO)
        }

        val exception2 = assertFailsWith<IllegalArgumentException> {
            interactionService.addComment(createCommentDTO2)
        }

        assertEquals("Traffic model with id 999 does not exist", exception.message)
        assertEquals("User with id 999 does not exist", exception2.message)
    }

    @Test
    fun test_Update_Comment_Fail() {
        val createCommentDTO = CreateCommentDTO(
            id = 999,
            trafficModelId = 1,
            userId = 1,
            content = "Updated comment content"
        )

        val existingComment = Comment(
            id = CommentId(1),
            content = "Original comment content",
            creationDate = Instant.now(),
            trafficModelProvider = { trafficModel },
            userProvider = { user }
        )

        whenever(commentRepository.getCommentById(CommentId(1))).thenReturn(existingComment)

        val exception = assertFailsWith<DataWithIdentifierNotFoundException> {
            interactionService.updateComment(createCommentDTO)
        }

        assertEquals("Comment by id 999 not found.", exception.message)
    }
    @Test
    fun test_Update_Comment() {
        val createCommentDTO = CreateCommentDTO(
            id = 1,
            trafficModelId = 1,
            userId = 1,
            content = "Updated comment content"
        )

        val existingComment = Comment(
            id = CommentId(1),
            content = "Original comment content",
            creationDate = Instant.now(),
            trafficModelProvider = { trafficModel },
            userProvider = { user }
        )

        whenever(commentRepository.getCommentById(CommentId(1))).thenReturn(existingComment)
        whenever(commentRepository.updateComment(any())).thenAnswer { invocation ->
            val comment = invocation.arguments[0] as Comment
            comment
        }

        val commentDTO = interactionService.updateComment(createCommentDTO)

        assertNotNull(commentDTO.id)
        assertEquals(createCommentDTO.content, commentDTO.content)
        assertEquals(createCommentDTO.userId, commentDTO.userId)
        assertEquals(createCommentDTO.trafficModelId, commentDTO.trafficModelId)
    }

    @Test
    fun test_Delete_Comment_Fails() {
        val userId = UserId(1)
        val commentId = CommentId(1)

        val comment = Comment(
            id = commentId,
            content = "Test comment content",
            creationDate = Instant.now(),
            trafficModelProvider = { mock() },
            userProvider = { user }
        )

        whenever(userRepository.getById(userId)).thenReturn(user)
        whenever(commentRepository.getCommentById(commentId)).thenReturn(comment)
        whenever(commentRepository.getCommentsForUser(userId)).thenReturn(listOf(comment))


        val exception = assertFailsWith<DataWithIdentifierNotFoundException> {
            interactionService.deleteComment(CommentId(999), UserId(1))
        }

        val exception2 = assertFailsWith<UnauthorizedException> {
            interactionService.deleteComment(CommentId(1), UserId(999))
        }

        assertEquals("Comment by id 999 not found.", exception.message)
        assertEquals("Error: unauthorized access for Delete comment by UserId(id=999)", exception2.message)
    }
    @Test
    fun test_Delete_Comment() {
        val userId = UserId(1)
        val commentId = CommentId(1)

        val comment = Comment(
            id = commentId,
            content = "Test comment content",
            creationDate = Instant.now(),
            trafficModelProvider = { mock() },
            userProvider = { user }
        )

        whenever(userRepository.getById(userId)).thenReturn(user)
        whenever(commentRepository.getCommentById(commentId)).thenReturn(comment)
        whenever(commentRepository.getCommentsForUser(userId)).thenReturn(listOf(comment))

        interactionService.deleteComment(commentId, userId) // no exceptions expected
    }

    @Test
    fun addRatingSuccessfully() {
        val trafficModelId = TrafficModelId(1)
        val userId = UserId(1)
        val rating = 4

        whenever(ratingRepository.addRating(any())).thenAnswer { invocation ->
            invocation.arguments[0] as Rating
        }
        whenever(ratingRepository.getAverageRatingForTrafficModel(trafficModelId)).thenReturn(4.0)

        val ratingDTO = interactionService.addRating(trafficModelId, rating, userId)

        assertEquals(rating, ratingDTO.usersRating)
        assertEquals(4.0, ratingDTO.averageRating)
    }

    @Test
    fun addRatingFailsDueToInvalidRating() {
        val trafficModelId = TrafficModelId(1)
        val userId = UserId(1)
        val rating = 6

        val exception = assertFailsWith<IllegalArgumentException> {
            interactionService.addRating(trafficModelId, rating, userId)
        }

        assertEquals("Invalid rating amount 6", exception.message)
    }

    @Test
    fun addRatingFailsDueToTrafficModelNotFound() {

        val exception = assertFailsWith<DataWithIdentifierNotFoundException> {
            interactionService.addRating(TrafficModelId(999), 5, UserId(1))
        }

        val exception2 = assertFailsWith<DataWithIdentifierNotFoundException> {
            interactionService.addRating(TrafficModelId(1), 5, UserId(999))
        }

        assertEquals("TrafficModel by id 999 not found.", exception.message)
        assertEquals("User by id 999 not found.", exception2.message)
    }

    @Test
    fun updateRatingSuccessfully() {
        val trafficModelId = TrafficModelId(1)
        val userId = UserId(1)
        val rating = 5

        whenever(ratingRepository.updateRating(any())).thenAnswer { invocation ->
            invocation.arguments[0] as Rating
        }
        whenever(ratingRepository.getAverageRatingForTrafficModel(trafficModelId)).thenReturn(5.0)

        val ratingDTO = interactionService.updateRating(trafficModelId, rating, userId)

        assertEquals(rating, ratingDTO.usersRating)
        assertEquals(5.0, ratingDTO.averageRating)
    }

    @Test
    fun deleteRatingSuccessfully() {
        val trafficModelId = TrafficModelId(1)
        val userId = UserId(1)
        interactionService.deleteRating(trafficModelId, userId)

    }

    @Test
    fun deleteRatingFailsDueToTrafficModelNotFound() {
        val trafficModelId = TrafficModelId(1)
        val userId = UserId(1)

        whenever(trafficModelRepository.getById(trafficModelId)).thenReturn(null)

        val exception = assertFailsWith<DataWithIdentifierNotFoundException> {
            interactionService.deleteRating(trafficModelId, userId)
        }

        assertEquals("Trafficmodel by id 1 not found.", exception.message)
    }

    @Test
    fun deleteRatingFailsDueToUserNotFound() {
        val trafficModelId = TrafficModelId(1)
        val userId = UserId(1)

        whenever(userRepository.getById(userId)).thenReturn(null)

        val exception = assertFailsWith<DataWithIdentifierNotFoundException> {
            interactionService.deleteRating(trafficModelId, userId)
        }

        assertEquals("User by id 1 not found.", exception.message)
    }

    @Test
    fun addFavoriteSuccessfully() {
        val trafficModelId = TrafficModelId(1)
        val userId = UserId(1)

        interactionService.addFavourite(trafficModelId, userId)
    }

    @Test
    fun addFavoriteFailsDueToTrafficModelNotFound() {
        val trafficModelId = TrafficModelId(1)
        val userId = UserId(1)

        whenever(trafficModelRepository.getById(trafficModelId)).thenReturn(null)

        val exception = assertFailsWith<DataWithIdentifierNotFoundException> {
            interactionService.addFavourite(trafficModelId, userId)
        }

        assertEquals("TrafficModel by trafficModelId 1 not found.", exception.message)
    }

    @Test
    fun addFavoriteFailsDueToUserNotFound() {
        val trafficModelId = TrafficModelId(1)
        val userId = UserId(1)

        whenever(userRepository.getById(userId)).thenReturn(null)

        val exception = assertFailsWith<DataWithIdentifierNotFoundException> {
            interactionService.addFavourite(trafficModelId, userId)
        }

        assertEquals("User by userId 1 not found.", exception.message)
    }

    @Test
    fun removeFavoriteSuccessfully() {
        val trafficModelId = TrafficModelId(1)
        val userId = UserId(1)

        interactionService.removeFavourite(trafficModelId, userId)
    }

    @Test
    fun removeFavoriteFailsDueToTrafficModelNotFound() {
        val trafficModelId = TrafficModelId(1)
        val userId = UserId(1)

        whenever(trafficModelRepository.getById(trafficModelId)).thenReturn(null)

        val exception = assertFailsWith<DataWithIdentifierNotFoundException> {
            interactionService.removeFavourite(trafficModelId, userId)
        }

        assertEquals("TrafficModel by trafficModelId 1 not found.", exception.message)
    }

    @Test
    fun removeFavoriteFailsDueToUserNotFound() {
        val trafficModelId = TrafficModelId(1)
        val userId = UserId(1)

        whenever(userRepository.getById(userId)).thenReturn(null)

        val exception = assertFailsWith<DataWithIdentifierNotFoundException> {
            interactionService.removeFavourite(trafficModelId, userId)
        }

        assertEquals("User by userId 1 not found.", exception.message)
    }

    @Test
    fun getFavoritesForUserSuccessfully() {
        val userId = UserId(1)
        val trafficModels = listOf(trafficModel)

        whenever(favouriteRepository.getFavouritesByUserId(userId)).thenReturn(trafficModels)

        val result = interactionService.getFavouritesOfUser(userId)

        assertEquals(result[0].id, trafficModel.id?.id!!)
    }
}