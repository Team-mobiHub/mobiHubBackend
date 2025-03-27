package com.mobihub.utils.file

import java.net.http.HttpRequest

private const val AUTHORIZATION_HEADER_KEY = "Authorization"
private const val AUTHORIZATION_HEADER_VALUE_TEMPLATE = "Basic %s"
private const val AUTHORIZATION_USER_PASSWORD_TEMPLATE = "%s:%s"

/**
 * Basic authentication for HTTP requests. Implements the [Authenticator] interface.
 *
 * @property username The username for the authentication.
 * @property password The password for the authentication.
 *
 * @author Team-MobiHub
 */
class BasicAuth(
    private val username: String, private val password: String
) : Authenticator {

    override fun authenticate(builder: HttpRequest.Builder): HttpRequest.Builder {
        return builder.header(
            AUTHORIZATION_HEADER_KEY, AUTHORIZATION_HEADER_VALUE_TEMPLATE.format(
                java.util.Base64.getEncoder()
                    .encodeToString(AUTHORIZATION_USER_PASSWORD_TEMPLATE.format(username, password).toByteArray())
            )
        )
    }

    override fun getUsername(): String {
        return username
    }
}