package com.mobihub.dtos

import kotlinx.serialization.Serializable

/**
 * Data transfer object for login requests.
 *
 * @property email The email of the user.
 * @property password The password of the user.
 *
 * @author Team-MobiHub
 */
@Serializable
data class LoginDTO(
    val email: String,
    val password: String
)
