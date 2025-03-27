package com.mobihub.dtos

import kotlinx.serialization.Serializable

/**
 * Data transfer object for team invitation requests.
 *
 * @property teamId The id of the team.
 * @property emailAddresses The email addresses of the users to invite.
 *
 * @author Team-MobiHub
 */
@Serializable
data class TeamInvitationRequestDTO (
    val teamId: Int,
    val emailAddresses: List<String>
)