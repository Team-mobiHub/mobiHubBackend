package com.mobihub.utils.file

import java.net.http.HttpRequest

/**
 * Interface for adding authentication to HTTP requests.
 *
 * @author Team-MobiHub
 */
interface Authenticator {

    /**
     * Adds authentication to the request.
     *
     * @param builder The request the authentication should be added to
     * @return the request including the authentication
     */
    fun authenticate(builder: HttpRequest.Builder): HttpRequest.Builder

    /**
     * Gets the username for the authentication.
     *
     * @return The username for the authentication.
     */
    fun getUsername(): String
}