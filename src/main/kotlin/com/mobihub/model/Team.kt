package com.mobihub.model

import com.mobihub.dtos.TeamDTO

/**
 * Represents a team. A team is a group of users that can share traffic models and collaborate on them. This class extends the [Identity] class.
 *
 * @property _id the ID of the team
 * @property _name the name of the team
 * @property _email the email of the team
 * @property _profilePicure the profile picture of the team. The profile picture is lazily loaded.
 * @property description the description of the team
 * @property owner the owner of team, loaded lazily
 * @property members the members of the team, loaded lazily
 *
 * @author Team-MobiHub
 */
data class Team(
    val _id: TeamId?,
    val _name: String,
    val _email: String,
    val _profilePicureProvider: () -> Image?,
    val _trafficModelProvider: () -> List<TrafficModel>,
    val description: String,
    val ownerProvider: () -> User,
    val membersProvider: () -> List<User>
) : Identity(_id?.id?.let { IdentityId(it) }, _name, _email, _profilePicureProvider, _trafficModelProvider) {

    val owner by lazy { ownerProvider() }
    val members by lazy { membersProvider() }

    /**
     * Gets the type of the owner of the identity.
     *
     * @return the type of the owner of the identity
     */
    override fun getOwnerType(): OwnerType {
        return OwnerType.TEAM
    }

    /**
     * Converts the team to a DTO.
     *
     * @return the DTO representation of the team
     */
    fun toDTO(baseUrl: String): TeamDTO {
        return TeamDTO(
            id = id?.id,
            name = name,
            email = email,
            profilePicture = ByteArray(0),
            profilePictureLink = "",
            description = description,
            ownerUserId = owner.id!!.id,
            ownerUserName = owner.name,
            owner = owner.toDTO(nextcloudBaseUrl = baseUrl),
            members = members.map { it.toDTO(nextcloudBaseUrl = baseUrl) }
        )
    }
}
