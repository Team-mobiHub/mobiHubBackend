package com.mobihub.repositories

import com.mobihub.model.ModelLevel
import com.mobihub.model.ModelMethod
import com.mobihub.model.TrafficModelId

/**
 * Repository for characteristics mapping.
 *
 * This interface defines the methods that a repository for characteristics mapping must implement.
 * @see [CharacteristicsMappingDbRepository]
 *
 * @author Team-MobiHub
 */
interface CharacteristicsMappingRepository {

    /**
     * Creates a new characteristics mapping for a traffic model.
     *
     * @param trafficModelId The ID of the traffic model.
     * @param mapping The mapping of model levels to model methods.
     *
     * @return The created mapping of model levels to model methods.
     */
    fun create(
        trafficModelId: TrafficModelId,
        mapping: List<Pair<ModelLevel, ModelMethod>>
    ): List<Pair<ModelLevel, ModelMethod>>

    /**
     * Updates the characteristics mapping for a traffic model.
     *
     * @param trafficModelId The ID of the traffic model.
     * @param mapping The new mapping of model levels to model methods.
     */
    fun update(trafficModelId: TrafficModelId, mapping: List<Pair<ModelLevel, ModelMethod>>)

    /**
     * Retrieves the characteristics mapping for a given traffic model.
     *
     * @param trafficModelId The ID of the traffic model.
     * @return The characteristics mapping for the given traffic model.
     */
    fun get(trafficModelId: TrafficModelId): List<Pair<ModelLevel, ModelMethod>>

    /**
     * Deletes the characteristics mapping for a given traffic model.
     *
     * @param trafficModelId The ID of the traffic model.
     */
    fun delete(trafficModelId: TrafficModelId)
}
