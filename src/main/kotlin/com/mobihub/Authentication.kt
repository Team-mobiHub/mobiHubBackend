package com.mobihub

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.mobihub.model.UserId
import com.mobihub.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

// Constants
const val TOKEN_EXPIRED_ERROR = "Token is not valid or has expired"

const val AUTH_JWT = "auth-jwt"
const val AUTH_JWT_OPTIONAL = "auth-jwt-opt"
const val JWT_SECRET = "jwt.secret"
const val JWT_ISSUER = "jwt.issuer"
const val JWT_AUDIENCE = "jwt.audience"
const val JWT_REALM = "jwt.realm"

/**
 * Function to configure the authentication for the Ktor application.
 *
 * @param userService The user service to use for authentication.
 */
fun Application.configureAuthentication(userService: UserService) {

    // Retrieve JWT configuration from environment
    val secret = environment.config.property(JWT_SECRET).getString()
    val issuer = environment.config.property(JWT_ISSUER).getString()
    val audience = environment.config.property(JWT_AUDIENCE).getString()
    val myRealm = environment.config.property(JWT_REALM).getString()


    install(Authentication) {
        jwt(AUTH_JWT) {
            realm = myRealm
            verifier(secret, audience, issuer)
            validate(audience, userService)
            challenge { _, _ ->
                call.respondText(TOKEN_EXPIRED_ERROR, status = HttpStatusCode.Unauthorized)
            }
        }

        jwt(AUTH_JWT_OPTIONAL) {
            realm = myRealm
            verifier(secret, audience, issuer)
            validate(audience, userService)
            challenge { _, _ -> }
        }
    }
}

/**
 * Extension function to configure the JWT validation.
 *
 * @param audience The expected audience of the JWT.
 * @param userService The user service to use for retrieving user information.
 */
private fun JWTAuthenticationProvider.Config.validate(
    audience: String,
    userService: UserService
) {
    validate { credential ->
        if (credential.payload.audience.contains(audience)) {
            credential.payload.claims["userId"]?.asInt()?.let { userService.getById(UserId(it)) }
        } else {
            null
        }
    }
}

/**
 * Extension function to configure the JWT verifier.
 *
 * @param secret The secret key used to sign the JWT.
 * @param audience The expected audience of the JWT.
 * @param issuer The expected issuer of the JWT.
 */
private fun JWTAuthenticationProvider.Config.verifier(
    secret: String,
    audience: String,
    issuer: String
) {
    verifier(
        JWT
            .require(Algorithm.HMAC256(secret))
            .withAudience(audience)
            .withIssuer(issuer)
            .build()
    )
}

