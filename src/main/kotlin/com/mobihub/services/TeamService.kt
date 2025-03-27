package com.mobihub.services

import com.mobihub.dtos.TeamDTO
import com.mobihub.dtos.TeamInvitationRequestDTO
import com.mobihub.model.TeamId
import com.mobihub.model.User
import com.mobihub.model.UserId
import com.mobihub.repositories.TeamRepository
import com.mobihub.repositories.TrafficModelRepository
import com.mobihub.repositories.UserRepository
import com.mobihub.utils.email.EmailService
import io.ktor.server.application.*
import org.slf4j.LoggerFactory
import java.util.*

/**
 * Service class for managing team-related operations.
 *
 * @property teamRepository The repository for team entities.
 * @property trafficModelRepository The repository for traffic model entities.
 * @property userRepository The repository for user entities.
 * @property linkService The service for managing links.
 * @property emailService The service for sending emails.
 * @property environment The application environment.
 *
 * author Team-MobiHub
 */
class TeamService(
    private val teamRepository: TeamRepository,
    private val trafficModelRepository: TrafficModelRepository,
    private val userRepository: UserRepository,
    private val linkService: LinkService,
    private val emailService: EmailService,
    private val environment: ApplicationEnvironment
) {
    private val log = LoggerFactory.getLogger(TeamService::class.java)
    private val nextcloudBaseUrl = environment.config.property("nextcloud.baseUrl").getString()

    fun create(team: TeamDTO): TeamDTO {
        TODO()
    }

    fun update(team: TeamDTO): TeamDTO {
        TODO()
    }

    fun getById(id: TeamId): TeamDTO {
        TODO()
    }

    /**
     * Retrieves a list of teams of which the specified user is a member.
     *
     * @param userId the ID of the user
     * @return a list of TeamDTO objects representing the teams
     */
    fun getForUser(userId: UserId): List<TeamDTO> {
        return teamRepository.getByUserId(userId).map { it.toDTO(nextcloudBaseUrl) }
    }

    fun delete(teamId: TeamId) {
        TODO()
    }

    fun transferOwnerShipWithEmail(teamId: TeamId, emailAddress: String) {
        TODO()
    }

    fun useTransferOwnerShipLink(token: UUID) {
        TODO()
    }

    fun getTeamInvitationLink(teamId: TeamId): String {
        TODO()
    }

    fun sendTeamInvitationByEmail(request: TeamInvitationRequestDTO) {
        TODO()
    }

    fun useTeamInvitationLink(token: String, userId: UserId) {
        TODO()
    }
}