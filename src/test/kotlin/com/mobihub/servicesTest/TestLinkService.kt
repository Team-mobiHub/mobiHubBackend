package com.mobihub.servicesTest

import com.mobihub.exceptions.DataWithIdentifierNotFoundException
import com.mobihub.model.LinkData
import com.mobihub.model.LinkType
import com.mobihub.repositories.LinkTokensRepository
import com.mobihub.services.LinkService
import io.ktor.server.config.*
import io.ktor.server.engine.*
import org.mockito.kotlin.*
import java.sql.Timestamp
import java.time.Instant
import java.util.*
import kotlin.test.Test
import kotlin.test.BeforeTest
import kotlin.test.*

/**
 * Test class for the LinkService.
 * This class contains unit tests to verify the functionality of the LinkService methods.
 */
class TestLinkService {

    /**
     * Repository for link tokens, used to interact with the database.
     */
    private lateinit var linkRepo: LinkTokensRepository

    /**
     * Service under test, responsible for generating links.
     */
    private lateinit var linkService: LinkService

    /**
     * Type of link to be generated.
     */
    private lateinit var linkType: LinkType;
    private var linkTokenRepository: LinkTokensRepository = mock()

    /**
     * Setup method executed before each test case.
     * Initializes some mocks and the service.
     */
    @BeforeTest
    fun setup() {

        val testConfig = MapApplicationConfig(
            "nextcloud.baseUrl" to "http://ifv-mobihub.ifv.kit.edu:8443",
            "frontend.baseUrl" to "http://localhost:8080"
        )

        val testEnvironment = applicationEnvironment {
            config = testConfig
        }

        linkType = mock<LinkType>()
        linkService = LinkService(linkTokenRepository, testEnvironment)
    }

    /**
     * Test case to verify the successful creation of a password reset link.
     * Ensures that the generated link starts with the expected base URL.
     */
    @Test
    fun createResetPasswordLinkSuccessfully() {

        val email = "foo@foo.de"
        val linkWithToken = linkService.createLink(null, null, email, LinkType.PASSWORD_RESET);

        val wishedResult = "http://localhost:8080/user/resetpassword"

        println(linkWithToken)
        assertTrue(linkWithToken.startsWith(wishedResult))
    }

    /**
     * Test case to verify the successful creation of an email verification link.
     * Ensures that the generated link starts with the expected base URL.
     */
    @Test
    fun createVerifyEmailLinkSuccessfully() {
        val email = "foo@foo.de"

        val linkWithToken = linkService.createLink(null, null, email, LinkType.EMAIL_ADDRESS_VERIFICATION)

        val wishedResult = "http://localhost:8080/auth/verify-email"

        assertTrue(linkWithToken.startsWith(wishedResult))
    }

    @Test
    fun getLinkDataSuccessully() {
        val email = "foo@foo.de"
        val linkWithToken = linkService.createLink(null, null, email, LinkType.PASSWORD_RESET);
        val token = "/([a-z0-9-]*)$".toRegex().find(linkWithToken)!!.groupValues[1]

        whenever(linkTokenRepository.getToken(UUID.fromString(token))).thenReturn(
            LinkData(
                UUID.fromString(token),
                email,
                Timestamp.from(Instant.now()),
                null,
                null,
                LinkType.PASSWORD_RESET
            )
        )
        val linkData = linkService.getLinkData(UUID.fromString(token))

        assertEquals(linkData.token, UUID.fromString(token))

        assertFailsWith<DataWithIdentifierNotFoundException> {
            linkService.getLinkData(UUID.randomUUID())
        }
    }

    @Test
    fun deleteLinkDataSuccessfully() {
        val email = "foo@foo.de"
        val linkWithToken = linkService.createLink(null, null, email, LinkType.PASSWORD_RESET);
        val token = "/([a-z0-9-]*)$".toRegex().find(linkWithToken)!!.groupValues[1]

        doNothing().whenever(linkTokenRepository).deleteToken(UUID.fromString(token))
        assertEquals(linkService.deleteLink(UUID.fromString(token)), Unit)
    }
}
