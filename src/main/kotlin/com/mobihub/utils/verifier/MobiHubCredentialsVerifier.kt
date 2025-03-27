package com.mobihub.utils.verifier

import com.mobihub.utils.verifier.exceptions.InvalidCredentialException
import java.util.regex.Pattern

/**
 * This regex is used to verify the email address.
 */
private val emailRegex = Pattern.compile(
    "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]|[\\w-]{2,}))@"
            + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
            + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
            + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9]))|"
            + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$"
)

private const val MAX_EMAIL_LENGTH = 256
private const val MIN_PASSWORD_LENGTH = 8
private const val MAX_PASSWORD_LENGTH = 60
private const val MIN_NAME_LENGTH = 3
private const val MAX_NAME_LENGTH = 60

/**
 * CredentialsVerifier for the MobiHub platform.
 * - Checks the email to conform to the ISO-Norm
 * - Checks the password to have 6 to 60 characters wit at least: 1 lowercase, 1 uppercase letter, 1 non-alphanumeric character
 * - Checks the name to have at 6 to 60 characters
 *
 * @property name the name to be verified
 * @property email the email to be verified
 * @property password the password to be verified
 *
 * @author Team-MobiHub
 */
data class MobiHubCredentialsVerifier(
    val name: String?,
    val email: String?,
    val password: String?) : CredentialsVerifier {

    override fun verify(verifyName: Boolean, verifyEmail: Boolean, verifyPassword: Boolean) {
        if(verifyEmail) {
            if (email == null) throw InvalidCredentialException("email", "")
            require(
                emailRegex.matcher(this.email).matches() &&
                        this.email.length <= MAX_EMAIL_LENGTH
            ) {
                throw InvalidCredentialException("email", email)
            }
        }

        if(verifyPassword) {
            if (password == null) throw InvalidCredentialException("password", "")
            require(
                this.password.length in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH &&
                        this.password.any { it.isUpperCase() } &&
                        this.password.any { it.isLowerCase() } &&
                        this.password.any { !it.isLetterOrDigit() }
            ) {
                throw InvalidCredentialException("password", "")
            }
        }

        if(verifyName) {
            if (name == null) throw InvalidCredentialException("name", "")
            require(
                this.name.length in MIN_NAME_LENGTH..MAX_NAME_LENGTH
            ) {
                throw InvalidCredentialException("username", name)
            }
        }
    }
}
