package com.mobihub.model

import com.mobihub.dtos.UserDTO
import com.mobihub.utils.file.ShareLink
import io.ktor.server.engine.*

/**
 * Represents a user.
 *
 * @property _id the ID of the user
 * @property _name the name of the user
 * @property _email the email of the user
 * @property _profilePicture the profile picture of the user. The profile picture is lazily loaded.
 * @property _trafficModel the traffic models of the user. The traffic models are lazily loaded.
 * @property isEmailVerified whether the user's email is verified
 * @property passwordHash the hash of the user's password
 * @property isAdmin whether the user is an admin
 * @property teams the teams of the user, loaded lazily
 *
 * @author Team-MobiHub
 */
data class User(
    val _id: UserId?,
    val _name: String,
    val _email: String,
    val _trafficModelProvider: () -> List<TrafficModel>,
    val _profilePictureProvider: () -> Image?,
    val isEmailVerified: Boolean,
    val passwordHash: String,
    val isAdmin: Boolean,
    val teamsProvider: () -> List<Team>,
) : Identity(_id?.id?.let { IdentityId(it) }, _name, _email, _profilePictureProvider, _trafficModelProvider) {

    val teams: List<Team> by lazy(teamsProvider)

    override fun getOwnerType(): OwnerType {
        return OwnerType.USER
    }

    /**
     * Converts the user to a DTO.
     *
     * @param nextcloudBaseUrl the base URL of the Nextcloud instance
     *
     * @return the DTO representation of the user
     */
    fun toDTO(nextcloudBaseUrl: String): UserDTO {
        return UserDTO(
            id = id?.id,
            name = name,
            email = email,
            profilePicture = ByteArray(0),
            isEmailVerified = isEmailVerified,
            isAdmin = isAdmin,
            teams = teams.map { it.toDTO(nextcloudBaseUrl) },
            profilePictureLink = if (this.profilePicture != null && this.profilePicture!!.shareToken != null) {
                ShareLink(
                    shareToken = this.profilePicture!!.shareToken!!.value,
                    fileName = this.profilePicture!!.getNextcloudFileName(),
                ).getShareLink(nextcloudBaseUrl)
            } else {
                ""
            }
        )
    }
}
