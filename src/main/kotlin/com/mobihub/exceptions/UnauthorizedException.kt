package com.mobihub.exceptions

private const val ERROR_MESSAGE = "Error: unauthorized access for %s by %s"

/**
 * Exception to throw, when an authenticated user has made
 * a request that should not be accepted by the internal logic
 *
 * @property accessor the authenticated user that tries to access the endpoint
 * @property context the request context
 */
class UnauthorizedException(val accessor: String, val context: String): Exception() {
    override val message: String
        get() = ERROR_MESSAGE.format(context, accessor)
}