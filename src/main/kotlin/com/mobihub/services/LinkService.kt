package com.mobihub.services

import com.mobihub.exceptions.DataWithIdentifierNotFoundException
import com.mobihub.model.LinkData
import com.mobihub.model.LinkType
import com.mobihub.model.TeamId
import com.mobihub.model.UserId
import com.mobihub.repositories.LinkTokensRepository
import io.ktor.server.application.*
import java.sql.Timestamp
import java.util.*

/**
 * Service class responsible for generating and managing links for various purposes such as password reset,
 * email verification, team invitations, and ownership transfers.
 *
 * @property linkTokensRepository The repository used to store and retrieve link-related data.
 * @author mobiHub
 */
class LinkService(
    private val linkTokensRepository: LinkTokensRepository,
    private val environment: ApplicationEnvironment
) {
    /**
     * The base URL used for constructing links.
     */
    val baseURL = environment.config.property("frontend.baseUrl").getString()

    /**
     * The URL path for the password reset link.
     */
    val PASSWORD_RESET_LINK_TEXT = "$baseURL/user/resetpassword";

    /**
     * The URL path for the email verification link.
     */
    val EMAIL_VERIFICATION_LINK_TEXT = "$baseURL/auth/verify-email"

    /**
     * Creates a link based on the specified type (e.g., password reset, email verification).
     *
     * @param userId The ID of the user associated with the link (optional).
     * @param teamId The ID of the team associated with the link (optional).
     * @param email The email address associated with the link (optional).
     * @param type The type of link to create (e.g., `LinkType.PASSWORD_RESET`).
     * @return The generated link as a string.
     */
    fun createLink(userId: UserId?, teamId: TeamId?, email: String?, type: LinkType): String {
        when (type) {
            LinkType.PASSWORD_RESET -> {
                return getResetPasswordLink(email)
            }

            LinkType.TEAM_INVITATION -> {
                TODO()
            }

            LinkType.TRANSFER_OWNERSHIP -> {
                TODO()
            }

            LinkType.EMAIL_ADDRESS_VERIFICATION -> {
                return getEmailAddressVerificationLink(email)
            }
        }
    }

    /**
     * Generates an email address verification link.
     *
     * @param email The email address associated with the link (optional).
     * @return The generated email verification link as a string.
     */
    private fun getEmailAddressVerificationLink(email: String?): String {
        val token = UUID.randomUUID();
        val linkData = LinkData(
            token = token,
            email = email,
            createdAt = Timestamp(System.currentTimeMillis()),
            linkType = LinkType.EMAIL_ADDRESS_VERIFICATION,
            user = null,
            team = null
        )
        linkTokensRepository.storeToken(linkData);
        return "$EMAIL_VERIFICATION_LINK_TEXT/$token"
    }

    /**
     * Generates a password reset link.
     *
     * @param email The email address associated with the link (optional).
     * @return The generated password reset link as a string.
     */
    private fun getResetPasswordLink(email: String?): String {
        val token = UUID.randomUUID();
        val linkData = LinkData(
            token = token,
            email = email,
            createdAt = Timestamp(System.currentTimeMillis()),
            linkType = LinkType.PASSWORD_RESET,
            user = null,
            team = null
        )
        linkTokensRepository.storeToken(linkData);
        return "$PASSWORD_RESET_LINK_TEXT/$token";
    }

    /**
     * Retrieves the [LinkData] object associated with a given UUID.
     *
     * @param token The UUID which identifies the [LinkData].
     * @return The [LinkData] object associated with the token.
     * @throws DataWithIdentifierNotFoundException If no [LinkData] is found for the given token.
     */
    fun getLinkData(token: UUID): LinkData {
        return linkTokensRepository.getToken(token) ?: throw DataWithIdentifierNotFoundException(
            "token",
            "LinkData",
            token.toString()
        )
    }

    /**
     * Retrieves the [LinkData] object associated with a given team ID.
     *
     * @param teamId The ID of the team associated with the link.
     * @return The [LinkData] object associated with the team.
     * @throws DataWithIdentifierNotFoundException If no [LinkData] is found for the given token.
     */
    fun getLinkByTeam(teamId: TeamId): LinkData {
        TODO()
    }

    /**
     * Deletes the link associated with the given token.
     *
     * @param token The UUID of the link to delete.
     */
    fun deleteLink(token: UUID) {
        return linkTokensRepository.deleteToken(token)
    }
}