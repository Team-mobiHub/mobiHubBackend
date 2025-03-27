package com.mobihub.repositories

import com.mobihub.model.*
import com.mobihub.repositories.db.MembershipTable
import com.mobihub.repositories.db.TeamTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Repository class for managing [Team] entities in the database.
 * Implements the [TeamRepository] interface.
 *
 * @property repositoryProvider The [RepositoryProvider] instance.
 *
 * @author Team-MobiHub
 */
class TeamDbRepository(
    private val repositoryProvider: RepositoryProvider
) : TeamRepository {
    override fun create(team: Team): Team {
        TODO("Not yet implemented")
    }

    override fun update(team: Team): Team {
        TODO("Not yet implemented")
    }

    override fun getById(id: TeamId): Team? {
        return transaction {
            TeamTable.selectAll().where { TeamTable.id eq id.id }
                .map { toTeam(it) }
                .firstOrNull()
        }
    }

    override fun getByUserId(userId: UserId): List<Team> {
        return transaction {
            TeamTable.join(
                MembershipTable,
                JoinType.INNER,
                additionalConstraint = { TeamTable.id eq MembershipTable.teamId })
                .selectAll().where { MembershipTable.userId eq userId.id }
                .map { toTeam(it) }
        }
    }

    override fun delete(teamId: TeamId) {
        TODO("Not yet implemented")
    }

    override fun addTeamMember(teamId: TeamId, userId: UserId) {
        TODO("Not yet implemented")
    }

    override fun deleteTeamMember(teamId: TeamId, userId: UserId) {
        return transaction {
            MembershipTable.deleteWhere { (MembershipTable.teamId eq teamId.id) and (MembershipTable.userId eq userId.id) }
        }
    }

    override fun getTeamMembers(teamId: TeamId): List<User> {
        return transaction {
            TeamTable.join(
                MembershipTable,
                JoinType.INNER,
                additionalConstraint = { TeamTable.id eq MembershipTable.teamId })
                .selectAll().where { TeamTable.id eq teamId.id }
                .map { it[MembershipTable.userId] }
                .map { repositoryProvider.userRepository.getById(UserId(it))!! }
        }
    }

    private fun toTeam(row: ResultRow): Team {
        return Team(
            _id = TeamId(row[TeamTable.id]),
            _name = row[TeamTable.name],
            _email = row[TeamTable.email],
            _profilePicureProvider = {
                row[TeamTable.profilePictureToken]?.let { repositoryProvider.imageRepository.get(it) }
            },
            _trafficModelProvider = {
                repositoryProvider.trafficModelRepository.getByTeam(TeamId(row[TeamTable.id]))
            },
            description = row[TeamTable.description],
            ownerProvider = {
                repositoryProvider.userRepository.getById(UserId(row[TeamTable.ownerUserId]))!!
            },
            membersProvider = {
                getTeamMembers(TeamId(row[TeamTable.id]))
                    .mapNotNull { repositoryProvider.userRepository.getById(UserId(it.id!!.id)) }
            },
        )
    }
}
