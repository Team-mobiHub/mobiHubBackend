package com.mobihub

import com.mobihub.controllers.InteractionController
import com.mobihub.controllers.TeamController
import com.mobihub.controllers.TrafficModelController
import com.mobihub.controllers.UserController
import com.mobihub.dtos.serializer.InstantSerializer
import com.mobihub.repositories.RepositoryProvider
import com.mobihub.services.*
import com.mobihub.utils.email.EmailConfig
import com.mobihub.utils.email.EmailService
import com.mobihub.utils.file.BasicAuth
import com.mobihub.utils.file.NextcloudFileHandler
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.serializersModuleOf
import java.time.Instant

/**
 * Main function to start the Ktor server.
 *
 * @param args The command line arguments.
 */
fun main(args: Array<String>) {
    EngineMain.main(args)
}

/**
 * Function to configure the Ktor application.
 */
fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            serializersModule = serializersModuleOf(Instant::class, InstantSerializer)
        })
    }

    val emailService = EmailService(getEmailConfig())

    val nextcloudFileHandler = getNextcloudFileHandler()

    // initialize Repositories
    val repositoryProvider = RepositoryProvider(nextcloudFileHandler)

    // initialize Services
    val linkService = LinkService(repositoryProvider.linkTokensRepository, environment)
    val interactionService =
        InteractionService(
            repositoryProvider.ratingRepository,
            repositoryProvider.favouriteRepository,
            repositoryProvider.commentRepository,
            repositoryProvider.userRepository,
            repositoryProvider.trafficModelRepository,
            environment
        )
    val trafficModelService = TrafficModelService(
        repositoryProvider.trafficModelRepository,
        repositoryProvider.characteristicsMappingRepository,
        repositoryProvider.userRepository,
        repositoryProvider.teamRepository,
        linkService = linkService,
        fileHandler = nextcloudFileHandler,
        environment = environment
    )
    val teamService = TeamService(
        repositoryProvider.teamRepository,
        repositoryProvider.trafficModelRepository,
        repositoryProvider.userRepository,
        linkService,
        emailService,
        environment
    )
    val userService = UserService(
        trafficModelService,
        repositoryProvider.userRepository,
        repositoryProvider.favouriteRepository,
        repositoryProvider.trafficModelRepository,
        repositoryProvider.commentRepository,
        repositoryProvider.ratingRepository,
        repositoryProvider.teamRepository,
        linkService,
        emailService,
        nextcloudFileHandler, environment
    )

    // initialize Controllers
    val interactionController = InteractionController(interactionService)
    val trafficModelController = TrafficModelController(trafficModelService)
    val teamController = TeamController(teamService)
    val userController = UserController(userService)

    // Config
    configureCors()
    configureDatabases()
    configureAuthentication(userService)
    configureRouting(interactionController, trafficModelController, teamController, userController)
}


/**
 * Retrieves the NextcloudFileHandler instance configured with the application environment properties.
 *
 * @return NextcloudFileHandler instance
 */
private fun Application.getNextcloudFileHandler() = NextcloudFileHandler(
    baseUrl = environment.config.property("nextcloud.baseUrl").getString(),
    authenticator = BasicAuth(
        username = environment.config.property("nextcloud.username").getString(),
        password = environment.config.property("nextcloud.password").getString()
    ),
)

/**
 * Retrieves the EmailConfig instance configured with the application environment properties.
 *
 * @return EmailConfig instance
 */
private fun Application.getEmailConfig() = EmailConfig(
    host = environment.config.property("email.host").getString(),
    port = environment.config.property("email.port").getString().toInt(),
    username = environment.config.property("email.username").getString(),
    password = environment.config.property("email.password").getString(),
    fromAddress = environment.config.property("email.fromAddress").getString(),
)

/**
 * Configures the CORS plugin for the application.
 */
private fun Application.configureCors() {
    install(CORS) {
        anyHost()

        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)

        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }
}

