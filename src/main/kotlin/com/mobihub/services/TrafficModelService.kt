package com.mobihub.services

import com.mobihub.dtos.*
import com.mobihub.exceptions.DataWithIdentifierNotFoundException
import com.mobihub.exceptions.UnauthorizedException
import com.mobihub.model.*
import com.mobihub.repositories.*
import com.mobihub.utils.file.FileHandler
import com.mobihub.utils.inspect.FileInspector
import com.mobihub.utils.inspect.FileInspectorResult
import com.mobihub.utils.inspect.PseudoFileInspector
import com.mobihub.utils.inspect.exceptions.FileInfectedException
import io.ktor.server.application.*
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

private const val USER_NOT_EXISTING_ERROR_TEMPLATE = "User with id: %s does not exist"
private const val TEAM_NOT_EXISTING_ERROR_TEMPLATE = "Team with id: %s does not exist"
private const val INCORRECT_OWNER_CONFIGURATION = "Incorrect owner configuration"
private const val TRAFFIC_MODEL_NOT_EXISTING_ERROR_TEMPLATE = "Traffic model with id: %s does not exist"
private const val THE_FILE_NOT_VALID_ERROR = "The file is not valid"
private const val INVALID_FILE_TOKEN = "The file token is not valid"
private const val TRAFFIC_MODEL_ID_NULL_ERROR = "The traffic model ID is null"

private const val GENERATE_DOWNLOAD_LINK_LOG_MESSAGE = "Download link for traffic model with id: %s: %s"
private const val CREATED_TRAFFIC_MODEL_MESSAGE = "Created traffic model with id: %d"
private const val UPDATED_TRAFFIC_MODEL_MESSAGE = "Updated traffic model with id: %d"
private const val FILE_UPLOAD_SUCCESSFUL = "Successfully uploaded the file %s for the token %s."
private const val START_SEARCH_LOG = "Searching for traffic models with search request: %s"

/**
 * Template for the path to the traffic model file on the NextCloud server.
 * The path is based on the traffic model ID. The ID is split into two parts, each with two digits.
 */
private const val NEXTCLOUD_DIRECTORY_PATH_TEMPLATE = "trafficModels/%02d/%02d"
private const val NEXTCLOUD_ZIP_FILE_PATH_TEMPLATE = "${NEXTCLOUD_DIRECTORY_PATH_TEMPLATE}/%s.zip"
private const val NEXTCLOUD_IMAGE_PATH_TEMPLATE = "${NEXTCLOUD_DIRECTORY_PATH_TEMPLATE}/images/%s"
private const val IMAGE_FILE_NAME = "%s.%s"

private const val NO_LOGGED_IN_USER = "not logged in User."
private const val TRAFFIC_MODEL = "Traffic model"

private const val UPDATE_TRAFFIC_MODEL = "update traffic model"

/**
 * Service class for managing traffic model-related operations.
 *
 * @property [trafficModelRepository] The repository for managing [TrafficModel] entities.
 * @property [characteristicsMappingRepository] The repository for managing characteristics mapping.
 * @property [userRepository] The repository for managing [User] entities.
 * @property [teamRepository] The repository for managing [Team] entities.
 * @property [linkService] The service for managing links.
 * @property [fileHandler] The handler for managing files.
 * @property [environment] The environment of the application.
 *
 * @author Team-MobiHub
 */
class TrafficModelService(
    private val trafficModelRepository: TrafficModelRepository,
    private val characteristicsMappingRepository: CharacteristicsMappingRepository,
    private val userRepository: UserRepository,
    private val teamRepository: TeamRepository,
    private val linkService: LinkService,
    private val fileHandler: FileHandler,
    private val environment: ApplicationEnvironment
) {
    private val log = LoggerFactory.getLogger(TrafficModelService::class.java)
    private val dmsBaseUrl = environment.config.property("nextcloud.baseUrl").getString()
    private val fileInspector: FileInspector = PseudoFileInspector()

    /**
     * Creates a new traffic model.
     *
     * @param request The [ChangeTrafficModelDTO] object containing the data of the traffic model to create.
     * @param userId The ID of the user creating the traffic model.
     * @return The [CreateTrafficModelResponseDTO] object representing the created traffic model.
     */
    fun create(request: ChangeTrafficModelDTO, user: UserDTO): CreateTrafficModelResponseDTO {
        request.validate()

        val newTrafficModel = trafficModelRepository.create(
            TrafficModel(
                // ID is null because it has not been assigned yet by the repository
                id = null,
                name = request.name,
                description = request.description,
                isVisibilityPublic = request.isVisibilityPublic,
                dataSourceUrl = request.dataSourceUrl,
                location = Location(
                    region = Region(request.region), coordinates = request.coordinates?.let { Coordinates(it) }),
                framework = request.framework,
                methodLevelPairProvider = {
                    request.characteristics.map {
                        Pair(it.modelLevel, it.modelMethod)
                    }
                },
                authorProvider = getAuthorProvider(request),

                // At this point, the user doesn't have the option to upload a markdown file
                markdownFileUrlProvider = { "" },

                // At this point, the user doesn't have the option to upload images
                imagesProvider = { getUpdatedImages(null, emptyList(), request.changedImages) },

                // At this point, no user could have had the option to rate the model
                averageRatingProvider = { 0.0 },
                ratingsProvider = { emptyList() },

                // At this point, no user could have had the option to comment on the model
                commentsProvider = { emptyList() },

                // At this point, no user could have had the option to favorite the model
                favoritesProvider = { emptyList() },
                zipFileToken = UUID.randomUUID(),
                isZipFileUploaded = false
            )
        )

        characteristicsMappingRepository.create(
            newTrafficModel.id!!, request.characteristics.map {
                Pair(it.modelLevel, it.modelMethod)
            })

        trafficModelRepository.updateImages(
            newTrafficModel.id, newTrafficModel.images
        )

        log.info(CREATED_TRAFFIC_MODEL_MESSAGE.format(newTrafficModel.id.id))

        return CreateTrafficModelResponseDTO(newTrafficModel)
    }

    /**
     * Retrieves a traffic model by its ID.
     *
     * @param id The ID of the traffic model to retrieve.
     * @param currentUser The user requesting the traffic model. Needed to determine if the TM is a users favorite.
     * @return The [TrafficModelDTO] object representing the traffic model.
     * @throws IllegalArgumentException If the traffic model with the given ID does not exist.
     * @throws UnauthorizedException If the traffic model is not public and the user is not the owner.
     */
    fun getById(id: TrafficModelId, currentUser: UserDTO?): TrafficModelDTO {
        val trafficModel = trafficModelRepository.getById(id) ?: throw DataWithIdentifierNotFoundException(
            TRAFFIC_MODEL, "id", id.id.toString()
        )

        val userId = if (currentUser != null) UserId(currentUser.id!!) else null

        if (!trafficModel.isVisibilityPublic && !isUserOwner(currentUser, trafficModel)) {
            throw UnauthorizedException(currentUser?.name ?: NO_LOGGED_IN_USER, TRAFFIC_MODEL)
        }

        return trafficModel.toDTO(
            userId = userId, dmsBaseUrl = dmsBaseUrl
        )
    }

    /**
     * Checks if the current user is the owner of the given traffic model.
     *
     * @param currentUser The current user making the request.
     * @param trafficModel The traffic model to check ownership for.
     * @return `true` if the current user is the owner of the traffic model, `false` otherwise.
     */
    private fun isUserOwner(
        currentUser: UserDTO?,
        trafficModel: TrafficModel
    ) = (currentUser?.id != null
            && ((trafficModel.author.getOwnerType() == OwnerType.USER
            && trafficModel.author.id!!.id == currentUser.id)
            || (trafficModel.author.getOwnerType() == OwnerType.TEAM
            && currentUser.teams.any { it.id == trafficModel.author.id!!.id })))

    /**
     * Retrieves all traffic models of a given owner, depending on if the authenticated user is the owner or not.
     *
     * @param ownerId The ID of the owner.
     * @param ownerType The type of the owner.
     * @return A list of [TrafficModelDTO] objects representing the traffic models of the owner.
     * @throws IllegalArgumentException If the owner does not have any traffic models.
     */
    fun getByOwner(ownerId: Int, ownerType: String, currentUser: UserDTO?): List<TrafficModelDTO> {
        val trafficModels = when (OwnerType.valueOf(ownerType)) {
            OwnerType.USER -> trafficModelRepository.getByUser(UserId(ownerId))
            OwnerType.TEAM -> trafficModelRepository.getByTeam(TeamId(ownerId))
        }.map { trafficModel -> trafficModel.toDTO(currentUser?.let { UserId(it.id!!) }, dmsBaseUrl) }

        return if (currentUser != null && currentUser.id == ownerId) trafficModels else trafficModels.filter { it.isVisibilityPublic }
    }

    /**
     * Updates an existing traffic model.
     *
     * @param request The [ChangeTrafficModelDTO] object containing the updated data of the traffic model.
     * @param user The user updating the traffic model.
     * @return The [CreateTrafficModelResponseDTO] object representing the updated traffic model.
     * @throws IllegalArgumentException If the traffic model with the given ID does not exist.
     */
    fun update(request: ChangeTrafficModelDTO, user: UserDTO): CreateTrafficModelResponseDTO {
        request.validate()

        require(request.id != null) { TRAFFIC_MODEL_ID_NULL_ERROR }

        val existingTrafficModel = trafficModelRepository.getById(TrafficModelId(request.id))
        if (existingTrafficModel == null) {
            log.info(TRAFFIC_MODEL_NOT_EXISTING_ERROR_TEMPLATE.format(request.id))
            throw DataWithIdentifierNotFoundException(
                TRAFFIC_MODEL, "id", request.id.toString()
            )
        }

        if (!isUserOwner(user, existingTrafficModel)) {
            throw UnauthorizedException(user.id.toString(), UPDATE_TRAFFIC_MODEL)
        }

        val updatedTrafficModel = trafficModelRepository.update(
            TrafficModel(
                id = TrafficModelId(request.id),
                name = request.name,
                description = request.description,
                isVisibilityPublic = request.isVisibilityPublic,
                dataSourceUrl = request.dataSourceUrl,
                location = Location(
                    region = Region(request.region), coordinates = request.coordinates?.let { Coordinates(it) }),
                framework = request.framework,
                methodLevelPairProvider = {
                    request.characteristics.map {
                        Pair(it.modelLevel, it.modelMethod)
                    }
                },
                authorProvider = getAuthorProvider(request),
                markdownFileUrlProvider = { "" },
                imagesProvider = {
                    getUpdatedImages(
                        existingTrafficModel.id,
                        existingTrafficModel.images,
                        request.changedImages
                    )
                },
                averageRatingProvider = { 0.0 },
                ratingsProvider = { emptyList() },
                commentsProvider = { emptyList() },
                favoritesProvider = { emptyList() },
                zipFileToken = if (request.hasZipFileChanged) getNewZipFileToken(existingTrafficModel) else existingTrafficModel.zipFileToken,
                isZipFileUploaded = !request.hasZipFileChanged
            )
        )

        characteristicsMappingRepository.update(updatedTrafficModel.id!!, updatedTrafficModel.methodLevelPair)

        trafficModelRepository.updateImages(
            updatedTrafficModel.id, updatedTrafficModel.images
        )

        log.info(UPDATED_TRAFFIC_MODEL_MESSAGE.format(updatedTrafficModel.id.id))

        return CreateTrafficModelResponseDTO(
            updatedTrafficModel.copy(imagesProvider = {
                // Filter images for empty shareToken to send only the new image tokens to the client
                updatedTrafficModel.images.filter { it.shareToken == null }
            })
        )
    }

    /**
     * Generates a map of image tokens based on the old tokens and the new tokens.
     *
     * @param trafficModelId The ID of the traffic model.
     * @param oldImages A map of existing image tokens.
     * @param newImages A map of new image tokens with their change types.
     * @return A map of updated image tokens.
     */
    private fun getUpdatedImages(
        trafficModelId: TrafficModelId?,
        oldImages: List<Image>,
        newImages: List<FileStatusDTO>
    ): List<Image> {
        val images = oldImages.toMutableList()

        newImages.forEach() { image ->
            when (image.status) {
                FileChangeType.ADDED -> addImage(images, image)

                FileChangeType.REMOVED -> removeImage(images, image, trafficModelId)

                else -> {}
            }
        }
        return images
    }

    /**
     * Adds an image to the list of images.
     *
     * @param images The list of images to which to add the image.
     * @param image The image to add.
     */
    private fun addImage(
        images: MutableList<Image>,
        image: FileStatusDTO
    ) {
        images.add(
            Image(
                UUID.randomUUID(),
                image.fileName.substringBeforeLast('.'),
                image.fileName.substringAfterLast('.'),
                null
            )
        )
    }

    /**
     * Removes an image from the list of images and the DMS.
     *
     * @param images The list of images from which to remove the image.
     * @param image The image to remove.
     * @param trafficModelId The ID of the traffic model to which the image belongs.
     */
    private fun removeImage(
        images: MutableList<Image>,
        image: FileStatusDTO,
        trafficModelId: TrafficModelId?
    ) {
        images.find { it.getNextcloudFileName() == image.fileName }
            ?.let { img ->
                trafficModelId?.let { id ->
                    fileHandler.removeFile(
                        getImageFilePath(
                            id, img.token, File(image.fileName)
                        )
                    )
                }
                images.remove(img)
            }
    }

    /**
     * Generates a new ZIP file token for a traffic model.
     * The old ZIP file is removed from the DMS.
     *
     * @param trafficModel The traffic model for which to generate the new ZIP file token.
     */
    private fun getNewZipFileToken(trafficModel: TrafficModel): UUID {
        fileHandler.removeFile(getZipFilePath(trafficModel.zipFileToken, trafficModel.id!!))
        return UUID.randomUUID()
    }


    /**
     * Generates a function that provides the author of a traffic model.
     *
     * @param trafficModel The traffic model for which to generate the author provider.
     * @return A function that provides the author of the traffic model.
     */
    private fun getAuthorProvider(trafficModel: ChangeTrafficModelDTO): () -> Identity = {
        if (trafficModel.ownerUserId != null) {
            val user = userRepository.getById(UserId(trafficModel.ownerUserId))
            require(user != null) {
                USER_NOT_EXISTING_ERROR_TEMPLATE.format(trafficModel.ownerUserId)
            }
            user
        } else if (trafficModel.ownerTeamId != null) {
            val team = teamRepository.getById(TeamId(trafficModel.ownerTeamId))
            require(team != null) {
                TEAM_NOT_EXISTING_ERROR_TEMPLATE.format(trafficModel.ownerTeamId)
            }
            team
        } else {
            error(INCORRECT_OWNER_CONFIGURATION)
        }
    }

    /**
     * Deletes a traffic model by its ID.
     *
     * @param trafficModelId The ID of the traffic model to delete.
     * @throws DataWithIdentifierNotFoundException If the traffic model with the given ID does not exist.
     */
    fun delete(trafficModelId: TrafficModelId) {
        val trafficModel = trafficModelRepository.getById(trafficModelId)

        if (trafficModel == null) {
            log.info(TRAFFIC_MODEL_NOT_EXISTING_ERROR_TEMPLATE.format(trafficModelId))
            throw DataWithIdentifierNotFoundException(
                TRAFFIC_MODEL, "id", trafficModelId.id.toString()
            )
        }

        fileHandler.removeFile(
            NEXTCLOUD_DIRECTORY_PATH_TEMPLATE.format(
                (trafficModelId.id / 100), (trafficModelId.id % 100)
            )
        )

        trafficModelRepository.delete(trafficModelId)
    }


    /**
     * Generates a download link for the ZIP file of a traffic model.
     *
     * @param trafficModelId The ID of the traffic model.
     * @return The download link for the ZIP file of the traffic model.
     * @throws DataWithIdentifierNotFoundException If the traffic model with the given ID does not exist.
     */
    fun getDownloadLink(trafficModelId: TrafficModelId): String {
        val trafficModel = trafficModelRepository.getById(trafficModelId)

        if (trafficModel == null) {
            log.info(TRAFFIC_MODEL_NOT_EXISTING_ERROR_TEMPLATE.format(trafficModelId))
            throw DataWithIdentifierNotFoundException(
                TRAFFIC_MODEL, "id", trafficModelId.id.toString()
            )
        }

        val reference = fileHandler.getDownloadReference(
            getZipFilePath(trafficModel.zipFileToken, trafficModel.id!!), true
        ).getShareLink(baseUrl = dmsBaseUrl)

        log.info(GENERATE_DOWNLOAD_LINK_LOG_MESSAGE.format(trafficModelId, reference))

        return reference
    }

    /**
     * Retrieves the path to the ZIP file on the DMS.
     *
     * @param zipFileToken The token of the ZIP file.
     * @param id The ID of the traffic model.
     * @return The path to the ZIP file on the DMS.
     */
    private fun getZipFilePath(zipFileToken: UUID, id: TrafficModelId) = NEXTCLOUD_ZIP_FILE_PATH_TEMPLATE.format(
        id.id / 100, id.id % 100, zipFileToken.toString()
    )


    /**
     * Uploads a file for a traffic model.
     *
     * @param file The file to upload.
     * @param trafficModelId The ID of the traffic model to which the file belongs.
     * @param fileToken The token associated with the upload request.
     * @param fileType The type of the file to upload.
     * @throws FileInfectedException If the file is infected.
     * @throws DataWithIdentifierNotFoundException If the traffic model with the given ID does not exist.
     */
    fun uploadFile(
        file: File, trafficModelId: TrafficModelId, fileToken: UUID, fileType: FileType
    ) {
        if (fileInspector.isFileValid(file) != FileInspectorResult.CLEAN) {
            log.info(THE_FILE_NOT_VALID_ERROR)
            throw FileInfectedException(THE_FILE_NOT_VALID_ERROR)
        }

        val trafficModel = trafficModelRepository.getById(trafficModelId)
        if (trafficModel == null) {
            log.info(TRAFFIC_MODEL_NOT_EXISTING_ERROR_TEMPLATE.format(trafficModelId))
            throw DataWithIdentifierNotFoundException(
                TRAFFIC_MODEL, "id", trafficModelId.id.toString()
            )
        }

        when (fileType) {
            FileType.ZIP -> uploadZipFile(trafficModel, fileToken, file)
            FileType.IMAGE -> uploadImage(trafficModel, fileToken, file)
        }
    }

    /**
     * Uploads a ZIP file for the specified traffic model.
     *
     * @param trafficModel The traffic model to which the file belongs.
     * @param fileToken The token associated with the upload request.
     * @param file The ZIP file to upload.
     * @throws IllegalArgumentException If the file token is invalid.
     */
    private fun uploadZipFile(
        trafficModel: TrafficModel, fileToken: UUID, file: File
    ) {
        assert(
            trafficModel.zipFileToken == fileToken && !trafficModel.isZipFileUploaded
        ) { INVALID_FILE_TOKEN }

        fileHandler.uploadFile(
            file = file, targetFilePath = getZipFilePath(fileToken, trafficModel.id!!)
        )

        trafficModelRepository.update(
            trafficModel.copy(isZipFileUploaded = true)
        )

        log.info(FILE_UPLOAD_SUCCESSFUL.format(file.name, fileToken))
    }

    /**
     * Uploads an image for the specified traffic model.
     *
     * @param trafficModel The traffic model to which the file belongs to.
     * @param token The token associated with the upload request.
     * @param file The image file to upload.
     */
    private fun uploadImage(
        trafficModel: TrafficModel, token: UUID, file: File
    ) {
        val image = trafficModel.images.find { it.token == token }
        assert(image != null && image.shareToken == null) { INVALID_FILE_TOKEN }

        val imagePath = getImageFilePath(trafficModel.id!!, token, file)

        fileHandler.uploadFile(
            file = file, targetFilePath = imagePath
        )

        trafficModelRepository.updateImage(
            trafficModel.id, image!!.copy(
                shareToken = ShareToken(
                    fileHandler.getDownloadReference(imagePath, false)
                        .shareToken
                )
            )
        )

        log.info(FILE_UPLOAD_SUCCESSFUL.format(file.name, token))
    }

    /**
     * Retrieves the path to the image file on the DMS.
     *
     * @param id The ID of the traffic model.
     * @param fileToken The token associated with the image file.
     * @param file The image file.
     */
    private fun getImageFilePath(
        id: TrafficModelId, fileToken: UUID, file: File
    ) = NEXTCLOUD_IMAGE_PATH_TEMPLATE.format(
        id.id / 1000, id.id % 1000, IMAGE_FILE_NAME.format(
            fileToken.toString(), file.extension
        )
    )

    /**
     * Retrieves the traffic models in the specified range that match the search criteria.
     *
     * @param searchRequest The [SearchRequestDTO] object containing the search criteria.
     *
     * @return A list of [SearchResultItemDTO] objects representing the traffic models that match the search criteria.
     */
    fun getPaginatedAndFiltered(searchRequest: SearchRequestDTO): SearchResultDTO {
        log.info(START_SEARCH_LOG.format(searchRequest))

        val searchResult = trafficModelRepository.searchPaginated(
            page = searchRequest.page,
            size = searchRequest.pageSize,
            name = searchRequest.name,
            authorName = searchRequest.authorName,
            region = searchRequest.region?.let { Region(it) },
            modelLevels = searchRequest.modelLevels,
            modelMethods = searchRequest.modelMethods,
            frameworks = searchRequest.frameworks
        )

        return SearchResultDTO(
            searchResult.first.map {
                it.toSearchResultItemDTO(dmsBaseUrl)
            },
            searchResult.second
        )
    }

    fun getTransferOwnershipLink(trafficModelId: TrafficModelId): String {
        TODO()
    }

    fun sendTransferOwnershipLink(trafficModelId: TrafficModelId, email: String) {
        TODO()
    }

    fun useTransferOwnershipLink(token: UUID, ownerType: OwnerType, ownerId: Int) {
        TODO()
    }
}
