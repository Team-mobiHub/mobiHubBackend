package com.mobihub.dtos

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Data Transfer Object for Comment.
 *
 * @property id the unique identifier of the comment
 * @property trafficModelId the unique identifier of the traffic model
 * @property userId the unique identifier of the user
 * @property username the username of the user
 * @property content the content of the comment
 * @property creationDate the creation date of the comment
 */
@Serializable
class CommentDTO (
    val id: Int,
    val trafficModelId: Int,
    val userId: Int,
    val username: String,
    val content: String,
    val creationDate: @Contextual Instant
)