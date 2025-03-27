package com.mobihub.emailTest

import com.mobihub.utils.email.EmailConfig
import com.mobihub.utils.email.EmailMessage
import com.mobihub.utils.email.EmailService
import com.mobihub.utils.email.EmailType
import org.mockito.kotlin.*
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * This class provides unit tests for the different functionalities of the [EmailService] class.
 */
class EmailServiceTest {

    /**
     * Configuration for the email service, including SMTP server details and credentials.
     * This is used to initialize the `EmailService` for sending emails.
     */
    private val config = EmailConfig(
        "",
        587,
        "",
        "",
        ""
    )

    /**
     * Instance of the `EmailService` initialized with the provided configuration.
     * This is the service under test.
     */
    private val emailService = EmailService(config)

    /**
     * Mocked instance of `EmailType` used to simulate different email types in tests.
     */
    private val emailType = mock<EmailType>()

    /**
     * A sample email message containing recipients, subject, and content.
     * It is used to simulate the email message in tests.
     */
    private val message = EmailMessage(listOf("foo@foo.de"), "Subject", "Content")

    /**
     * Tests if any simple email will be sent.
     */
    @Test
    fun sendEmailSuccessfullySimple() {
        whenever(emailType.getMessage(any(), any())).thenReturn(message)
        val result = emailService.sendEmail(emailType, listOf("foo@foo.de"), "http://example.com")
        assertTrue(result)
    }

    /**
     * Tests if a specific email type will be successfully sent.
     */
    @Test
    fun sendEmailSuccessfully() {
        val result = emailService.sendEmail(EmailType.INVITE_TO_TEAM, listOf("foo@foo.de"), "http://example.com")
        assertTrue(result)
    }

    /**
     * Tests if a message with an empty recipient list will return a false value.
     */
    @Test
    fun sendEmailWithEmptyRecipients() {
        whenever(emailType.getMessage(any(), any())).thenReturn(message)
        val result = emailService.sendEmail(emailType, emptyList(), "http://example.com")
        assertFalse(result)
    }
}
