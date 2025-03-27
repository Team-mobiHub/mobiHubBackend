package com.mobihub.repositories.db

import org.jetbrains.exposed.sql.Table

/**
 * Table for mapping characteristics to traffic models.
 *
 * @property trafficModelId the unique identifier of the traffic model
 * @property modelLevelId the unique identifier of the model level
 * @property modelMethodId the unique identifier of the model method
 *
 * @author Team-MobiHub
 */
object CharacteristicsMappingTable : Table("CharacteristicsMapping") {
    val trafficModelId = integer("trafficModelId").references(TrafficModelTable.id)
    val modelLevelId = integer("modelLevelId")
    val modelMethodId = integer("modelMethodId")

    override val primaryKey = PrimaryKey(
        trafficModelId, modelLevelId, modelMethodId,
        name = "PK_CharacteristicsMapping_TrafficModelID_ModelLevelID_ModelMethodID"
    )
}
