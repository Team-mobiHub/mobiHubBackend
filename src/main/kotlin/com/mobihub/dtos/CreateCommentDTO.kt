package com.mobihub.dtos

import kotlinx.serialization.Serializable

private const val BLANK_CONTENT_ERROR = "Content of an Comment cannot be blank"

/**
 * DTO for creating and updating a comment.
 *
 * @property id the Id of the comment
 * @property trafficModelId the Id of the traffic model
 * @property userId the Id of the user
 * @property content the content of the comment
 *
 * @author Team-MobiHub
 */

@Serializable
class CreateCommentDTO (
    val id : Int?,
    val trafficModelId: Int,
    val userId: Int,
    val content: String
) {

    /**
     * Validate the comment by, for example requiring the text to not be blank.
     *
     * @throws IllegalArgumentException if the comment is not valid
     */
    fun validate() {
        require(content.isNotBlank()) {
            throw IllegalArgumentException(BLANK_CONTENT_ERROR)
        }
    }
}