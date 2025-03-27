package com.mobihub.repositories

import com.mobihub.model.*
import com.mobihub.repositories.db.TrafficModelTable

/**
 * Repository for managing [TrafficModel] entities.
 */
interface TrafficModelRepository {

    /**
     * Stores a new [TrafficModel] in the [TrafficModelTable].
     *
     * @param trafficModel The [TrafficModel] entity to be created.
     * @return The created [TrafficModel] entity with the generated ID.
     */
    fun create(trafficModel: TrafficModel): TrafficModel

    /**
     * Retrieves a [TrafficModel] by its ID.
     *
     * @param id The ID of the [TrafficModel] entity.
     * @return The [TrafficModel] entity with the given ID.
     */
    fun getById(id: TrafficModelId): TrafficModel?

    /**
     * Retrieves all [TrafficModel]s of a given team.
     *
     * @param teamId The ID of the team.
     * @return The [TrafficModel]s of the given team.
     */
    fun getByTeam(teamId: TeamId): List<TrafficModel>

    /**
     * Retrieves all [TrafficModel]s of a given user.
     *
     * @param userId The ID of the user.
     * @return The [TrafficModel]s of the given user.
     */
    fun getByUser(userId: UserId): List<TrafficModel>

    /**
     * Updates a [TrafficModel] entity in the database.
     *
     * @param trafficModel The [TrafficModel] entity to be updated.
     * @return The updated [TrafficModel] entity.
     */
    fun update(trafficModel: TrafficModel): TrafficModel

    /**
     * Retrieves a list of [TrafficModel]s that match the given search criteria.
     * It only retrieves the public traffic models.
     *
     * @param page The page number of the search result.
     * @param size The number of items per page.
     * @param name The name of the traffic model.
     * @param authorName The name of the user who created the traffic model.
     * @param region The region of the traffic model.
     * @param modelLevels The model levels of the traffic model.
     * @param modelMethods The model methods of the traffic model.
     * @param frameworks The framework of the traffic model.
     *
     * @return The list of [TrafficModel]s that match the search criteria, and the total number of results.
     */
    fun searchPaginated(
        page: Int,
        size: Int,
        name: String?,
        authorName: String?,
        region: Region?,
        modelLevels: List<ModelLevel>,
        modelMethods: List<ModelMethod>,
        frameworks: List<Framework>
    ): Pair<List<TrafficModel>, Long>

    /**
     * Updates the images of a [TrafficModel] entity in the database.
     *
     * @param trafficModelId The ID of the traffic model.
     * @param images The images of the traffic model.
     */
    fun updateImages(trafficModelId: TrafficModelId, images: List<Image>)

    /**
     * Updates the image of a [TrafficModel] entity in the database.
     *
     * @param trafficModelId The ID of the traffic model.
     * @param image The image of the traffic model.
     */
    fun updateImage(trafficModelId: TrafficModelId, image: Image)

    /**
     * Deletes a [TrafficModel] entity from the database including all related entities.
     *
     * @param id The ID of the [TrafficModel] entity to be deleted.
     */
    fun delete(id: TrafficModelId)
}
