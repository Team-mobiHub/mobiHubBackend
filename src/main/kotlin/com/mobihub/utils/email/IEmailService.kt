package com.mobihub.utils.email

/**
 * Interface defining the contract for sending emails.
 *
 * An implementation of this interface is responsible for sending emails of different types,
 * as defined in [EmailType].
 *
 * @author mobiHub
*/
fun interface IEmailService {

    /**
     * Sends an email of the specified type to the given recipients with a provided link.
     *
     * @param emailType The type of email to be sent, represented by the [EmailType] enum.
     * @param recipients A list of email addresses of the recipients.
     * @param link A URL to be included in the email message.
     */
    fun sendEmail(emailType: EmailType, recipients: List<String>, link: String) : Boolean
}