package com.mobihub.dtos

import com.mobihub.model.*
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for CreateTrafficModelResponse.
 * This class is used as a response to creating or updating a traffic model.
 *
 * @property id The unique identifier of the traffic model.
 * @property name The name of the traffic model.
 * @property description The description of the traffic model.
 * @property ownerUserId The unique identifier of the user who owns the traffic model.
 * @property ownerTeamId The unique identifier of the team who owns the traffic model.
 * @property isVisibilityPublic Whether the traffic model is public.
 * @property dataSourceUrl The URL of the data source.
 * @property characteristics The list of model level and method pairs.
 * @property framework The framework of the traffic model.
 * @property region The region of the traffic model.
 * @property coordinates The coordinates of the traffic model.
 * @property zipFileToken The token for the zip file.
 * @property imageTokens The list of tokens for the images.
 *
 * @author Team-MobiHub
 */
@Serializable
data class CreateTrafficModelResponseDTO(
    val id: Int?,
    val name: String,
    val description: String,
    val ownerUserId: Int?,
    val ownerTeamId: Int?,
    val isVisibilityPublic: Boolean,
    val dataSourceUrl: String,
    val characteristics: List<Pair<ModelLevel, ModelMethod>>,
    val framework: Framework,
    val region: String,
    val coordinates: String?,

    // Upload-Tokens:
    val zipFileToken: String,
    val imageTokens: List<String>
) {
    /**
     * Constructor for the CreateTrafficModelResponseDTO.
     *
     * @param trafficModel The traffic model to create the response from.
     * @return A CreateTrafficModelResponseDTO object.
     */
    constructor(trafficModel: TrafficModel) : this(
        id = trafficModel.id?.id,
        name = trafficModel.name,
        description = trafficModel.description,
        ownerUserId = if (trafficModel.author.getOwnerType() == OwnerType.USER) trafficModel.author.id?.id else null,
        ownerTeamId = if (trafficModel.author.getOwnerType() == OwnerType.TEAM) trafficModel.author.id?.id else null,
        isVisibilityPublic = trafficModel.isVisibilityPublic,
        dataSourceUrl = trafficModel.dataSourceUrl,
        characteristics = trafficModel.methodLevelPair,
        framework = trafficModel.framework,
        region = trafficModel.location.region.name,
        coordinates = trafficModel.location.coordinates?.value,

        zipFileToken = trafficModel.zipFileToken.toString(),
        imageTokens = trafficModel.images.map { it.token.toString() }
    )
}
