package com.mobihub.exceptions

private const val ERROR_MESSAGE = "%s by %s %s not found."

/**
 * Exception thrown when a data type is not found by a specific identifier.
 *
 * @param dataType The type of data that was not found.
 * @param identifierType The type of identifier that was used to search for the data.
 * @param value The value of the identifier that was used to search for the data.
 */
class DataWithIdentifierNotFoundException(val dataType: String, val identifierType: String, val value: String) : Exception() {
    override val message: String
        get() = ERROR_MESSAGE.format(dataType, identifierType, value)
}
