package com.mobihub.repositoriesTest

import com.mobihub.model.*
import com.mobihub.repositories.*
import com.mobihub.repositories.db.CommentTable
import com.mobihub.repositories.db.TrafficModelTable
import com.mobihub.repositories.db.UserTable
import com.mobihub.utils.file.FileHandler
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.mockito.kotlin.mock
import java.time.Instant
import java.util.*
import kotlin.test.*

class CommentDbRepositoryTest {

    private var repositoryProvider: RepositoryProvider = RepositoryProvider(fileHandler = mock<FileHandler>())
    private var commentRepository: CommentRepository = CommentDbRepository(repositoryProvider)
    private var userRepository: UserRepository = UserDbRepository(repositoryProvider)
    private var trafficModelRepository: TrafficModelRepository = TrafficModelDbRepository(repositoryProvider)

    private fun createAndGetUser(name: Int): User {
        val user = User(
            null, // name will not be id, because create changes it
            "Test User$name",
            _email = "test$name@gmail.com",
            _trafficModelProvider = { emptyList() },
            _profilePictureProvider = { null },
            isEmailVerified = true,
            passwordHash = "passwordHash",
            isAdmin = false,
            teamsProvider = { emptyList() }
        )
        return userRepository.getByName("Test User$name") ?: return userRepository.create(user)
    }

    private fun createAndGetTrafficModel(name: String): TrafficModel {
        val trafficModel = TrafficModel(
            id = null,
            name = name,
            description = "Test Description",
            isVisibilityPublic = true,
            dataSourceUrl = "http://example.com",
            location = Location(Region("Test Region"), Coordinates("0,0")),
            framework = Framework.fromId(FrameworkId(1)),
            zipFileToken = UUID.randomUUID(),
            isZipFileUploaded = false,
            methodLevelPairProvider = { emptyList() },
            authorProvider = { createAndGetUser(1) },
            markdownFileUrlProvider = { null },
            favoritesProvider = { emptyList() },
            imagesProvider = { emptyList() },
            averageRatingProvider = { 0.0 },
            ratingsProvider = { emptyList() },
            commentsProvider = { emptyList() }
        )
        return trafficModelRepository.create(trafficModel)
    }

    /**
     * Setup method executed before each test case.
     * Connects to an in-memory H2 database, creates the necessary schemas, and initializes the repository provider.
     */
    @BeforeTest
    fun setup() {
        // Connect to the in-memory H2 database
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")

        // Create the UserTable schema
        transaction {
            SchemaUtils.create(UserTable, TrafficModelTable, CommentTable)
        }
    }

    @Test
    fun addCommentSuccessfully() {
        val comment = Comment(
            id = CommentId(0),
            content = "This is a test comment",
            creationDate = Instant.now(),
            trafficModelProvider = { createAndGetTrafficModel("1") },
            userProvider = { createAndGetUser(1) }
        )

        val result = commentRepository.addComment(comment)

        assertEquals(comment.content, result.content)
        assertNotNull(result.id)
    }

    @Test
    fun updateCommentSuccessfully() {
        val comment = Comment(
            id = CommentId(0),
            content = "This is a test comment",
            creationDate = Instant.now(),
            trafficModelProvider = { createAndGetTrafficModel("2") },
            userProvider = { createAndGetUser(2) }
        )

        val addedComment = commentRepository.addComment(comment)
        val updatedComment = addedComment.copy(content = "Updated comment content")

        val result = commentRepository.updateComment(updatedComment)

        assertEquals("Updated comment content", result.content)
    }

    @Test
    fun deleteCommentSuccessfully() {
        val comment = Comment(
            id = CommentId(0),
            content = "This is a test comment",
            creationDate = Instant.now(),
            trafficModelProvider = { createAndGetTrafficModel("3") },
            userProvider = { createAndGetUser(3) }
        )

        val addedComment = commentRepository.addComment(comment)
        commentRepository.deleteComment(addedComment.id)

        val result = commentRepository.getCommentById(addedComment.id)
        assertNull(result)
    }

    @Test
    fun getCommentsForTrafficModelSuccessfully() {
        val trafficModel = createAndGetTrafficModel("4")
        val comments = listOf(
            Comment(
                id = CommentId(0),
                content = "First comment",
                creationDate = Instant.now(),
                trafficModelProvider = { trafficModel },
                userProvider = { createAndGetUser(4) }
            ),
            Comment(
                id = CommentId(0),
                content = "Second comment",
                creationDate = Instant.now(),
                trafficModelProvider = { trafficModel },
                userProvider = { createAndGetUser(5) }
            )
        )

        comments.forEach { commentRepository.addComment(it) }

        val result = commentRepository.getCommentsForTrafficModel(trafficModel.id!!)

        assertEquals(comments.size, result.size)
        assertTrue(result.map { it.content }.containsAll(comments.map { it.content }))
    }

    @Test
    fun getCommentsForUserSuccessfully() {
        val user = createAndGetUser(6)
        val comments = listOf(
            Comment(
                id = CommentId(0),
                content = "First comment",
                creationDate = Instant.now(),
                trafficModelProvider = { createAndGetTrafficModel("5") },
                userProvider = { user }
            ),
            Comment(
                id = CommentId(0),
                content = "Second comment",
                creationDate = Instant.now(),
                trafficModelProvider = { createAndGetTrafficModel("6") },
                userProvider = { user }
            )
        )

        comments.forEach { commentRepository.addComment(it) }

        val result = commentRepository.getCommentsForUser(UserId(user.id?.id!!))

        assertEquals(comments.size, result.size)
        assertTrue(result.map { it.content }.containsAll(comments.map { it.content }))
    }

    @Test
    fun getCommentByIdSuccessfully() {
        val comment = Comment(
            id = CommentId(0),
            content = "This is a test comment",
            creationDate = Instant.now(),
            trafficModelProvider = { createAndGetTrafficModel("7") },
            userProvider = { createAndGetUser(7) }
        )

        val addedComment = commentRepository.addComment(comment)
        val result = commentRepository.getCommentById(addedComment.id)

        assertNotNull(result)
        assertEquals(addedComment.content, result.content)
    }
}