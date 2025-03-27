package com.mobihub.model

import java.sql.Timestamp
import java.util.UUID

/**
 * Data class for a link.
 *
 * @property token The token of the link.
 * @property email The email of the user.
 * @property createdAt The creation date of the link.
 * @property user The user of the link.
 * @property team The team of the link.
 * @property linkType The type of the link.
 *
 * @author Team-MobiHub
 */
data class LinkData(
    val token: UUID,
    val email: String?,
    val createdAt: Timestamp,
    val user: User?,
    val team: Team?,
    val linkType: LinkType
)