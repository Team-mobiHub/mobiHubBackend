package com.mobihub.servicesTest

import com.mobihub.TokenBlackList
import com.mobihub.dtos.RegisterDTO
import com.mobihub.dtos.UserDTO
import com.mobihub.exceptions.DataAlreadyExistsException
import com.mobihub.utils.verifier.exceptions.InvalidCredentialException
import com.mobihub.exceptions.DataWithIdentifierNotFoundException
import com.mobihub.exceptions.UnauthorizedException
import com.mobihub.model.*
import com.mobihub.repositories.*
import com.mobihub.services.LinkService
import com.mobihub.services.TrafficModelService
import com.mobihub.services.UserService
import com.mobihub.utils.email.EmailConfig
import com.mobihub.utils.email.EmailService
import com.mobihub.utils.file.FileHandler
import com.mobihub.utils.file.ShareLink
import com.mobihub.utils.file.exceptions.InvalidFileException
import io.ktor.server.config.*
import kotlin.test.*
import org.mockito.kotlin.*
import io.ktor.server.engine.*
import org.mindrot.jbcrypt.BCrypt
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyInt
import java.sql.Timestamp
import java.time.Instant
import java.util.*

/**
 * Test class for the `UserService`.
 * This class contains unit tests to verify the functionality of the `UserService` methods,
 * including user creation, deletion, updates, and retrieval.
 */
class TestUserService {

    /**
     * Mocked instance of `UserRepository` used to simulate database interactions.
     */
    private var userRepository: UserRepository = mock()

    private val trafficModelRepository: TrafficModelRepository = mock()
    private val teamRepository : TeamRepository = mock()

    /**
     * Instance of `UserService` under test, initialized with mocked dependencies.
     */
    private lateinit var userService: UserService

    /**
     * Mocked instance of `LinkService` used to mock Link creation behaviour
     */
    private lateinit var linkService: LinkService
    private var fileHandler: FileHandler = mock()
    private val trafficModelService = mock<TrafficModelService>()

    /**
     * Setup method executed before each test case.
     * Initializes mocked dependencies and configures the `UserService` with a test environment.
     */
    @BeforeTest
    fun setup() {
        val emailConfig = EmailConfig(
            "smtp.kit.edu",
            587,
            "ifv-mobihub-0001",
            "y-W_W9!rfI",
            "mobihub-noreply@ifv.kit.edu"
        )

        val testConfig = ApplicationConfig("application.yaml")


        val testEnvironment = applicationEnvironment {
            config = testConfig
        }

        val emailService = EmailService(emailConfig)
        linkService = mock()
        userService = UserService(
            trafficModelService = trafficModelService,
            ratingRepository = mock<RatingRepository>(),
            teamRepository = teamRepository,
            linkService = linkService,
            emailService = emailService,
            environment = testEnvironment,
            trafficModelRepository = trafficModelRepository,
            commentRepository = mock<CommentRepository>(),
            fileHandler = fileHandler,
            userRepository = userRepository,
            favouriteRepository = mock(),
        )
    }

    /**
     * Tests the successful creation of a user using valid input data.
     * Verifies that the returned `UserDTO` has a non-null ID and matches the input attributes.
     */
    @Test
    fun test_Create_User() {
        // Create a new `RegisterDTO` object with valid data.
        val registerDTO = RegisterDTO("testuser", "test@example.com", "Password123&")

        // Mock the repository behavior
        whenever(userRepository.getByEmail(registerDTO.email)).thenReturn(null)
        whenever(userRepository.create(any())).thenAnswer { invocation ->
            val user = invocation.arguments[0] as User
            user.copy(_id = UserId(1))
        }

        // Call the `create` method of `UserService`.
        val userDTO = userService.create(registerDTO)


        // Assert that the returned `UserDTO` has a non-null ID.
        assertNotNull(userDTO.id)

        // Assert that attributes of the returned `UserDTO` match the input attributes.
        assertEquals(registerDTO.username, userDTO.name)
        assertEquals(registerDTO.email, userDTO.email)
    }

    /**
     * Tests the successful deletion of a user by an authorized user.
     * Verifies that the repository's `delete` method is called with the correct user.
     */
    @Test
    fun delete_User_Successfully() {
        val currentUserDTO = UserDTO(
            id = 1,
            name = "currentuser",
            email = "current@example.com",
            profilePicture = null,
            isEmailVerified = true,
            isAdmin = false,
            teams = emptyList(),
            profilePictureLink = ""
        )

        val mockedOwner1 = mock<User>()
        whenever(mockedOwner1._id).thenReturn(UserId(1))
        val mockedOwner2 = mock<User>()
        whenever(mockedOwner2._id).thenReturn(UserId(2))
        val userToDelete = User(
            _id = UserId(1),
            _name = "currentuser",
            _email = "current@example.com",
            _profilePictureProvider = { null },
            _trafficModelProvider = { emptyList() },
            isEmailVerified = true,
            passwordHash = "hashedPassword",
            isAdmin = false,
            teamsProvider = { listOf(
                Team(
                    _id = TeamId(1),
                    _name = "Team 1",
                    _email = "email1",
                    _profilePicureProvider = { null },
                    _trafficModelProvider = { emptyList() },
                    description = "desc 1",
                    ownerProvider = { mockedOwner1 },
                    membersProvider = { listOf(mockedOwner1) }
                ),
                Team(
                    _id = TeamId(2),
                    _name = "Team 2",
                    _email = "email2",
                    _profilePicureProvider = { null },
                    _trafficModelProvider = { emptyList() },
                    description = "desc 2",
                    ownerProvider = { mockedOwner2 },
                    membersProvider = { listOf(mockedOwner2) }
                )
            ) }
        )

        val trafficModel = mock<TrafficModel>()
        whenever(trafficModel.id).thenReturn(TrafficModelId(1))
        whenever(trafficModelRepository.getByUser(UserId(anyInt()))).thenReturn(listOf(trafficModel))
        whenever(userRepository.getById(UserId(1))).thenReturn(userToDelete)
        userService.delete(UserId(1), currentUserDTO)

        verify(userRepository).delete(userToDelete)
    }

    /**
     * Tests that deleting a user fails when the current user is not authorized.
     * Verifies that an `UnauthorizedException` is thrown with the correct message.
     */
    @Test
    fun delete_User_Fails_Due_To_Unauthorized() {
        val currentUserDTO = UserDTO(
            id = 2,
            name = "otheruser",
            email = "other@example.com",
            profilePicture = null,
            isEmailVerified = true,
            isAdmin = false,
            teams = emptyList(),
            profilePictureLink = ""
        )
        val userToDelete = User(
            _id = UserId(1),
            _name = "currentuser",
            _email = "current@example.com",
            _profilePictureProvider = { null },
            _trafficModelProvider = { emptyList() },
            isEmailVerified = true,
            passwordHash = "hashedPassword",
            isAdmin = false,
            teamsProvider = { emptyList() }
        )

        whenever(userRepository.getById(UserId(1))).thenReturn(userToDelete)

        val exception = assertFailsWith<UnauthorizedException> {
            userService.delete(UserId(1), currentUserDTO)
        }

        assertEquals("Error: unauthorized access for delete User by 2", exception.message)
    }

    /**
     * Tests that deleting a user fails when the user to be deleted is not found.
     * Verifies that a `DataWithIdentifierNotFoundException` is thrown with the correct message.
     */
    @Test
    fun delete_User_Fails_Due_To_User_Not_Found() {
        val currentUserDTO = UserDTO(
            id = 1,
            name = "currentuser",
            email = "current@example.com",
            profilePicture = null,
            isEmailVerified = true,
            isAdmin = false,
            teams = emptyList(),
            profilePictureLink = ""
        )

        whenever(userRepository.getById(UserId(1))).thenReturn(null)

        val exception = assertFailsWith<DataWithIdentifierNotFoundException> {
            userService.delete(UserId(1), currentUserDTO)
        }

        assertEquals("User by id 1 not found.", exception.message)
    }

    /**
     * Tests that creating a user fails when invalid input (e.g., empty email) is provided.
     * Verifies that an `InvalidCredentialException` is thrown with the correct message.
     */
    @Test
    fun test_Create_User_with_Invalid_Input() {
        // Create a new `RegisterDTO` object with invalid data (e.g., empty email).
        val registerDTO = RegisterDTO("testuser", "", "Password123&")

        // Call the `create` method of `UserService` and assert that an exception is thrown.
        val exception = assertFailsWith<InvalidCredentialException> {
            run {
                userService.create(registerDTO)
            }
        }

        // Assert that the exception message is as expected.
        assertEquals("Invalid email: .", exception.message)
    }

    /**
     * Tests that creating a user fails when the email is too long.
     * Verifies that an `InvalidCredentialException` is thrown.
     */
    @Test
    fun test_Create_User_with_too_long_Email() {
        // Create a new `RegisterDTO` object with invalid data (e.g., empty email).
        val registerDTO = RegisterDTO("testuser", "a".repeat(256) + "ea@aaa.com", "Password123&")
        // Call the `create` method of `UserService` and assert that an exception is thrown.
        assertFailsWith<InvalidCredentialException> {
            run {
                userService.create(registerDTO)
            }
        }
    }

    /**
     * Tests that creating a user fails when a user with the same email already exists.
     * Verifies that a `DataAlreadyExistsException` is thrown with the correct message.
     */
    @Test
    fun test_Create_User_with_duplicate_Name() {
        // Create a new `RegisterDTO` object with duplicate name.
        val registerDTO = RegisterDTO("testuser", "aaa@gmail.com", "Password123&")

        // Repo already has name "testuser"
        whenever(userRepository.getByName(registerDTO.username)).thenReturn(
            User(
                _id = UserId(1),
                _name = "testuser",
                _email = "test@gmail.com",
                _profilePictureProvider = { null },
                _trafficModelProvider = { emptyList() },
                isEmailVerified = false,
                passwordHash = "hashedPassword",
                isAdmin = false,
                teamsProvider = { emptyList() },
            )
        )

        // Call the `create` method of `UserService` and assert that an exception is thrown.
        val exception = assertFailsWith<DataAlreadyExistsException> {
            userService.create(registerDTO)
        }

        // Assert that the exception message is as expected.
        assertEquals("User by name testuser already found.", exception.message)
    }

    @Test
    fun test_Create_User_with_duplicate_Email() {
        // Create a new `RegisterDTO` object with duplicate name.
        val registerDTO = RegisterDTO("testuser", "aaa@gmail.com", "Password123&")

        // Repo already has name "testuser"
        whenever(userRepository.getByEmail(registerDTO.email)).thenReturn(
            User(
                _id = UserId(1),
                _name = "testuser",
                _email = "aaa@gmail.com",
                _profilePictureProvider = { null },
                _trafficModelProvider = { emptyList() },
                isEmailVerified = false,
                passwordHash = "hashedPassword",
                isAdmin = false,
                teamsProvider = { emptyList() },
            )
        )

        // Call the `create` method of `UserService` and assert that an exception is thrown.
        val exception = assertFailsWith<DataAlreadyExistsException> {
            userService.create(registerDTO)
        }

        // Assert that the exception message is as expected.
        assertEquals("User by email aaa@gmail.com already found.", exception.message)
    }

    /**
     * Tests the successful update of a user's information.
     * Verifies that the updated user's attributes match the provided input.
     */
    @Test
    fun update_User_Successfully() {
        val currentUserDTO = UserDTO(
            id = 1,
            name = "currentuser",
            email = "current@example.com",
            profilePicture = null,
            isEmailVerified = true,
            isAdmin = false,
            teams = emptyList(),
            profilePictureLink = ""
        )
        val updatedUserDTO = UserDTO(
            id = 1,
            name = "updateduser",
            email = "updated@example.com",
            profilePicture = null,
            isEmailVerified = true,
            isAdmin = false,
            teams = emptyList(),
            profilePictureLink = ""
        )

        val currentUser = User(
            _id = UserId(1),
            _name = "currentuser",
            _email = "current@example.com",
            _profilePictureProvider = { null },
            _trafficModelProvider = { emptyList() },
            isEmailVerified = true,
            passwordHash = "hashedPassword",
            isAdmin = false,
            teamsProvider = { emptyList() }
        )

        whenever(userRepository.getById(UserId(1))).thenReturn(currentUser)
        whenever(userRepository.getByEmail(updatedUserDTO.email)).thenReturn(null)
        whenever(userRepository.getByName(updatedUserDTO.name)).thenReturn(null)
        whenever(userRepository.update(any())).thenReturn(
            currentUser.copy(
                _name = updatedUserDTO.name,
                _email = updatedUserDTO.email
            )
        )

        val result = userService.update(updatedUserDTO, currentUserDTO)

        assertEquals(updatedUserDTO.name, result.name)
        assertEquals(updatedUserDTO.email, result.email)
    }

    /**
     * Tests that updating a user fails when the new email is already in use by another user.
     * Verifies that a `DataAlreadyExistsException` is thrown with the correct message.
     */
    @Test
    fun update_User_Fails_Due_To_Duplicate_Email() {
        val currentUserDTO = UserDTO(
            id = 1,
            name = "currentuser",
            email = "current@example.com",
            profilePicture = null,
            isEmailVerified = true,
            isAdmin = false,
            teams = emptyList(),
            profilePictureLink = ""
        )
        val updatedUserDTO = UserDTO(
            id = 1,
            name = "updateduser",
            email = "duplicate@example.com",
            profilePicture = null,
            isEmailVerified = true,
            isAdmin = false,
            teams = emptyList(),
            profilePictureLink = ""
        )

        val currentUser = User(
            _id = UserId(1),
            _name = "currentuser",
            _email = "current@example.com",
            _profilePictureProvider = { null },
            _trafficModelProvider = { emptyList() },
            isEmailVerified = true,
            passwordHash = "hashedPassword",
            isAdmin = false,
            teamsProvider = { emptyList() }
        )

        whenever(userRepository.getById(UserId(1))).thenReturn(currentUser)
        whenever(userRepository.getByEmail(updatedUserDTO.email)).thenReturn(currentUser.copy(_email = "duplicate@example.com"))

        val exception = assertFailsWith<DataAlreadyExistsException> {
            run {
                userService.update(updatedUserDTO, currentUserDTO)
            }
        }

        assertEquals("User by email duplicate@example.com already found.", exception.message)
    }

    /**
     * Tests that updating a user fails when the new name is already in use by another user.
     * Verifies that a `DataAlreadyExistsException` is thrown with the correct message.
     */
    @Test
    fun update_User_Fails_Due_To_Duplicate_Name() {
        val currentUserDTO = UserDTO(
            id = 1,
            name = "currentuser",
            email = "current@example.com",
            profilePicture = null,
            isEmailVerified = true,
            isAdmin = false,
            teams = emptyList(),
            profilePictureLink = ""
        )
        val updatedUserDTO = UserDTO(
            id = 1,
            name = "duplicateuser",
            email = "updated@example.com",
            profilePicture = null,
            isEmailVerified = true,
            isAdmin = false,
            teams = emptyList(),
            profilePictureLink = ""
        )

        val currentUser = User(
            _id = UserId(1),
            _name = "currentuser",
            _email = "current@example.com",
            _profilePictureProvider = { null },
            _trafficModelProvider = { emptyList() },
            isEmailVerified = true,
            passwordHash = "hashedPassword",
            isAdmin = false,
            teamsProvider = { emptyList() }
        )

        whenever(userRepository.getById(UserId(1))).thenReturn(currentUser)
        whenever(userRepository.getByName(updatedUserDTO.name)).thenReturn(currentUser.copy(_name = "duplicateuser"))

        val exception = assertFailsWith<DataAlreadyExistsException> {
            userService.update(updatedUserDTO, currentUserDTO)
        }

        assertEquals("User by name duplicateuser already found.", exception.message)
    }

    /**
     * Tests the successful retrieval of a user by their ID.
     * Verifies that the returned `UserDTO` matches the expected user.
     */
    @Test
    fun test_Get_User_By_Id() {
        val demoUser = User(
            _id = UserId(1),
            _name = "currentuser",
            _email = "current@example.com",
            _profilePictureProvider = { null },
            _trafficModelProvider = { emptyList() },
            isEmailVerified = true,
            passwordHash = "hashedPassword",
            isAdmin = false,
            teamsProvider = { emptyList() })
        whenever(userRepository.getById(UserId(1))).thenReturn(demoUser)

        val userDTO = userService.getById(UserId(1))

        assertNotNull(userDTO)

        assertEquals(demoUser._id!!.id, userDTO.id)
        assertEquals(demoUser._name, userDTO.name)
        assertEquals(demoUser._email, userDTO.email)
        assertEquals(demoUser.isEmailVerified, userDTO.isEmailVerified)
    }

    @Test
    fun test_Get_User_By_Id_Fails() {
        val exception = assertFailsWith<DataWithIdentifierNotFoundException> {
            userService.getById(UserId(1))
        }

        assertEquals("User by id 1 not found.", exception.message)
    }

    /**
     * Tests the successful retrieval of a user by their email.
     * Verifies that the returned `UserDTO` matches the expected user.
     */
    @Test
    fun test_Get_User_By_Email() {
        val demoUser = User(
            _id = UserId(1),
            _name = "currentuser",
            _email = "current@example.com",
            _profilePictureProvider = { null },
            _trafficModelProvider = { emptyList() },
            isEmailVerified = true,
            passwordHash = "hashedPassword",
            isAdmin = false,
            teamsProvider = { emptyList() })
        whenever(userRepository.getByEmail("current@example.com")).thenReturn(demoUser)

        val userDTO = userService.getByEmail("current@example.com")

        assertNotNull(userDTO)

        assertEquals(demoUser._id!!.id, userDTO.id)
        assertEquals(demoUser._name, userDTO.name)
        assertEquals(demoUser._email, userDTO.email)
        assertEquals(demoUser.isEmailVerified, userDTO.isEmailVerified)
    }

    @Test
    fun changePasswordSuccessfully() {
        val userId = UserId(1)
        val oldPassword = "oldPassword123&"
        val newPassword = "newPassword123&"
        val user = User(
            _id = userId,
            _name = "testuser",
            _email = "test@example.com",
            _profilePictureProvider = { null },
            _trafficModelProvider = { emptyList() },
            isEmailVerified = true,
            passwordHash = BCrypt.hashpw(oldPassword, BCrypt.gensalt()),
            isAdmin = false,
            teamsProvider = { emptyList() }
        )

        whenever(userRepository.getById(userId)).thenReturn(user)

        userService.changePassword(oldPassword, newPassword, userId)

        verify(userRepository).changePassword(check {
            assertTrue(BCrypt.checkpw(newPassword, it.passwordHash))
        })
    }

    @Test
    fun changePasswordFailsDueToIncorrectOldPassword() {
        val userId = UserId(1)
        val oldPassword = "oldPassword123&"
        val newPassword = "newPassword123&"
        val user = User(
            _id = userId,
            _name = "testuser",
            _email = "test@example.com",
            _profilePictureProvider = { null },
            _trafficModelProvider = { emptyList() },
            isEmailVerified = true,
            passwordHash = BCrypt.hashpw("differentOldPassword", BCrypt.gensalt()),
            isAdmin = false,
            teamsProvider = { emptyList() }
        )

        whenever(userRepository.getById(userId)).thenReturn(user)

        val exception = assertFailsWith<IllegalArgumentException> {
            userService.changePassword(oldPassword, newPassword, userId)
        }

        assertEquals("Invalid password", exception.message)
    }

    @Test
    fun changePasswordFailsDueToUserNotFound() {
        val userId = UserId(1)
        val oldPassword = "oldPassword123&"
        val newPassword = "newPassword123&"

        whenever(userRepository.getById(userId)).thenReturn(null)

        val exception = assertFailsWith<DataWithIdentifierNotFoundException> {
            userService.changePassword(oldPassword, newPassword, userId)
        }

        assertEquals("User by id 1 not found.", exception.message)
    }

    @Test
    fun changePasswordFailsDueToInvalidNewPassword() {
        val userId = UserId(1)
        val oldPassword = "oldPassword123&"
        val newPassword = "short" // Invalid new password (too short)
        val user = User(
            _id = userId,
            _name = "testuser",
            _email = "test@example.com",
            _profilePictureProvider = { null },
            _trafficModelProvider = { emptyList() },
            isEmailVerified = true,
            passwordHash = BCrypt.hashpw(oldPassword, BCrypt.gensalt()),
            isAdmin = false,
            teamsProvider = { emptyList() }
        )

        whenever(userRepository.getById(userId)).thenReturn(user)

        val exception = assertFailsWith<InvalidCredentialException> {
            userService.changePassword(oldPassword, newPassword, userId)
        }

        assertEquals("Invalid password: .", exception.message)
    }

    @Test
    fun loginSuccessfully() {
        val email = "test@example.com"
        val password = "Password123&"
        val user = User(
            _id = UserId(1),
            _name = "testuser",
            _email = email,
            _profilePictureProvider = { null },
            _trafficModelProvider = { emptyList() },
            isEmailVerified = true,
            passwordHash = BCrypt.hashpw(password, BCrypt.gensalt()),
            isAdmin = false,
            teamsProvider = { emptyList() }
        )

        whenever(userRepository.getByEmail(email)).thenReturn(user)

        val authResponse = userService.login(email, password)

        assertNotNull(authResponse.token)
        assertTrue(authResponse.expiresAt.isAfter(Instant.now()))
    }

    @Test
    fun loginFailsDueToIncorrectPassword() {
        val email = "test@example.com"
        val password = "Password123&"
        val user = User(
            _id = UserId(1),
            _name = "testuser",
            _email = email,
            _profilePictureProvider = { null },
            _trafficModelProvider = { emptyList() },
            isEmailVerified = true,
            passwordHash = BCrypt.hashpw("differentPassword", BCrypt.gensalt()),
            isAdmin = false,
            teamsProvider = { emptyList() }
        )

        whenever(userRepository.getByEmail(email)).thenReturn(user)

        val exception = assertFailsWith<IllegalArgumentException> {
            userService.login(email, password)
        }

        assertEquals("Invalid password", exception.message)
    }

    @Test
    fun loginFailsDueToUserNotFound() {
        val email = "test@example.com"
        val password = "Password123&"

        whenever(userRepository.getByEmail(email)).thenReturn(null)

        val exception = assertFailsWith<DataWithIdentifierNotFoundException> {
            userService.login(email, password)
        }

        assertEquals("User by email test@example.com not found.", exception.message)
    }

    @Test
    fun logoutSuccessfully() {
        // login
        val email = "test@example.com"
        val password = "Password123&"
        val user = User(
            _id = UserId(1),
            _name = "testuser",
            _email = email,
            _profilePictureProvider = { null },
            _trafficModelProvider = { emptyList() },
            isEmailVerified = true,
            passwordHash = BCrypt.hashpw(password, BCrypt.gensalt()),
            isAdmin = false,
            teamsProvider = { emptyList() }
        )

        whenever(userRepository.getByEmail(email)).thenReturn(user)

        val authResponse = userService.login(email, password)

        // logout

        userService.logout(user.name, authResponse.token)

        assertTrue(TokenBlackList.contains(authResponse.token))
    }

    @Test
    fun resetPasswordWithEmailSuccessfully() {
        val email = "test@example.com"
        val user = User(
            _id = UserId(1),
            _name = "testuser",
            _email = email,
            _profilePictureProvider = { null },
            _trafficModelProvider = { emptyList() },
            isEmailVerified = true,
            passwordHash = BCrypt.hashpw("Password123&", BCrypt.gensalt()),
            isAdmin = false,
            teamsProvider = { emptyList() }
        )

        whenever(userRepository.getByEmail(email)).thenReturn(user)
        whenever(linkService.createLink(null, null, email, LinkType.PASSWORD_RESET)).thenReturn("resetLink")

        assertEquals(userService.resetPasswordWithEmail(email), Unit)

    }

    @Test
    fun resetPasswordWithEmailFailsDueToUserNotFound() {
        val email = "nonexistent@example.com"

        whenever(userRepository.getByEmail(email)).thenReturn(null)

        val exception = assertFailsWith<DataWithIdentifierNotFoundException> {
            userService.resetPasswordWithEmail(email)
        }

        assertEquals("User by email nonexistent@example.com not found.", exception.message)
    }

    @Test
    fun resetPasswordSetNewSuccessfully() {
        val token = UUID.randomUUID()
        val newPassword = "newPassword123&"
        val email = "test@example.com"
        val user = User(
            _id = UserId(1),
            _name = "testuser",
            _email = email,
            _profilePictureProvider = { null },
            _trafficModelProvider = { emptyList() },
            isEmailVerified = true,
            passwordHash = BCrypt.hashpw("oldPassword123&", BCrypt.gensalt()),
            isAdmin = false,
            teamsProvider = { emptyList() }
        )
        val tokenData = LinkData(
            email = email,
            token = token,
            createdAt = Timestamp(0L),
            user = user,
            team = null,
            linkType = LinkType.PASSWORD_RESET
        )

        whenever(linkService.getLinkData(token)).thenReturn(tokenData)
        whenever(userRepository.getByEmail(email)).thenReturn(user)

        assertEquals(userService.resetPasswordSetNew(token, newPassword), Unit)
    }

    @Test
    fun resetPasswordSetNewFailsDueToInvalidToken() {
        val token = UUID.randomUUID()
        val newPassword = "newPassword123&"

        whenever(linkService.getLinkData(token)).thenAnswer{throw DataWithIdentifierNotFoundException("LinkData", "token", token.toString()) }

        val exception = assertFailsWith<DataWithIdentifierNotFoundException> {
            userService.resetPasswordSetNew(token, newPassword)
        }

        assertEquals("LinkData by token $token not found.", exception.message)
    }

    @Test
    fun resetPasswordSetNewFailsDueToUserNotFound() {
        val token = UUID.randomUUID()
        val newPassword = "newPassword123&"
        val email = "nonexistent@example.com"
        val tokenData = LinkData(
        email = email,
        token = token,
        createdAt = Timestamp(0L),
        user = mock(),
        team = null,
        linkType = LinkType.PASSWORD_RESET
        )

        whenever(linkService.getLinkData(token)).thenReturn(tokenData)
        whenever(userRepository.getByEmail(email)).thenReturn(null)

        val exception = assertFailsWith<DataWithIdentifierNotFoundException> {
            userService.resetPasswordSetNew(token, newPassword)
        }

        assertEquals("user by email $email not found.", exception.message)
    }

    @Test
    fun uploadProfilePicture() {
        val userCaptor = ArgumentCaptor.forClass(User::class.java)
        val currentUserDTO = UserDTO(
            id = 1,
            name = "user",
            email = "current@example.com",
            profilePicture = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()), // JPG signature
            isEmailVerified = true,
            isAdmin = false,
            teams = emptyList(),
            profilePictureLink = ""
        )
        val updatedUserDTO = currentUserDTO.copy()

        val currentUser = User(
            _id = UserId(1),
            _name = "currentuser",
            _email = "current@example.com",
            _profilePictureProvider = { null },
            _trafficModelProvider = { emptyList() },
            isEmailVerified = true,
            passwordHash = "hashedPassword",
            isAdmin = false,
            teamsProvider = { emptyList() }
        )

        whenever(userRepository.getById(UserId(1))).thenReturn(currentUser)
        whenever(userRepository.update(any())).thenAnswer { invocation -> invocation.arguments[0] as User }
        whenever(fileHandler.getDownloadReference(any(), any())).thenReturn(
            ShareLink("shareToken", "fileName")
        )

        // no profile picture at first
        assertEquals(userService.getById(UserId(1)).profilePictureLink, "")

        userService.update(updatedUserDTO, currentUserDTO)

        // Use argumentCaptor() from Mockito-Kotlin
        argumentCaptor<User>().apply {
            verify(userRepository).update(capture())
            val capturedUser = firstValue
            assertNotNull(capturedUser.profilePicture)
            assertEquals("jpg", capturedUser.profilePicture?.fileExtension)
            assertNotNull(capturedUser.profilePicture?.token)
            assertEquals(capturedUser.trafficModels, emptyList())
        }
    }

    @Test
    fun uploadInvalidProfilePicture() {
        val userCaptor = ArgumentCaptor.forClass(User::class.java)
        val currentUserDTO = UserDTO(
            id = 1,
            name = "user",
            email = "current@example.com",
            profilePicture = byteArrayOf(0xFF.toByte()), // JPG signature
            isEmailVerified = true,
            isAdmin = false,
            teams = emptyList(),
            profilePictureLink = ""
        )
        val updatedUserDTO = currentUserDTO.copy()

        val currentUser = User(
            _id = UserId(1),
            _name = "currentuser",
            _email = "current@example.com",
            _profilePictureProvider = { null },
            _trafficModelProvider = { emptyList() },
            isEmailVerified = true,
            passwordHash = "hashedPassword",
            isAdmin = false,
            teamsProvider = { emptyList() }
        )

        whenever(userRepository.getById(UserId(1))).thenReturn(currentUser)
        whenever(userRepository.update(any())).thenAnswer { invocation -> invocation.arguments[0] as User }
        whenever(fileHandler.getDownloadReference(any(), any())).thenReturn(
            ShareLink("shareToken", "fileName")
        )

        // no profile picture at first
        assertEquals(userService.getById(UserId(1)).profilePictureLink, "")

        assertFailsWith<InvalidFileException> {
            userService.update(updatedUserDTO, currentUserDTO)
        }
    }
}