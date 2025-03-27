package com.mobihub.model

import java.util.*

private const val FILE_TEMPLATE = "%s.%s"

/**
 * Data class for an image.
 *
 * @property token The token of the image.
 * @property name The name of the image.
 * @property fileExtension The file extension of the image.
 * @property shareToken The share token of the image. If the share token is null, the image is not uploaded yet.
 *
 * @author Team-MobiHub
 */
data class Image(
    val token: UUID,
    var name: String,
    val fileExtension: String,

    // Empty shareToken means the image is not uploaded yet:
    val shareToken: ShareToken?
) {
    /**
     * Returns the full file name of the image including the [fileExtension].
     *
     * @return The file name of the image.
     */
    fun getNextcloudFileName(): String {
        return FILE_TEMPLATE.format(token, fileExtension)
    }
}
