package com.mobihub.model

import kotlinx.serialization.Serializable

/**
 * Represents the framework used to model the traffic simulation.
 *
 * @property id the unique identifier of the framework used in the database
 *
 * @author Team-MobiHub
 */
@Serializable
enum class Framework(val id: FrameworkId) {
    PTV_VISSIM(FrameworkId(0)),
    PTV_VISUM(FrameworkId(1)),
    SUMO(FrameworkId(2)),
    CUBE(FrameworkId(3)),
    VISUM_VISSIM_HYBRID(FrameworkId(4)),
    OPEN_TRAFFIC_SIM(FrameworkId(5)),
    MAT_SIM(FrameworkId(6)),
    OTS(FrameworkId(7)),
    DYNUS_T(FrameworkId(8)),
    TRANS_MODELER(FrameworkId(9)),
    EMME(FrameworkId(10)),
    AIMSUN_NEXT(FrameworkId(11)),
    SATURN(FrameworkId(12)),
    LEGION(FrameworkId(13)),
    PARAMICS(FrameworkId(14)),
    MOBITOPP(FrameworkId(15)),
    OTHER(FrameworkId(16));

    companion object {
        /**
         * Returns the framework with the given identifier.
         *
         * @param id the identifier of the framework
         * @return the framework with the given identifier
         */
        fun fromId(id: FrameworkId): Framework {
            return entries.first { it.id == id }
        }
    }
}