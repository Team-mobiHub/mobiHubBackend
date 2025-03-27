package com.mobihub.util.file

import com.mobihub.utils.file.BasicAuth
import com.mobihub.utils.file.NextcloudRemover
import com.mobihub.utils.file.exceptions.UnexpectedHttpResponse
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Ignore

/**
 * Test the Nextcloud remover.
 *
 * @author Team-MobiHub
 */
class NextcloudRemoverTest {

    private val targetPath = "trafficModels/00/01/tmFile.zip"
    private val baseUrl = "https://nextcloud.example.com"
    private val username = "TestUser"
    val password = "TestPassword"

    @Test
    fun `test removeFile with valid file`() {
        val mockClient = mockk<HttpClient>()
        val mockResponse = mockk<HttpResponse<String>>()

        val slots = mutableListOf<HttpRequest>()

        every { mockClient.send(capture(slots), any<HttpResponse.BodyHandler<String>>()) } returns mockResponse
        every { mockResponse.statusCode() } returns 201
        every { mockResponse.body() } returns ""

        val remover = NextcloudRemover(
            baseUrl = baseUrl,
            authenticator = BasicAuth(
                username = username,
                password = password,
            ),
            client = mockClient,
        )

        remover.removeFile(targetPath)

        assert(slots.size == 1)

        assert(
            slots[0].uri()
                .toString() == "$baseUrl/remote.php/dav/files/$username/$targetPath" && slots[0].method() == "DELETE" && slots[0].headers()
                .firstValue("Authorization").orElse("").startsWith("Basic ")
        )
    }

    @Test
    fun `test removeFile with invalid file path`() {
        val mockClient = mockk<HttpClient>()
        val mockResponse = mockk<HttpResponse<String>>()

        val slots = mutableListOf<HttpRequest>()

        every { mockClient.send(capture(slots), any<HttpResponse.BodyHandler<String>>()) } returns mockResponse
        every { mockResponse.statusCode() } returns 404
        every { mockResponse.body() } returns "File not found"

        val remover = NextcloudRemover(
            baseUrl = baseUrl,
            authenticator = BasicAuth(
                username = username,
                password = password,
            ),
            client = mockClient,
        )

        assertDoesNotThrow { remover.removeFile(targetPath) }
    }

    @Test
    fun `test removeFile with invalid credentials`() {
        val mockClient = mockk<HttpClient>()
        val mockResponse = mockk<HttpResponse<String>>()

        val slots = mutableListOf<HttpRequest>()

        every { mockClient.send(capture(slots), any<HttpResponse.BodyHandler<String>>()) } returns mockResponse
        every { mockResponse.statusCode() } returns 401
        every { mockResponse.body() } returns "Unauthorized"

        val remover = NextcloudRemover(
            baseUrl = baseUrl,
            authenticator = BasicAuth(
                username = username,
                password = password,
            ),
            client = mockClient,
        )

        assertThrows<UnexpectedHttpResponse> { remover.removeFile(targetPath) }
    }

    /**
     * Tests the removal of a file from the Nextcloud server.
     */
    @Ignore
    @Test
    fun timeRemoveFile() = runTest {
        val fileHandler = NextcloudRemover(
            baseUrl = baseUrl,
            authenticator = BasicAuth(
                username = username,
                password = password,
            ),
            client = HttpClient.newBuilder().build(),
        )

        assertDoesNotThrow { fileHandler.removeFile(targetFilePath = targetPath) }
    }
}