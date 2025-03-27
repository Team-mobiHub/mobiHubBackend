package com.mobihub.model


/**
 * Enum class for LinkType
 *
 * @param id id of the LinkType
 *
 * @auhtor: Team-MobiHub
 */
enum class LinkType(val id: LinkTypeId) {
    EMAIL_ADDRESS_VERIFICATION(LinkTypeId(0)),
    PASSWORD_RESET(LinkTypeId(1)),
    TRANSFER_OWNERSHIP(LinkTypeId(2)),
    TEAM_INVITATION(LinkTypeId(3));

    companion object {
        private val idMap: Map<LinkTypeId, LinkType> = entries.associateBy { it.id } // Create a map of IDs to enum values

        /**
         * Function to get the LinkType from the unique identifier.
         *
         * @param id the unique identifier of the LinkType
         * @return the LinkType with the given unique identifier
         */
        fun fromId(id: Int): LinkType {
            return idMap[LinkTypeId(id)] ?: throw IllegalArgumentException("No Status found with id: $id")
        }
    }
}