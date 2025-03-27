package com.mobihub.utils.file.exceptions

/**
 * Exception thrown when a file is invalid.
 *
 * @property message The message of the exception.
 *
 * @author Team-Mobihub
 */
class InvalidFileException(override val message: String) : Exception()