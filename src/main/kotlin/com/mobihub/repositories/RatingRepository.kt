package com.mobihub.repositories

import com.mobihub.model.Rating
import com.mobihub.model.TrafficModelId
import com.mobihub.model.UserId

/**
 * Repository for ratings.
 *
 * This interface defines the methods that a repository for ratings must implement.
 * @see [RatingDbRepository]
 *
 * @author Team-MobiHub
 */
interface RatingRepository {

    /**
     * Adds a new rating to a traffic model by a user.
     *
     * @param rating The rating to add.
     * @return The added rating.
     */
    fun addRating(rating: Rating): Rating

    /**
     * Updates an existing rating to a traffic model by a user.
     *
     * @param rating The rating to update.
     * @return The updated rating.
     */
    fun updateRating(rating: Rating): Rating

    /**
     * Deletes an existing rating to a traffic model by a user.
     *
     * @param rating The rating to delete.
     */
    fun deleteRating(rating: Rating)

    /**
     * Retrieves all ratings for a given traffic model.
     *
     * @param trafficModelId The ID of the traffic model.
     * @return The ratings for the given traffic model.
     */
    fun getRatingsForTrafficModel(trafficModelId: TrafficModelId): List<Rating>

    /**
     * Retrieves all ratings for a given user.
     *
     * @param userId the ID of the given user.
     * @return the ratings of the given user.
     */
    fun getRatingsForUser(userId: UserId): List<Rating>
    /**
     * Retrieves the average rating for a given traffic model.
     *
     * @param trafficModelId The ID of the traffic model.
     * @return The average rating for the given traffic model.
     */
    fun getAverageRatingForTrafficModel(trafficModelId: TrafficModelId): Double
}