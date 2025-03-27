package com.mobihub.dtos

import com.mobihub.utils.verifier.MobiHubCredentialsVerifier
import kotlinx.serialization.Serializable

private const val MAXIMUM_PROFILE_PICTURE_SIZE  = 1048576 // 1 MiB is maximum size
private const val PROFILE_PICTURE_SIZE_EXCEEDS_MAX_SIZE = "Profile picture size exceeds $MAXIMUM_PROFILE_PICTURE_SIZE Bytes."

/**
 * A DTO that represents a user.
 *
 * @property id The unique identifier of the user.
 * @property name The name of the user.
 * @property email The email address of the user.
 * @property profilePicture The profile picture of the user as a byte array.
 * @property profilePictureLink The link to the user's profile picture.
 * @property isEmailVerified Indicates if the user's email is verified.
 * @property isAdmin Indicates if the user has admin privileges.
 * @property teams The list of teams the user is part of.

 * @author Mobihub-team
 */
@Serializable
data class UserDTO(
    val id: Int?,
    val name: String,
    val email: String,
    val profilePicture: ByteArray?,
    val profilePictureLink: String,
    val isEmailVerified: Boolean,
    val isAdmin: Boolean,
    val teams: List<TeamDTO>,
) {
    /**
     * Validates the name, email and profile picture size of a DTO from an update request
     */
    fun validateUpdate() {
        // we use the functionality from registerDTO to validate name and email
       MobiHubCredentialsVerifier(name, email, null). verify(
           verifyPassword = false,
           verifyName = true,
           verifyEmail = true
       )

        require((profilePicture?.size ?: 0) <= MAXIMUM_PROFILE_PICTURE_SIZE) { PROFILE_PICTURE_SIZE_EXCEEDS_MAX_SIZE }
    }
}