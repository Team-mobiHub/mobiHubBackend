package com.mobihub.repositories.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

/**
 * Table for link tokens.
 *
 * @property token the token of the link
 * @property userId the unique identifier of the user
 * @property teamId the unique identifier of the team
 * @property email the email of the user
 * @property typeId the unique identifier of the type
 * @property createdAt the creation date of the token
 *
 * @author Team-MobiHub
 */
object LinkTokensTable : Table("LinkTokens") {
    val token = uuid("token")
    val userId = integer("userId").references(UserTable.id).nullable()
    val teamId = integer("teamId").references(TeamTable.id).nullable()
    val email = varchar("email", 320)
    val typeId = integer("typeId")
    val createdAt = datetime("createdAt")

    override val primaryKey = PrimaryKey(token, name = "PK_LinkTokens_Token")
}
