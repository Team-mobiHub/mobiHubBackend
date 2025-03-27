package com.mobihub.repositories.db

import org.jetbrains.exposed.sql.Table

/**
 * Table for favourite traffic models.
 *
 * @property userId the unique identifier of the user
 * @property trafficModelId the unique identifier of the traffic model
 *
 * @author Team-MobiHub
 */
object FavouriteTable : Table("Favourite") {
    val userId = integer("userId").references(UserTable.id)
    val trafficModelId = integer("trafficModelId").references(TrafficModelTable.id)

    override val primaryKey = PrimaryKey(userId, trafficModelId, name = "PK_Favourite_UserID_TrafficModelID")
}
