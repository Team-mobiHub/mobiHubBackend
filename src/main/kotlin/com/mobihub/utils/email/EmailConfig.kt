package com.mobihub.utils.email

/**
 * Configuration class for email server settings.
 *
 * This class contains the necessary information required to connect to an
 * email server and send emails.
 *
 * @property host The hostname or IP address of the email server.
 * @property port The port number used to connect to the email server.
 * @property username The username for authentication with the email server.
 * @property password The password for authentication with the email server.
 * @property fromAddress The address the recipient will see it got the email from.
 *
 * @author Team-MobiHub
 */
class EmailConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val fromAddress: String
)