package com.mobihub.controllers

import com.mobihub.TokenBlackList
import com.mobihub.model.FileType
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*

/**
 * Function to configure the routes of the Ktor application, concerning the user interactions with traffic models.
 */
fun Routing.interactions(interactionController: InteractionController) {
    comment(interactionController)

    rating(interactionController)

    favorite(interactionController)
}

/**
 * Configures the routes for comments.
 */
private fun Routing.comment(interactionController: InteractionController) {
    authenticateWithBlacklistCheck("auth-jwt") {
        route("/comment") {
            post {
                interactionController.addComment(call)
            }
            put("/{commentId}") {
                interactionController.updateComment(call)
            }
            delete("/{commentId}") {
                interactionController.deleteComment(call)
            }
        }
    }
}

/**
 * Configures the routes for ratings.
 */
private fun Routing.rating(interactionController: InteractionController) {
    authenticateWithBlacklistCheck("auth-jwt") {
        route("/rating") {
            post("/{trafficModelId}/{rating}") {
                interactionController.addRating(call)
            }

            put("/{trafficModelId}/{rating}") {
                interactionController.updateRating(call)
            }

            delete("/{userId}/{trafficModelId}") {
                interactionController.deleteRating(call)
            }
        }
    }
}

/**
 * Configures the routes for favorites.
 */
private fun Routing.favorite(interactionController: InteractionController) {
    authenticateWithBlacklistCheck("auth-jwt") {
        route("/favorite") {
            post("{trafficModelId}") {
                interactionController.makeToFavourite(call)
            }

            delete("{trafficModelId}") {
                interactionController.removeFavourite(call)
            }

            get {
                interactionController.getFavoritesOfUser(call)
            }
        }
    }
}

/**
 * Function to configure the routes of the Ktor application, concerning the traffic models.
 */
fun Routing.trafficModels(trafficModelController: TrafficModelController) {
    route("/trafficModel") {
        authenticateWithBlacklistCheck("auth-jwt") {
            post {
                trafficModelController.create(call)
            }
        }

        authenticateWithBlacklistCheck("auth-jwt-opt") {
            get("/{modelId}") {
                trafficModelController.getById(call)
            }
        }
        authenticateWithBlacklistCheck("auth-jwt") {
            put("/{modelId}") {
                trafficModelController.update(call)
            }
        }

        authenticateWithBlacklistCheck("auth-jwt") {
            delete("/{modelId}") {
                trafficModelController.delete(call)
            }
        }

        post("search") {
            trafficModelController.searchModel(call)
        }

        authenticateWithBlacklistCheck("auth-jwt-opt") {
            get("{ownerType}/{ownerId}") {
                trafficModelController.getByOwner(call)
            }
        }

        authenticateWithBlacklistCheck("auth-jwt") {
            post("{modelId}/uploadZip/{token}") {
                trafficModelController.uploadFile(call, FileType.ZIP)
            }
        }

        authenticateWithBlacklistCheck("auth-jwt") {
            post("{modelId}/uploadImage/{token}") {
                trafficModelController.uploadFile(call, FileType.IMAGE)
            }
        }

        get("{modelId}/download") {
            trafficModelController.getDownloadLink(call)
        }

        authenticateWithBlacklistCheck("auth-jwt") {
            post("{modelId}/transfer") {
                trafficModelController.getTransferOwnershipLink(call)
            }
        }

        authenticateWithBlacklistCheck("auth-jwt") {
            post("{modelId}/{email}/transfer") {
                trafficModelController.sendTransferOwnershipLinkByEmail(call)
            }
        }

        authenticateWithBlacklistCheck("auth-jwt") {
            get("transfer/accept/{token}/{ownerType}/{ownerId}") {
                trafficModelController.useTransferOwnershipLink(call)
            }
        }
    }
}

/**
 * Function to configure the routes of the Ktor application, concerning the teams.
 */
fun Routing.teams(teamController: TeamController) {
    authenticateWithBlacklistCheck("auth-jwt") {
        route("/team") {
            post {
                teamController.create(call)
            }
            get("/{teamId}") {
                teamController.getById(call)
            }
            put {
                teamController.update(call)
            }
            get("/{userId}") {
                teamController.getForUser(call)
            }
            delete("/{teamId") {
                teamController.delete(call)
            }
        }
    }
}

/**
 * Function to configure the routes of the Ktor application, concerning the users.
 */
fun Routing.users(userController: UserController) {
    route("/user") {
        post {
            userController.create(call)
        }

        get("/{userId}") {
            userController.getById(call)
        }

        authenticateWithBlacklistCheck("auth-jwt") {
            get("/email/{email}") {
                userController.getByEmail(call)
            }
        }

        authenticateWithBlacklistCheck("auth-jwt") {
            put {
                userController.update(call)
            }
        }

        authenticateWithBlacklistCheck("auth-jwt") {
            delete("/{userId}") {
                userController.delete(call)
            }
        }
    }
}

/**
 * Function to configure the routes of the Ktor application, concerning authentication.
 */
fun Routing.auth(userController: UserController) {
    route("auth") {
        post("/login") {
            userController.login(call)
        }

        authenticateWithBlacklistCheck("auth-jwt") {
            post("/logout") {
                userController.logout(call)
            }
        }

        authenticateWithBlacklistCheck("auth-jwt") {
            put("/change-password") {
                userController.changePassword(call)
            }
        }

        post("/verify-email/{email}") {
            userController.handleEmailVerificationFromLink(call)
        }

        post("/reset-password/start/{email}") {
            userController.startResetPasswordProcess(call)
        }

        put("/reset-password/complete/{uuid}") {
            userController.resetPasswordSetNew(call)
        }
    }
}

/**
 * Checks if a token from the authorization header is blacklisted.
 * @return `true` if the token is valid, `false` if blacklisted.
 */
private fun ApplicationCall.isTokenValid(): Boolean {
    val authHeader = request.header("Authorization") ?: return true
    val token = authHeader.removePrefix("Bearer ")
    return !TokenBlackList.contains(token)
}

/**
 * Function to add a plugin to the route that checks if the token is blacklisted.
 *
 * @param configurations the configurations for the authentication
 * @param optional whether the authentication is optional
 * @param build the route configuration
 */
fun Route.authenticateWithBlacklistCheck(
    vararg configurations: String,
    optional: Boolean = false,
    build: Route.() -> Unit
) {
    authenticate(*configurations, optional = optional) {
        install(createRouteScopedPlugin("TokenBlacklistCheck") {
            onCall { call ->
                if (!call.isTokenValid()) {
                    call.respond(HttpStatusCode.Unauthorized, "Token is blacklisted")
                    return@onCall
                }
            }
        })

        build()
    }
}

