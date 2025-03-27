package com.mobihub.repositoriesTest

import com.mobihub.model.*
import com.mobihub.repositories.*
import com.mobihub.repositories.db.*
import com.mobihub.model.ModelMethod
import com.mobihub.model.ModelLevel
import kotlin.test.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.BeforeClass
import org.mockito.kotlin.*
import java.time.Instant
import java.util.*

class TrafficModelDbRepositoryTest {

    companion object INIT {

        private lateinit var repository: TrafficModelDbRepository
        private lateinit var repositoryProvider: RepositoryProvider

        private fun getUser(id: Int) = User(
            UserId(id), "Test_User$id",
            _email = "test$id@gmail.com",
            _trafficModelProvider = { emptyList() },
            _profilePictureProvider = { null },
            isEmailVerified = true,
            passwordHash = "passwordHash",
            isAdmin = false,
            teamsProvider = { emptyList() }
        )

        private val team: Team = Team(
            _id = TeamId(1),
            _name = "Team",
            _email = "team@gmail.com",
            _profilePicureProvider = { null },
            _trafficModelProvider = { emptyList() },
            description = "team",
            ownerProvider = { repositoryProvider.userRepository.getById(UserId(1))!! },
            membersProvider = { emptyList() }
        )

        @JvmStatic
        @BeforeClass
        fun setup() {
            // Connect to the in-memory H2 database
            Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")

            // Create the TrafficModelTable schema
            transaction {
                SchemaUtils.create(TrafficModelTable, ImageMappingTable, CharacteristicsMappingTable, CommentTable, RatingTable, UserTable, FavouriteTable)
            }

            repositoryProvider = mock()
            repository = TrafficModelDbRepository(repositoryProvider)
            whenever(repositoryProvider.teamRepository).thenReturn(mock<TeamRepository>())
            whenever(repositoryProvider.commentRepository).thenReturn(CommentDbRepository(repositoryProvider))
            whenever(repositoryProvider.ratingRepository).thenReturn(RatingDbRepository(repositoryProvider))
            whenever(repositoryProvider.favouriteRepository).thenReturn(FavouriteDbRepository(repositoryProvider))
            whenever(repositoryProvider.imageRepository).thenReturn(ImageDbRepository(repositoryProvider))
            whenever(repositoryProvider.characteristicsMappingRepository).thenReturn(CharacteristicsMappingDbRepository(repositoryProvider))
            whenever(repositoryProvider.trafficModelRepository).thenReturn(repository)
            val userRepository = UserDbRepository(repositoryProvider)

            whenever(repositoryProvider.userRepository).thenReturn(userRepository)

            userRepository.create(getUser(1))
            userRepository.create(getUser(2))
            userRepository.create(getUser(3))

            team.apply {
                whenever(repositoryProvider.teamRepository.getById(TeamId(1))).thenReturn(this)
                whenever(repositoryProvider.teamRepository.getByUserId(UserId(1))).thenReturn(listOf(this))
            }
            }
        }


    @Test
    fun createTrafficModel_createsAndReturnsTrafficModel() {
        val result = createTrafficModel("Traffic Model")

        assertNotNull(result.id)
        assertEquals("Traffic Model", result.name)
    }

    @Test
    fun getById_existingId_returnsTrafficModel() {
        val resultTrafficModel = createTrafficModel("Traffic Model 1")
        val fetchedTrafficModel = repository.getById(resultTrafficModel.id!!)!!

        // assert to test lazy loading
        assertEquals(fetchedTrafficModel.methodLevelPair, listOf(Pair(ModelLevel.CAR_OWNER, ModelMethod.MIXED_LOGIT)))
        assertEquals(fetchedTrafficModel.author.id, IdentityId(1))
        assertEquals(fetchedTrafficModel.markdownFileURL, null)
        assertEquals(fetchedTrafficModel.averageRating, 3.0)
        assert(fetchedTrafficModel.ratings.isNotEmpty())
        assert(fetchedTrafficModel.comments.isNotEmpty())
        assert(fetchedTrafficModel.favorites.isNotEmpty())
        assertNotNull(fetchedTrafficModel)
        assertEquals(fetchedTrafficModel.id, resultTrafficModel.id)
    }

    @Test
    fun getById_nonExistingId_returnsNull() {
        val trafficModelId = TrafficModelId(999)
        val result = repository.getById(trafficModelId)

        assertNull(result)
    }

    @Test
    fun updateTrafficModel_updatesAndReturnsTrafficModel() {
        val trafficModelResult = createTrafficModel("Traffic Model 3")

        val updatedTrafficModel = TrafficModel(
            id = trafficModelResult.id,
            name = "Updated Model",
            description = "Updated Description",
            isVisibilityPublic = true,
            dataSourceUrl = "http://example.com",
            location = Location(Region("Updated Region"), Coordinates("0,0")),
            framework = Framework.fromId(FrameworkId(1)),
            zipFileToken = UUID.randomUUID(),
            isZipFileUploaded = false,
            methodLevelPairProvider = { emptyList() },
            authorProvider = { getUser(1) },
            markdownFileUrlProvider = { null },
            favoritesProvider = { emptyList() },
            imagesProvider = { emptyList() },
            averageRatingProvider = { 0.0 },
            ratingsProvider = { emptyList() },
            commentsProvider = { emptyList() }
        )
        val result = repository.update(updatedTrafficModel)

        assertEquals("Updated Model", result.name)
        assertEquals("Updated Description", result.description)
        assertEquals(result.id, trafficModelResult.id)
    }

    @Test
    fun deleteTrafficModel_deletesTrafficModel() {
        val trafficModel = createTrafficModel("Traffic Model 4")
        repository.delete(trafficModel.id!!)

        // database should also not complain about missing foreign keys (for comments, favorites, ratings, etc.)
        assertNull(repository.getById(trafficModel.id!!))
    }

    @Ignore
    @Test
    fun getByTeam_existingTeamId_returnsTrafficModels() {
        val trafficModel =
            TrafficModel(
                id = TrafficModelId(1),
                name = "Test Model Team",
                description = "Test Description 1",
                isVisibilityPublic = true,
                dataSourceUrl = "http://example.com",
                location = Location(Region("Test Region"), Coordinates("0,0")),
                framework = Framework.fromId(FrameworkId(1)),
                zipFileToken = UUID.randomUUID(),
                isZipFileUploaded = false,
                methodLevelPairProvider = { emptyList() },
                authorProvider = { team },
                markdownFileUrlProvider = { null },
                favoritesProvider = { emptyList() },
                imagesProvider = { emptyList() },
                averageRatingProvider = { 0.0 },
                ratingsProvider = { emptyList() },
                commentsProvider = { emptyList() }
            )

        val resultTrafficModel = repository.create(trafficModel)
        val result = repository.getByTeam(TeamId(resultTrafficModel.author.id?.id!!))

        assertEquals(1, result.size)
        assertEquals("Test Model Team", result[0].name)
    }

    @Test
    fun getByUser_existingUserId_returnsTrafficModels() {
        val trafficModel =
            TrafficModel(
                id = TrafficModelId(1),
                name = "Test Model User",
                description = "Test Description 1",
                isVisibilityPublic = true,
                dataSourceUrl = "http://example.com",
                location = Location(Region("Test Region"), Coordinates("0,0")),
                framework = Framework.fromId(FrameworkId(1)),
                zipFileToken = UUID.randomUUID(),
                isZipFileUploaded = false,
                methodLevelPairProvider = { emptyList() },
                authorProvider = { getUser(2) },
                markdownFileUrlProvider = { null },
                favoritesProvider = { emptyList() },
                imagesProvider = { emptyList() },
                averageRatingProvider = { 0.0 },
                ratingsProvider = { emptyList() },
                commentsProvider = { emptyList() }
            )

        val resultTrafficModel = repository.create(trafficModel)
        val result = repository.getByUser(UserId(resultTrafficModel.author.id?.id!!))

        assert(result.isNotEmpty())
        assert(result.map { it.name }.contains("Test Model User"))
    }

    @Test
    fun searchPaginated_validCriteria_returnsTrafficModels() {
        val trafficModel =
        TrafficModel(
            id = TrafficModelId(1),
            name = "Test Model Search",
            description = "Test Description 1",
            isVisibilityPublic = true,
            dataSourceUrl = "http://example.com",
            location = Location(Region("Test Region"), Coordinates("0,0")),
            framework = Framework.SATURN,
            zipFileToken = UUID.randomUUID(),
            isZipFileUploaded = false,
            methodLevelPairProvider = { listOf(Pair(ModelLevel.CAR_OWNER, ModelMethod.MULTINOMIAL_LOGIT)) },
            authorProvider = { getUser(3) },
            markdownFileUrlProvider = { null },
            favoritesProvider = { emptyList() },
            imagesProvider = { emptyList() },
            averageRatingProvider = { 0.0 },
            ratingsProvider = { emptyList() },
            commentsProvider = { emptyList() }
        )
        val createdTrafficModel = repository.create(trafficModel)
        repositoryProvider.characteristicsMappingRepository.create(createdTrafficModel.id!!, listOf(Pair(ModelLevel.CAR_OWNER, ModelMethod.MULTINOMIAL_LOGIT)))
        val result = repository.searchPaginated(
            page = 0,
            size = 10,
            name = "Test",
            authorName = "User",
            region = Region("Test Region"),
            modelLevels = listOf(ModelLevel.CAR_OWNER),
            modelMethods = listOf(ModelMethod.MULTINOMIAL_LOGIT),
            frameworks = listOf(Framework.SATURN)
        ).first

        assert(result.isNotEmpty(), {result})
        assert(result.map { it.name }.contains("Test Model Search"))
    }

    @Test
    fun updateImages_updatesImagesForTrafficModel() {
        var trafficModel = createTrafficModel("Traffic Model 6")
        val images = listOf(
            Image(UUID.randomUUID(), "image1", "jpg", ShareToken("token1")),
            Image(UUID.randomUUID(), "image2", "png", ShareToken("token2"))
        )

        repository.updateImages(trafficModel.id!!, images)
        trafficModel = repository.getById(trafficModel.id!!)!! // update image lazy function
        assertEquals(HashSet(trafficModel.images), HashSet(images))

        // update again to check delete code
        val image2 = Image(UUID.randomUUID(), "image3", "jpg", ShareToken("token3"))

        repository.updateImages(trafficModel.id!!, listOf(image2))
        assertEquals(HashSet(trafficModel.imagesProvider.invoke()), HashSet(listOf(image2)))

        val imageCopy = image2.copy()
        imageCopy.name = "imageCopy"

        repository.updateImage(trafficModel.id!!, imageCopy)

        trafficModel = repository.getById(trafficModel.id!!)!!
        assertEquals(HashSet(trafficModel.imagesProvider.invoke()), HashSet(listOf(imageCopy)))
    }

    private fun createTrafficModel(name: String): TrafficModel {
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
            authorProvider = { getUser(1) },
            markdownFileUrlProvider = { null },
            favoritesProvider = { emptyList() },
            imagesProvider = { emptyList() },
            averageRatingProvider = { 0.0 },
            ratingsProvider = { emptyList() },
            commentsProvider = { emptyList() }
        )
        val createdTrafficModel = repository.create(trafficModel)
        repositoryProvider.characteristicsMappingRepository.create(createdTrafficModel.id!!, listOf(Pair(ModelLevel.CAR_OWNER, ModelMethod.MIXED_LOGIT)))
        repositoryProvider.commentRepository.addComment(Comment(
            id = CommentId(1),
            content = "Test Comment",
            creationDate = Instant.now(),
            trafficModelProvider = { createdTrafficModel },
            userProvider = { getUser(1) }
        ))
        repositoryProvider.ratingRepository.addRating(Rating(
            rating = 3,
            trafficModelProvider = {createdTrafficModel},
            userProvider = { getUser(2) }
        ))
        repositoryProvider.favouriteRepository.addFavourite(createdTrafficModel.id!!, UserId(2))
        val image = Image(
            UUID.randomUUID(),
            "image999",
            "jpg",
            ShareToken("token")
        )
        repository.updateImages(createdTrafficModel.id!!, listOf(image))

        return createdTrafficModel
    }


}