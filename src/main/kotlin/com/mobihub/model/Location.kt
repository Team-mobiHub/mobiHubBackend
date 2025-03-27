package com.mobihub.model

/**
 * Represents a location.
 *
 * @property region the region of the location
 * @property coordinates the coordinates of the location
 *
 * @author Team-MobiHub
 */
data class Location(
    val region: Region,
    val coordinates: Coordinates?
)