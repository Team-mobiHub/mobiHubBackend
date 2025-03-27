package com.mobihub.utils.file

import com.mobihub.utils.file.exceptions.UnexpectedHttpResponse
import io.ktor.http.*
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Template for the path to the file on the NextCloud server. This is the final location of the file.
 *
 * Usage: FILE_PATH_TEMPLATE.format(baseUrl, username, targetFileName)
 */
const val FILE_PATH_TEMPLATE = "%s/remote.php/dav/files/%s/%s"

private const val COULD_NOT_CREATE_DIRECTORY_ERROR = """
Could not create directory
Response code was: %s
Response body:
%s
"""

private const val CREATED_DIRECTORY_INFO = "Created directory for file: %s"

/**
 * The Nextcloud directory creator. Creates directories for files on a Nextcloud instance.
 *
 * @property baseUrl The base URL of the Nextcloud instance.
 * @property authenticator The authenticator to use for authentication.
 * @property client The HTTP client to use for the requests.
 *
 * @author Team-MobiHub
 */
class NextcloudDirectoryCreator(
    val baseUrl: String, val authenticator: Authenticator, val client: HttpClient
) {

    private val log = LoggerFactory.getLogger(NextcloudDirectoryCreator::class.java)

    /**
     * Creates the directory for the file.
     *
     * @param pathToFile The path to the file.
     */
    fun createDirectoryForFile(pathToFile: String) {
        val filePathArray = pathToFile.substringBeforeLast("/").split("/")

        for (i in filePathArray.indices) {
            val path = filePathArray.subList(0, i + 1).joinToString("/")
            createDirectory(path)
        }
        log.info(CREATED_DIRECTORY_INFO.format(pathToFile))
    }

    private fun createDirectory(path: String) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(FILE_PATH_TEMPLATE.format(baseUrl, authenticator.getUsername(), path)))
            .method("MKCOL", HttpRequest.BodyPublishers.noBody()).apply { authenticator.authenticate(this) }.build()

        val result = client.send(request, HttpResponse.BodyHandlers.ofString())
        // Check if the response was successful or if the directory already exists (The server responds with 405 if the directory already exists)
        require(result.statusCode() in RESPONSE_SUCCESSFUL || result.statusCode() == HttpStatusCode.MethodNotAllowed.value) {
            log.error(COULD_NOT_CREATE_DIRECTORY_ERROR.format(result.statusCode(), result.body()))
            throw UnexpectedHttpResponse(
                COULD_NOT_CREATE_DIRECTORY_ERROR.format(
                    result.statusCode(), result.body()
                )
            )

        }
    }
}