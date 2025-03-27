package com.mobihub.utils.file

import com.mobihub.utils.file.exceptions.InvalidFileException
import com.mobihub.utils.file.exceptions.UnexpectedHttpResponse
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

// Error messages
private const val CHUNK_SIZE_ERROR = "Chunk size must be between 5MB and 5GB"
private const val NO_FILE_ERROR = "The file with the path %s does not exist or is empty"
private const val FILE_TOO_LARGE_ERROR = "The file with the path %s is too large for the specified chunk size (%sMB)"

private const val COULD_NOT_CREATE_UPLOAD_SESSION_ERROR = """
Could not create upload session
Response code was: %s
Response body: 
%s
"""
private const val COULD_NOT_UPLOAD_CHUNK_ERROR = """
Could not upload chunk
Response code was: %s
Response body: 
%s
"""
private const val COULD_NOT_ASSEMBLE_FILE_ERROR = """
Could not assemble file
Response code was: %s
Response body: 
%s
"""

// Logger Info
private const val CREATED_UPLOAD_SESSION_INFO = "Created upload session with ID: %s"
private const val UPLOADED_CHUNK_INFO = "Uploaded chunk %05d of size %.02fMB"
private const val ASSEMBLED_FILE_INFO = "Assembled file %s"

// Constants for the NextCloud API
/**
 * Template for the path to the uploads directory on the NextCloud server.
 * This directory is used to store the chunks of the file before they are assembled.
 * 24 hours after the directory is created, it is automatically deleted.
 *
 * Usage: UPLOADS_PATH_TEMPLATE.format(baseUrl, username, uploadId)
 */
private const val UPLOADS_PATH_TEMPLATE = "%s/remote.php/dav/uploads/%s/%s"

/**
 * Template for the path to a single chunk of the file on the NextCloud server.
 *
 * Usage: UPLOADS_PATH_TEMPLATE.format(baseUrl, username, uploadId, chunkNumber)
 */
private const val UPLOADS_UPLOAD_CHUNKS_PATH_TEMPLATE = "$UPLOADS_PATH_TEMPLATE/%04d"

/**
 * Template to the "assemble chunks" path on the NextCloud server.
 *
 * Usage: UPLOADS_PATH_TEMPLATE.format(baseUrl, username, uploadId)
 */
private const val UPLOADS_ASSEMBLE_CHUNKS_PATH_TEMPLATE = "$UPLOADS_PATH_TEMPLATE/.file"

private const val DESTINATION_HEADER_KEY = "Destination"
private const val TOTAL_FILE_LENGTH_HEADER_KEY = "OC-Total-Length"


/**
 * An uploader that uploads files to a NextCloud server in chunks.
 * This uploader is suitable for large files.
 *
 * @property baseUrl The base URL of the NextCloud server.
 * @property authenticator Basic Authentication for the Nextcloud server.
 * @property client The HttpClient to use for the requests.
 * @property chunkSize The size of the chunks in bytes.
 *
 * @author Team-MobiHub
 */
class NextcloudChunkedUploader(
    val baseUrl: String, val authenticator: Authenticator, val client: HttpClient,
    // Chunk size is set to 10MB by default
    private val chunkSize: Int = 10e6.toInt()
) : Uploader {

    private val log = LoggerFactory.getLogger(NextcloudChunkedUploader::class.java)
    private val directoryCreator = NextcloudDirectoryCreator(baseUrl, authenticator, client)

    /**
     * Initializes the NextcloudChunkedUploader.
     * The chunk size must be between 5MB and 5GB.
     */
    init {
        require(chunkSize > 5e6 && chunkSize < 5e9) {
            log.error(CHUNK_SIZE_ERROR)
            CHUNK_SIZE_ERROR
        }
    }

    override fun uploadFile(
        file: File, targetFilePath: String
    ) {
        require(file.exists() && file.length() > 0) {
            log.error(NO_FILE_ERROR.format(file.name))
            throw InvalidFileException(NO_FILE_ERROR.format(file.name))
        }

        require(file.length() < chunkSize * 10000L) {
            log.error(FILE_TOO_LARGE_ERROR.format(file.name, chunkSize / 1e6))
            throw InvalidFileException(FILE_TOO_LARGE_ERROR.format(file.name, chunkSize / 1e6))
        }

        // Create directory for the file
        directoryCreator.createDirectoryForFile(targetFilePath)

        val uploadId = java.util.UUID.randomUUID().toString()

        createUploadSession(uploadId, targetFilePath)

        val chunks = splitFileIntoChunks(file)

        chunks.forEachIndexed { index, chunk ->
            // The index shift is necessary because the index is zero-based and the chunk numbers are one-based
            uploadChunk(
                uploadId = uploadId,
                chunkNumber = index + 1,
                chunk = chunk,
                fullSize = file.length(),
                fileName = targetFilePath
            )
        }

        assembleChunks(
            uploadId = uploadId, totalFileSize = file.length(), fileName = targetFilePath
        )
    }

    /**
     * Creates a new upload session on the NextCloud server.
     *
     * @param uploadId The ID of the upload session.
     */
    private fun createUploadSession(uploadId: String, fileName: String) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(UPLOADS_PATH_TEMPLATE.format(baseUrl, authenticator.getUsername(), uploadId)))
            .method("MKCOL", HttpRequest.BodyPublishers.noBody()).apply { authenticator.authenticate(this) }.header(
                DESTINATION_HEADER_KEY, FILE_PATH_TEMPLATE.format(baseUrl, authenticator.getUsername(), fileName)
            ).build()
        val result = client.send(request, HttpResponse.BodyHandlers.ofString())
        require(result.statusCode() in RESPONSE_SUCCESSFUL) {
            log.error(COULD_NOT_CREATE_UPLOAD_SESSION_ERROR.format(result.statusCode(), result.body()))
            throw UnexpectedHttpResponse(
                COULD_NOT_CREATE_UPLOAD_SESSION_ERROR.format(
                    result.statusCode(), result.body()
                )
            )

        }
        log.info(CREATED_UPLOAD_SESSION_INFO.format(uploadId))
    }

    /**
     * Splits a file into chunks of the specified size.
     *
     * @param file The file to split.
     * @return A Sequence of byte arrays, each representing a chunk of the file.
     */
    private fun splitFileIntoChunks(file: File): Sequence<ByteArray> {
        return sequence {
            file.inputStream().buffered().use { inputStream ->
                val buffer = ByteArray(chunkSize)
                while (true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    yield(buffer.copyOf(bytesRead))
                }
            }
        }
    }

    /**
     * Uploads a chunk to the NextCloud server.
     *
     * @param uploadId The ID of the upload session.
     * @param chunkNumber The number of the chunk.
     * @param chunk The chunk to upload.
     * @param fullSize The size of the complete file
     * @param fileName The name of the file
     */
    private fun uploadChunk(
        uploadId: String, chunkNumber: Int, chunk: ByteArray, fullSize: Long, fileName: String
    ) {
        val request = HttpRequest.newBuilder().uri(
            URI.create(
                UPLOADS_UPLOAD_CHUNKS_PATH_TEMPLATE.format(
                    baseUrl, authenticator.getUsername(), uploadId, chunkNumber
                )
            )
        ).PUT(HttpRequest.BodyPublishers.ofByteArray(chunk)).apply { authenticator.authenticate(this) }
            .header(DESTINATION_HEADER_KEY, FILE_PATH_TEMPLATE.format(baseUrl, authenticator.getUsername(), fileName))
            .header(TOTAL_FILE_LENGTH_HEADER_KEY, fullSize.toString()).build()

        val result = client.send(request, HttpResponse.BodyHandlers.ofString())

        require(result.statusCode() in RESPONSE_SUCCESSFUL) {
            log.error(COULD_NOT_UPLOAD_CHUNK_ERROR.format(result.statusCode(), result.body()))
            throw UnexpectedHttpResponse(
                COULD_NOT_UPLOAD_CHUNK_ERROR.format(
                    result.statusCode(), result.body()
                )
            )
        }
        log.info(UPLOADED_CHUNK_INFO.format(chunkNumber, chunk.size / 1e6))
    }

    /**
     * Assembles the chunks of the file on the NextCloud server.
     *
     * @param uploadId The ID of the upload session.
     * @param totalFileSize The size of the complete file
     */
    private fun assembleChunks(uploadId: String, totalFileSize: Long, fileName: String) {
        val request = HttpRequest.newBuilder().uri(
            URI.create(
                UPLOADS_ASSEMBLE_CHUNKS_PATH_TEMPLATE.format(
                    baseUrl, authenticator.getUsername(), uploadId
                )
            )
        ).method("MOVE", HttpRequest.BodyPublishers.noBody()).apply { authenticator.authenticate(this) }
            .header(DESTINATION_HEADER_KEY, FILE_PATH_TEMPLATE.format(baseUrl, authenticator.getUsername(), fileName))
            .header(TOTAL_FILE_LENGTH_HEADER_KEY, totalFileSize.toString()).build()

        val result = client.send(request, HttpResponse.BodyHandlers.ofString())

        require(result.statusCode() in RESPONSE_SUCCESSFUL) {
            log.error(COULD_NOT_ASSEMBLE_FILE_ERROR.format(result.statusCode(), result.body()))
            throw UnexpectedHttpResponse(
                COULD_NOT_ASSEMBLE_FILE_ERROR.format(
                    result.statusCode(), result.body()
                )
            )
        }
        log.info(ASSEMBLED_FILE_INFO.format(fileName))
    }
}