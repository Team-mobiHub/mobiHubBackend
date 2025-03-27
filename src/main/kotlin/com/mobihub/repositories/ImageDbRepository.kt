package com.mobihub.repositories

import com.mobihub.model.Image
import com.mobihub.model.ShareToken
import com.mobihub.repositories.db.ImageTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

/**
 * Repository class for managing [Image] entities in the database.
 * Implements the [ImageRepository] interface.
 *
 * @property repositoryProvider the provider for other repositories
 *
 * @author Team-MobiHub
 */
class ImageDbRepository(
    private val repositoryProvider: RepositoryProvider
) : ImageRepository {
    override fun create(image: Image): Image {
        return transaction {
            ImageTable.insert {
                it[token] = image.token
                it[name] = image.name
                it[fileExtension] = image.fileExtension
                it[shareToken] = image.shareToken?.value
            }

            return@transaction image
        }
    }

    override fun update(image: Image): Image {
        return transaction {
            ImageTable.update({ ImageTable.token eq image.token }) {
                it[name] = image.name
                it[fileExtension] = image.fileExtension
                it[shareToken] = image.shareToken?.value
            }

            return@transaction image
        }
    }

    override fun get(token: UUID): Image? {
        return transaction {
            ImageTable.selectAll().where { ImageTable.token eq token }
                .map { toImage(it) }
                .firstOrNull()
        }
    }

    private fun toImage(row: ResultRow): Image {
        return Image(
            token = row[ImageTable.token],
            name = row[ImageTable.name],
            fileExtension = row[ImageTable.fileExtension],
            shareToken = row[ImageTable.shareToken]?.let { ShareToken(it) }
        )
    }

    override fun delete(token: UUID) {
        transaction {
            ImageTable.deleteWhere { ImageTable.token eq token }
        }
    }
}
