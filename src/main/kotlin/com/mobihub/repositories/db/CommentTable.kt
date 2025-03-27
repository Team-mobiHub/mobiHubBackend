package com.mobihub.repositories.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Table for comments.
 *
 * @property id the unique identifier of the comment
 * @property userId the unique identifier of the user
 * @property trafficModelId the unique identifier of the traffic model
 * @property content the content of the comment
 * @property dateTime the creation date of the comment
 *
 * @author Team-MobiHub
 */
object CommentTable : Table("Comment") {
    val id = integer("id").autoIncrement()
    val userId = integer("userId").references(UserTable.id)
    val trafficModelId = integer("trafficModelId").references(TrafficModelTable.id)
    val content = varchar("content", 1024)
    val dateTime = timestamp("creationDate")

    override val primaryKey = PrimaryKey(id, name = "PK_Comment_ID")
}
