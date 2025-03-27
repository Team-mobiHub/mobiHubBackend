package com.mobihub.utils.file

import com.mobihub.utils.file.exceptions.UnexpectedHttpResponse
import io.ktor.http.*
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private const val REMOVE_FILE_ERROR = """
Could not remove file
Response code was: %s
Response body: %s
"""

private const val REMOVE_TEMPLATE = "%s/remote.php/dav/files/%s/%s"

private const val START_REMOVING_FILE = "Start removing file at %s"
private const val SUCCESSFULLY_REMOVED_FILE = "Successfully removed file at %s"

/**
 * The Nextcloud remover. Removes files from a Nextcloud instance.
 * Implements the [Remover] interface.
 *
 * @property baseUrl The base URL of the Nextcloud instance.
 * @property authenticator The authenticator to use for authentication.
 * @property client The HTTP client to use for the requests.
 *
 * @author Team-MobiHub
 */
class NextcloudRemover(
    val baseUrl: String, val authenticator: Authenticator, val client: HttpClient
) : Remover {
    private val log = LoggerFactory.getLogger(NextcloudRemover::class.java)

    override fun removeFile(targetFilePath: String) {
        log.info(START_REMOVING_FILE.format(targetFilePath))
        val request = HttpRequest.newBuilder()
            .uri(URI.create(REMOVE_TEMPLATE.format(baseUrl, authenticator.getUsername(), targetFilePath)))
            .apply { authenticator.authenticate(this) }
            .DELETE()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in RESPONSE_SUCCESSFUL && response.statusCode() != HttpStatusCode.NotFound.value) {
            throw UnexpectedHttpResponse(REMOVE_FILE_ERROR.format(response.statusCode(), response.body()))
        }
        log.info(SUCCESSFULLY_REMOVED_FILE.format(targetFilePath))
    }
}
