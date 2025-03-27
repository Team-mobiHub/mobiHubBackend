package com.mobihub

import com.mobihub.repositories.db.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

// Constants
const val H2_CONNECTION_STRING = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
const val H2_USER = "root"
const val H2_PASSWORD = ""
const val POSTGRES_URL_PATH = "postgres.url"
const val POSTGRES_USER_PATH = "postgres.user"
const val POSTGRES_PASSWORD_PATH = "postgres.password"
const val EMBEDDED_DB_LOG_MESSAGE = "Using embedded H2 database for testing; replace this flag to use postgres"
const val POSTGRES_DB_LOG_MESSAGE = "Connecting to postgres database at %s"
private val log = LoggerFactory.getLogger(Application::class.java)


/**
 * Configures the database connection.
 */
fun Application.configureDatabases() {
    val url = environment.config.property(POSTGRES_URL_PATH).getString()
    log.info(POSTGRES_DB_LOG_MESSAGE.format(url))

    val password = environment.config.property(POSTGRES_PASSWORD_PATH).getString()
    val userName = environment.config.property(POSTGRES_USER_PATH).getString()
    log.info("User: $userName")

    Database.connect(
        url = url,
        user = userName,
        password = password
    )

    setUpDatabase()
}

/**
 * Sets up the database schema.
 */
fun Application.setUpDatabase() {
    transaction {
        SchemaUtils.createMissingTablesAndColumns(
            UserTable,
            TeamTable,
            MembershipTable,
            LinkTokensTable,
            TrafficModelTable,
            RatingTable,
            CommentTable,
            CharacteristicsMappingTable,
            FavouriteTable,
            ImageMappingTable
        )
    }
}

/**
 * Makes a connection to a Postgres database.
 *
 * In order to connect to your running Postgres process,
 * please specify the following parameters in your configuration file:
 * - postgres.url -- Url of your running database process.
 * - postgres.user -- Username for database connection
 * - postgres.password -- Password for database connection
 *
 * If you don't have a database process running yet, you may need to [download]((https://www.postgresql.org/download/))
 * and install Postgres and follow the instructions [here](https://postgresapp.com/).
 * Then, you would be able to edit your url,  which is usually "jdbc:postgresql://host:port/database", as well as
 * user and password values.
 *
 *
 * @param embedded -- if [true] defaults to an embedded database for tests that runs locally in the same process.
 * In this case you don't have to provide any parameters in configuration file, and you don't have to run a process.
 *
 * @return [Connection] that represent connection to the database. Please, don't forget to close this connection when
 * your application shuts down by calling [Connection.close]
 * */
fun Application.connectToPostgres(embedded: Boolean): Connection {
    Class.forName("org.postgresql.Driver")
    return if (embedded) {
        log.info(EMBEDDED_DB_LOG_MESSAGE)
        DriverManager.getConnection(H2_CONNECTION_STRING, H2_USER, "")
    } else {
        val url = environment.config.property(POSTGRES_URL_PATH).getString()
        log.info(POSTGRES_DB_LOG_MESSAGE.format(url))
        val user = environment.config.property(POSTGRES_USER_PATH).getString()
        val password = environment.config.property(POSTGRES_PASSWORD_PATH).getString()

        DriverManager.getConnection(url, user, password)
    }
}
