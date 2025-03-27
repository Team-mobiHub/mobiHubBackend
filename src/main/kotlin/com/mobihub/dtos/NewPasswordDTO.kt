package com.mobihub.dtos

import kotlinx.serialization.Serializable

/**
 * Data transfer object for new passwords.
 *
 * @property newPassword The new password.
 *
 * @author Team-MobiHub
 */
@Serializable
data class NewPasswordDTO(val newPassword: String)
