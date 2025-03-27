package com.mobihub.repositories

import com.mobihub.model.Team
import com.mobihub.model.TeamId
import com.mobihub.model.User
import com.mobihub.model.UserId

/**
 * Repository for teams.
 * This interface defines the methods that a repository for teams must implement.
 *
 * author Team-MobiHub
 */
interface TeamRepository {

    /**
     * Creates a new team.
     *
     * @param team The team to be created.
     *
     * @return The created team.
     */
    fun create(team: Team): Team

    /**
     * Updates an existing team.
     *
     * @param team The team to be updated.
     *
     * @return The updated team.
     */
    fun update(team: Team): Team

    /**
     * Retrieves a team by its ID.
     *
     * @param id The ID of the team.
     *
     * @return The team with the given ID.
     */
    fun getById(id: TeamId): Team?

    /**
     * Retrieves all teams.
     *
     * @param userId The ID of the user
     *
     * @return All teams of the user
     */
    fun getByUserId(userId: UserId): List<Team>

    /**
     * Deletes a team.
     *
     * @param teamId The ID of the team to be deleted.
     */
    fun delete(teamId: TeamId)

    /**
     * Adds a team member.
     *
     * @param teamId The ID of the team.
     * @param userId The ID of the user to be added.
     */
    fun addTeamMember(teamId: TeamId, userId: UserId)

    /**
     * Deletes a team member that is not the owner of a team.
     *
     * @param teamId The ID of the team.
     * @param userId The ID of the user to be deleted.
     */
    fun deleteTeamMember(teamId: TeamId, userId: UserId)

    /**
     * Retrieves the team members of a team.
     *
     * @param teamId The ID of the team.
     * @return The team members of the team.
     */
    fun getTeamMembers(teamId: TeamId): List<User>
}
