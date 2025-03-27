package com.mobihub.repositoriesTest

import com.mobihub.model.*
import com.mobihub.repositories.*
import com.mobihub.repositories.db.CharacteristicsMappingTable
import com.mobihub.repositories.db.CommentTable
import com.mobihub.repositories.db.TrafficModelTable
import com.mobihub.repositories.db.UserTable
import com.mobihub.utils.file.FileHandler
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.mockito.kotlin.mock
import java.util.*
import kotlin.test.*

class CharacteristicsMappingRepositoryTest {

    private var repositoryProvider: RepositoryProvider = RepositoryProvider(fileHandler = mock<FileHandler>())
    private var characteristicsMappingRepository: CharacteristicsMappingRepository = CharacteristicsMappingDbRepository(repositoryProvider)
    private var userRepository: UserRepository = UserDbRepository(repositoryProvider)
    private var trafficModelRepository: TrafficModelRepository = TrafficModelDbRepository(repositoryProvider)

    private fun createAndGetUser(name: Int): User {
        val user = User(
            null, // name will not be id, because create changes it
            "Test User$name",
            _email = "test$name@gmail.com",
            _trafficModelProvider = { emptyList() },
            _profilePictureProvider = { null },
            isEmailVerified = true,
            passwordHash = "passwordHash",
            isAdmin = false,
            teamsProvider = { emptyList() }
        )
        return userRepository.getByName("Test User$name") ?: return userRepository.create(user)
    }

    private fun createAndGetTrafficModel(name: String): TrafficModel {
        val trafficModel = TrafficModel(
            id = null,
            name = name,
            description = "Test Description",
            isVisibilityPublic = true,
            dataSourceUrl = "http://example.com",
            location = Location(Region("Test Region"), Coordinates("0,0")),
            framework = Framework.fromId(FrameworkId(1)),
            zipFileToken = UUID.randomUUID(),
            isZipFileUploaded = false,
            methodLevelPairProvider = { emptyList() },
            authorProvider = { createAndGetUser(1) },
            markdownFileUrlProvider = { null },
            favoritesProvider = { emptyList() },
            imagesProvider = { emptyList() },
            averageRatingProvider = { 0.0 },
            ratingsProvider = { emptyList() },
            commentsProvider = { emptyList() }
        )
        return trafficModelRepository.create(trafficModel)
    }

    /**
     * Setup method executed before each test case.
     * Connects to an in-memory H2 database, creates the necessary schemas, and initializes the repository provider.
     */
    @BeforeTest
    fun setup() {
        // Connect to the in-memory H2 database
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")

        // Create the UserTable schema
        transaction {
            SchemaUtils.create(CharacteristicsMappingTable, TrafficModelTable)
        }
    }

    @Test
    fun createMappingSuccessfully() {
        val trafficModel = createAndGetTrafficModel("Model 1")
        val mapping = listOf(
            Pair(ModelLevel.fromId(ModelLevelId(1)), ModelMethod.fromId(ModelMethodId(1))),
            Pair(ModelLevel.fromId(ModelLevelId(2)), ModelMethod.fromId(ModelMethodId(2)))
        )

        val result = characteristicsMappingRepository.create(trafficModel.id!!, mapping)

        assertEquals(mapping.size, result.size)
        assertTrue(result.containsAll(mapping))
    }

    @Test
    fun updateMappingSuccessfully() {
        val trafficModel = createAndGetTrafficModel("Model 2")
        val initialMapping = listOf(
            Pair(ModelLevel.fromId(ModelLevelId(1)), ModelMethod.fromId(ModelMethodId(1)))
        )
        val updatedMapping = listOf(
            Pair(ModelLevel.fromId(ModelLevelId(2)), ModelMethod.fromId(ModelMethodId(2))),
            Pair(ModelLevel.fromId(ModelLevelId(3)), ModelMethod.fromId(ModelMethodId(3)))
        )

        characteristicsMappingRepository.create(trafficModel.id!!, initialMapping)
        characteristicsMappingRepository.update(trafficModel.id!!, updatedMapping)

        val result = characteristicsMappingRepository.get(trafficModel.id!!)

        assertEquals(updatedMapping.size, result.size)
        assertTrue(result.containsAll(updatedMapping))
    }

    @Test
    fun getMappingSuccessfully() {
        val trafficModel = createAndGetTrafficModel("Model 3")
        val mapping = listOf(
            Pair(ModelLevel.fromId(ModelLevelId(1)), ModelMethod.fromId(ModelMethodId(1))),
            Pair(ModelLevel.fromId(ModelLevelId(2)), ModelMethod.fromId(ModelMethodId(2)))
        )

        characteristicsMappingRepository.create(trafficModel.id!!, mapping)

        val result = characteristicsMappingRepository.get(trafficModel.id!!)

        assertEquals(mapping.size, result.size)
        assertTrue(result.containsAll(mapping))
    }

    @Test
    fun deleteMappingSuccessfully() {
        val trafficModel = createAndGetTrafficModel("Model 4")
        val mapping = listOf(
            Pair(ModelLevel.fromId(ModelLevelId(1)), ModelMethod.fromId(ModelMethodId(1))),
            Pair(ModelLevel.fromId(ModelLevelId(2)), ModelMethod.fromId(ModelMethodId(2)))
        )

        characteristicsMappingRepository.create(trafficModel.id!!, mapping)
        characteristicsMappingRepository.delete(trafficModel.id!!)

        val result = characteristicsMappingRepository.get(trafficModel.id!!)

        assertTrue(result.isEmpty())
    }
}