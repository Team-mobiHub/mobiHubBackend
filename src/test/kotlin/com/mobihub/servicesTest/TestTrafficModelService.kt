package com.mobihub.servicesTest

import com.mobihub.dtos.*
import com.mobihub.exceptions.DataWithIdentifierNotFoundException
import com.mobihub.exceptions.UnauthorizedException
import com.mobihub.model.*
import com.mobihub.repositories.*
import com.mobihub.services.LinkService
import com.mobihub.services.TrafficModelService
import com.mobihub.utils.file.FileHandler
import com.mobihub.utils.file.ShareLink
import com.mobihub.utils.inspect.exceptions.FileInfectedException
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.config.*
import io.ktor.server.engine.*
import kotlin.test.Test
import kotlin.test.BeforeTest
import org.mockito.Answers
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.*
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith
import java.io.File

/**
 * Test class for the TrafficModelService.
 *
 * @author Team-MobiHub
 */
class TestTrafficModelService {
    private lateinit var trafficModelService: TrafficModelService
    private lateinit var trafficModelRepository: TrafficModelRepository
    private lateinit var characteristicsMappingRepository: CharacteristicsMappingRepository
    private lateinit var userRepository: UserRepository
    private lateinit var teamRepository: TeamRepository
    private lateinit var linkService: LinkService
    private lateinit var fileHandler: FileHandler
    private lateinit var environment: ApplicationEnvironment
    private lateinit var config: ApplicationConfig
    private lateinit var configValue: ApplicationConfigValue

    private lateinit var expectedTrafficModel: TrafficModel
    private lateinit var testUser1: User
    private lateinit var testUser2: User

    @BeforeTest
    fun setUp() {
        trafficModelRepository = mock()

        characteristicsMappingRepository = mock()
        userRepository = mock()
        teamRepository = mock()
        linkService = mock()
        fileHandler = mock()
        environment = mock()
        config = mock(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
        configValue = mock()

        val testConfig = MapApplicationConfig(
            "nextcloud.baseUrl" to "http://ifv-mobihub.ifv.kit.edu:8443"
        )

        environment = applicationEnvironment {
            config = testConfig
        }

        trafficModelService = TrafficModelService(
            trafficModelRepository,
            characteristicsMappingRepository,
            userRepository,
            teamRepository,
            linkService,
            fileHandler,
            environment
        )

        testUser1 = User(
            _id = UserId(1),
            _name = "Test User",
            _email = "test@example.com",
            _trafficModelProvider = mock(),
            _profilePictureProvider = mock(),
            isEmailVerified = true,
            passwordHash = "hashed_password",
            isAdmin = false,
            teamsProvider = { emptyList() }
        )
        testUser2 = User(
            _id = UserId(2),
            _name = "Test User",
            _email = "test@example.com",
            _trafficModelProvider = mock(),
            _profilePictureProvider = mock(),
            isEmailVerified = true,
            passwordHash = "hashed_password",
            isAdmin = false,
            teamsProvider = { emptyList() }
        )

        whenever(userRepository.getById(UserId(1))).thenReturn(testUser1)
        whenever(userRepository.getById(UserId(2))).thenReturn(testUser2)

        expectedTrafficModel = TrafficModel(
            id = TrafficModelId(1),
            name = "Test Model",
            description = "Test Description",
            isVisibilityPublic = true,
            dataSourceUrl = "http://example.com",
            location = Location(Region("Test Region"), Coordinates("abc")),
            framework = Framework.TRANS_MODELER,
            methodLevelPairProvider = {
                listOf(
                    Pair(
                        ModelLevel.CHOICE_OF_TRANSPORTATION,
                        ModelMethod.GRAVITATION_MODEL
                    ),
                    Pair(
                        ModelLevel.CAR_OWNER,
                        ModelMethod.NESTED_LOGIT
                    )
                )
            },
            authorProvider = {
                User(
                    UserId(1),
                    "Test User",
                    "test@example.com",
                    { listOf(expectedTrafficModel) },
                    { null },
                    true,
                    "passwordHash",
                    false,
                    { emptyList() })
            },
            markdownFileUrlProvider = { "" },
            imagesProvider = { listOf(
                Image(
                    UUID.randomUUID(),
                    "Test Image",
                    "png",
                    null
                ))
            },
            averageRatingProvider = { 0.0 },
            ratingsProvider = { emptyList() },
            commentsProvider = { emptyList() },
            favoritesProvider = { emptyList() },
            zipFileToken = UUID.randomUUID(),
            isZipFileUploaded = false
        )
    }

    @Test
    fun createTrafficModelSuccessfully() {
        val request = ChangeTrafficModelDTO(
            id = null,
            name = "Test Model",
            description = "Test Description",
            isVisibilityPublic = true,
            dataSourceUrl = "http://example.com",
            region = "Test Region",
            framework = Framework.TRANS_MODELER,
            characteristics = listOf(
                CharacteristicDTO(
                    modelLevel = ModelLevel.CHOICE_OF_TRANSPORTATION,
                    modelMethod = ModelMethod.MULTINOMIAL_LOGIT
                ),
                CharacteristicDTO(
                    modelLevel = ModelLevel.CAR_OWNER,
                    modelMethod = ModelMethod.NESTED_LOGIT
                )
            ),
            ownerUserId = 1,
            ownerTeamId = null,
            changedImages = listOf(FileStatusDTO("Test Image", FileChangeType.ADDED)),
            hasZipFileChanged = false,
            coordinates = ""
        )
        var capturedModel: TrafficModel
        whenever(trafficModelRepository.create(any())).thenReturn(expectedTrafficModel)

        val result = trafficModelService.create(request, testUser1.toDTO(""))

        argumentCaptor<TrafficModel>().let {
            verify(trafficModelRepository).create(it.capture())
            capturedModel = it.firstValue
            assertEquals("Test Model", capturedModel.name)
            assertEquals("Test Description", capturedModel.description)
            assertEquals(true, capturedModel.isVisibilityPublic)
            assertEquals("http://example.com", capturedModel.dataSourceUrl)
            assertEquals("Test Region", capturedModel.location.region.name)
            assertEquals("", capturedModel.location.coordinates!!.value)
            assert(capturedModel.methodLevelPair.contains(Pair(ModelLevel.CHOICE_OF_TRANSPORTATION, ModelMethod.MULTINOMIAL_LOGIT)))
            assertEquals(capturedModel.markdownFileURL, "")
            assert(capturedModel.images.map { it.name }.contains("Test Image"))
            assertEquals(capturedModel.averageRating, 0.0)
            assertEquals(capturedModel.ratings, emptyList())
            assertEquals(capturedModel.comments, emptyList())
            assertEquals(capturedModel.favorites, emptyList())
        }

        assertNotNull(result)
        assertEquals(expectedTrafficModel.id!!.id, result.id)
    }

    @Test
    fun getTrafficModelByIdSuccessfully() {
        // Arrange
        val trafficModelId = TrafficModelId(1)

        whenever(trafficModelRepository.getById(TrafficModelId(1))).thenReturn(
            expectedTrafficModel
        )

        // Act
        val result = trafficModelService.getById(trafficModelId, null)

        // Assert
        assertNotNull(result)
        assertEquals(expectedTrafficModel.id!!.id, result.id)
        assertEquals(expectedTrafficModel.name, result.name)
    }

    @Test
    fun getTrafficModelIdFailure() {
        // Arrange
        val trafficModelId = TrafficModelId(1)

        // Act & Assert
        val exception = assertFailsWith<DataWithIdentifierNotFoundException> {
            trafficModelService.getById(trafficModelId, null)
        }

        assertEquals("Traffic model by id 1 not found.", exception.message)
    }

    @Test
    fun getTrafficModelIdUnauthorized() {
        // Arrange
        val trafficModelId = TrafficModelId(1)
        val expectedTrafficModel = expectedTrafficModel.copy(isVisibilityPublic = false)
        whenever(trafficModelRepository.getById(TrafficModelId(1))).thenReturn(expectedTrafficModel)
        // Act & Assert
        val exception = assertFailsWith<UnauthorizedException> {
            trafficModelService.getById(trafficModelId, null)
        }

        assertEquals("Error: unauthorized access for Traffic model by not logged in User.", exception.message)
    }

    @Test
    fun getByOwnerUserIdSuccessfully() {
        whenever(trafficModelRepository.getByUser(UserId(1))).thenReturn(listOf(expectedTrafficModel, expectedTrafficModel.copy(isVisibilityPublic = false)))

        val result = trafficModelService.getByOwner(1, "USER", testUser1.toDTO(""))
        val result2 = trafficModelService.getByOwner(1, "USER", testUser1.toDTO("").copy(id = 2))

        assertEquals(2, result.size)
        assert(result.map { it.id }.contains(expectedTrafficModel.id!!.id))
        assertEquals(1, result2.size)
    }

    @Test
    fun deleteTrafficModelSuccessfully() {
        // Arrange
        val trafficModelId = TrafficModelId(1)

        whenever(trafficModelRepository.getById(trafficModelId)).thenReturn(expectedTrafficModel)

        // Act
        trafficModelService.delete(trafficModelId)
        whenever(trafficModelRepository.getById(trafficModelId)).then {
            throw DataWithIdentifierNotFoundException(
                "",
                "",
                ""
            )
        }

        // Assert
        assertFailsWith<DataWithIdentifierNotFoundException> {
            trafficModelService.getById(trafficModelId, null) // Call the actual method
        }
    }

    @Test
    fun deleteTrafficModelFailure() {
        // Arrange
        val trafficModelId = TrafficModelId(1)

        // Act & Assert
        val exception = assertFailsWith<DataWithIdentifierNotFoundException> {
            trafficModelService.delete(trafficModelId)
        }

        assertEquals("Traffic model by id 1 not found.", exception.message)
    }

    @Test
    fun updateTrafficModelSuccessfully() {
        val trafficModelCaptor = ArgumentCaptor.forClass(TrafficModel::class.java)
        whenever(trafficModelRepository.getById(TrafficModelId((1)))).thenReturn(expectedTrafficModel)
        val updatedTrafficModel = ChangeTrafficModelDTO(
            id = 1,
            name = "updated TrafficModel",
            description = "updated description",
            ownerUserId = 1,
            ownerTeamId = null,
            isVisibilityPublic = true,
            dataSourceUrl = "https://example.com",
            characteristics = listOf(CharacteristicDTO(ModelLevel.CAR_OWNER, ModelMethod.MULTINOMIAL_LOGIT)),
            framework = Framework.SATURN,
            region = "Test Region 2",
            coordinates = "0,2",
            hasZipFileChanged = false,
            changedImages = emptyList()
        )
        whenever(trafficModelRepository.update(any<TrafficModel>())).thenAnswer { it.arguments[0] }

        val result = trafficModelService.update(updatedTrafficModel, testUser1.toDTO(""))

        argumentCaptor<TrafficModel>().apply {
            verify(trafficModelRepository).update(capture())
            val capturedModel = firstValue
            assertEquals("updated TrafficModel", firstValue.name)
            assertEquals("", firstValue.markdownFileURL)
            assertEquals(0.0, firstValue.averageRating)
            assertEquals(emptyList(), firstValue.ratings)
            assertEquals(emptyList(), firstValue.favorites)
            assertEquals(emptyList(), firstValue.comments)
        }

    }

    @Test
    fun updateTrafficModelInvalidAuth() {
        whenever(trafficModelRepository.getById(TrafficModelId((1)))).thenReturn(expectedTrafficModel)
        val updatedTrafficModel = ChangeTrafficModelDTO(
            id = 1,
            name = "updated TrafficModel",
            description = "updated description",
            ownerUserId = 1,
            ownerTeamId = null,
            isVisibilityPublic = true,
            dataSourceUrl = "https://example.com",
            characteristics = listOf(CharacteristicDTO(ModelLevel.CAR_OWNER, ModelMethod.MULTINOMIAL_LOGIT)),
            framework = Framework.SATURN,
            region = "Test Region 2",
            coordinates = "0,2",
            hasZipFileChanged = false,
            changedImages = emptyList()
        )
        whenever(trafficModelRepository.update(any<TrafficModel>())).thenAnswer { it.arguments[0] }

        assertFailsWith<UnauthorizedException> {
            val result = trafficModelService.update(updatedTrafficModel, testUser2.toDTO(""))
        }
    }

    @Test
    fun updateTrafficModelDNE() {
        whenever(trafficModelRepository.getById(TrafficModelId((1)))).thenReturn(null)
        val updatedTrafficModel = ChangeTrafficModelDTO(
            id = 1,
            name = "updated TrafficModel",
            description = "updated description",
            ownerUserId = 1,
            ownerTeamId = null,
            isVisibilityPublic = true,
            dataSourceUrl = "https://example.com",
            characteristics = listOf(CharacteristicDTO(ModelLevel.CAR_OWNER, ModelMethod.MULTINOMIAL_LOGIT)),
            framework = Framework.SATURN,
            region = "Test Region 2",
            coordinates = "0,2",
            hasZipFileChanged = false,
            changedImages = emptyList()
        )
        whenever(trafficModelRepository.update(any<TrafficModel>())).thenAnswer { it.arguments[0] }

        assertFailsWith<DataWithIdentifierNotFoundException> {
            val result = trafficModelService.update(updatedTrafficModel, testUser2.toDTO(""))
        }
    }

    @Test
    fun updateTrafficModelWithImages() {
        var capturedModel: TrafficModel
        whenever(trafficModelRepository.getById(TrafficModelId((1)))).thenReturn(expectedTrafficModel)
        val updatedTrafficModel = ChangeTrafficModelDTO(
            id = 1,
            name = "updated TrafficModel",
            description = "updated description",
            ownerUserId = 1,
            ownerTeamId = null,
            isVisibilityPublic = true,
            dataSourceUrl = "https://example.com",
            characteristics = listOf(CharacteristicDTO(ModelLevel.CAR_OWNER, ModelMethod.MULTINOMIAL_LOGIT)),
            framework = Framework.SATURN,
            region = "Test Region 2",
            coordinates = "0,2",
            hasZipFileChanged = false,
            changedImages = listOf(
                FileStatusDTO("Test_Image2.png", FileChangeType.ADDED),
                FileStatusDTO("${expectedTrafficModel.images[0].token}.${expectedTrafficModel.images[0].fileExtension}", FileChangeType.REMOVED)
            )
        )
        whenever(trafficModelRepository.update(any<TrafficModel>())).thenAnswer { it.arguments[0] }

        val result = trafficModelService.update(updatedTrafficModel, testUser1.toDTO(""))

        argumentCaptor<TrafficModel>().apply {
            verify(trafficModelRepository).update(capture())
            capturedModel = firstValue
            assertEquals(capturedModel.images.size, 1)
            assertEquals(capturedModel.images[0].name, "Test_Image2")
        }

        // File doesn't exist
        var imageFile = File("src/test/recources/profile_pictures/Test_Image2.png")
        assert(!imageFile.exists())
        assertFailsWith<FileInfectedException> {
            trafficModelService.uploadFile(
                imageFile,
                TrafficModelId(1),
                UUID.fromString(result.imageTokens[0]),
                FileType.IMAGE
            )
        }

        // File exists but TM doesn't exist
        imageFile = File("src/test/resources/profile_pictures/Test_Image2.png")
        assert(imageFile.exists())
        whenever(trafficModelRepository.getById(TrafficModelId(1))).thenReturn(null) // Simulate that model doesn't exist
        assertFailsWith<DataWithIdentifierNotFoundException> {
            trafficModelService.uploadFile(
                imageFile,
                TrafficModelId(1),
                UUID.fromString(result.imageTokens[0]),
                FileType.IMAGE
            )
        }

        // now traffic model exists but image doesn't
        whenever(trafficModelRepository.getById(TrafficModelId(1))).thenReturn(capturedModel)
        assertFailsWith<AssertionError> {
            trafficModelService.uploadFile(
                imageFile,
                TrafficModelId(1),
                UUID.randomUUID(),
                FileType.IMAGE
            )
        }

        whenever(fileHandler.getDownloadReference(any(), any())).thenReturn(ShareLink("shareToken", "Test_Image2.png"))
        // now traffic model exists and image exists
        trafficModelService.uploadFile(
            imageFile,
            TrafficModelId(1),
            UUID.fromString(result.imageTokens[0]),
            FileType.IMAGE
        )
    }

    @Test
    fun uploadZipFile() {
        val zipFile = File("src/test/resources/testfiles/smallTestFile.zip")
        var capturedModel: TrafficModel
        val updatedTrafficModel = ChangeTrafficModelDTO(
            id = 1,
            name = "updated TrafficModel",
            description = "updated description",
            ownerUserId = 1,
            ownerTeamId = null,
            isVisibilityPublic = true,
            dataSourceUrl = "https://example.com",
            characteristics = listOf(CharacteristicDTO(ModelLevel.CAR_OWNER, ModelMethod.MULTINOMIAL_LOGIT)),
            framework = Framework.SATURN,
            region = "Test Region 2",
            coordinates = "0,2",
            hasZipFileChanged = true,
            changedImages = emptyList()
        )
        whenever(trafficModelRepository.getById(TrafficModelId(1))).thenReturn(expectedTrafficModel)
        whenever(trafficModelRepository.update(any<TrafficModel>())).thenAnswer { it.arguments[0] }

        // Zipfile not updated first
        assertFailsWith<AssertionError> {
            trafficModelService.uploadFile(
                zipFile,
                TrafficModelId(1),
                UUID.randomUUID(),
                FileType.ZIP
            )
        }

        // now update first
        trafficModelService.update(updatedTrafficModel, testUser1.toDTO(""))
        argumentCaptor<TrafficModel>().apply {
            verify(trafficModelRepository).update(capture())
            capturedModel = firstValue
            assertNotNull(capturedModel.zipFileToken)
            assertEquals(capturedModel.isZipFileUploaded, false)
        }

        whenever(trafficModelRepository.getById(TrafficModelId(1))).thenReturn(capturedModel)
        trafficModelService.uploadFile(zipFile, TrafficModelId(1), capturedModel.zipFileToken, FileType.ZIP)

        // get download link

        whenever(fileHandler.getDownloadReference(any(), any())).thenReturn(ShareLink("shareToken", "smallTestFile.png"))

        // get download link for invalid TM

        assertFailsWith<DataWithIdentifierNotFoundException> {
            trafficModelService.getDownloadLink(TrafficModelId(999))
        }

        // get download link for valid TM

        val downloadLink = trafficModelService.getDownloadLink(TrafficModelId(1))

        assert(downloadLink.contains("shareToken"))
    }

    @Test
    fun searchPaginatedAndFiltered() {
        val searchRequest = SearchRequestDTO(
            page = 0,
            pageSize = 20,
            name = "Test Model",
            authorName = "Test User",
            region = "Test Region",
            modelLevels = listOf(ModelLevel.CHOICE_OF_TRANSPORTATION),
            modelMethods = listOf(ModelMethod.MULTINOMIAL_LOGIT),
            frameworks = listOf(Framework.TRANS_MODELER)
        )

        whenever(trafficModelRepository.searchPaginated(
            page = searchRequest.page,
            size = searchRequest.pageSize,
            name = searchRequest.name,
            authorName = searchRequest.authorName,
            region = Region(searchRequest.region!!),
            modelLevels = searchRequest.modelLevels,
            modelMethods = searchRequest.modelMethods,
            frameworks = searchRequest.frameworks
        )).thenReturn(Pair(listOf(expectedTrafficModel),1))

        val result = trafficModelService.getPaginatedAndFiltered(searchRequest)
        assert(result.searchResult.map { it.trafficModelId }.contains(expectedTrafficModel.id!!.id))
        assertEquals(result.totalCount, 1)
    }
}