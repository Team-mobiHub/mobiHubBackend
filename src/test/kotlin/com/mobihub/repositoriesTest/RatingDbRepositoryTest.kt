package com.mobihub.repositoriesTest

import com.mobihub.model.*
import com.mobihub.repositories.*
import com.mobihub.repositories.db.FavouriteTable
import com.mobihub.repositories.db.RatingTable
import com.mobihub.repositories.db.TrafficModelTable
import com.mobihub.repositories.db.UserTable
import com.mobihub.utils.file.FileHandler
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.mockito.kotlin.mock
import java.util.*
import kotlin.test.*

class RatingDbRepositoryTest {

    private var repositoryProvider: RepositoryProvider = RepositoryProvider(fileHandler = mock<FileHandler>())
    private var ratingRepository: RatingRepository = RatingDbRepository(repositoryProvider)
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
            SchemaUtils.create(RatingTable, UserTable, TrafficModelTable)
        }
    }


    @Test
    fun addRatingSuccessfully() {
        val rating = Rating(
            rating = 5,
            trafficModelProvider = { createAndGetTrafficModel("1") },
            userProvider = { createAndGetUser(1) }
        )

        val result = ratingRepository.addRating(rating)

        assertEquals(rating, result)
    }

    @Test
    fun updateRatingSuccessfully() {
        val trafficModel = createAndGetTrafficModel("2")
        val user = createAndGetUser(2)
        val rating = Rating(
            rating = 4,
            trafficModelProvider = { trafficModel },
            userProvider = { user }
        )

        var result = ratingRepository.addRating(rating)

        result = ratingRepository.updateRating(rating.copy(rating = 5))

        assertEquals(5, result.rating)
    }

    @Test
    fun deleteRatingSuccessfully() {
        val user = createAndGetUser(3)
        val rating = Rating(
            rating = 3,
            trafficModelProvider = { createAndGetTrafficModel("3") },
            userProvider = { user }
        )

        ratingRepository.addRating(rating)

        ratingRepository.deleteRating(rating)

        val ratings = ratingRepository.getRatingsForUser(UserId(user.id!!.id))
        assertFalse(ratings.contains(rating))
    }

    @Test
    fun getRatingsForTrafficModelSuccessfully() {
        val trafficModel = createAndGetTrafficModel("4")
        val ratings = listOf(
            Rating(
                rating = 5,
                trafficModelProvider = { trafficModel },
                userProvider = { createAndGetUser(4) }
            ),
            Rating(
                rating = 4,
                trafficModelProvider = { trafficModel },
                userProvider = { createAndGetUser(5) }
            )
        )

        ratingRepository.addRating(ratings[0])
        ratingRepository.addRating(ratings[1])

        val result = ratingRepository.getRatingsForTrafficModel(trafficModel.id!!)

        assertEquals(ratings.size, result.size)
        assertEquals(trafficModel.id?.id!!, result[0].trafficModel.id?.id!!)
        assertEquals(trafficModel.id?.id!!, result[1].trafficModel.id?.id!!)
    }

    @Test
    fun getRatingsForUserSuccessfully() {
        val user = createAndGetUser(6)
        val ratings = listOf(
            Rating(
                rating = 5,
                trafficModelProvider = { createAndGetTrafficModel("5") },
                userProvider = { user }
            ),
            Rating(
                rating = 4,
                trafficModelProvider = { createAndGetTrafficModel("6") },
                userProvider = { user }
            )
        )

        ratingRepository.addRating(ratings[0])
        ratingRepository.addRating(ratings[1])

        val result = ratingRepository.getRatingsForUser(UserId(user.id?.id!!))

        assertEquals(ratings.size, result.size)
        assertEquals(user.id?.id!!, result[0].user.id?.id!!)
        assertEquals(user.id?.id!!, result[1].user.id?.id!!)
    }

    @Test
    fun getAverageRatingForTrafficModelSuccessfully() {
        val trafficModel = createAndGetTrafficModel("7")
        val ratings = listOf(
            Rating(
                rating = 5,
                trafficModelProvider = { trafficModel },
                userProvider = { createAndGetUser(7) }
            ),
            Rating(
                rating = 1,
                trafficModelProvider = { trafficModel },
                userProvider = { createAndGetUser(8) }
            )
        )

        assertEquals(ratingRepository.getAverageRatingForTrafficModel(trafficModel.id!!), 0.0)

        ratingRepository.addRating(ratings[0])
        ratingRepository.addRating(ratings[1])

        val result = ratingRepository.getAverageRatingForTrafficModel(trafficModel.id!!)

        assertEquals(3.0, result)
    }

}