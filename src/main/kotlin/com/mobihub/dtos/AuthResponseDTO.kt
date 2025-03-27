package com.mobihub.dtos

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Data transfer object for authentication response.
 *
 * @property token The authentication token.
 * @property expiresAt The expiration time of the token.
 */
@Serializable
data class AuthResponseDTO(
    val token: String,
    val expiresAt: @Contextual Instant
)
