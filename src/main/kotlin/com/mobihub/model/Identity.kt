package com.mobihub.model

/**
 * Represents a generic identity in the system.
 *
 * @property id The unique identifier of the identity.
 * @property name The name of the identity.
 * @property email The email of the identity.
 * @property profilePicture A function that provides the profile picture of the identity. The profile picture is lazily loaded.
 * @property trafficModel A function that provides the traffic models of the identity. The traffic models are lazily loaded.
 *
 * @author Team-MobiHub
 */
abstract class Identity(
    val id: IdentityId?,
    val name: String,
    val email: String,
    val profilePictureProvider: () -> Image?,
    private val trafficModelProvider: () -> List<TrafficModel>,
) {
    val trafficModels: List<TrafficModel> by lazy(trafficModelProvider)
    val profilePicture: Image? by lazy(profilePictureProvider)

    /**
     * Gets the type of the owner of the identity.
     *
     * @return the type of the owner of the identity
     */
    abstract fun getOwnerType(): OwnerType
}

