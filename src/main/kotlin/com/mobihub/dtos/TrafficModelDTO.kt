package com.mobihub.dtos

import com.mobihub.model.Framework
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object (DTO) for Traffic Model.
 *
 * @property id the unique identifier of the traffic model
 * @property name the name of the traffic model
 * @property description the description of the traffic model
 * @property userId the unique identifier of the user
 * @property teamId the unique identifier of the team
 * @property isVisibilityPublic whether the traffic model is public
 * @property dataSourceUrl the URL of the data source
 * @property framework the framework of the traffic model
 * @property region the named region of the traffic model
 * @property coordinates the geographic coordinates for the traffic model
 * @property imageURLs the list of image URLs associated with the traffic model
 * @property markDownFileURL the URL of the markdown file
 * @property isFavorite whether the traffic model is marked as favorite
 * @property comments the list of comments on the traffic model
 * @property rating the rating of the traffic model
 * @property characteristics the list of characteristics with their levels and methods
 * @property zipFileToken the token for the zip file
 *
 * @author Team-MobiHub
 */
@Serializable
data class TrafficModelDTO(
    val id: Int,
    val name: String,
    val description: String,
    val userId: Int?,
    val teamId: Int?,
    val isVisibilityPublic: Boolean,
    val dataSourceUrl: String,
    val framework: Framework,
    val region: String,
    val coordinates: String,
    val imageURLs: List<String>,
    val markDownFileURL: String,
    val isFavorite: Boolean,
    val comments: List<CommentDTO>,
    val rating: RatingDTO,
    val characteristics: List<CharacteristicDTO>,
    val zipFileToken: String
)
