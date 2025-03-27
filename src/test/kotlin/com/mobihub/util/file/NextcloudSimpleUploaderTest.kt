package com.mobihub.util.file

import com.mobihub.utils.file.BasicAuth
import com.mobihub.utils.file.NextcloudSimpleUploader
import com.mobihub.utils.file.exceptions.InvalidFileException
import com.mobihub.utils.file.exceptions.UnexpectedHttpResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Ignore
import kotlin.time.measureTime

/**
 * Test the Nextcloud simple uploader.
 *
 * @author Team-MobiHub
 */
class NextcloudSimpleUploaderTest {

    // The file is 10.2MB in size. Therefore, it is too large to be uploaded by the simple uploader.
    private val filePathTooLarge = "src/test/resources/testfiles/testfile.zip"
    private val filePath = "src/test/resources/testfiles/smallTestFile.zip"
    private val targetPath = "trafficModels/00/01/tmFile.zip"
    private val baseUrl = "https://nextcloud.example.com"
    private val username = "TestUser"
    val password = "TestPassword"

    @Test
    fun `test uploadFile with valid file`() {
        val mockClient = mockk<HttpClient>()
        val mockResponse = mockk<HttpResponse<String>>()
        val file = File(filePath)

        val slots = mutableListOf<HttpRequest>()

        every { mockClient.send(capture(slots), any<HttpResponse.BodyHandler<String>>()) } returns mockResponse
        every { mockResponse.statusCode() } returns 201
        every { mockResponse.body() } returns ""

        val uploader = NextcloudSimpleUploader(
            baseUrl = baseUrl, authenticator = BasicAuth(username, password), client = mockClient
        )

        uploader.uploadFile(file, targetPath)

        verify(exactly = 4) { mockClient.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<String>>()) }

        println("Request Size: ${slots.size}")

        for (request in slots) {
            println(request)
        }

        assert(
            slots[0].uri() == URI.create(
                "$baseUrl/remote.php/dav/files/$username/trafficModels"
            ) && slots[0].method() == "MKCOL" && slots[2].headers().firstValue("Authorization").orElse("")
                .startsWith("Basic ")
        )
        assert(
            slots[1].uri() == URI.create(
                "$baseUrl/remote.php/dav/files/$username/trafficModels/00"
            ) && slots[1].method() == "MKCOL" && slots[2].headers().firstValue("Authorization").orElse("")
                .startsWith("Basic ")
        )
        assert(
            slots[2].uri() == URI.create(
                "$baseUrl/remote.php/dav/files/$username/trafficModels/00/01"
            ) && slots[2].method() == "MKCOL" && slots[2].headers().firstValue("Authorization").orElse("")
                .startsWith("Basic ")
        )

        assert(
            slots[3].uri() == URI.create(
                "$baseUrl/remote.php/dav/files/$username/$targetPath"
            ) && slots[3].method() == "PUT" && slots[3].headers().firstValue("Authorization").orElse("")
                .startsWith("Basic ")
        )
    }

    @Test
    fun `test uploadFile with too large valid file`() {
        val mockClient = mockk<HttpClient>()
        val mockResponse = mockk<HttpResponse<String>>()
        val file = File(filePathTooLarge)

        val slots = mutableListOf<HttpRequest>()

        every { mockClient.send(capture(slots), any<HttpResponse.BodyHandler<String>>()) } returns mockResponse
        every { mockResponse.statusCode() } returns 201
        every { mockResponse.body() } returns ""

        val uploader = NextcloudSimpleUploader(
            baseUrl = baseUrl, authenticator = BasicAuth(username, password), client = mockClient
        )

        assertThrows<InvalidFileException> { uploader.uploadFile(file, targetPath) }



        verify(exactly = 0) { mockClient.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<String>>()) }
    }

    @Test
    fun `test uploadFile with unexpected http response`() {
        val mockClient = mockk<HttpClient>()
        val mockResponse = mockk<HttpResponse<String>>()
        val file = File(filePath)

        val slots = mutableListOf<HttpRequest>()

        every { mockClient.send(capture(slots), any<HttpResponse.BodyHandler<String>>()) } answers {
            if (firstArg<HttpRequest>().method() == "PUT") {
                mockResponse.apply { every { statusCode() } returns 404 }
            } else {
                mockResponse.apply { every { statusCode() } returns 201 }
            }
            mockResponse.apply {
                every { body() } returns ""
            }
        }

        val uploader = NextcloudSimpleUploader(
            baseUrl = baseUrl, authenticator = BasicAuth(username, password), client = mockClient
        )

        assertThrows<UnexpectedHttpResponse> { uploader.uploadFile(file, targetPath) }

        verify(exactly = 4) { mockClient.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<String>>()) }
    }

    /**
     * Test the upload of a file to the Nextcloud server.
     */
    @Ignore
    @Test
    fun timeUpload() = runTest {
        val time = measureTime {
            NextcloudSimpleUploader(
                baseUrl = baseUrl,
                authenticator = BasicAuth(
                    username = username,
                    password = password,
                ),
                client = HttpClient.newBuilder().build(),
            ).uploadFile(File(filePathTooLarge), targetPath)
        }
        println("Upload completed in $time")
    }
}