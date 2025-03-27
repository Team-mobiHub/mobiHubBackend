package com.mobihub.repositories.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or

/**
 * Table for storing traffic models.
 *
 * @property id the unique identifier of the traffic model
 * @property name the name of the traffic model
 * @property description the description of the traffic model
 * @property ownerUserId the unique identifier of the owner user
 * @property ownerTeamId the unique identifier of the owner team
 * @property isVisibilityPublic the visibility of the traffic model
 * @property dataSourceUrl the URL of the data source
 * @property frameworkId the unique identifier of the framework
 * @property region the region of the traffic model
 * @property coordinates the coordinates of the traffic model
 * @property zipFileToken the token of the ZIP file
 * @property isZipFileUploaded the status of the ZIP file
 *
 * @author Team-MobiHub
 */
object TrafficModelTable : Table("TrafficModel") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", length = 64)
    val description = varchar("description", length = 1024)
    val ownerUserId = integer("ownerUserId").references(UserTable.id).nullable()
    val ownerTeamId = integer("ownerTeamId").references(TeamTable.id).nullable()
    val isVisibilityPublic = bool("isVisibilityPublic")
    val dataSourceUrl = varchar("dataSourceUrl", length = 2048)
    val frameworkId = integer("frameworkId")
    val region = varchar("region", length = 128)
    val coordinates = varchar("coordinates", length = 128).nullable()
    val zipFileToken = uuid("zipFileToken")
    val isZipFileUploaded = bool("isZipFileUploaded").default(false)

    override val primaryKey = PrimaryKey(id, name = "PK_TrafficModel_ID")

    /**
     * Check that either the owner user or the owner team is set.
     */
    init {
        check("user_or_team") {
            ((ownerUserId.isNotNull() and ownerTeamId.isNull())
                    or (ownerUserId.isNull() and ownerTeamId.isNotNull()))
        }
    }
}
