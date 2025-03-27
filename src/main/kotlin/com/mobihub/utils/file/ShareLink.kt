package com.mobihub.utils.file

private const val LINK_FORMAT = "%s/s/%s/download/%s"

/**
 * Data class for a Nextcloud share link.
 *
 * @property fileName The path to the file.
 * @property shareToken The token for the share link.
 *
 * @author Team-MobiHub
 */
class ShareLink(
    val shareToken: String,
    val fileName: String
) {
    /**
     * Returns the fully assembled share link for the file.
     *
     * @param baseUrl The base URL of the Nextcloud instance.
     * @return The share link for the file.
     */
    fun getShareLink(baseUrl: String): String =
        LINK_FORMAT.format(baseUrl, shareToken, fileName.substringAfterLast('/'))
}
