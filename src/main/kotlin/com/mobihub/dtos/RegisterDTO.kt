package com.mobihub.dtos

import com.mobihub.utils.verifier.MobiHubCredentialsVerifier
import kotlinx.serialization.Serializable

/**
 * Data transfer object for registration requests.
 *
 * @property username The username of the user.
 * @property email The email of the user.
 * @property password The password of the user.
 *
 * @author Team-MobiHub
 */
@Serializable
data class RegisterDTO(
    val username: String,
    val email: String,
    val password: String,
) {
    /**
     * Validates all fields of the [RegisterDTO] object
     *
     * @return the [RegisterDTO] object
     * @throws [InvalidCredentialException] if any field is invalid
     */
    fun validate(): RegisterDTO {
            MobiHubCredentialsVerifier(username, email, password).verify(
                verifyPassword = true,
                verifyName = true,
                verifyEmail = true
            )
        return this
    }
}