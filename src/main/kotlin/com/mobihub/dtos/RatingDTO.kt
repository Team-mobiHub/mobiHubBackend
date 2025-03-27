package com.mobihub.dtos

import kotlinx.serialization.Serializable

/**
 * Data transfer object for rating.
 *
 * @property trafficModelId The id of the traffic model.
 * @property usersRating The rating given by the users.
 * @property averageRating The average rating of the traffic model.
 *
 * @author Team-MobiHub
 */
@Serializable
data class RatingDTO (
    val trafficModelId: Int,
    val usersRating: Int,
    val averageRating: Double
)