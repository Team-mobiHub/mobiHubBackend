package com.mobihub

import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Function to configure the health check for the Ktor application.
 */
fun Routing.healthCheck() {
    get("/healthCheck") {
        call.respondText("I'm alive!")
    }
}