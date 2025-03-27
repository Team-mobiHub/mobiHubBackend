package com.mobihub.model

import com.mobihub.dtos.CommentDTO
import java.time.Instant

private const val TRAFFIC_MODEL_ID_IS_NULL_ERROR = "Traffic model id is null"

private const val USER_ID_IS_NULL_ERROR = "User id is null"

/**
 * Represents a comment made by a user on a traffic model.
 *
 * @property id the unique identifier of the comment
 * @property content the content of the comment
 * @property creationDate the date the comment was created
 * @property trafficModel the traffic model the comment is associated with. The traffic model is lazily loaded
 * @property user the user who made the comment on the traffic model. The user is lazily loaded
 *
 * @author Team-MobiHub
 */
data class Comment(
    val id: CommentId,
    val content: String,
    val creationDate: Instant,
    val trafficModelProvider: () -> TrafficModel,
    val userProvider: () -> User
) {
    val trafficModel: TrafficModel by lazy(trafficModelProvider);
    val user: User by lazy(userProvider)

    /**
     * Converts the comment to a DTO.
     *
     * @return the DTO representation of the comment
     */
    fun toDTO(): CommentDTO {
        requireNotNull(trafficModel.id?.id) { TRAFFIC_MODEL_ID_IS_NULL_ERROR }
        requireNotNull(user.id) { USER_ID_IS_NULL_ERROR }
        return CommentDTO(
            id = id.id,
            content = content,
            creationDate = creationDate,
            trafficModelId = trafficModel.id!!.id,
            userId = user.id!!.id,
            username = user.name
        )
    }
}