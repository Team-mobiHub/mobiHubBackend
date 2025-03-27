package com.mobihub.model

import com.mobihub.dtos.RatingDTO

/**
 * Represents a rating.
 *
 * @property rating the rating given by the user
 * @property trafficModel the traffic model the rating is associated with. The traffic model is lazily initialized
 * @property user the user who made the rating. The user is lazily initialized
 *
 * @author Team-MobiHub
 */
data class Rating(
    val rating: Int,
    val trafficModelProvider: () -> TrafficModel,
    val userProvider: () -> User
) {
    val trafficModel: TrafficModel by lazy(trafficModelProvider)
    val user: User by lazy(userProvider)

    /**
     * Converts the rating to a [RatingDTO] object.
     *
     * @param averageRating the average rating of the traffic model
     * @return the [RatingDTO] object
     */
    fun toDTO(averageRating: Double): RatingDTO {
        return RatingDTO(
            trafficModelId = trafficModel.id?.id!!,
            usersRating = rating,
            averageRating = averageRating
        )
    }
}
