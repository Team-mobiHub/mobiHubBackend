package com.mobihub.utils.file.exceptions

/**
 * Exception thrown when the server responds with an status code other than specified.
 *
 * @property message The message of the exception.
 *
 * @author Team-Mobihub
 */
class UnexpectedHttpResponse(override val message: String) : Exception()