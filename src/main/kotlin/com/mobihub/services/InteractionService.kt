package com.mobihub.services

import com.mobihub.dtos.CommentDTO
import com.mobihub.dtos.CreateCommentDTO
import com.mobihub.dtos.RatingDTO
import com.mobihub.dtos.TrafficModelDTO
import com.mobihub.exceptions.DataWithIdentifierNotFoundException
import com.mobihub.exceptions.UnauthorizedException
import com.mobihub.model.*
import com.mobihub.model.CommentId
import com.mobihub.model.Rating
import com.mobihub.model.TrafficModelId
import com.mobihub.model.UserId
import com.mobihub.repositories.*
import io.ktor.server.application.*
import org.slf4j.LoggerFactory
import java.time.Instant

private const val RATING_INVALID_TEMPLATE = "Invalid rating amount %s"
private const val USER_NOT_EXIST_ERROR_TEMPLATE = "User with id %d does not exist"
private const val TRAFFIC_MODEL_NOT_EXIST_ERROR_TEMPLATE = "Traffic model with id %d does not exist"

private const val ADDED_COMMENT_MESSAGE = "New comment created and added. id: %s"
private const val UPDATED_COMMENT_MESSAGE = "Comment updated. id: %s"
private const val DELETED_COMMENT_MESSAGE = "Comment deleted. id: %s"
private const val COMMENT_ID_NULL_ERROR = "Comment ID cannot be null"
private const val DELETE_COMMENT_CONTEXT = "Delete comment"

private const val REMOVE_FAVOURITE_LOG_MESSAGE = "User with id %d removed traffic model with id %d from favourites"
private const val ADD_FAVOURITE_LOG_MESSAGE = "User with id %d added traffic model with id %d to favourites"

private const val USER_NOT_FOUND_ERROR = "User with id %d not found"

/**
 * Service for handling interactions with traffic models
 *
 * @property ratingRepository the rating repository
 * @property favouriteRepository the favourite repository
 * @property commentRepository the comments repository
 * @property userRepository the user repository
 * @property trafficModelRepository the traffic model repository
 * @property environment the application environment
 *
 * @author Team-MobiHub
 */
class InteractionService(
    val ratingRepository: RatingRepository,
    val favouriteRepository: FavouriteRepository,
    val commentRepository: CommentRepository,
    val userRepository: UserRepository,
    val trafficModelRepository: TrafficModelRepository,
    private val environment: ApplicationEnvironment
) {
    private val log = LoggerFactory.getLogger(InteractionService::class.java)
    private val dmsBaseUrl = environment.config.property("nextcloud.baseUrl").getString()

    /**
     * Creates a new Comment and adds it to the Comment of the TM
     *
     * @param commentRequest the [CreateCommentDTO] that holds the necessary information to create a new Comment
     * @return the [CommentDTO] representation of the newly created Comment
     * @throws IllegalArgumentException if the Comment content is null, or user/traffic model not found
     */
    fun addComment(commentRequest: CreateCommentDTO): CommentDTO {
        commentRequest.validate()

        val newComment = commentRepository.addComment(
            createCommentDtoToComment(commentRequest)
        )

        log.info(ADDED_COMMENT_MESSAGE.format(newComment.id.id))
        return newComment.toDTO()
    }

    /**
     * Updates an existing Comment
     *
     * @param commentRequest the [CreateCommentDTO] that holds the necessary information to update a Comment
     * @return the [CommentDTO] representation of the updated Comment
     */
    fun updateComment(commentRequest: CreateCommentDTO): CommentDTO {
        commentRequest.validate()

        // additionally check that comment actually exists
        require(commentRequest.id != null) { COMMENT_ID_NULL_ERROR }
        if(commentRepository.getCommentById(CommentId(commentRequest.id)) == null) {
            throw DataWithIdentifierNotFoundException("Comment", "id", commentRequest.id.toString())
        }

        val updatedComment = commentRepository.updateComment(
            createCommentDtoToComment(commentRequest)
        )
        log.info(UPDATED_COMMENT_MESSAGE.format(updatedComment.id.id))
        return updatedComment.toDTO()
    }

    /**
     * Converts a [CreateCommentDTO] to a [Comment]
     *
     * @param commentRequest the [CreateCommentDTO] to convert
     * @return the [Comment] representation of the [CreateCommentDTO]
     */
    private fun createCommentDtoToComment(commentRequest: CreateCommentDTO): Comment {
        return Comment(
            id = CommentId(commentRequest.id ?: 0),
            content = commentRequest.content,
            creationDate = Instant.now(),
            trafficModelProvider = {
                val trafficModel = trafficModelRepository.getById(TrafficModelId(commentRequest.trafficModelId))
                require(trafficModel != null) {
                    TRAFFIC_MODEL_NOT_EXIST_ERROR_TEMPLATE.format(commentRequest.trafficModelId)
                }
                trafficModel
            },
            userProvider = {
                val user = userRepository.getById(UserId(commentRequest.userId))
                require(user != null) {
                    USER_NOT_EXIST_ERROR_TEMPLATE.format(commentRequest.userId)
                }
                user
            }
        )
    }

    /**
     * Deletes a Comment based on its ID
     *
     * @param commentId the ID of the Comment to be deleted
     * @param userId the ID of the User that wants to delete the Comment
     */
    fun deleteComment(commentId: CommentId, userId: UserId) {
        if (commentRepository.getCommentById(commentId) == null)
            throw DataWithIdentifierNotFoundException("Comment", "id", commentId.id.toString())
        if(!commentRepository.getCommentsForUser(userId).map { it.id }.contains(commentId))
            throw UnauthorizedException(userId.toString(), DELETE_COMMENT_CONTEXT)

        commentRepository.deleteComment(commentId)
        log.info(DELETED_COMMENT_MESSAGE.format(commentId.id))
    }

    /**
     * Adds a rating to a traffic model
     *
     * @param trafficModelId the traffic model id
     * @param rating the rating
     * @param userId the userId that adds the rating
     *
     * @return the created rating
     */
    fun addRating(trafficModelId: TrafficModelId, rating: Int, userId: UserId): RatingDTO {
        return createOrUpdateRating(ratingRepository::addRating, trafficModelId, rating, userId)
    }

    /**
     * Updates a rating
     *
     * @param trafficModelId the traffic model id
     * @param rating the rating
     * @param userId the user id
     *
     * @return the updated rating
     */
    fun updateRating(trafficModelId: TrafficModelId, rating: Int, userId: UserId): RatingDTO {
        return createOrUpdateRating(ratingRepository::updateRating, trafficModelId, rating, userId)
    }

    /**
     * Deletes a rating
     *
     * @param trafficModelId the traffic model id
     * @param userId the user id
     *
     * @throws DataWithIdentifierNotFoundException if the traffic model does not exist
     */
    fun deleteRating(trafficModelId: TrafficModelId, userId: UserId): Double {
        if (trafficModelRepository.getById(trafficModelId) == null)
            throw DataWithIdentifierNotFoundException("Trafficmodel", "id", trafficModelId.id.toString())
        if (userRepository.getById(userId) == null)
            throw DataWithIdentifierNotFoundException("User", "id", userId.id.toString())

        ratingRepository.deleteRating(
            Rating(
                rating = 0,
                trafficModelProvider = { trafficModelRepository.getById(trafficModelId)!! }, // assert because of check
                userProvider = { userRepository.getById(userId)!! } // assert because of check
            )
        )
        return ratingRepository.getAverageRatingForTrafficModel(trafficModelId)
    }

    /**
     * Adds a traffic model to the favourites of a user.
     *
     * @param trafficModelId the traffic model id
     * @param userId the user id
     *
     * @throws DataWithIdentifierNotFoundException if the user or traffic model does not exist
     */
    fun addFavourite(trafficModelId: TrafficModelId, userId: UserId) {
        validateFavorite(trafficModelId, userId)

        favouriteRepository.addFavourite(trafficModelId, userId)
        log.info(ADD_FAVOURITE_LOG_MESSAGE.format(userId.id, trafficModelId.id))
    }

    /**
     * Removes a traffic model from the favourites of a user
     *
     * @param trafficModelId the traffic model id
     * @param userId the user id
     * @throws DataWithIdentifierNotFoundException if the user or traffic model does not exist
     */
    fun removeFavourite(trafficModelId: TrafficModelId, userId: UserId) {
        validateFavorite(trafficModelId, userId)

        favouriteRepository.deleteFavourite(trafficModelId, userId)
        log.info(REMOVE_FAVOURITE_LOG_MESSAGE.format(userId.id, trafficModelId.id))
    }

    /**
     * Gets the favourites of a user
     *
     * @param userId the user id
     * @return the list of traffic models
     */
    fun getFavouritesOfUser(userId: UserId): List<TrafficModelDTO> {
        return favouriteRepository.getFavouritesByUserId(userId).map { it.toDTO(userId, dmsBaseUrl) }
    }

    /**
     * Validates if the user and traffic model exist
     *
     * @param trafficModelId the traffic model id
     * @param userId the user id
     * @throws DataWithIdentifierNotFoundException if the user or traffic model does not exist
     */
    private fun validateFavorite(trafficModelId: TrafficModelId, userId: UserId) {
        if (userRepository.getById(userId) == null) {
            log.info(USER_NOT_FOUND_ERROR.format(userId.id))
            throw DataWithIdentifierNotFoundException(
                dataType = "User",
                identifierType = "userId",
                value = userId.id.toString()
            )
        }
        if (trafficModelRepository.getById(trafficModelId) == null) {
            log.info(TRAFFIC_MODEL_NOT_EXIST_ERROR_TEMPLATE.format(trafficModelId.id))
            throw DataWithIdentifierNotFoundException(
                dataType = "TrafficModel",
                identifierType = "trafficModelId",
                value = trafficModelId.id.toString()
            )
        }
    }

    /**
     * Creates or updates a rating. This method is a helper function for addRating and updateRating.
     *
     * @param repositoryMethod the repository method to call
     * @param trafficModelId the traffic model id
     * @param rating the rating
     * @param userId the user id
     */
    private fun createOrUpdateRating(
        repositoryMethod: (Rating) -> Rating,
        trafficModelId: TrafficModelId,
        rating: Int,
        userId: UserId
    ): RatingDTO {
        require(rating in 0..5) { RATING_INVALID_TEMPLATE.format(rating) }

        if (trafficModelRepository.getById(trafficModelId) == null)
            throw DataWithIdentifierNotFoundException("TrafficModel", "id", trafficModelId.id.toString())
        if (userRepository.getById(userId) == null)
            throw DataWithIdentifierNotFoundException("User", "id", userId.id.toString())

        return repositoryMethod(
            Rating(
                rating = rating,
                trafficModelProvider = { trafficModelRepository.getById(trafficModelId)!! }, // assert because of check
                userProvider = { userRepository.getById(userId)!! } // assert because of check
            )
        ).toDTO(ratingRepository.getAverageRatingForTrafficModel(trafficModelId))
    }
}
