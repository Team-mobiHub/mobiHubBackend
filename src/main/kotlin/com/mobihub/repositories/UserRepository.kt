package com.mobihub.repositories

import com.mobihub.model.User
import com.mobihub.model.UserId

/**
 * Repository for users.
 *
 * This interface defines the methods that a repository for users must implement.
 * @see [UserDbRepository]
 *
 * @author Team-MobiHub
 */
interface UserRepository {

    /**
     * Creates a new [User] in the database.
     *
     * @param user The [User] entity to be created.
     * @return The created [User] entity with the generated ID.
     */
    fun create(user: User): User

    /**
     * Retrieves a [User] by its ID.
     *
     * @param id The ID of the [User] entity.
     * @return The [User] entity with the given ID.
     */
    fun getById(id: UserId): User?

    /**
     * Retrieves a [User] by its email address.
     *
     * @param email The email address of the [User] entity.
     * @return The [User] entity with the given email address.
     */
    fun getByEmail(email: String): User?
    /**
     * Retrieves a [User] by its username.
     *
     * @param name The username of the [User] entity.
     * @return The [User] entity with the given username.
     */
    fun getByName(name: String): User?
    /**
     * Updates a [User] entity in the database.
     *
     * @param user The [User] entity to be updated.
     * @return The updated [User] entity.
     */
    fun update(user: User): User

    /**
     * Changes the password of a [User] entity.
     *
     * @param user The [User] entity whose password is to be changed.
     * @param newPassword The new password.
     */
    fun changePassword(user: User)

    /**
     * Deletes a [User] entity from the database.
     *
     * @param user The [User] entity to be deleted.
     * @return The deleted [User] entity.
     */
    fun delete(user: User)
}