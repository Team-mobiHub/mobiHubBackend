package com.mobihub.repositories

import com.mobihub.model.Image
import java.util.UUID

/**
 * Repository interface for managing Image entities.
 *
 * @author Team-MobiHub
 */
interface ImageRepository {

    /**
     * Creates a new Image entity.
     *
     * @param image The Image entity to create.
     * @return The created Image entity.
     */
    fun create(image: Image): Image

    /**
     * Updates an existing Image entity.
     *
     * @param image The Image entity to update.
     * @return The updated Image entity.
     */
    fun update(image: Image): Image

    /**
     * Retrieves an Image entity by its token.
     *
     * @param token The token of the Image entity.
     * @return The Image entity with the given token.
     */
    fun get(token: UUID): Image?

    /**
     * Deletes an existing Image entity.
     *
     * @param token The token of the Image entity.
     */
    fun delete(token: UUID)
}
