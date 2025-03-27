package com.mobihub.controllers


import com.mobihub.dtos.*
import com.mobihub.exceptions.DataWithIdentifierNotFoundException
import com.mobihub.exceptions.UnauthorizedException
import com.mobihub.model.FileType
import com.mobihub.model.TrafficModelId
import com.mobihub.services.TrafficModelService
import com.mobihub.utils.inspect.exceptions.FileInfectedException
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.request.ContentTransformationException
import io.ktor.util.cio.*
import io.ktor.server.response.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import kotlin.reflect.KFunction2

private const val ERROR_RETRIEVING_TRAFFIC_MODEL = """
Error retrieving traffic model: %s
Stack trace: %s
"""

private const val ERROR_CREATING_TRAFFIC_MODEL = """
Error creating traffic model: %s
Stack trace: %s
"""

private const val ERROR_UPLOADING_FILE = """
Error uploading file: %s
Stack trace: %s
"""

private const val ERROR_CREATING_SHARED_LINK = """
Error creating shared link: %s
Stack trace: %s
"""

private const val ERROR_DELETING_TRAFFIC_MODEL = """
Error deleting traffic model: %s
Stack trace: %s
"""

private const val UPLOAD_DIR = "uploads"
private const val UPLOADS_PATH = "${UPLOAD_DIR}/%s.%s"

private const val FILE_DELETE_LOG = "File %s.zip deleted"
private const val FILE_DELETE_ERROR = "Could not delete File %s.zip"
private const val MULTIPART_MAX_FILE_SIZE = 10737418240
private const val TRAFFIC_MODEL_FOR_OWNER_FETCHED = "Traffic model for owner with id &d fetched"
private const val MISSING_OWNER_TYPE = "Error while reading ownerType"


/**
 * Controller class for managing traffic model-related operations.
 *
 * @property [trafficModelService] The service for managing traffic models.
 *
 * @author Team-MobiHub
 */
class TrafficModelController(private val trafficModelService: TrafficModelService) {
    private val log = LoggerFactory.getLogger(TrafficModelController::class.java)

    /**
     * Creates a new traffic model.
     *
     * The traffic model is created based on the data provided in the [ChangeTrafficModelDTO] object.
     *
     * @param ctx The [ApplicationCall] object representing the HTTP request.
     */
    suspend fun create(ctx: ApplicationCall) {
        createOrUpdate(trafficModelService::create, ctx)
    }

    /**
     * Retrieves a traffic model by its ID (a valid Int).
     *
     * @param ctx The [ApplicationCall] object representing the HTTP request.
     */
    suspend fun getById(ctx: ApplicationCall) {
        try {
            val id = ctx.parameters["modelId"]?.toInt()
            val userDTO = ctx.principal<UserDTO>()
            val trafficModel = trafficModelService.getById(TrafficModelId(id!!), userDTO)
            ctx.respond(trafficModel, typeInfo = typeInfo<TrafficModelDTO>())
        } catch (e: UnauthorizedException) {
            log.info(ERROR_RETRIEVING_TRAFFIC_MODEL.format(e.message, e.stackTrace.contentDeepToString()))
            ctx.respond(HttpStatusCode.Unauthorized, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: NumberFormatException) {
            log.info(ERROR_RETRIEVING_TRAFFIC_MODEL.format(e.message, e.stackTrace.contentDeepToString()))
            ctx.respond(HttpStatusCode.BadRequest, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: IllegalArgumentException) {
            log.info(ERROR_RETRIEVING_TRAFFIC_MODEL.format(e.message, e.stackTrace.contentDeepToString()))
            ctx.respond(HttpStatusCode.BadRequest, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: DataWithIdentifierNotFoundException) {
            log.info(ERROR_RETRIEVING_TRAFFIC_MODEL.format(e.message, e.stackTrace.contentDeepToString()))
            ctx.respond(HttpStatusCode.NotFound, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: Exception) {
            log.info(ERROR_RETRIEVING_TRAFFIC_MODEL.format(e.message, e.stackTrace.contentDeepToString()))
            ctx.respond(HttpStatusCode.InternalServerError, typeInfo = typeInfo<HttpStatusCode>())
        }
    }

    /**
     * Retrieves the traffic models in the specified range that match the search criteria.
     * The range is specified by the parameters `page` and `size` in the [SearchRequestDTO] object.
     * The search criteria are provided in the [SearchRequestDTO] object.
     *
     * @param ctx The [ApplicationCall] object representing the HTTP request.
     */
    suspend fun searchModel(ctx: ApplicationCall) {
        try {
            ctx.respond(
                trafficModelService.getPaginatedAndFiltered(ctx.receive<SearchRequestDTO>()),
                typeInfo = typeInfo<SearchResultDTO>()
            )
        } catch (e: BadRequestException) {
            log.error(e.toString())
            ctx.respond(HttpStatusCode.BadRequest, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: IllegalArgumentException) {
            log.error(e.toString())
            ctx.respond(HttpStatusCode.BadRequest, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: Exception) {
            log.error(ERROR_RETRIEVING_TRAFFIC_MODEL.format(e.toString(), e.stackTraceToString()))
            ctx.respond(HttpStatusCode.InternalServerError, typeInfo = typeInfo<HttpStatusCode>())
        }
    }

    /**
     * Updates a traffic model.
     * The traffic model is updated based on the data provided in the [ChangeTrafficModelDTO] object.
     *
     * The user must be authenticated to update a traffic model.
     *
     * @param ctx The [ApplicationCall] object representing the HTTP request.
     */
    suspend fun update(ctx: ApplicationCall) {
        createOrUpdate(trafficModelService::update, ctx)
    }

    /**
     * Deletes a traffic model by its ID.
     * The user must be authenticated to delete a traffic model.
     *
     * @param ctx The [ApplicationCall] object representing the HTTP request.
     */
    suspend fun delete(ctx: ApplicationCall) {
        if (ctx.principal<UserDTO>() == null) {
            ctx.respond(HttpStatusCode.Unauthorized, typeInfo = typeInfo<HttpStatusCode>())
            return
        }

        if (ctx.parameters["modelId"] == null) {
            ctx.respond(HttpStatusCode.BadRequest, typeInfo = typeInfo<HttpStatusCode>())
            return
        }

        try {
            trafficModelService.delete(TrafficModelId(ctx.parameters["modelId"]?.toInt()!!))
            ctx.respond(HttpStatusCode.OK, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: NumberFormatException) {
            log.error(ERROR_DELETING_TRAFFIC_MODEL.format(e.message, e.stackTraceToString()))
            ctx.respond(HttpStatusCode.BadRequest, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: DataWithIdentifierNotFoundException) {
            log.error(ERROR_DELETING_TRAFFIC_MODEL.format(e.message, e.stackTraceToString()))
            ctx.respond(HttpStatusCode.NotFound, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: Exception) {
            log.error(ERROR_DELETING_TRAFFIC_MODEL.format(e.message, e.stackTraceToString()))
            ctx.respond(HttpStatusCode.InternalServerError, typeInfo = typeInfo<HttpStatusCode>())
        }
    }

    /**
     * Uploads a file to the server.
     *
     * The file is uploaded as a multipart form data.
     *
     * @param ctx The [ApplicationCall] object representing the HTTP request.
     * @param fileType The type of the file to be uploaded.
     */
    suspend fun uploadFile(ctx: ApplicationCall, fileType: FileType) {
        val uploadDir = File(UPLOAD_DIR)
        if (!uploadDir.exists()) uploadDir.mkdirs()
        val fileToken = UUID.fromString(ctx.parameters["token"])

        var uploadPath = ""

        ctx.receiveMultipart(formFieldLimit = MULTIPART_MAX_FILE_SIZE).forEachPart { part ->
            when (part) {
                is PartData.FileItem -> {
                    val fileName = part.originalFileName ?: ""
                    uploadPath = UPLOADS_PATH.format(fileToken, fileName.substringAfterLast("."))
                    part.provider().copyAndClose(File(uploadPath).writeChannel())
                }

                else -> part.dispose()
            }
        }

        try {
            trafficModelService.uploadFile(
                File(uploadPath),
                TrafficModelId(ctx.parameters["modelId"]?.toInt()!!),
                fileToken,
                fileType
            )
            ctx.respond(HttpStatusCode.Created, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: NumberFormatException) {
            log.info(e.message)
            ctx.respond(HttpStatusCode.BadRequest, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: FileInfectedException) {
            ctx.respond(HttpStatusCode.BadRequest, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: DataWithIdentifierNotFoundException) {
            ctx.respond(HttpStatusCode.NotFound, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: Exception) {
            log.info(ERROR_UPLOADING_FILE.format(e.message, e.stackTrace.contentDeepToString()))
            ctx.respond(HttpStatusCode.InternalServerError, typeInfo = typeInfo<HttpStatusCode>())
        }

        if (File(uploadPath).delete()) log.info(FILE_DELETE_LOG.format(fileToken))
        else log.error(FILE_DELETE_ERROR.format(fileToken))
    }

    /**
     * Retrieves the download link for a traffic model.
     *
     * @param ctx The [ApplicationCall] object representing the HTTP request.
     */
    suspend fun getDownloadLink(ctx: ApplicationCall) {
        try {
            val downloadLink = trafficModelService.getDownloadLink(TrafficModelId(ctx.parameters["modelId"]?.toInt()!!))
            ctx.respond(downloadLink, typeInfo = typeInfo<String>())
        } catch (e: NumberFormatException) {
            ctx.respond(HttpStatusCode.BadRequest, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: DataWithIdentifierNotFoundException) {
            log.info(e.message)
            ctx.respond(HttpStatusCode.NotFound, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: Exception) {
            log.error(ERROR_CREATING_SHARED_LINK.format(e.message, e.stackTrace.contentDeepToString()))
            ctx.respond(HttpStatusCode.InternalServerError, typeInfo = typeInfo<HttpStatusCode>())
        }
    }

    /**
     * Retrieves traffic models of a given owner. If the user is authenticated and the userId is the ownerId, then all
     * traffic models are returned, else only the public ones are returned.
     *
     * The owner is identified by its ID and type.
     *
     * @param ctx The [ApplicationCall] object representing the HTTP request.
     */
    suspend fun getByOwner(ctx: ApplicationCall) {
        val userDTO = ctx.principal<UserDTO>()
        val ownerId = ctx.parameters["ownerId"]
        if (ownerId == null) {
            ctx.respond(HttpStatusCode.BadRequest, typeInfo = typeInfo<HttpStatusCode>())
            return
        }

        try {
            val ownerType = ctx.parameters["ownerType"] ?: throw IllegalArgumentException(MISSING_OWNER_TYPE)
            ctx.respond(
                trafficModelService.getByOwner(ownerId.toInt(), ownerType, userDTO),
                typeInfo = typeInfo<List<TrafficModelDTO>>()
            )
            log.info(TRAFFIC_MODEL_FOR_OWNER_FETCHED.format(ownerId))
        } catch (e: Exception) {
            log.error(e.message, e)
            when (e) {
                is NumberFormatException -> ctx.respond(HttpStatusCode.BadRequest, typeInfo<HttpStatusCode>())
                is IllegalArgumentException -> ctx.respond(HttpStatusCode.BadRequest, typeInfo<HttpStatusCode>())
                is DataWithIdentifierNotFoundException -> ctx.respondText(e.message, status = HttpStatusCode.BadRequest)
                else -> ctx.respond(HttpStatusCode.InternalServerError, typeInfo = typeInfo<HttpStatusCode>())
            }
        }
    }

    fun getTransferOwnershipLink(ctx: ApplicationCall) {
        TODO()
    }

    fun sendTransferOwnershipLinkByEmail(ctx: ApplicationCall) {
        TODO()
    }

    fun useTransferOwnershipLink(ctx: ApplicationCall) {
        TODO()
    }

    /**
     * Helper function to create or update a traffic model.
     *
     * @param serviceMethod The service method to call.
     * @param ctx The [ApplicationCall] object representing the HTTP request.
     */
    private suspend fun createOrUpdate(
        serviceMethod: KFunction2<ChangeTrafficModelDTO, UserDTO, CreateTrafficModelResponseDTO>,
        ctx: ApplicationCall
    ) {
        try {
            val currentUser = ctx.principal<UserDTO>()
                ?: throw BadRequestException("User not authenticated")

            val trafficModelDTO = ctx.receive<ChangeTrafficModelDTO>()
            ctx.respond(
                serviceMethod(trafficModelDTO, currentUser),
                typeInfo = typeInfo<CreateTrafficModelResponseDTO>()
            )
        } catch (e: ContentTransformationException) {
            log.info(ERROR_CREATING_TRAFFIC_MODEL.format(e, e.stackTrace.contentDeepToString()))
            ctx.respond(HttpStatusCode.BadRequest, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: BadRequestException) {
            log.info(ERROR_CREATING_TRAFFIC_MODEL.format(e, e.stackTrace.contentDeepToString()))
            ctx.respond(HttpStatusCode.BadRequest, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: IllegalArgumentException) {
            log.info(ERROR_CREATING_TRAFFIC_MODEL.format(e, e.stackTrace.contentDeepToString()))
            ctx.respond(HttpStatusCode.BadRequest, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: UnauthorizedException) {
            log.info(ERROR_CREATING_TRAFFIC_MODEL.format(e, e.stackTrace.contentDeepToString()))
            ctx.respond(HttpStatusCode.Unauthorized, typeInfo = typeInfo<HttpStatusCode>())
        } catch (e: Exception) {
            log.info(ERROR_CREATING_TRAFFIC_MODEL.format(e.message, e.stackTrace.contentDeepToString()))
            ctx.respond(HttpStatusCode.InternalServerError, typeInfo = typeInfo<HttpStatusCode>())
        }
    }
}
