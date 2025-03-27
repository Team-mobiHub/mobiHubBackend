package com.mobihub


/**
 * A simple token blacklist.
 *
 * This object is used to store tokens that have been invalidated.
 *
 * @property blackList -- list of invalidated tokens.
 *
 * @author Team-mobiHub
 */
object TokenBlackList {
    private val blackList = mutableListOf<String>()

    /**
     * Adds a token to the blacklist.
     *
     * @param token -- token to be added to the blacklist.
     */
    fun add(token: String) {
        blackList.add(token)
    }

    /**
     * Checks if a token is in the blacklist.
     *
     * @param token -- token to be checked.
     *
     * @return [Boolean] -- true if the token is in the blacklist, false otherwise.
     */
    fun contains(token: String): Boolean {
        return blackList.contains(token)
    }
}