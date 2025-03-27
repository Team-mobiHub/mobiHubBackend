package com.mobihub.repositories.db

import org.jetbrains.exposed.sql.Table

/**
 * Table for storing teams.
 *
 * @property id the unique identifier of the team
 * @property name the name of the team
 * @property email the email of the team
 * @property ownerUserId the unique identifier of the owner
 * @property description the description of the team
 * @property profilePictureToken the token of the profile picture
 *
 * @author Team-MobiHub
 */
object TeamTable : Table("IdentityTeam") {
    val id = integer("id").autoIncrement()
    val name = varchar("username", 64).uniqueIndex()
    val email = varchar("email", 320)
    val ownerUserId = integer("ownerUserId").references(UserTable.id)
    val description = varchar("description", 1024)
    val profilePictureToken = uuid("profilePictureToken").references(ImageTable.token).nullable()

    override val primaryKey = PrimaryKey(id, name = "PK_Team_ID")
}
