package com.mobihub.dtos

import kotlinx.serialization.Serializable

/**
 * Data transfer object for search result items.
 *
 * @property trafficModelId The id of the traffic model.
 * @property name The name of the traffic model.
 * @property description The description of the traffic model.
 * @property averageRating The average rating of the traffic model.
 * @property imageURL The image URL of the traffic model.
 *
 * @author Team-MobiHub
 */
@Serializable
data class SearchResultItemDTO (
    val trafficModelId: Int,
    val name: String,
    val description: String,
    val averageRating: Double,
    val imageURL: String,
)