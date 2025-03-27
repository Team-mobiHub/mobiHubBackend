package com.mobihub.utils.email

import io.ktor.server.plugins.*


private const val ACCOUNT_CONFIRMATION_TITLE = "mobiHub Account Confirmation"
private const val TRANSFER_OWNERSHIP_TITLE = "mobiHub Model Ownership Transfer"
private const val INVITE_TO_TEAM_TITLE = "mobiHub Team Invitation"
private const val RESET_PASSWORD_TITLE = "mobiHub Password Reset"

private const val ACCOUNT_CONFIRMATION_HTML_FILE_NAME = "account-confirmation.html"
private const val TRANSFER_OWNERSHIP_HTML_FILE_NAME = "transfer-ownership.html"
private const val INVITE_TO_TEAM_HTML_FILE_NAME = "team-invitation.html"
private const val RESET_PASSWORD_HTML_FILE_NAME = "password-reset.html"

private const val EMAIL_TEMPLATES_PATH = "/templates/email/%s"

/**
 *  Enum representing the different possible types of emails to be sent,
 *  as well as defining methods for generating [EmailMessage] objects.
 *
 * @property id A unique identifier for the email type.
 *
 * @author mobiHub
 */
enum class EmailType(val id: Int) {
    ACCOUNT_CONFIRMATION(0) {
        override val subject = ACCOUNT_CONFIRMATION_TITLE;
        override val emailContent = getHTMLBody(ACCOUNT_CONFIRMATION_HTML_FILE_NAME);
    },
    TRANSFER_OWNERSHIP(1) {
        override val subject = TRANSFER_OWNERSHIP_TITLE;
        override val emailContent = getHTMLBody(TRANSFER_OWNERSHIP_HTML_FILE_NAME);
    },
    INVITE_TO_TEAM(2) {
        override val subject = INVITE_TO_TEAM_TITLE;
        override val emailContent = getHTMLBody(INVITE_TO_TEAM_HTML_FILE_NAME);
    },
    RESET_PASSWORD(3) {
        override val subject = RESET_PASSWORD_TITLE;
        override val emailContent = getHTMLBody(RESET_PASSWORD_HTML_FILE_NAME);
    };

    /**
     * A subject line for the email message
     */
    abstract val subject: String;

    /**
     * The body of the email.
     */
    abstract val emailContent: String;

    /**
     * Generates an [EmailMessage] according to this enum type including the subject line and body content for each type.
     *
     * @param recipients The list of recipients for the message
     * @param link The URL to be included in the message.
     */
    fun getMessage(recipients: List<String>, link: String): EmailMessage {
        val bodyWithLink = emailContent.replace("{{confirmationLink}}", link);
        return EmailMessage(recipients, this.subject, bodyWithLink);
    }

    /**
     * Locates the email HTML Template resource that is requested in [fileName]
     * and converts it to a multi-line [String] object.
     *
     * @param fileName The name of the HTML file to be found
     */
    fun getHTMLBody(fileName: String): String {
        val body = object {}.javaClass.getResourceAsStream(EMAIL_TEMPLATES_PATH.format(fileName))?.bufferedReader()?.readLines()

        if (body == null) {
            throw NotFoundException();
        }

        val sb = StringBuilder()
        for (line in body) {
            sb.append(line)
            sb.append("\n");
        }
        return sb.toString();
    }

}