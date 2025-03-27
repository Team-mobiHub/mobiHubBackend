package com.mobihub.repositoriesTest

import com.mobihub.model.Image
import com.mobihub.model.ShareToken
import com.mobihub.model.User
import com.mobihub.model.UserId
import com.mobihub.repositories.*
import com.mobihub.repositories.db.UserTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.*
import kotlin.test.*

/**
 * Test class for the UserDbRepository.
 * Contains unit tests to verify the functionality of the UserDbRepository methods.
 */
class UserDbRepositoryTest {

    /**
     * Repository provider to access the user repository.
     */
    private lateinit var repositoryProvider: RepositoryProvider

    /**
     * Setup method executed before each test case.
     * Connects to an in-memory H2 database, creates the UserTable schema, and initializes the repository provider.
     */
    @BeforeTest
    fun setup() {
        // Connect to the in-memory H2 database
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")

        // Create the UserTable schema
        transaction {
            SchemaUtils.create(UserTable)
        }

        val trafficModelRepository: TrafficModelRepository = mock()
        val teamRepository: TeamRepository = mock()

        // Initialize the repository
        repositoryProvider = mock()
        whenever(repositoryProvider.imageRepository).thenReturn(ImageDbRepository(repositoryProvider))
        whenever(repositoryProvider.userRepository).thenReturn(UserDbRepository(repositoryProvider))
        whenever(repositoryProvider.trafficModelRepository).thenReturn(trafficModelRepository)
        whenever(repositoryProvider.teamRepository).thenReturn(teamRepository)
        whenever(trafficModelRepository.getByUser(UserId(anyInt()))).thenReturn(mock())
        whenever(teamRepository.getByUserId(UserId(anyInt()))).thenReturn(mock())
    }

    /**
     * Test case to verify the successful creation of a user.
     * Ensures that the created user has a non-null ID and matches the input attributes.
     */
    @Test
    fun c1_Test_Create_User() {
        // Create a new `User` object with sample data.
        val user = getUser(1)
        // Call the `create` method of `UserDbRepository`.
        val createdUser = repositoryProvider.userRepository.create(user)
        // Assert that the returned user has a non-null ID.
        assertNotNull(createdUser.id)
        // Assert that attributes of the returned user match the input attributes.
        assertEquals(user.email, createdUser.email)
    }

    /**
     * Test case to verify the retrieval of a user by ID.
     * Ensures that the retrieved user matches the original user.
     */
    @Test
    fun test_Get_User_By_Id() {
        // Create a new `User` object with sample data.
        val user = getUser(2)
        // Insert the user into the repository.
        val createdUser = repositoryProvider.userRepository.create(user)

        // Retrieve the user by ID.
        val retrievedUser = repositoryProvider.userRepository.getById(UserId(createdUser.id!!.id))

        // Assert that the retrieved user is not null and matches the original user.
        assertNotNull(retrievedUser)
        assertEquals(user.email, retrievedUser.email)
        // check that lazy evaluation is working
        assertEquals(retrievedUser.profilePicture, null)
        print(retrievedUser.trafficModels)
        print(retrievedUser.teams)
        assert(retrievedUser.trafficModels.isNotEmpty())
        assert(retrievedUser.teams.isNotEmpty())
    }

    /**
     * Test case to verify the retrieval of a user by email.
     * Ensures that the retrieved user matches the original user.
     */
    @Test
    fun test_Get_User_By_Email() {
        // Create a new `User` object with sample data.

        val user = getUser(3)
        // Insert the user into the repository.
        repositoryProvider.userRepository.create(user)

        // Retrieve the user by email.
        val retrievedUser = repositoryProvider.userRepository.getByEmail("john_doe3@gmail.com")

        // Assert that the retrieved user is not null and matches the original user.
        assertNotNull(retrievedUser)
        assertEquals(user.email, retrievedUser.email)
    }

    /**
     * Test case to verify the successful update of a user.
     * Ensures that the updated user matches the new attributes.
     */
    @Test
    fun test_Update_User() {
        // Create a new `User` object with sample data.
        val user = User(
            _id = UserId(0),
            _name = "John Doe4",
            _email = "john_doe4@gmail.com",
            _profilePictureProvider = { null },
            _trafficModelProvider = { emptyList() },
            isEmailVerified = false,
            passwordHash = "password123",
            isAdmin = false,
            teamsProvider = { emptyList() },
        )

        val image = Image(
            token = UUID(5, 1),
            name = "user_pp",
            fileExtension = ".png",
            shareToken = ShareToken("239alactinjhaube")
        )
        // Insert the user into the repository with a new image
        val userWithId = repositoryProvider.userRepository.create(user)
        val updatedUser = User(
            _id = UserId(userWithId.id!!.id),
            _name = "NewJohn Doe4",
            _email = "newjohn_doe4@gmail.com",
            _profilePictureProvider = { image },
            _trafficModelProvider = { emptyList() },
            isEmailVerified = false,
            passwordHash = "password123",
            isAdmin = false,
            teamsProvider = { emptyList() },
        )

        var retrievedUpdatedUser = repositoryProvider.userRepository.update(updatedUser)
        var newRetrievedUser = repositoryProvider.userRepository.getById(UserId(userWithId.id!!.id))
        // Assert that the retrieved user is not null and matches the original user.
        assertNotNull(retrievedUpdatedUser)
        assertEquals(newRetrievedUser?.email, retrievedUpdatedUser.email)
        assertEquals(newRetrievedUser?.name, retrievedUpdatedUser.name)
        assertEquals(newRetrievedUser?.profilePicture?.token, image.token)

        // update profile picture, but it has the same UUID (how is that even possible?)
        val newImage = Image(
            token = image.token,
            name = "user_pp2",
            fileExtension = ".jpeg",
            shareToken = ShareToken("239alactin")
        )

        val updatedUser2 = User(
            _id = UserId(userWithId.id!!.id),
            _name = "NewJohn Doe4",
            _email = "newjohn_doe4@gmail.com",
            _profilePictureProvider = { newImage },
            _trafficModelProvider = { emptyList() },
            isEmailVerified = false,
            passwordHash = "password123",
            isAdmin = false,
            teamsProvider = { emptyList() },
        )

        retrievedUpdatedUser = repositoryProvider.userRepository.update(updatedUser2)
        newRetrievedUser = repositoryProvider.userRepository.getById(UserId(userWithId.id!!.id))

        // the users profile picture should have changed
        assertNotNull(retrievedUpdatedUser)
        assertEquals(newRetrievedUser?.profilePicture?.token, image.token)
        assertEquals(newRetrievedUser?.profilePicture?.name, newImage.name)
        assertEquals(newRetrievedUser?.profilePicture?.fileExtension, newImage.fileExtension)
        assertEquals(newRetrievedUser?.profilePicture?.shareToken, newImage.shareToken)
    }

    @Test
    fun test_Get_User_By_Name() {
        val user = getUser(4)

        repositoryProvider.userRepository.create(user)
        val retrievedUser = repositoryProvider.userRepository.getByName("4")
        assertNotNull(retrievedUser)
        assertEquals(user.name, retrievedUser.name)
    }

    @Test
    fun test_Change_User_Password() {
        val user = getUser(5)
        val createdUser = repositoryProvider.userRepository.create(user)
        val updatedUser = createdUser.copy(passwordHash = "newpassword123")
        repositoryProvider.userRepository.changePassword(updatedUser)
        val retrievedUser = repositoryProvider.userRepository.getByName("5")
        assertNotNull(retrievedUser)
        assertEquals("newpassword123", retrievedUser.passwordHash)
    }

    @Test
    fun test_Delete_User() {
        val user = getUser(6)
        val createdUser = repositoryProvider.userRepository.create(user)
        repositoryProvider.userRepository.delete(createdUser)
        val retrievedUser = repositoryProvider.userRepository.getByName("6")
        assertNull(retrievedUser)
    }

    private fun getUser(id: Int) = User(
        _id = UserId(0),
        _name = id.toString(),
        _email = "john_doe$id@gmail.com",
        _trafficModelProvider = { emptyList() },
        isEmailVerified = false,
        passwordHash = "password123",
        isAdmin = false,
        teamsProvider = { emptyList() },
        _profilePictureProvider = { null },
    )
}