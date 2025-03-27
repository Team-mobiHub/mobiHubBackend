package com.mobihub.repositories

import com.mobihub.utils.file.FileHandler

/**
 * Repository class that initializes all the repositories and provides access to them.
 *
 * @property userRepository The repository for managing [User] entities.
 * @property teamRepository The repository for managing [Team] entities.
 * @property trafficModelRepository The repository for managing [TrafficModel] entities.
 * @property characteristicsMappingRepository The repository for managing [CharacteristicsMapping] entities.
 *
 * @author Team-MobiHub
 */
class RepositoryProvider(private val fileHandler: FileHandler) {
    val userRepository: UserRepository by lazy { UserDbRepository(this) }
    val teamRepository: TeamRepository by lazy { TeamDbRepository(this) }
    val trafficModelRepository: TrafficModelRepository by lazy { TrafficModelDbRepository(this) }
    val characteristicsMappingRepository: CharacteristicsMappingRepository by lazy {
        CharacteristicsMappingDbRepository(
            this
        )
    }
    val commentRepository: CommentRepository by lazy { CommentDbRepository(this) }
    val ratingRepository: RatingRepository by lazy { RatingDbRepository(this) }
    val favouriteRepository: FavouriteRepository by lazy { FavouriteDbRepository(this) }
    val linkTokensRepository: LinkTokensRepository by lazy { LinkTokensDbRepository(this) }
    val imageRepository: ImageRepository by lazy { ImageDbRepository(this) }
}
