package com.mobihub.repositories.db

import org.jetbrains.exposed.sql.Table

/**
 * Table for images.
 *
 * @property token the token of the image
 * @property name the name of the image
 * @property fileExtension the file extension of the image
 * @property shareToken the token of the share
 *
 * @author Team-MobiHub
 */
object ImageTable : Table("Image") {
    val token = uuid("token")
    val name = varchar("name", 64)
    val fileExtension = varchar("fileExtension", 10)

    // Empty shareToken means the image is not uploaded yet:
    val shareToken = varchar("shareToken", 64).nullable()

    override val primaryKey = PrimaryKey(token, name = "PK_Image_ImageToken")
}
