package com.mobihub.utils.inspect

/**
 * This enum class represents one of the two possible values after performing a file inspection check.
 * Those are either [FileInspectorResult.CLEAN] and [FileInspectorResult.NOT_CLEAN].
 * @author Mobihub
 * @see FileInspector
 */
enum class FileInspectorResult {
    /**
     * The file inspector has determined that the file is safe.
     */
    CLEAN,
    /**
     * The file inspector has determined that the file is NOT safe.
     */
    NOT_CLEAN
}