package com.mobihub

import com.mobihub.controllers.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

/**
 * Function to configure the routing for the Ktor application.
 *
 * @param interactionController The interaction controller to use for handling interactions.
 * @param trafficModelController The traffic model controller to use for handling traffic models.
 * @param teamController The team controller to use for handling teams.
 * @param userController The user controller to use for handling users.
 */
fun Application.configureRouting(
    interactionController: InteractionController,
    trafficModelController: TrafficModelController,
    teamController: TeamController,
    userController: UserController
) {
    val log = LoggerFactory.getLogger(Application::class.java)

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            log.error("An error occurred", cause)

            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        users(userController)
        auth(userController)
        teams(teamController)
        trafficModels(trafficModelController)
        interactions(interactionController)

        healthCheck()
    }
}
