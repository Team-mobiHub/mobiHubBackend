package com.mobihub.model

import com.mobihub.dtos.CharacteristicDTO
import com.mobihub.dtos.RatingDTO
import com.mobihub.dtos.SearchResultItemDTO
import com.mobihub.dtos.TrafficModelDTO
import com.mobihub.utils.file.ShareLink
import java.util.*

private const val ID_IS_NULL_ERROR = "Id is null"

/**
 * Represents a traffic model.
 *
 * @property id the unique identifier of the traffic model
 * @property name the name of the traffic model
 * @property description the description of the traffic model
 * @property isVisibilityPublic whether the traffic model is public
 * @property dataSourceUrl the URL of the data source
 * @property location the location of the traffic model
 * @property framework the framework of the traffic model
 * @property zipFileToken the token for the zip file
 * @property isZipFileUploaded whether the zip file is uploaded
 * @property methodLevelPair the list of model level and method pairs. This is a lazy property
 * @property author the author of the traffic model. This is a lazy property
 * @property markdownFileUrl the markdown file URL of the traffic model. This is a lazy property
 * @property favorites the list of users who have favorited the traffic model. This is a lazy property
 * @property images the list of images associated with the traffic model. This is a lazy property
 * @property ratings the list of ratings associated with the traffic model. This is a lazy property
 * @property averageRating the average rating of the traffic model. This is a lazy property
 * @property comments the list of comments associated with the traffic model. This is a lazy property
 *
 * @author Team-MobiHub
 */
data class TrafficModel(
    val id: TrafficModelId?,
    val name: String,
    val description: String,
    val isVisibilityPublic: Boolean,
    val dataSourceUrl: String,
    val location: Location,
    val framework: Framework,
    val zipFileToken: UUID,
    val isZipFileUploaded: Boolean,

    val methodLevelPairProvider: () -> List<Pair<ModelLevel, ModelMethod>>,
    val authorProvider: () -> Identity,
    val markdownFileUrlProvider: () -> String?,
    val favoritesProvider: () -> List<User>,
    val imagesProvider: () -> List<Image>,
    val averageRatingProvider: () -> Double,
    val ratingsProvider: () -> List<Rating>,
    val commentsProvider: () -> List<Comment>
) {
    val methodLevelPair: List<Pair<ModelLevel, ModelMethod>> by lazy(methodLevelPairProvider)
    val author: Identity by lazy(authorProvider)
    val markdownFileURL: String? by lazy(markdownFileUrlProvider)
    val images: List<Image> by lazy(imagesProvider)
    val averageRating: Double by lazy(averageRatingProvider)
    val ratings: List<Rating> by lazy(ratingsProvider)
    val comments: List<Comment> by lazy(commentsProvider)
    val favorites: List<User> by lazy(favoritesProvider)

    /**
     * Converts the traffic model to a DTO that can be tailored to a specific user
     *
     * @param userId the ID of the user viewing the traffic model.
     * @return the DTO representation of the traffic model
     */
    fun toDTO(userId: UserId?, dmsBaseUrl: String): TrafficModelDTO {
        return TrafficModelDTO(
            id = checkNotNull(id?.id) { ID_IS_NULL_ERROR },
            name = name,
            description = description,
            userId = if (author.getOwnerType() == OwnerType.USER) author.id!!.id else null,
            teamId = if (author.getOwnerType() == OwnerType.TEAM) author.id!!.id else null,
            isVisibilityPublic = isVisibilityPublic,
            dataSourceUrl = dataSourceUrl,
            framework = framework,
            region = location.region.name,
            coordinates = location.coordinates!!.value,
            imageURLs = images.filter { image -> image.shareToken != null }.map {
                ShareLink(
                    shareToken = it.shareToken!!.value,
                    fileName = it.getNextcloudFileName(),
                ).getShareLink(baseUrl = dmsBaseUrl)
            },
            markDownFileURL = markdownFileURL ?: "",
            // if an unlogged-in user views the model, isFavorite is false, so we have to use the AuthGuard to
            // display a "favoriting disabled" button
            isFavorite = (userId?.id != null) && (favorites.any { user -> user.id!!.id == userId.id }),
            rating = RatingDTO(
                id!!.id,
                if (userId?.id != null && ratings.isNotEmpty())
                    ratings.find { rating -> rating.user.id!!.id == userId.id }?.rating ?: 0;
                // if userId is null, the default value is 0
                else 0,
                averageRating
            ),
            comments = comments.map { it.toDTO() },
            characteristics = methodLevelPair.map { CharacteristicDTO(it.first, it.second) },
            zipFileToken = zipFileToken.toString()
        )
    }

    /**
     * Converts the traffic model to a DTO that can be used in search results
     *
     * @param dmsBaseUrl the base URL of the application
     *
     * @return the DTO representation of the traffic model
     */
    fun toSearchResultItemDTO(dmsBaseUrl: String): SearchResultItemDTO {
        return SearchResultItemDTO(
            trafficModelId = checkNotNull(id?.id) { ID_IS_NULL_ERROR },
            name = name,
            description = description,
            averageRating = averageRating,
            imageURL = if (images.isNotEmpty() && images[0].shareToken != null) ShareLink(
                shareToken = images[0].shareToken!!.value,
                fileName = images[0].getNextcloudFileName(),
            ).getShareLink(baseUrl = dmsBaseUrl) else "",
        )
    }
}
