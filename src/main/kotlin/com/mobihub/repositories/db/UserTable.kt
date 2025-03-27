package com.mobihub.repositories.db

import org.jetbrains.exposed.sql.Table

/**
 * Table for storing user data.
 *
 * @property id the unique identifier of the user
 * @property name the username of the user
 * @property email the email address of the user
 * @property passwordHash the hashed password of the user
 * @property isEmailVerified whether the email address is verified
 * @property profilePictureToken the token of the profile picture
 * @property isAdmin whether the user is an administrator
 *
 * @author Team-MobiHub
 */
object UserTable : Table("IdentityUser") {
    val id = integer("id").autoIncrement()
    val name = varchar("username", 64).uniqueIndex()
    val email = varchar("email", 320)
    val passwordHash = varchar("password", 64)
    val isEmailVerified = bool("isEmailVerified").default(false)
    val profilePictureToken = uuid("profilePictureToken").references(ImageTable.token).nullable()
    val isAdmin = bool("isAdmin").default(false)

    override val primaryKey = PrimaryKey(id, name = "PK_IdentityUser_ID")
}
