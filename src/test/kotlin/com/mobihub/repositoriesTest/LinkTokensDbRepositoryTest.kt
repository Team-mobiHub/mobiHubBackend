package com.mobihub.repositoriesTest

import com.mobihub.model.LinkData
import com.mobihub.model.LinkType
import com.mobihub.repositories.LinkTokensDbRepository
import com.mobihub.repositories.LinkTokensRepository
import com.mobihub.repositories.RepositoryProvider
import com.mobihub.repositories.db.FavouriteTable
import com.mobihub.repositories.db.LinkTokensTable
import com.mobihub.repositories.db.TrafficModelTable
import com.mobihub.repositories.db.UserTable
import com.mobihub.utils.file.FileHandler
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.mockito.kotlin.mock
import java.sql.Timestamp
import java.util.*
import kotlin.test.*

class LinkTokensDbRepositoryTest {

    private var repositoryProvider: RepositoryProvider = RepositoryProvider(fileHandler = mock<FileHandler>())
    private val linkTokensRepository: LinkTokensRepository = LinkTokensDbRepository(repositoryProvider)
    @BeforeTest
    fun setup() {
        // Connect to the in-memory H2 database
        Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")

        // Create the LinkTokensTable schema
        transaction {
        SchemaUtils.create(LinkTokensTable)
        }
    }

    @Test
    fun storeTokenSuccessfully() {
        val linkData = LinkData(
            token = UUID.randomUUID(),
            email = "test@example.com",
            createdAt = Timestamp(System.currentTimeMillis()),
            user = null,
            team = null,
            linkType = LinkType.PASSWORD_RESET
        )

        linkTokensRepository.storeToken(linkData)

        val storedLinkData = linkTokensRepository.getToken(linkData.token)
        assertNotNull(storedLinkData)
        assertEquals(linkData.token, storedLinkData.token)
        assertEquals(linkData.email, storedLinkData.email)
        assertEquals(linkData.linkType, storedLinkData.linkType)
    }

    @Test
    fun getTokenSuccessfully() {
        val linkData = LinkData(
            token = UUID.randomUUID(),
            email = "test@example.com",
            createdAt = Timestamp(System.currentTimeMillis()),
            user = null,
            team = null,
            linkType = LinkType.EMAIL_ADDRESS_VERIFICATION
        )

        linkTokensRepository.storeToken(linkData)

        val retrievedLinkData = linkTokensRepository.getToken(linkData.token)
        assertNotNull(retrievedLinkData)
        assertEquals(linkData.token, retrievedLinkData.token)
        assertEquals(linkData.email, retrievedLinkData.email)
        assertEquals(linkData.linkType, retrievedLinkData.linkType)
    }

    @Test
    fun getTokenReturnsNullForNonExistentToken() {
        val nonExistentToken = UUID.randomUUID()

        val retrievedLinkData = linkTokensRepository.getToken(nonExistentToken)
        assertNull(retrievedLinkData)
    }

    @Test
    fun deleteTokenSuccessfully() {
        val linkData = LinkData(
            token = UUID.randomUUID(),
            email = "test@example.com",
            createdAt = Timestamp(System.currentTimeMillis()),
            user = null,
            team = null,
            linkType = LinkType.PASSWORD_RESET
        )

        linkTokensRepository.storeToken(linkData)
        linkTokensRepository.deleteToken(linkData.token)

        val retrievedLinkData = linkTokensRepository.getToken(linkData.token)
        assertNull(retrievedLinkData)
    }

    @Test
    fun storeTokenFailsDueToNullEmail() {
        val linkData = LinkData(
            token = UUID.randomUUID(),
            email = null,
            createdAt = Timestamp(System.currentTimeMillis()),
            user = null,
            team = null,
            linkType = LinkType.PASSWORD_RESET
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            linkTokensRepository.storeToken(linkData)
        }

        assertEquals("Email cannot be null", exception.message)
    }
}