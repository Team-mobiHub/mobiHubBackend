package com.mobihub.model

private const val NOT_VALID_OWNER_TYPE_FORMAT_TEMPLATE = "%s is not a valid owner type"

/**
 * Represents the concrete owner types mobiHub offers.
 *
 * @author Team-MobiHub
 */
enum class OwnerType {
    USER,
    TEAM;

    /**
     * Converts a string representation of an owner type to an enum.
     *
     * @param name the string representation of the owner type
     * @return the enum representation of the owner type
     * @throws IllegalArgumentException if the owner type is invalid
     */
    fun representationToEnum(name: String): OwnerType{
        return when(name){
            "USER" -> USER
            "TEAM" -> TEAM
            else -> throw IllegalArgumentException(NOT_VALID_OWNER_TYPE_FORMAT_TEMPLATE.format(name))
        }
    }
}