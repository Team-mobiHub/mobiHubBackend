package com.mobihub.utils.inspect

import java.io.File

/**
 * An interface for validating files.
 *
 * The interface defines a method [isFileValid],
 * where the validation logic needs to be implemented.
 *
 * @author Mobihub
 */
interface FileInspector {

    /**
     * Performs the file validation check.
     * @param file An object of class [File] to be checked.
     * @return An instance of [FileInspectorResult].
     */
    fun isFileValid(file: File): FileInspectorResult
}