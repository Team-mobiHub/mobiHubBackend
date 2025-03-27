package com.mobihub.utils.file

import java.io.File
import java.net.http.HttpClient

/**
 * All responses that are considered successful.
 */
val RESPONSE_SUCCESSFUL = 200..299

/**
 * File handler for Nextcloud. Handles file uploads and downloads.
 * Implements the [FileHandler] interface.
 *
 * @property baseUrl The base URL of the Nextcloud instance.
 * @property authenticator The authenticator to use for authentication.
 * @property client The HTTP client to use for the requests.
 *
 * @author Team-MobiHub
 */
class NextcloudFileHandler(
    val baseUrl: String,
    val authenticator: Authenticator
) : FileHandler {

    private val client: HttpClient = HttpClient.newBuilder().build()
    private val nextcloudChunkedUploader: Uploader =
        NextcloudChunkedUploader(baseUrl = baseUrl, authenticator = authenticator, client = client)
    private val nextcloudSimpleUploader: Uploader =
        NextcloudSimpleUploader(baseUrl = baseUrl, authenticator = authenticator, client = client)
    private val nextcloudDownloader: Downloader =
        NextcloudDownloader(baseUrl = baseUrl, authenticator = authenticator, client = client)
    private val nextcloudRemover: Remover =
        NextcloudRemover(baseUrl = baseUrl, authenticator = authenticator, client = client)



    override fun uploadFile(file: File, targetFilePath: String) {
        if (file.length() > 10000000) {
            nextcloudChunkedUploader.uploadFile(file, targetFilePath)
        } else {
            nextcloudSimpleUploader.uploadFile(file, targetFilePath)
        }
    }

    override fun removeFile(targetFilePath: String) {
        nextcloudRemover.removeFile(targetFilePath)
    }

    override fun getDownloadReference(reference: String, shouldExpire: Boolean): ShareLink {
        return nextcloudDownloader.getDownloadReference(reference, shouldExpire)
    }

    override fun downloadFile(reference: String): File {
        return nextcloudDownloader.downloadFile(reference)
    }
}
