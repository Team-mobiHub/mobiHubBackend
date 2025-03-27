package com.mobihub.utils.inspect.exceptions

/**
 * Exception thrown when a file contains a virus or similar malware.
 *
 * @property message The message of the exception.
 *
 * @author Team-Mobihub
 */
class FileInfectedException(override val message: String) : Exception()