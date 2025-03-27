package com.mobihub.utils.verifier

/**
 * An Interface for verifying matching criteria for a name, email, and password
 *
 * @author Team-Mobihub
 */
    fun interface CredentialsVerifier {

    /**
     * verifies the credentials for which the booleans are true
     * @param verifyName a boolean flag to verify the name
     * @param verifyEmail a boolean flag to verify the email
     * @param verifyPassword a boolean flag to verify the password
    */
    fun verify(verifyName : Boolean, verifyEmail: Boolean, verifyPassword: Boolean)
}