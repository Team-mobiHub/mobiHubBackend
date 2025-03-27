package com.mobihub.utils.file

import com.mobihub.utils.file.exceptions.UnexpectedHttpResponse

/**
 * Interface for downloading files.
 *
 * @author Team-MobiHub
 */
interface Downloader {

    /**
     * Gets the Share Link for a file on the server.
     *
     * @param reference The reference to the file on the server.
     * @return The Share Link for the file.
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
    fun downloadFile(reference: String): java.io.File
}