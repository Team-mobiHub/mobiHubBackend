package com.mobihub.emailTest

import com.mobihub.utils.email.EmailType
import kotlin.test.assertTrue
import io.ktor.server.plugins.*
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * This class tests the functionality of the [EmailType] enum class.
 */
class EmailTypeTest {

    /**
     * Tests if the [EmailType.getMessage] method successfully adds the account confirmation link to the message content.
     */
    @Test
    fun accountConfirmationMessageContainsLink() {
        val recipients = listOf("user@example.com")
        val link = "http://example.com/confirm"
        val message = EmailType.ACCOUNT_CONFIRMATION.getMessage(recipients, link)
        assertTrue(message.content.contains(link))
    }

    /**
     * Tests if the [EmailType.getMessage] method successfully adds the transfer ownership link to the message content.
     */
    @Test
    fun transferOwnershipMessageContainsLink() {
        val recipients = listOf("user@example.com")
        val link = "http://example.com/transfer"
        val message = EmailType.TRANSFER_OWNERSHIP.getMessage(recipients, link)
        assertTrue(message.content.contains(link))
    }

    /**
     * Tests if the [EmailType.getMessage] method successfully adds the team invite link to the message content.
     */
    @Test
    fun inviteToTeamMessageContainsLink() {
        val recipients = listOf("user@example.com")
        val link = "http://example.com/invite"
        val message = EmailType.INVITE_TO_TEAM.getMessage(recipients, link)
        assertTrue(message.content.contains(link))
    }

    /**
     * Tests if the [EmailType.getMessage] method successfully adds the password reset link to the message content.
     */
    @Test
    fun resetPasswordMessageContainsLink() {
        val recipients = listOf("user@example.com")
        val link = "http://example.com/reset"
        val message = EmailType.RESET_PASSWORD.getMessage(recipients, link)
        assertTrue(message.content.contains(link))
    }

    /**
     * Checks if the [EmailType.getHTMLBody] successfully throws a [NotFoundException] if the input file cannot be found.
     */
    @Test
    fun getHTMLBodyThrowsNotFoundExceptionForInvalidFile() {
        assertFailsWith<NotFoundException> {
            EmailType.ACCOUNT_CONFIRMATION.getHTMLBody("non-existent-file.html")
        }
    }
}