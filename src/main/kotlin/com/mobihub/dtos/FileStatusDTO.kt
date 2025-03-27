package com.mobihub.dtos

import kotlinx.serialization.Serializable

/**
 * Data Transfer Object for the status of a file.
 *
 * @property fileName the name of the file
 * @property status the status of the file
 *
 * @author Team-MobiHub
 */
@Serializable
data class FileStatusDTO(
    val fileName: String,
    val status: FileChangeType
)
