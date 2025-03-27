package com.mobihub.utils.file

import com.mobihub.utils.file.exceptions.InvalidFileException
import com.mobihub.utils.file.exceptions.UnexpectedHttpResponse

/**
 * Interface for uploading files to a target location.
 *
 * Implementations of this interface should be able to upload files to a target location.
 *
 * @author Team-MobiHub
 */
fun interface Uploader {
    /**
     * Uploads a file to a target location.
     *
     * @param file The file to upload.
     * @param targetFilePath The name of the file on the server.
     *
     * @throws InvalidFileException if the file does not exist or is empty.
     * @throws UnexpectedHttpResponse if the server responds with an unexpected status code.
     */
    fun uploadFile(
        file: java.io.File, targetFilePath: String
    )
}