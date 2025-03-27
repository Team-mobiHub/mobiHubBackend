package com.mobihub.repositories.db

import org.jetbrains.exposed.sql.Table

/**
 * Table for mapping users to teams.
 *
 * @property userId the unique identifier of the user
 * @property teamId the unique identifier of the team
 *
 * @author Team-MobiHub
 */
object MembershipTable : Table("Membership") {
    val userId = integer("userId").references(UserTable.id)
    val teamId = integer("teamId").references(TeamTable.id)

    override val primaryKey = PrimaryKey(userId, teamId, name = "PK_Membership_UserID_TeamID")
}
