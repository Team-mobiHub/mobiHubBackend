package com.mobihub.utils.verifier.exceptions

private const val ERROR_MESSAGE = "Invalid %s: %s."

/**
 * Exception thrown when a credential is invalid.
 *
 * @property tokenType The type of token that was invalid.
 * @property value The value of the token that was invalid.
 *
 * @author Team-MobiHub
 */
class InvalidCredentialException(val tokenType: String, val value: String) : Exception() {
    override val message: String
        get() = ERROR_MESSAGE.format(tokenType, value)
}