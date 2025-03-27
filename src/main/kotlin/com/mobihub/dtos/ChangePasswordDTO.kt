package com.mobihub.dtos

import kotlinx.serialization.Serializable

/**
 * Data transfer object for changing the password of a user.
 *
 * @property oldPassword The old password of the user.
 * @property newPassword The new password of the user.
 */
@Serializable
data class ChangePasswordDTO(
    val oldPassword: String,
    val newPassword: String
)
