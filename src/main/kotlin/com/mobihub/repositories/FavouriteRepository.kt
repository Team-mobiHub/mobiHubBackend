package com.mobihub.repositories

import com.mobihub.model.TrafficModel
import com.mobihub.model.TrafficModelId
import com.mobihub.model.User
import com.mobihub.model.UserId

/**
 * Repository for managing the favourites of [User]s.
 *
 * author Team-MobiHub
 */
interface FavouriteRepository {

    /**
     * Adds a [TrafficModel] to the favourites of a [User].
     *
     * @param trafficModelId The ID of the [TrafficModel] entity.
     * @param userId The ID of the [User] entity.
     */
    fun addFavourite(trafficModelId: TrafficModelId, userId: UserId)

    /**
     * Deletes a [TrafficModel] from the favourites of a [User].
     *
     * @param trafficModelId The ID of the [TrafficModel] entity.
     * @param userId The ID of the [User] entity.
     */
    fun deleteFavourite(trafficModelId: TrafficModelId, userId: UserId)

    /**
     * Deletes all the favourites of a [TrafficModel].
     *
     * @param trafficModelId The ID of the [TrafficModel] entity.
     */
    fun deleteFavourites(trafficModelId: TrafficModelId)

    /**
     * Retrieves the favourites of a [User].
     *
     * @param userId The ID of the [User] entity.
     * @return The list of [TrafficModel]s that are favourites of the given [User].
     */
    fun getFavouritesByUserId(userId: UserId): List<TrafficModel>

    /**
     * Retrieves the users that have a [TrafficModel] as a favourite.
     *
     * @param trafficModelId The ID of the [TrafficModel] entity.
     * @return The list of [User]s that have the given [TrafficModel] as a favourite.
     */
    fun getFavoritesByTrafficModelId(trafficModelId: TrafficModelId): List<User>
}