package com.mobihub.dtos

import com.mobihub.model.ModelLevel
import com.mobihub.model.ModelMethod
import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for Characteristic.
 * This class is used to transfer data related to characteristics
 * between different layers of the application.
 * A Characteristic is a mapping between a model level and a model method.
 *
 * @property modelLevel The model level of the characteristic.
 * @property modelMethod The model method of the characteristic.
 *
 * @author Team-MobiHub
 */
@Serializable
data class CharacteristicDTO(
    val modelLevel: ModelLevel,
    val modelMethod: ModelMethod
)
