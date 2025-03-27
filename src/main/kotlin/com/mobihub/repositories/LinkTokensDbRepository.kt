package com.mobihub.repositories

import com.mobihub.model.LinkData
import com.mobihub.model.LinkType
import com.mobihub.model.TeamId
import com.mobihub.repositories.db.LinkTokensTable
import java.sql.Timestamp
import java.util.*
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * <<<<<<< HEAD Database repository implementation for managing link tokens. This class handles the
 * storage, retrieval, and deletion of [LinkData] objects in a database.
 *
 * @property repositoryProvider The [RepositoryProvider] used to access database operations. =======
 * Repository for link tokens. This class implements the [LinkTokensRepository] interface.
 *
 * @property repositoryProvider The [RepositoryProvider] instance.
 *
 * author Team-MobiHub >>>>>>> main
 */
class LinkTokensDbRepository(private val repositoryProvider: RepositoryProvider) :
        LinkTokensRepository {

    /**
     * Stores a [LinkData] object in the database.
     *
     * @param linkData The [LinkData] object to store.
     * @throws IllegalArgumentException If the email in [linkData] is null.
     */
    override fun storeToken(linkData: LinkData) {
        return transaction {
            LinkTokensTable.insert {
                it[token] = linkData.token
                it[email] = linkData.email ?: throw IllegalArgumentException("Email cannot be null")
                it[typeId] = linkData.linkType.id.id
                it[createdAt] = linkData.createdAt.toLocalDateTime()
            } get LinkTokensTable.token
        }
    }

    /**
     * Retrieves a [LinkData] object from the database using the provided token.
     *
     * @param token The UUID token used to identify the [LinkData].
     * @return The [LinkData] object if found, or `null` if no data is associated with the token.
     */
    override fun getToken(token: UUID): LinkData? {
        return transaction {
            LinkTokensTable.selectAll()
                    .where { LinkTokensTable.token eq token }
                    .map { toLinkData(it) }
                    .firstOrNull()
        }
    }

    /**
     * Retrieves a [LinkData] object associated with the given team ID. This method is not yet
     * implemented.
     *
     * @param teamId The ID of the team used to identify the [LinkData].
     * @throws NotImplementedError This method is not yet implemented.
     */
    override fun getTokenByTeam(teamId: TeamId): LinkData {
        TODO("Not yet implemented")
    }

    /**
     * Deletes the [LinkData] object associated with the given token from the database.
     *
     * @param token The UUID token used to identify the [LinkData] to delete.
     */
    override fun deleteToken(token: UUID) {
        transaction { LinkTokensTable.deleteWhere { LinkTokensTable.token eq token } }
    }

    /**
     * Converts a database [ResultRow] into a [LinkData] object.
     *
     * @param row The [ResultRow] from the database query.
     * @return The [LinkData] object created from the database row.
     */
    private fun toLinkData(row: ResultRow): LinkData {
        return LinkData(
                row[LinkTokensTable.token],
                row[LinkTokensTable.email],
                Timestamp.valueOf(row[LinkTokensTable.createdAt]),
                null,
                null,
                LinkType.fromId(row[LinkTokensTable.typeId])
        )
    }
}
