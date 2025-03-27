package com.mobihub.utils.inspect

import java.io.File

/**
 * The [PseudoFileInspector] class implements the [FileInspector] interface and simulates how a file could be checked for
 * security. The function it provides only does a simple check to see if the file exists and is readable.
 *
 * @author Mobihub
 */
class PseudoFileInspector : FileInspector {

    /**
     * The function takes a file object and checks if it's an existing, readable file and not a directory.
     * @param file The file to be checked
     * @return An instance of [FileInspectorResult]
     */
    override fun isFileValid(file: File): FileInspectorResult {
        return if (file.exists() && !file.isDirectory && file.canRead())
            FileInspectorResult.CLEAN
        else FileInspectorResult.NOT_CLEAN
    }
}
