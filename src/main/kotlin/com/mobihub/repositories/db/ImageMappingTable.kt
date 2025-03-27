package com.mobihub.repositories.db

import org.jetbrains.exposed.sql.Table

/**
 * Table for mapping images to traffic models.
 *
 * @property trafficModelId the unique identifier of the traffic model
 * @property imageToken the token of the image
 *
 * @author Team-MobiHub
 */
object ImageMappingTable : Table("ImageMapping") {
    val trafficModelId = integer("trafficModelId").references(TrafficModelTable.id)
    val imageToken = uuid("imageToken").references(ImageTable.token)

    override val primaryKey = PrimaryKey(trafficModelId, imageToken, name = "PK_ImageMapping_TrafficModelId_ImageToken")
}
