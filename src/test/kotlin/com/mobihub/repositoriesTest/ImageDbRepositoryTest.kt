package com.mobihub.repositoriesTest

import com.mobihub.model.Image
import com.mobihub.model.ShareToken
import com.mobihub.repositories.ImageDbRepository
import com.mobihub.repositories.RepositoryProvider
import com.mobihub.repositories.db.FavouriteTable
import com.mobihub.repositories.db.ImageTable
import com.mobihub.repositories.db.TrafficModelTable
import com.mobihub.repositories.db.UserTable
import com.mobihub.utils.file.FileHandler
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.mockito.kotlin.mock
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.*

class ImageDbRepositoryTest {

    private lateinit var imageDbRepository: ImageDbRepository
    private lateinit var repositoryProvider: RepositoryProvider

    @BeforeTest
    fun setup() {
        // Connect to the in-memory H2 database
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")

        // Create the ImageTable schema
        transaction {
            SchemaUtils.create(ImageTable)
        }

        // Initialize the repository provider and the image repository
        repositoryProvider = RepositoryProvider(fileHandler = mock<FileHandler>())
        imageDbRepository = ImageDbRepository(repositoryProvider)
    }

    @Test
    fun addImageSuccessfully() {
        val image = Image(
            token = UUID.randomUUID(),
            name = "testImage",
            fileExtension = "jpg",
            shareToken = ShareToken("shareToken")
        )

        imageDbRepository.create(image)

        val storedImage = imageDbRepository.get(image.token)
        assertNotNull(storedImage)
        assertEquals(image.token, storedImage.token)
        assertEquals(image.name, storedImage.name)
        assertEquals(image.fileExtension, storedImage.fileExtension)
        assertEquals(image.token, storedImage.token)
        assertEquals(image.shareToken, storedImage.shareToken)
    }

    @Test
    fun getImageByIdReturnsNullForNonExistentImage() {
        val nonExistentImageId = UUID.randomUUID()

        val retrievedImage = imageDbRepository.get(nonExistentImageId)
        assertNull(retrievedImage)
    }

    @Test
    fun deleteImageSuccessfully() {
        val image = Image(
            name = "testImage",
            fileExtension = "jpg",
            token = UUID.randomUUID(),
            shareToken = ShareToken("shareToken")
        )

        imageDbRepository.create(image)
        imageDbRepository.delete(image.token)

        val retrievedImage = imageDbRepository.get(image.token)
        assertNull(retrievedImage)
    }

    @Test
    fun updateImageSuccessfully() {
        val image = Image(
            name = "testImage",
            fileExtension = "jpg",
            token = UUID.randomUUID(),
            shareToken = ShareToken("shareToken")
        )

        imageDbRepository.create(image)

        val updatedImage = image.copy(name = "updatedImage")
        imageDbRepository.update(updatedImage)

        val retrievedImage = imageDbRepository.get(image.token)
        assertNotNull(retrievedImage)
        assertEquals(updatedImage.name, retrievedImage.name)
    }
}