package com.mobihub.utils.file

import com.mobihub.utils.file.exceptions.InvalidFileException
import com.mobihub.utils.file.exceptions.UnexpectedHttpResponse
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private const val NO_FILE_ERROR = "The file with the path %s does not exist or is empty"
private const val FILE_TOO_LARGE_ERROR = "The file with the path %s is too large"

private const val COULD_NOT_UPLOAD_FILE_ERROR = """
Could not upload chunk
Response code was: %s
Response body: 
%s
"""

private const val UPLOAD_FILE_INFO = "Uploaded File %s"

/**
 * Uploader that uploads a file to a NextCloud server, without chunking the file.
 *
 * @property baseUrl The base URL of the NextCloud server.
 * @property authenticator The basic authentication credentials for the NextCloud server.
 * @property client The HTTP client to use for the upload.
 *
 * @author Team-MobiHub
 */
class NextcloudSimpleUploader(
    val baseUrl: String, val authenticator: Authenticator, val client: HttpClient
) : Uploader {

    private val log = LoggerFactory.getLogger(NextcloudSimpleUploader::class.java)
    private val directoryCreator = NextcloudDirectoryCreator(baseUrl, authenticator, client)

    override fun uploadFile(file: java.io.File, targetFilePath: String) {
        require(file.exists() && file.length() > 0) {
            log.error(NO_FILE_ERROR.format(file.name))
            throw InvalidFileException(
                NO_FILE_ERROR.format(file.name)
            )
        }

        require(file.length() <= 10e6) {
            log.error(FILE_TOO_LARGE_ERROR.format(file.name))
            throw InvalidFileException(
                FILE_TOO_LARGE_ERROR.format(file.name)
            )
        }

        directoryCreator.createDirectoryForFile(targetFilePath)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(FILE_PATH_TEMPLATE.format(baseUrl, authenticator.getUsername(), targetFilePath)))
            .PUT(HttpRequest.BodyPublishers.ofByteArray(file.readBytes())).apply { authenticator.authenticate(this) }
            .build()

        val result = client.send(request, HttpResponse.BodyHandlers.ofString())

        require(result.statusCode() in RESPONSE_SUCCESSFUL) {
            log.error(
                COULD_NOT_UPLOAD_FILE_ERROR.format(
                    result.statusCode(), result.body()
                )
            )
            throw UnexpectedHttpResponse(
                COULD_NOT_UPLOAD_FILE_ERROR.format(
                    result.statusCode(), result.body()
                )
            )
        }
        log.info(UPLOAD_FILE_INFO.format(targetFilePath))
    }
}