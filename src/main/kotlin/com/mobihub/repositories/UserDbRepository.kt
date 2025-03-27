package com.mobihub.repositories

import com.mobihub.model.User
import com.mobihub.model.UserId
import com.mobihub.repositories.db.UserTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/**
 * Repository class for managing [User] entities in the database.
 * Implements the [UserRepository] interface.
 *
 * @author Team-MobiHub
 */
class UserDbRepository(
    private val repositoryProvider: RepositoryProvider
) : UserRepository {

    override fun create(user: User): User {
        return transaction {
            val userId = UserTable.insert {
                it[name] = user.name
                it[email] = user.email
                it[passwordHash] = user.passwordHash
                it[isEmailVerified] = user.isEmailVerified
                it[isAdmin] = user.isAdmin
            } get UserTable.id
            user.copy(_id = UserId(userId));
        }
    }

    override fun getById(id: UserId): User? {
        return transaction {
            UserTable.selectAll().where { UserTable.id eq id.id }.map { toUser(it) }.firstOrNull()
        }
    }

    override fun getByEmail(email: String): User? {
        return transaction {
            UserTable.selectAll().where { UserTable.email eq email }.map { toUser(it) }.firstOrNull()
        }
    }

    override fun getByName(name: String): User? {
        return transaction {
            UserTable.selectAll().where { UserTable.name eq name }.map { toUser(it) }.firstOrNull()
        }
    }

    override fun update(user: User): User {
        return transaction {
            if (user.profilePicture?.token?.let { repositoryProvider.imageRepository.get(it) } == null) {
                user.profilePicture?.let { repositoryProvider.imageRepository.create(it) }
            } else {
                repositoryProvider.imageRepository.update(user.profilePicture!!)
            }

            UserTable.update({ UserTable.id eq user.id?.id!! }) {
                it[name] = user.name
                it[email] = user.email
                if (user.profilePicture != null) {
                    it[profilePictureToken] = user.profilePicture!!.token
                }
            }
            user
        }
    }

    override fun changePassword(user: User) {
        transaction {
            UserTable.update({ UserTable.id eq user.id?.id!! }) {
                it[passwordHash] = user.passwordHash
            }
        }
    }

    override fun delete(user: User) {
        return transaction {
            UserTable.deleteWhere { id eq user.id?.id!! }
        }
    }

    /**
     * Converts a [ResultRow] to a [User] entity.
     *
     * @param row The [ResultRow] to be converted.
     * @return The [User] entity.
     */
    private fun toUser(row: ResultRow): User {
        return User(
            _id = UserId(row[UserTable.id]),
            _name = row[UserTable.name],
            _email = row[UserTable.email],
            _profilePictureProvider = {
                if (row[UserTable.profilePictureToken] != null) {
                    repositoryProvider.imageRepository.get(row[UserTable.profilePictureToken]!!)
                } else {
                    null
                }
            },
            isEmailVerified = row[UserTable.isEmailVerified],
            passwordHash = row[UserTable.passwordHash],
            isAdmin = row[UserTable.isAdmin],
            _trafficModelProvider = {
                repositoryProvider.trafficModelRepository.getByUser(UserId(row[UserTable.id]))
            },
            teamsProvider = {
                repositoryProvider.teamRepository.getByUserId(UserId(row[UserTable.id]))
            })
    }
}
