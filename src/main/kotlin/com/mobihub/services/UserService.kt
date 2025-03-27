package com.mobihub.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.mobihub.TokenBlackList
import com.mobihub.dtos.AuthResponseDTO
import com.mobihub.dtos.RegisterDTO
import com.mobihub.dtos.UserDTO
import com.mobihub.exceptions.DataAlreadyExistsException
import com.mobihub.exceptions.DataWithIdentifierNotFoundException
import com.mobihub.exceptions.UnauthorizedException
import com.mobihub.model.*
import com.mobihub.repositories.*
import com.mobihub.utils.email.EmailService
import com.mobihub.utils.email.EmailType
import com.mobihub.utils.file.FileHandler
import com.mobihub.utils.file.exceptions.InvalidFileException
import com.mobihub.utils.file.exceptions.UnexpectedHttpResponse
import com.mobihub.utils.verifier.MobiHubCredentialsVerifier
import io.ktor.server.application.*
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.util.*

/**
 * The File path template for the profile picture in Nextcloud.
 * Usage: NEXTCLOUD_IMG_FILE_PATH_TEMPLATE.format((userId / 100), (userId % 100), UUID.randomUUID(), imageExtension)
 */
private const val NEXTCLOUD_IMG_FILE_PATH_TEMPLATE = "users/%02d/%02d/%s.%s"
private const val UPLOAD_DIR = "uploads"
private const val INTERNAL_FILE_PATH_TEMPLATE = "$UPLOAD_DIR/%s.%s"

private const val FILE_COULD_NOT_BE_DELETED = "Error: temporary profile picture file could not be deleted"
private const val INVALID_PASSWORD = "Invalid password"
private const val ERROR_LOGGED_IN_USER_DNE = "logged in user's id is null"
private const val ERROR_LOGGED_IN_USER_ID_DNE = "logged in user's id does not exist"
private const val INVALID_IMAGE_FILE_FORMAT = "Invalid image file format"

private const val USER_LOGGED_IN_MESSAGE = "User %s logged in"
private const val USER_FAILED_LOGIN_MESSAGE = "User %s failed to log in"
private const val USER_LOGGED_OUT_MESSAGE = "User %s logged out"
private const val USER_CREATED = "User created: id: %d"
private const val USER_UPDATED = "User updated: id: %d"
private const val GENERATED_PASSWORD_RESET_LOG_MESSAGE = "Generated a password reset token for %s: %s"

private const val JPG_BYTE_SIGNATURE = "FFD8"
private const val PNG_BYTE_SIGNATURE = "89504E47"
private const val JPG_FILE_EXTENSION = "jpg"
private const val PNG_FILE_EXTENSION = "png"
private const val HEX_BYTE_FORMAT = "%02X"

private const val PROFILE_PICTURE_NAME = "profilePicture_%s"

private const val UPLOADED_PP_MESSAGE = "Uploaded profile picture to DMS for user with ID: %d"

private const val TEAM_DELETED_USER_OWNER_TEMPLATE = "Team %s was deleted because owner %s was deleted."
private const val TEAM_MEMBER_DELETED_TEMPLATE = "Member %s was deleted from team %s"

private const val DELETED_USER_MESSAGE = "Deleted user with the id: %s"

/**
 * Service class for managing user-related operations.
 *
 * @property [trafficModelService] The service for managing traffic models.
 * @property [userRepository] The repository for managing [User] entities.
 * @property [favouriteRepository] The repository for managing [Favourite] entities.
 * @property [trafficModelRepository] The repository for managing [TrafficModel] entities.
 * @property [commentRepository] The repository for managing [Comment] entities.
 * @property [ratingRepository] The repository for managing [Rating] entities.
 * @property [teamRepository] The repository for managing [Team] entities.
 * @property [linkService] The service for managing links.
 * @property [emailService] The service for sending emails.
 * @property [fileHandler] The handler for managing file operations.
 * @property [environment] The application environment configuration.
 *
 * @author Team-MobiHub
 */
class UserService(
    private val trafficModelService: TrafficModelService,
    private val userRepository: UserRepository,
    private val favouriteRepository: FavouriteRepository,
    private val trafficModelRepository: TrafficModelRepository,
    private val commentRepository: CommentRepository,
    private val ratingRepository: RatingRepository,
    private val teamRepository: TeamRepository,
    private val linkService: LinkService,
    private val emailService: EmailService,
    private val fileHandler: FileHandler,
    private val environment: ApplicationEnvironment
) {
    private val log = LoggerFactory.getLogger(UserService::class.java)
    private val nextcloudBaseUrl = environment.config.property("nextcloud.baseUrl").getString()

    /**
     * Registers a new user.
     *
     * @param user The [RegisterDTO] object containing the user data.
     * @return The created [UserDTO] object.
     *
     * @throws DataAlreadyExistsException If the user data is invalid.
     * @throw [InvalidCredentialException] If the user data is invalid.
     */
    fun create(user: RegisterDTO): UserDTO {

        user.validate()

        require(userRepository.getByEmail(user.email) == null) {
            throw DataAlreadyExistsException("User", "email", user.email)
        }

        require(userRepository.getByName(user.username) == null) {
            throw DataAlreadyExistsException("User", "name", user.username)
        }

        return userRepository.create(
            User(
                _id = null, _name = user.username, _email = user.email,
            // At this point the user could not have uploaded a profile picture yet
            _profilePictureProvider = { null }, _trafficModelProvider = {
                emptyList()
            },
            // At this point, the email could not have been verified yet
            isEmailVerified = false, passwordHash = hashPassword(user.password),
            // By default, the user is not an admin
            isAdmin = false,
            // At this point, the user could not have joined any teams yet
            teamsProvider = {
                emptyList()
            })).toDTO(nextcloudBaseUrl).also { log.info(USER_CREATED.format(it.id)) }
    }

    /**
     * Updates an existing user in the database. Only the name, email and the profilePictureToken are updated.
     * The name and email have to be valid and the profile picture should not exceed a maximum size defined in [UserDTO].
     * Also requires the name and email to be valid as defined by [MobiHubCredentialsVerifier]
     *
     * @param updatedUser The [UserDTO] object containing the updated user data.
     * @param currentUserDTO The [UserDTO] object containing the current user data.
     *
     * @return The updated [UserDTO] object.
     *
     * @throws DataAlreadyExistsException if the given name or email already exist in the database
     * @throws UnexpectedHttpResponse if the profile picture could not be uploaded to Nextcloud
     */
    fun update(updatedUser: UserDTO, currentUserDTO: UserDTO): UserDTO {
        if (currentUserDTO.id == null || updatedUser.id == null) {
            // this shouldn't happen as the user is logged in
            error(ERROR_LOGGED_IN_USER_DNE)
        }
        val currentUser = userRepository.getById(UserId(currentUserDTO.id))
        // this also shouldn't happen as the user is logged in
            ?: error(ERROR_LOGGED_IN_USER_ID_DNE)

        updatedUser.validateUpdate()

        // check if updated name, email is not already taken
        if (userRepository.getByEmail(updatedUser.email) != null && updatedUser.email != currentUser.email) {
            throw DataAlreadyExistsException("User", "email", updatedUser.email)
        }
        if (userRepository.getByName(updatedUser.name) != null && updatedUser.name != currentUser._name) {
            throw DataAlreadyExistsException("User", "name", updatedUser.name)
        }

        return userRepository.update(
            User(
                _id = currentUser._id, _name = updatedUser.name, _email = updatedUser.email,
                // emptyList() is used because the update method does not use this field
                _trafficModelProvider = { emptyList() },
                // isEmailVerified is set to true because the update method does not use this field
                isEmailVerified = true,
                // passwordHash is set to "" because the update method does not use this field
                passwordHash = "", _profilePictureProvider = {
                    getProfilePictureImage(UserId(currentUser.id!!.id), updatedUser)
                },
                // isAdmin is set to false because the repository does not use this field
                isAdmin = false,
                // teamsProvider is set to emptyList() because the repository does not use this field
                teamsProvider = { emptyList() })
        ).toDTO(nextcloudBaseUrl).also { log.info(USER_UPDATED.format(currentUser.id?.id)) }
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param id The [UserId] of the user to retrieve.
     * @return The [UserDTO] object representing the user.
     *
     * @throws DataWithIdentifierNotFoundException If no user is found with the given ID.
     */
    fun getById(id: UserId): UserDTO {
        return userRepository.getById(id)?.toDTO(nextcloudBaseUrl) ?: throw DataWithIdentifierNotFoundException(
            "User", "id", id.id.toString()
        )
    }

    /**
     * Retrieves a user by their email.
     *
     * @param email The email of the user to retrieve.
     * @return The [UserDTO] object representing the user.
     *
     * @throws DataWithIdentifierNotFoundException If no user is found with the given email.
     */
    fun getByEmail(email: String): UserDTO {
        return userRepository.getByEmail(email)?.toDTO(nextcloudBaseUrl) ?: throw DataWithIdentifierNotFoundException(
            "User", "email", email
        )
    }

    /**
     * Changes the password of a user. The user must provide their old password to confirm the change.
     * The new password is hashed before being stored in the database.
     *
     * @param oldPassword The old password of the user.
     * @param newPassword The new password of the user.
     * @param userId The ID of the user changing their password.
     *
     * @throws [InvalidCredentialException] the new Password does not meet the requirements
     * @throws [IllegalArgumentException] if the old password is incorrect
     */
    fun changePassword(oldPassword: String, newPassword: String, userId: UserId) {
        userRepository.getById(userId)?.let { user ->
            // Check if the old password is correct
            require(BCrypt.checkpw(oldPassword, user.passwordHash)) {
                throw IllegalArgumentException(INVALID_PASSWORD)
            }

            RegisterDTO(
                username = user.name, email = user.email, password = newPassword
            ).validate()

            userRepository.changePassword(user.copy(passwordHash = hashPassword(newPassword)))
        } ?: throw DataWithIdentifierNotFoundException("User", "id", userId.id.toString())
    }

    /**
     * Deletes the (authenticated) user, including all of his traffic models. Deletes all
     * of his favorites, comments and ratings and removes him from all of his teams and removes
     * the teams if he is the owner of the team.
     *
     * @param userId The ID of the user to delete.
     * @param currentUser The [UserDTO] object representing the current user.
     */
    fun delete(userId: UserId, currentUser: UserDTO) {
        val userToDelete = userRepository.getById(userId) ?: throw DataWithIdentifierNotFoundException(
            "User",
            "id",
            userId.id.toString()
        )
        if (userId.id != currentUser.id) throw UnauthorizedException("${currentUser.id}", "delete User")

        commentRepository.getCommentsForUser(userToDelete._id!!).forEach { commentRepository.deleteComment(it.id) }
        ratingRepository.getRatingsForUser(userToDelete._id).forEach { ratingRepository.deleteRating(it) }
        favouriteRepository.getFavouritesByUserId(userId)
            .forEach { favouriteRepository.deleteFavourite(it.id!!, userId) }
        trafficModelRepository.getByUser(userId).forEach {
                it.id?.let { id -> trafficModelService.delete(id) }
            }
        userToDelete.teams.forEach {
            if (it.owner._id == userToDelete._id) {
                log.info(TEAM_DELETED_USER_OWNER_TEMPLATE.format(it.id, userToDelete.id))
                teamRepository.delete(it._id!!)
            } else {
                log.info(TEAM_MEMBER_DELETED_TEMPLATE.format(userToDelete.id, it.id))
                teamRepository.deleteTeamMember(it._id!!, userToDelete._id)
            }
        }

        userRepository.delete(userToDelete)

        log.info(DELETED_USER_MESSAGE.format(userId.id))
    }

    /**
     * Logs in a user by verifying their email and password, and generates a JWT token.
     *
     * @param email The email of the user attempting to log in.
     * @param password The password of the user attempting to log in.
     * @return A JWT token if the login is successful.
     *
     * @throws DataWithIdentifierNotFoundException If no user is found with the given email.
     * @throws IllegalArgumentException If the password is invalid.
     */
    fun login(email: String, password: String): AuthResponseDTO {
        val user = userRepository.getByEmail(email) ?: throw DataWithIdentifierNotFoundException("User", "email", email)

        require(BCrypt.checkpw(password, user.passwordHash)) {
            log.warn(USER_FAILED_LOGIN_MESSAGE.format(user.name))
            throw IllegalArgumentException(INVALID_PASSWORD)
        }

        val secret = environment.config.property("jwt.secret").getString()
        val issuer = environment.config.property("jwt.issuer").getString()
        val audience = environment.config.property("jwt.audience").getString()
        val expirationInSeconds = environment.config.property("jwt.expirationInSeconds").getString().toLong()

        val expiresAt = Instant.ofEpochMilli(System.currentTimeMillis() + expirationInSeconds * 1000);

        val token = JWT.create().withAudience(audience).withIssuer(issuer).withClaim("userId", user.id!!.id)
            .withClaim("username", user.name).withExpiresAt(expiresAt).sign(Algorithm.HMAC256(secret))

        log.info(USER_LOGGED_IN_MESSAGE.format(user.name));

        return AuthResponseDTO(token, expiresAt);
    }

    /**
     * Logs out a user by adding their JWT token to the blacklist.
     *
     * @param userName The username of the user logging out.
     * @param token The JWT token to be blacklisted.
     */
    fun logout(userName: String, token: String) {
        TokenBlackList.add(token)
        log.info(USER_LOGGED_OUT_MESSAGE.format(userName))
    }

    fun handleVerifyEmailLink(token: UUID) {
        TODO()
    }

    /**
     * Initiates the password reset by creating a token and sending it via E-Mail to the user.
     * This token is then accepted at [resetPasswordSetNew] for authenticating the new password.
     *
     * @param email The email of the user, whose password is to be changed.
     *
     * @throws DataWithIdentifierNotFoundException If no user is found with the given email.
     */
    fun resetPasswordWithEmail(email: String) {
        //Used to check if user exists. If not, an exception is thrown
        getByEmail(email)

        val linkWithToken = linkService.createLink(null, null, email, LinkType.PASSWORD_RESET);
        log.info(GENERATED_PASSWORD_RESET_LOG_MESSAGE.format(email, linkWithToken));
        emailService.sendEmail(EmailType.RESET_PASSWORD, listOf(email), linkWithToken)
    }

    /**
     * Receives and validates a token that was created for resetting a user's password.
     * If the token is valid then the user's password is replaced with the newly given one.
     *
     * @param token The UUID which references to the LinkData data class object.
     * @param newPassword The new password of the user.
     */
    fun resetPasswordSetNew(token: UUID, newPassword: String) {
        val tokenData = linkService.getLinkData(token)

        val user = userRepository.getByEmail(tokenData.email!!) ?: throw DataWithIdentifierNotFoundException(
            "user", "email", tokenData.email
        )
        val updatedUser = user.copy(passwordHash = hashPassword(newPassword))

        userRepository.changePassword(updatedUser)
        linkService.deleteLink(token)
    }

    /**
     * Hashes a password using BCrypt.
     *
     * @param password The password to hash.
     * @return The hashed password.
     */
    private fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    /**
     * Retrieves the profile picture of a user.
     *
     * @param userId The ID of the user to whom the profile picture belongs.
     * @param updatedUser The [UserDTO] object representing the user.
     *
     * @return The [Image] object representing the profile picture.
     *
     * @throws InvalidFileException If the image file is not valid.
     * @throws UnexpectedHttpResponse If the image file could not be uploaded.
     */
    private fun getProfilePictureImage(userId: UserId, updatedUser: UserDTO): Image? {
        return if (updatedUser.profilePicture != null && updatedUser.profilePicture.isNotEmpty()) {
            getDownloadableImage(
                userId, uploadProfilePicture(updatedUser.profilePicture, userId)
            )
        } else {
            userRepository.getById(userId)?.profilePicture
        }
    }

    /**
     * Uploads a profile picture to Nextcloud and returns the image object without the shareToken
     *
     * @param profilePicture The byte array representing the profile picture.
     * @param userId The ID of the user to whom the profile picture belongs.
     *
     * @return The [Image] object representing the profile picture.
     *
     * @throws InvalidFileException If the image file is not valid.
     * @throws UnexpectedHttpResponse If the image file could not be uploaded.
     */
    private fun uploadProfilePicture(profilePicture: ByteArray, userId: UserId): Image {
        val fileExtension = getImageExtension(profilePicture)
        if (fileExtension == null) {
            log.error(INVALID_IMAGE_FILE_FORMAT)
            throw InvalidFileException(INVALID_IMAGE_FILE_FORMAT)
        }

        val uploadDir = File(UPLOAD_DIR)
        if (!uploadDir.exists()) uploadDir.mkdirs()

        val fileId = UUID.randomUUID()
        val file = File(INTERNAL_FILE_PATH_TEMPLATE.format(fileId, fileExtension))
        file.writeBytes(profilePicture)

        try {
            fileHandler.uploadFile(
                file,
                NEXTCLOUD_IMG_FILE_PATH_TEMPLATE.format((userId.id / 100), (userId.id % 100), fileId, fileExtension)
            )
        } catch (e: Exception) {
            // The exception is rethrown to be handled by the caller
            throw e
        } finally {
            if (!file.delete()) {
                log.error(FILE_COULD_NOT_BE_DELETED)
            }
        }

        log.info(UPLOADED_PP_MESSAGE.format(userId.id))

        return Image(
            token = fileId,
            name = PROFILE_PICTURE_NAME.format(userId.id),
            fileExtension = fileExtension,
            shareToken = null
        )
    }

    /**
     * Retrieves the shareToken for the profile picture and returns the image object with the shareToken
     *
     * @param userId The ID of the user to whom the profile picture belongs.
     * @param image The [Image] object representing the profile picture.
     *
     * @return The [Image] object representing the profile picture with the shareToken.
     *
     * @throws UnexpectedHttpResponse If the shareToken could not be retrieved.
     */
    private fun getDownloadableImage(userId: UserId, image: Image): Image {
        return image.copy(
            shareToken = ShareToken(
                fileHandler.getDownloadReference(
                    NEXTCLOUD_IMG_FILE_PATH_TEMPLATE.format(
                        (userId.id / 100), (userId.id % 100), image.token, image.fileExtension
                    ), false
                ).shareToken
            )
        )
    }

    /**
     * Determines the image file extension based on the byte signature of the provided byte array.
     *
     * @param byteArray The byte array representing the beginning of the image file.
     * @return The file extension as a string (e.g., "jpg", "png", "gif") if a matching signature is found,
     *         or null if the signature is not recognized.
     */
    private fun getImageExtension(byteArray: ByteArray): String? {
        val signatures = mapOf(
            JPG_BYTE_SIGNATURE to JPG_FILE_EXTENSION,
            PNG_BYTE_SIGNATURE to PNG_FILE_EXTENSION,
        )

        val hexSignature =
            byteArray.take(4).joinToString("") { HEX_BYTE_FORMAT.format(it) } // Convert first 4 bytes to Hex

        return signatures.entries.firstOrNull { hexSignature.startsWith(it.key) }?.value
    }
}

