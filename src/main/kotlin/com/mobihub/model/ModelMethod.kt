package com.mobihub.model

import kotlinx.serialization.Serializable

/**
 * Represents the model method.
 *
 * @property id the id of the model method
 *
 * @author Team-MobiHub
 */
@Serializable
enum class ModelMethod(val id: ModelMethodId) {
    GRAVITATION_MODEL(ModelMethodId(0)),
    MULTINOMIAL_LOGIT(ModelMethodId(1)),
    NESTED_LOGIT(ModelMethodId(2)),
    CROSS_NESTED_LOGIT(ModelMethodId(3)),
    MIXED_LOGIT(ModelMethodId(4)),
    ILP(ModelMethodId(5)),
    MATRIX_MATCHING(ModelMethodId(6)),
    POISSON_REGRESSION(ModelMethodId(7)),
    SHORT_PATH_REALLOCATION(ModelMethodId(8)),
    SUCCESSIVE_REALLOCATION(ModelMethodId(9)),
    DYNAMIC_REALLOCATION(ModelMethodId(10)),
    WIEDEMANN_74(ModelMethodId(11)),
    WIEDEMANN_99(ModelMethodId(12)),
    GIBBS(ModelMethodId(13));

    companion object {
        /**
         * Returns the model method with the given identifier.
         *
         * @param id the identifier of the model method
         * @return the model method with the given identifier
         */
        fun fromId(id: ModelMethodId): ModelMethod {
            return ModelMethod.entries.first { it.id == id }
        }
    }
}