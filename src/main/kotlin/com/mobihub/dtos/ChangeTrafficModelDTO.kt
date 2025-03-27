package com.mobihub.dtos

import com.mobihub.model.Framework
import kotlinx.serialization.Serializable

private const val BLANK_NAME_ERROR = "Name cannot be blank"
private const val OWNER_CONFIG_INVALID_ERROR = "Owner configuration is not valid"
private const val INVALID_CHARACTERISTIC_MAPPING_ERROR = "Method %s is not accepted for level %s"
private const val CHARACTERISTICS_LIST_CONTAINS_DUPLICATES = "Characteristics list contains duplicates"

/**
 * Data Transfer Object for creating a traffic model.
 *
 * @property id The unique identifier of the traffic model.
 * @property name The name of the traffic model.
 * @property description A brief description of the traffic model.
 * @property ownerUserId The user ID of the owner.
 * @property ownerTeamId The team ID of the owner.
 * @property isVisibilityPublic Visibility status of the traffic model.
 * @property dataSourceUrl The URL of the data source.
 * @property characteristics A list of characteristics with their levels and methods.
 * @property framework The framework used for the traffic model.
 * @property region The region where the traffic model is applicable.
 * @property coordinates The coordinates related to the traffic model.
 * @property hasZipFileChanged Whether the zip file has changed.
 * @property changedImages The list of images that have changed.
 *
 * @author Team-MobiHub
 */
@Serializable
class ChangeTrafficModelDTO(
    val id: Int?,
    val name: String,
    val description: String,
    val ownerUserId: Int?,
    val ownerTeamId: Int?,
    val isVisibilityPublic: Boolean,
    val dataSourceUrl: String,
    val characteristics: List<CharacteristicDTO>,
    val framework: Framework,
    val region: String,
    val coordinates: String?,
    val hasZipFileChanged: Boolean,
    val changedImages: List<FileStatusDTO>,
) {

    /**
     * Validates the traffic model DTO.
     *
     * @throws IllegalArgumentException If the traffic model DTO is invalid.
     */
    fun validate() {
        require(name.isNotBlank()) {
            throw IllegalArgumentException(BLANK_NAME_ERROR)
        }

        require(ownerUserId != null && ownerTeamId == null || ownerUserId == null && ownerTeamId != null) {
            throw IllegalArgumentException(OWNER_CONFIG_INVALID_ERROR)
        }

        for (characteristic in characteristics) {
            val level = characteristic.modelLevel
            val method = characteristic.modelMethod
            require(level.acceptedMethods.contains(method)) {
                INVALID_CHARACTERISTIC_MAPPING_ERROR.format(method, level)
            }
        }
        
        val characteristicsSet = characteristics.toSet()
        require(characteristics.size == characteristicsSet.size) {
            CHARACTERISTICS_LIST_CONTAINS_DUPLICATES
        }
    }
}

/**
 * Data Transfer Object that describes if a file has been added, removed or not changed.
 *
 * @author Team-MobiHub
 */
@Serializable
enum class FileChangeType {
    NONE,
    ADDED,
    REMOVED
}
