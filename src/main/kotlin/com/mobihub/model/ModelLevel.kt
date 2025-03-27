package com.mobihub.model

import kotlinx.serialization.Serializable

/**
 * Represents the level of the model.
 *
 * @property acceptedMethods the accepted methods for the model level
 * @property id the unique identifier of the model level used in the database
 *
 * @author Team-MobiHub
 */
@Serializable
enum class ModelLevel(val acceptedMethods: List<ModelMethod>, val id: ModelLevelId) {
    CHOICE_OF_WORKPLACE(listOf(ModelMethod.GRAVITATION_MODEL, ModelMethod.MATRIX_MATCHING, ModelMethod.ILP),
        ModelLevelId(0)),
    CAR_OWNER(listOf(ModelMethod.MULTINOMIAL_LOGIT, ModelMethod.NESTED_LOGIT, ModelMethod.POISSON_REGRESSION),
        ModelLevelId(1)),
    TARGET_SELECTION(listOf(ModelMethod.MULTINOMIAL_LOGIT, ModelMethod.MIXED_LOGIT),
        ModelLevelId(2)),
    CHOICE_OF_TRANSPORTATION(listOf(ModelMethod.MULTINOMIAL_LOGIT, ModelMethod.NESTED_LOGIT, ModelMethod.CROSS_NESTED_LOGIT, ModelMethod.MIXED_LOGIT),
        ModelLevelId(3)),
    CHOICE_OF_ROUTE(listOf(ModelMethod.SHORT_PATH_REALLOCATION, ModelMethod.SUCCESSIVE_REALLOCATION, ModelMethod.DYNAMIC_REALLOCATION),
        ModelLevelId(4)),
    VEHICLE_FOLLOWING_DISTANCE(listOf(ModelMethod.WIEDEMANN_74, ModelMethod.WIEDEMANN_99, ModelMethod.GIBBS),
        ModelLevelId(5));

    companion object {
        /**
         * Function to get the model level from the unique identifier.
         *
         * @param id the unique identifier of the model level
         * @return the model level with the given unique identifier
         */
        fun fromId(id: ModelLevelId): ModelLevel {
            return ModelLevel.entries.first { it.id == id }
        }
    }
}

