package com.mobihub.controllers

import com.mobihub.dtos.*
import com.mobihub.exceptions.DataAlreadyExistsException
import com.mobihub.exceptions.DataWithIdentifierNotFoundException
import com.mobihub.exceptions.UnauthorizedException
import com.mobihub.model.UserId
import com.mobihub.services.UserService
import com.mobihub.utils.verifier.exceptions.InvalidCredentialException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.reflect.*
import java.util.*
import org.slf4j.LoggerFactory

private const val UPDATE_ERROR_MESSAGE = "Error while updating user: %s"
private const val CREATE_ERROR_MESSAGE = "Error while creating user: %s"

private const val USER_DELETED_TEMPLATE = "User %s has been deleted."
private const val USER_DELETED_ERROR_TEMPLATE = "Error while trying to delete user: %s"
private const val MESSAGE_INVALID_LOGIN_DATA = "Invalid login data."
private const val USER_FETCHED_BY_ID_TEMPLATE = "User %s was fetched"
private const val PASSWORD_SET_LOG_MESSAGE = "User password has been reset"

/**
 * Controller for handling user-related requests in the REST API.
 *
 * This class contains functions for creating, updating, and deleting users, as well as handling
 * user login and logout, password changes, and email verification.
 *
 * @property [userService] the [UserService] object to use for user operations
 * @author Team-MobiHub
 */
class UserController(private val userService: UserService) {
    private val log = LoggerFactory.getLogger(UserController::class.java)

    /**
     * Creates a new user.
     *
     * This function receives a `RegisterDTO` object from the request, attempts to create a new user
     * using the `UserService`, and responds with the created `UserDTO` object. If an
     * `IllegalArgumentException` is thrown, it responds with a `BadRequest` status. For any other
     * exceptions, it responds with an `InternalServerError` status.
     *
     * @param ctx the [ApplicationCall] context for the request
     */
    suspend fun create(ctx: ApplicationCall) {
        try {
            val registerDTO = ctx.receive<RegisterDTO>()
            val userDTO = userService.create(registerDTO)
            ctx.respond(userDTO, typeInfo = typeInfo<UserDTO>())
        } catch (e: Exception) {
            log.info(CREATE_ERROR_MESSAGE.format(e.message), e)
            when (e) {
                is ContentTransformationException ->
                    ctx.respond(HttpStatusCode.BadRequest, typeInfo<HttpStatusCode>())

                is InvalidCredentialException ->
                    ctx.respondText(e.message, status = HttpStatusCode.BadRequest)

                is DataAlreadyExistsException ->
                    ctx.respondText(e.message, status = HttpStatusCode.Conflict)

                else ->
                    ctx.respond(
                        HttpStatusCode.InternalServerError,
                        typeInfo = typeInfo<HttpStatusCode>()
                    )
            }
        }
    }

    /**
     * Updates an already existing user.
     *
     * This function receives a `UserDTO` object from the request, attempts to update the user using
     * the `UserService`, and responds with the updated `UserDTO` object. If an
     * `IllegalArgumentException` is thrown, it responds with a `BadRequest` status. For any other
     * exceptions, it responds with an `InternalServerError` status.
     *
     * @param ctx the [ApplicationCall] context for the request
     */
    suspend fun update(ctx: ApplicationCall) {
        try {
            ctx.respond(
                userService.update(ctx.receive<UserDTO>(), ctx.principal<UserDTO>()!!),
                typeInfo = typeInfo<UserDTO>()
            )
        } catch (e: Exception) {
            log.info(UPDATE_ERROR_MESSAGE.format(e.message), e)
            when (e) {
                is ContentTransformationException ->
                    ctx.respond(
                        HttpStatusCode.BadRequest,
                        typeInfo = typeInfo<HttpStatusCode>()
                    )
                is IllegalArgumentException ->
                    ctx.respond(HttpStatusCode.BadRequest, typeInfo = typeInfo<HttpStatusCode>())

                is InvalidCredentialException ->
                    ctx.respondText(e.message, status = HttpStatusCode.BadRequest)

                is DataAlreadyExistsException ->
                    ctx.respondText(e.message, status = HttpStatusCode.Conflict)

                else ->
                    ctx.respond(
                        HttpStatusCode.InternalServerError,
                        typeInfo = typeInfo<HttpStatusCode>()
                    )
            }
        }
    }

    /**
     * Retrieves a user by their ID.
     *
     * This function receives a user ID from the request, attempts to retrieve the user using the
     * `UserService`, and responds with the `UserDTO` object. If an `IllegalArgumentException` is
     * thrown, it responds with a `BadRequest` status. For any other exceptions, it responds with an
     * `InternalServerError` status.
     *
     * @param ctx the [ApplicationCall] context for the request
     */
    suspend fun getById(ctx: ApplicationCall) {
        try {
            val userDTO = userService.getById(UserId(ctx.parameters["userId"]?.toInt()!!))
            ctx.respond(userDTO, typeInfo = typeInfo<UserDTO>())
            log.info(USER_FETCHED_BY_ID_TEMPLATE.format(userDTO.id.toString()))
        } catch (e: Exception) {
            log.error(e.message, e)
            when (e) {
                is IllegalArgumentException, is ContentTransformationException ->
                    ctx.respond(
                        HttpStatusCode.BadRequest,
                        typeInfo = typeInfo<HttpStatusCode>()
                    )

                else ->
                    ctx.respond(
                        HttpStatusCode.InternalServerError,
                        typeInfo = typeInfo<HttpStatusCode>()
                    )
            }
        }
    }

    /**
     * Changes the password of the authenticated user. This function receives the old and new
     * passwords from the request parameters.
     *
     * @param ctx the [ApplicationCall] context for the request
     */
    suspend fun changePassword(ctx: ApplicationCall) {
        // Check if the user is authenticated
        val user = ctx.authentication.principal() as UserDTO?
        if (user?.id == null) {
            ctx.respond(HttpStatusCode.Unauthorized)
            return
        }

        // Try changing the password
        try {
            val changePasswordData = ctx.receive<ChangePasswordDTO>()
            userService.changePassword(
                changePasswordData.oldPassword,
                changePasswordData.newPassword,
                UserId(user.id)
            )
            ctx.respond(HttpStatusCode.OK, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: IllegalArgumentException) {
            log.error(e.message)
            ctx.respondText(
                e.message ?: MESSAGE_INVALID_LOGIN_DATA,
                status = HttpStatusCode.BadRequest
            )
        } catch (e: InvalidCredentialException) {
            log.error(e.message)
            ctx.respondText(e.message, status = HttpStatusCode.BadRequest)
        } catch (e: DataWithIdentifierNotFoundException) {
            log.error(e.message)
            ctx.respondText(e.message, status = HttpStatusCode.NotFound)
        } catch (e: Exception) {
            log.error(e.message)
            ctx.respond(HttpStatusCode.InternalServerError, typeInfo = typeInfo<HttpStatusCode>())
        }
    }

    /**
     * Deletes a user and all of his traffic models.
     *
     * This function receives the user ID from the request parameters, attempts to delete the
     * corresponding user using the `UserService`, and responds with `BadRequest`, if the user to
     * delete has not been found, else with an internal server error.
     *
     * @param ctx the [ApplicationCall] context for the request
     */
    suspend fun delete(ctx: ApplicationCall) {
        try {
            val currentUser = ctx.principal<UserDTO>()
            val userId = ctx.parameters["userId"]?.toInt()
            userService.delete(UserId(userId!!), currentUser!!)
            ctx.respond(HttpStatusCode.OK, typeInfo<HttpStatusCode>())
            log.info(USER_DELETED_TEMPLATE.format(userId))
        } catch (e: Exception) {
            log.error(USER_DELETED_ERROR_TEMPLATE.format(e.message), e)
            when (e) {
                is UnauthorizedException ->
                    ctx.respondText(e.message, status = HttpStatusCode.Unauthorized)

                is DataWithIdentifierNotFoundException ->
                    ctx.respondText(e.message, status = HttpStatusCode.NotFound)

                is NumberFormatException ->
                    ctx.respond(status = HttpStatusCode.BadRequest, typeInfo<HttpStatusCode>())
            }
        }
    }

    /**
     * Handles user login.
     *
     * This function receives a `LoginDTO` object from the request, attempts to authenticate the
     * user using the `UserService`, and responds with a token if successful. If the login data is
     * invalid or the user is not found, it responds with the appropriate HTTP status code.
     *
     * @param ctx the [ApplicationCall] context for the request
     */
    suspend fun login(ctx: ApplicationCall) {
        try {
            val loginData = ctx.receive<LoginDTO>()
            val authResponse = userService.login(loginData.email, loginData.password)
            ctx.respond(authResponse, typeInfo = typeInfo<AuthResponseDTO>())
        } catch (e: IllegalArgumentException) {
            log.error(e.message)
            ctx.respondText(
                e.message ?: MESSAGE_INVALID_LOGIN_DATA,
                status = HttpStatusCode.BadRequest
            )
        } catch (e: DataWithIdentifierNotFoundException) {
            log.error(e.message)
            ctx.respondText(e.message, status = HttpStatusCode.NotFound)
        } catch (e: Exception) {
            log.error(e.message)
            ctx.respond(HttpStatusCode.InternalServerError, typeInfo = typeInfo<HttpStatusCode>())
        }
    }

    /**
     * Handles user logout.
     *
     * This function retrieves the authenticated user and the token from the request, and attempts
     * to log out the user using the `UserService`. If the user or token is not found, it responds
     * with an `Unauthorized` status. For any other exceptions, it responds with an
     * `InternalServerError` status.
     *
     * @param ctx the [ApplicationCall] context for the request
     */
    suspend fun logout(ctx: ApplicationCall) {
        try {
            val user = ctx.authentication.principal() as UserDTO?
            val token = ctx.request.header("Authorization")?.removePrefix("Bearer ")
            if (user == null || token == null) {
                ctx.respond(HttpStatusCode.Unauthorized)
                return
            }
            userService.logout(user.name, token)
            ctx.respond(HttpStatusCode.OK, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: IllegalArgumentException) {
            log.error(e.message)
            ctx.respond(HttpStatusCode.BadRequest, typeInfo<HttpStatusCode>())
        } catch (e: Exception) {
            log.error(e.message)
            ctx.respond(HttpStatusCode.InternalServerError, typeInfo = typeInfo<HttpStatusCode>())
        }
    }

    fun handleEmailVerificationFromLink(ctx: ApplicationCall) {
        TODO()
    }

    /**
     * <<<<<<< HEAD Starts the password reset process for a user by sending a reset password link to
     * their email.
     *
     * @param ctx The [ApplicationCall] containing the request context.
     * @throws IllegalArgumentException If the email parameter is missing.
     * @throws Exception If an error occurs during the process. ======= Starts the process of
     * resetting a user's password. This function receives the user's email from the request
     * parameters, attempts to start the password reset process using the `UserService`,
     *
     * @param ctx the [ApplicationCall] context for the request >>>>>>> main
     */
    suspend fun startResetPasswordProcess(ctx: ApplicationCall) {
        try {
            val email = ctx.parameters["email"] ?: throw IllegalArgumentException()
            userService.resetPasswordWithEmail(email)
            ctx.respond(HttpStatusCode.OK, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: DataWithIdentifierNotFoundException) {
            log.error(e.message)
            ctx.respond(HttpStatusCode.NotFound, typeInfo<HttpStatusCode>())
        } catch (e: Exception) {
            log.error(e.message)
            ctx.respond(HttpStatusCode.BadRequest, typeInfo<HttpStatusCode>())
        }
    }

    /**
     * <<<<<<< HEAD Completes the password reset process by setting a new password for the user.
     *
     * @param ctx The [ApplicationCall] containing the request context.
     * @throws IllegalArgumentException If the token or new password is missing.
     * @throws Exception If an error occurs during the process. ======= Sets a new password for a
     * user after the password reset process. This function receives the user's token and the new
     * password from the request parameters, attempts to set the new password using the
     * `UserService`,
     *
     * @param ctx the [ApplicationCall] context for the request >>>>>>> main
     */
    suspend fun resetPasswordSetNew(ctx: ApplicationCall) {
        try {
            val token = ctx.parameters["uuid"] ?: throw IllegalArgumentException()
            val password = ctx.receive<NewPasswordDTO>().newPassword
            userService.resetPasswordSetNew(UUID.fromString(token), password)
            log.info(PASSWORD_SET_LOG_MESSAGE)
            ctx.respond(HttpStatusCode.OK, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: Exception) {
            log.error(e.message)
            ctx.respond(HttpStatusCode.BadRequest, typeInfo<HttpStatusCode>())
        }
    }

    /**
     * Retrieves a user by their email.
     *
     * This function receives the email from the request parameters, attempts to retrieve the
     * corresponding user using the `UserService`, and responds with the `UserDTO` object. If the
     * email is invalid, it responds with a `BadRequest` status. For any other exceptions, it
     * responds with an `InternalServerError` status.
     *
     * @param call the [ApplicationCall] context for the request
     */
    suspend fun getByEmail(call: ApplicationCall) {
        try {
            val email = call.parameters["email"] ?: throw IllegalArgumentException()
            val userDTO = userService.getByEmail(email)
            call.respond(userDTO, typeInfo = typeInfo<UserDTO>())
        } catch (e: IllegalArgumentException) {
            log.error(e.message)
            call.respond(HttpStatusCode.BadRequest, typeInfo<HttpStatusCode>())
        } catch (e: Exception) {
            log.error(e.message)
            call.respond(HttpStatusCode.InternalServerError, typeInfo = typeInfo<HttpStatusCode>())
        }
    }
}
