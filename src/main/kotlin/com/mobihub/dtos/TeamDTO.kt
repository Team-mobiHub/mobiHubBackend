package com.mobihub.dtos

import kotlinx.serialization.Serializable

/**
 * Data transfer object for a team.
 *
 * @property id The id of the team.
 * @property name The name of the team.
 * @property email The email of the team.
 * @property profilePicture The profile picture of the team.
 * @property profilePictureLink The link to the profile picture of the team.
 * @property description The description of the team.
 * @property ownerUserId The id of the owner of the team.
 * @property ownerUserName The name of the owner of the team.
 * @property owner The owner of the team.
 * @property members The members of the team.
 *
 * @author Team-MobiHub
 */
@Serializable
data class TeamDTO(
    val id: Int?,
    val name: String,
    val email: String,
    val profilePicture: ByteArray?,
    val profilePictureLink: String,
    val description: String,
    val ownerUserId: Int,
    val ownerUserName: String,
    val owner: UserDTO,
    val members: List<UserDTO>
)
