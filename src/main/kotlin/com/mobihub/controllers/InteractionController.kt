package com.mobihub.controllers

import com.mobihub.dtos.RatingDTO
import com.mobihub.dtos.TrafficModelDTO
import com.mobihub.dtos.UserDTO
import com.mobihub.exceptions.DataWithIdentifierNotFoundException
import com.mobihub.model.TrafficModelId
import com.mobihub.model.UserId
import com.mobihub.dtos.CommentDTO
import com.mobihub.dtos.CreateCommentDTO
import com.mobihub.exceptions.UnauthorizedException
import com.mobihub.model.CommentId
import com.mobihub.services.InteractionService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.util.reflect.*
import io.ktor.server.request.*
import org.slf4j.LoggerFactory

private const val MISSING_TRAFFIC_MODEL_ID_ERROR = "Traffic model id is missing"
private const val TRAFFIC_MODEL_NAN_ERROR = "Traffic model id is not a number"
private const val USER_NOT_AUTHENTICATED_ERROR = "User is not authenticated"
private const val RATING_UPDATED_TEMPLATE = "A rating from User %s has been updated, TM %s"
private const val RATING_DELETED_TEMPLATE = "A rating from User %s has been deleted, TM %s"

private const val FAVORITES_OF_USER_FETCHED_TEMPLATE = "Favorites of user %s have been fetched."

/**
 * Controller for handling requests related to interactions in the REST API.
 *
 * This class contains functions for adding, deleting and updating Favorites and Comments and Ratings
 * and for fetching favorites by user or by traffic model.
 *
 * @property [interactionService] the [interactionService] object to use for user operations
 * @author Team-MobiHub
 */
class InteractionController(
    private val interactionService: InteractionService
) {
    private val log = LoggerFactory.getLogger(InteractionController::class.java)

    /**
     * add a comment to a traffic model and a User
     * @param ctx to receive a CreateCommentDTO
     */
    suspend fun addComment(ctx: ApplicationCall) {
        try {
            val createComment = ctx.receive<CreateCommentDTO>()
            if (ctx.principal<UserDTO>()?.id != createComment.userId) ctx.respond(
                HttpStatusCode.Unauthorized,
                typeInfo<HttpStatusCode>()
            )

            val commentDTO = interactionService.addComment(createComment)
            ctx.respond(commentDTO, typeInfo = typeInfo<CommentDTO>())
        } catch (e: ContentTransformationException) {
            log.error(e.message)
            ctx.respond(HttpStatusCode.BadRequest, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: IllegalArgumentException) {
            log.error(e.message)
            ctx.respondText(e.message ?: "", status = HttpStatusCode.BadRequest)
        } catch (e: Exception) {
            log.error(e.message)
            ctx.respond(HttpStatusCode.InternalServerError, typeInfo = typeInfo<HttpStatusCode>())
        }
    }

    /**
     * update a comment to a traffic model
     * @param ctx to receive a CreateCommentDTO
     */
    suspend fun updateComment(ctx: ApplicationCall) {
        try {
            val createComment = ctx.receive<CreateCommentDTO>()
            // only the user himself can change his comments
            if (ctx.principal<UserDTO>()?.id != createComment.userId) ctx.respond(
                HttpStatusCode.Unauthorized,
                typeInfo<HttpStatusCode>()
            )

            val commentDTO = interactionService.updateComment(createComment)
            ctx.respond(commentDTO, typeInfo = typeInfo<CommentDTO>())
        } catch (e: ContentTransformationException) {
            log.error(e.message)
            ctx.respond(HttpStatusCode.BadRequest, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: DataWithIdentifierNotFoundException) {
            log.error(e.message)
            ctx.respondText(e.message, status = HttpStatusCode.NotFound)
        } catch (e: IllegalArgumentException) {
            log.error(e.message)
            ctx.respondText(e.message ?: "", status = HttpStatusCode.BadRequest)
        } catch (e: Exception) {
            ctx.respond(HttpStatusCode.InternalServerError, typeInfo<HttpStatusCode>())
        }
    }

    /**
     * delete a comment to a traffic model
     * @param ctx to receive a commentId and userId ???
     */
    suspend fun deleteComment(ctx: ApplicationCall) {
        try {
            val commentId = ctx.parameters["commentId"]?.toInt()
            interactionService.deleteComment(CommentId(commentId!!), UserId(ctx.principal<UserDTO>()?.id!!))
            ctx.respond(HttpStatusCode.OK, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: NumberFormatException) {
            log.error(e.message)
            ctx.respond(HttpStatusCode.BadRequest, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: UnauthorizedException) {
            log.error(e.message)
            ctx.respondText(e.message, status = HttpStatusCode.Unauthorized)
        } catch (e: DataWithIdentifierNotFoundException) {
            log.error(e.message)
            ctx.respondText(e.message, status = HttpStatusCode.NotFound)
        } catch (e: Exception) {
            log.error(e.message)
            ctx.respond(HttpStatusCode.InternalServerError, typeInfo<HttpStatusCode>())
        }
    }

    /**
     * Adds a rating to a traffic model.
     *
     * This function receives the traffic model ID and rating from the request parameters,
     * and the user ID from the authenticated user. It then adds the rating using the
     * [InteractionService] and responds with the created [RatingDTO] object.
     * The function responds with a BadRequest, if any Ids are not numbers or
     * the rating is not between 0 and 5 inclusive. It also responds with NotFound if the Ids
     * could not be found, or with InternalServerError else.
     *
     * @param ctx The [ApplicationCall] context for the request.
     */
    suspend fun addRating(ctx: ApplicationCall) {
        createOrUpdateRating(interactionService::addRating, ctx)
    }

    /**
     * Updates an existing rating to a traffic model.
     *
     * This function receives the traffic model ID and rating from the request parameters,
     * and the user ID from the authenticated user. It then updates the rating using the
     * [InteractionService] and responds with the updated [RatingDTO] object.
     * The function responds with a BadRequest, if any Ids are not numbers or
     * the rating is not between 0 and 5 inclusive. It also responds with NotFound if the Ids
     * could not be found, or with InternalServerError else.
     *
     * @param ctx The [ApplicationCall] context for the request.
     */
    suspend fun updateRating(ctx: ApplicationCall) {
        createOrUpdateRating(interactionService::updateRating, ctx)
    }

    /**
     * Deletes an existing rating to a traffic model.
     *
     * This function receives the traffic model ID and rating from the request parameters,
     * and the user ID from the authenticated user. It then deletes the rating using the
     * [InteractionService] and responds with OK.
     * The function responds with a BadRequest, if any Ids are not numbers.
     * It also responds with NotFound if the Ids could not be found, or with
     * InternalServerError else.
     *
     * @param ctx The [ApplicationCall] context for the request.
     */
    suspend fun deleteRating(ctx: ApplicationCall) {
        try {
            val trafficModelId = TrafficModelId(ctx.parameters["trafficModelId"]?.toInt()!!)
            val userId = UserId(ctx.principal<UserDTO>()?.id!!)
            val averageRating = interactionService.deleteRating(trafficModelId, userId)
            ctx.respond(averageRating, typeInfo<Double>())
            log.info(RATING_DELETED_TEMPLATE.format(userId.id, trafficModelId))
        } catch (e: Exception) {
            log.error(e.message, e)
            when (e) {
                is NumberFormatException -> ctx.respondText(e.message ?: "", status = HttpStatusCode.BadRequest)
                is DataWithIdentifierNotFoundException -> ctx.respondText(e.message, status = HttpStatusCode.NotFound)
                else -> ctx.respond(HttpStatusCode.InternalServerError, typeInfo<HttpStatusCode>())
            }
        }
    }

    /**
     * Adds a traffic model to the favourites of a user.
     *
     * @param ctx The application call context
     */
    suspend fun makeToFavourite(ctx: ApplicationCall) {
        addOrDeleteFavorite(interactionService::addFavourite, ctx)
    }

    /**
     * Removes a traffic model from the favourites of a user.
     *
     * @param ctx The application call context
     */
    suspend fun removeFavourite(ctx: ApplicationCall) {
        addOrDeleteFavorite(interactionService::removeFavourite, ctx)
    }

    /**
     * Retrieves the list of favorite traffic models for the authenticated user.
     * @param ctx The application call context containing the request and response.
     */
    suspend fun getFavoritesOfUser(ctx: ApplicationCall) {
        try {
            val userDTO = ctx.principal<UserDTO>()
            ctx.respond(
                interactionService.getFavouritesOfUser(UserId(userDTO?.id!!)),
                typeInfo = typeInfo<List<TrafficModelDTO>>()
            )
            log.info(FAVORITES_OF_USER_FETCHED_TEMPLATE.format(userDTO.id))
        } catch (e: Exception) {
            log.error(e.message, e)
            when (e) {
                is DataWithIdentifierNotFoundException -> ctx.respondText(e.message, status = HttpStatusCode.BadRequest)
                else -> ctx.respond(HttpStatusCode.InternalServerError, typeInfo = typeInfo<HttpStatusCode>())
            }
        }
    }

    /**
     * adds or removes a favorite traffic model for a user, depending on the service method
     *
     * @param serviceMethod the service method to use for adding or deleting the favorite
     * @param ctx the application call context
     */
    private suspend fun addOrDeleteFavorite(
        serviceMethod: (trafficModelId: TrafficModelId, userId: UserId) -> Unit, ctx: ApplicationCall
    ) {
        if (ctx.parameters["trafficModelId"] == null) {
            log.info(MISSING_TRAFFIC_MODEL_ID_ERROR)
            ctx.respond(HttpStatusCode.BadRequest, typeInfo = typeInfo<HttpStatusCode>())
            return
        }

        val trafficModelId = try {
            TrafficModelId(ctx.parameters["trafficModelId"]?.toInt()!!)
        } catch (e: NumberFormatException) {
            log.info(TRAFFIC_MODEL_NAN_ERROR)
            ctx.respond(HttpStatusCode.BadRequest, typeInfo = typeInfo<HttpStatusCode>())
            return
        }

        val userId = ctx.principal<UserDTO>()?.id

        if (userId == null) {
            log.info(USER_NOT_AUTHENTICATED_ERROR)
            ctx.respond(HttpStatusCode.Unauthorized, typeInfo = typeInfo<HttpStatusCode>())
            return
        }

        try {
            serviceMethod(
                trafficModelId, UserId(userId)
            )
            ctx.respond(HttpStatusCode.OK, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: DataWithIdentifierNotFoundException) {
            ctx.respond(HttpStatusCode.NotFound, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: Exception) {
            ctx.respond(HttpStatusCode.InternalServerError, typeInfo = typeInfo<HttpStatusCode>())
        }
    }

    /**
     * Creates or updates a rating for a traffic model.
     *
     * @param serviceMethod the service method to use for adding or updating the rating
     * @param ctx the application call context
     */
    private suspend fun createOrUpdateRating(
        serviceMethod: (trafficModelId: TrafficModelId, rating: Int, userId: UserId) -> RatingDTO, ctx: ApplicationCall
    ) {
        try {
            val trafficModelId = TrafficModelId(ctx.parameters["trafficModelId"]?.toInt()!!)
            val rating = ctx.parameters["rating"]?.toInt()!!
            val userId = UserId(ctx.principal<UserDTO>()?.id!!)
            val ratingDto = serviceMethod(trafficModelId, rating, userId)
            ctx.respond(
                ratingDto, typeInfo<RatingDTO>()
            ).also {
                log.info(RATING_UPDATED_TEMPLATE.format(userId.id, trafficModelId.id))
                log.info(ratingDto.toString())
            }
        } catch (e: Exception) {
            log.error(e.message, e)
            when (e) {
                is NumberFormatException, is IllegalArgumentException -> ctx.respondText(
                    e.message ?: "", status = HttpStatusCode.BadRequest
                )

                is DataWithIdentifierNotFoundException -> ctx.respondText(e.message, status = HttpStatusCode.NotFound)
                else -> ctx.respond(HttpStatusCode.InternalServerError, typeInfo<HttpStatusCode>())
            }
        }
    }
}