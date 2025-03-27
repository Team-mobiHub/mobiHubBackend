package com.mobihub.repositoriesTest

import com.mobihub.model.*
import com.mobihub.repositories.*
import com.mobihub.repositories.db.CommentTable
import com.mobihub.repositories.db.FavouriteTable
import com.mobihub.repositories.db.TrafficModelTable
import com.mobihub.repositories.db.UserTable
import com.mobihub.utils.file.FileHandler
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Schema
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.mockito.kotlin.mock
import java.util.*
import kotlin.test.*

class FavoriteDbRepositoryTest {

    private var repositoryProvider: RepositoryProvider = RepositoryProvider(fileHandler = mock<FileHandler>())
    private var favouriteRepository: FavouriteRepository = FavouriteDbRepository(repositoryProvider)
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
            SchemaUtils.create(FavouriteTable, UserTable, TrafficModelTable)
        }
    }

    @Test
    fun addFavouriteSuccessfully() {
        val user = createAndGetUser(1)
        val trafficModel = createAndGetTrafficModel("Model 1")

        favouriteRepository.addFavourite(trafficModel.id!!, user._id!!)

        val favourites = favouriteRepository.getFavouritesByUserId(user._id!!)
        assertEquals(favourites[0].id, trafficModel.id)
    }

    @Test
    fun deleteFavouriteSuccessfully() {
        val user = createAndGetUser(2)
        val trafficModel = createAndGetTrafficModel("Model 2")

        favouriteRepository.addFavourite(trafficModel.id!!, user._id!!)
        favouriteRepository.deleteFavourite(trafficModel.id!!, user._id!!)

        val favourites = favouriteRepository.getFavouritesByUserId(user._id!!)
        assertFalse(favourites.map { it.id }.contains(trafficModel.id))
    }

    @Test
    fun getFavouritesByUserIdSuccessfully() {
        val user = createAndGetUser(3)
        val trafficModel1 = createAndGetTrafficModel("Model 3")
        val trafficModel2 = createAndGetTrafficModel("Model 4")

        favouriteRepository.addFavourite(trafficModel1.id!!, user._id!!)
        favouriteRepository.addFavourite(trafficModel2.id!!, user._id!!)

        val favourites = favouriteRepository.getFavouritesByUserId(user._id!!)
        assertEquals(2, favourites.size)
        assertTrue(favourites.map { it.id }.containsAll(listOf(trafficModel1.id, trafficModel2.id)))
    }

    @Test
    fun getFavoritesByTrafficModelIdSuccessfully() {
        val user1 = createAndGetUser(4)
        val user2 = createAndGetUser(5)
        val trafficModel = createAndGetTrafficModel("Model 5")

        favouriteRepository.addFavourite(trafficModel.id!!, user1._id!!)
        favouriteRepository.addFavourite(trafficModel.id!!, user2._id!!)

        val users = favouriteRepository.getFavoritesByTrafficModelId(trafficModel.id!!)
        assertEquals(2, users.size)
        assertTrue(users.map { it._id }.containsAll(listOf(user1._id, user2._id)))
    }
}