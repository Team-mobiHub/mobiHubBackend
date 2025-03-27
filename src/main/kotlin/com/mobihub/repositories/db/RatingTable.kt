package com.mobihub.repositories.db

import org.jetbrains.exposed.sql.Table

/**
 * Table for storing ratings of traffic models by users.
 *
 * @property trafficModelId the unique identifier of the traffic model
 * @property userId the unique identifier of the user
 * @property usersRating the rating given by the user
 *
 * @author Team-MobiHub
 */
object RatingTable : Table("Rating") {
    val trafficModelId = integer("trafficModelId").references(TrafficModelTable.id)
    val userId = integer("userId").references(UserTable.id)
    val usersRating = integer("usersRating")

    override val primaryKey = PrimaryKey(trafficModelId, userId, name = "PK_Rating_TrafficModelID_UserID")
}
