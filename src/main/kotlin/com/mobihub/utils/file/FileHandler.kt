package com.mobihub.utils.file

import java.io.File
import com.mobihub.utils.file.exceptions.InvalidFileException
import com.mobihub.utils.file.exceptions.UnexpectedHttpResponse

/**
 * Interface for handling file uploads and downloads.
 *
 * @author Team-MobiHub
 */
interface FileHandler {

    /**
     * Uploads a file to the server.
     *
     * @param file The file to upload.
     * @param targetFilePath The name of the file on the server.
     *
     * @throws InvalidFileException if there is an issue with the file.
     * @throws UnexpectedHttpResponse if the server responds with an unexpected status code.
     */
    fun uploadFile(
        file: File, targetFilePath: String
    )

    /**
     * Deletes a file from the server.
     *
     * @param targetFilePath The name of the file on the server.
     */
    fun removeFile(targetFilePath: String)

    /**
     * Gets the download reference for a file on the server.
     *
     * @param reference The reference to the file on the server.
     * @param shouldExpire Whether the download reference should expire.
     * @return The download reference for the file.
     *
     * @throws UnexpectedHttpResponse if the server responds with an unexpected status code.
     */
    fun getDownloadReference(reference: String, shouldExpire: Boolean): ShareLink

    /**
     * Downloads a file from the server.
     *
     * @param reference The reference to the file on the server.
     * @return The downloaded file.
     *
     * @throws UnexpectedHttpResponse if the server responds with an unexpected status code.
     */
    fun downloadFile(reference: String): File
}