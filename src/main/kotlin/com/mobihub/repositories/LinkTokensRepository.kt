package com.mobihub.repositories

import com.mobihub.model.LinkData
import com.mobihub.model.TeamId
import java.util.*

/**
 * Repository for link tokens.
 *
 * This interface defines the methods that a repository for link tokens must implement.
 * @see [LinkTokensDbRepository]
 *
 * author Team-MobiHub
 */
interface LinkTokensRepository {

     /**
      * Stores a [LinkData] object in the repository.
      *
      * @param linkData The [LinkData] object to store.
      */
     fun storeToken(linkData: LinkData)

     /**
      * Retrieves a [LinkData] object associated with the given token.
      *
      * @param token The UUID token used to identify the [LinkData].
      * @return The [LinkData] object if found, or `null` if no data is associated with the token.
      */
     fun getToken(token: UUID): LinkData?

     /**
      * Retrieves a [LinkData] object associated with the given team ID.
      *
      * @param teamId The ID of the team used to identify the [LinkData].
      * @return The [LinkData] object associated with the team.
      */
     fun getTokenByTeam(teamId: TeamId): LinkData

     /**
      * Deletes the [LinkData] object associated with the given token.
      *
      * @param token The UUID token used to identify the [LinkData] to delete.
      */
     fun deleteToken(token: UUID)
}
