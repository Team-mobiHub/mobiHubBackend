package com.mobihub.exceptions

private const val ERROR_MESSAGE = "%s by %s %s already found."

/**
 * Exception thrown when a data type is already present in the database.
 *
 * @param dataType The type of data that was not found.
 * @param identifierType The identifier that was used to search for the data.
 * @param value The value of the identifier that was used to search for the data.
 */
class DataAlreadyExistsException(val dataType: String, val identifierType: String, val value: String) : Exception() {
    override val message: String
        get() = ERROR_MESSAGE.format(dataType, identifierType, value)
}