package com.mobihub.utils.email

import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import java.util.*

private const val FAILED_TO_SEND_EMAIL_ERROR = "Failed to send email: %s"

/**
 * Email service implementation for sending emails.
 *
 * This class is responsible for sending emails based on email types using a strategy pattern.
 * It utilizes the configurations provided by [EmailConfig] and methods defined in [EmailType]
 * to construct and send email messages.
 *
 * @property config The email server configuration settings.
 *
 * @author mobiHub
 */
class EmailService(
    private val config: EmailConfig
) : IEmailService {

    private val log = LoggerFactory.getLogger(EmailService::class.java)

    override fun sendEmail(emailType: EmailType, recipients: List<String>, link: String): Boolean {
        if (recipients.isEmpty()) return false;

        val messageToSend = emailType.getMessage(recipients, link);
        try {
            val properties = getProperties()

            val session = Session.getInstance(properties, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(config.username, config.password)
                }
            })
            session.debug = true

            // Create the email message
            val mimeMessage = MimeMessage(session)
            mimeMessage.setFrom(InternetAddress(config.fromAddress))
            //Parses the List<String> into an array containing InternetAddress objects.
            mimeMessage.setRecipients(
                Message.RecipientType.TO,
                messageToSend.to.flatMap { address -> InternetAddress.parse(address).toList() }.toTypedArray()
            )
            mimeMessage.subject = messageToSend.subject
            mimeMessage.setContent(messageToSend.content, "text/html")

            Transport.send(mimeMessage);
            return true;
        } catch (e: SendFailedException) {
            log.error(FAILED_TO_SEND_EMAIL_ERROR.format(e.message), e)
            return false;
        }
    }

    /**
     * Retrieves the properties required for the email session.
     *
     * @return Properties object containing the email session configurations.
     */
    private fun getProperties(): Properties {
        return Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", config.host)
            put("mail.smtp.port", config.port)
            put("mail.smtp.ssl.trust", "smtp.kit.edu")
        }
    }
}