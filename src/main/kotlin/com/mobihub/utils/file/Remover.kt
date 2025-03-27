package com.mobihub.utils.file

/**
 * Interface for removing files from the server.
 *
 * @author Team-MobiHub
 */
fun interface Remover {
    /**
     * Removes a file from the server.
     *
     * @param targetFilePath The path to the file on the server.
     */
    fun removeFile(targetFilePath: String)
}