package com.mobihub.util.file

import com.mobihub.utils.file.*
import io.mockk.*
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Ignore
import kotlin.time.Duration
import kotlin.test.Test

/**
 * Tests for the [NextcloudChunkedUploader].
 *
 * @author Team-MobiHub
 */
class TestNextcloudFileHandler {
    private val filePathLarge = "src/test/resources/testfiles/testfile.zip"
    private val filePath = "src/test/resources/testfiles/smallTestFile.zip"
    private val targetPath = "trafficModels/00/01/tmFile.zip"
    private val baseUrl = "https://nextcloud.example.com"
    private val username = "TestUser"
    private val password = "TestPassword"

    @Test
    fun `test uploadFile with large valid file`() {
        // Mock the constructors of NextcloudChunkedUploader and NextcloudSimpleUploader
        mockkConstructor(NextcloudChunkedUploader::class)
        mockkConstructor(NextcloudSimpleUploader::class)

        // Define the behavior of the uploadFile method on the mocked instances
        every { anyConstructed<NextcloudChunkedUploader>().uploadFile(any(), any()) } returns Unit
        every { anyConstructed<NextcloudSimpleUploader>().uploadFile(any(), any()) } returns Unit

        // Create an instance of NextcloudFileHandler
        val fileHandler = NextcloudFileHandler(
            baseUrl = baseUrl, authenticator = BasicAuth(
                username = username,
                password = password,
            )
        )

        // Call the method to be tested
        val file = File(filePathLarge)
        println(file.length())
        fileHandler.uploadFile(file, targetPath)

        // Verify the interactions
        verify(exactly = 1) { anyConstructed<NextcloudChunkedUploader>().uploadFile(file, targetPath) }
        verify(exactly = 0) { anyConstructed<NextcloudSimpleUploader>().uploadFile(file, targetPath) }
    }

    @Test
    fun `test uploadFile with small valid file`() {
        // Mock the constructors of NextcloudChunkedUploader and NextcloudSimpleUploader
        mockkConstructor(NextcloudChunkedUploader::class)
        mockkConstructor(NextcloudSimpleUploader::class)

        // Define the behavior of the uploadFile method on the mocked instances
        every { anyConstructed<NextcloudChunkedUploader>().uploadFile(any(), any()) } returns Unit
        every { anyConstructed<NextcloudSimpleUploader>().uploadFile(any(), any()) } returns Unit

        // Create an instance of NextcloudFileHandler
        val fileHandler = NextcloudFileHandler(
            baseUrl = baseUrl, authenticator = BasicAuth(
                username = username,
                password = password,
            )
        )

        // Call the method to be tested
        val file = File(filePath)
        println(file.length())
        fileHandler.uploadFile(file, targetPath)

        // Verify the interactions
        verify(exactly = 0) { anyConstructed<NextcloudChunkedUploader>().uploadFile(file, targetPath) }
        verify(exactly = 1) { anyConstructed<NextcloudSimpleUploader>().uploadFile(file, targetPath) }
    }

    @Test
    fun `test downloadFile`() {
        mockkConstructor(NextcloudDownloader::class)

        every { anyConstructed<NextcloudDownloader>().downloadFile(any()) } returns File(filePath)

        val fileHandler = NextcloudFileHandler(
            baseUrl = baseUrl,
            authenticator = BasicAuth(username, password),
        )

        val resultFile = fileHandler.downloadFile(filePath)

        assertNotNull(resultFile)

        verify(exactly = 1) { anyConstructed<NextcloudDownloader>().downloadFile(filePath) }
    }

    @Test
    fun `test getDownloadReference`() {
        mockkConstructor(NextcloudDownloader::class)

        every { anyConstructed<NextcloudDownloader>().getDownloadReference(targetPath, false) } returns ShareLink("token", "MyFile.zip")

        val fileHandler = NextcloudFileHandler(
            baseUrl = baseUrl,
            authenticator = BasicAuth(username, password),
        )

        val resultFile = fileHandler.getDownloadReference(targetPath, false)

        assertNotNull(resultFile)

        verify(exactly = 1) { anyConstructed<NextcloudDownloader>().getDownloadReference(targetPath, false) }
    }

    @Test
    fun `test removeFile`() {
        mockkConstructor(NextcloudRemover::class)

        every { anyConstructed<NextcloudRemover>().removeFile(any()) } returns Unit

        val fileHandler = NextcloudFileHandler(
            baseUrl = baseUrl,
            authenticator = BasicAuth(username, password),
        )

        fileHandler.removeFile(targetPath)

        verify(exactly = 1) { anyConstructed<NextcloudRemover>().removeFile(targetPath) }
    }

    /**
     * Tests the upload of a file to the Nextcloud server and the retrieval of a download reference.
     */
    @Ignore
    @Test
    fun timeFileHandler() = runTest(timeout = Duration.INFINITE) {
        val fileHandler = NextcloudFileHandler(
            baseUrl = baseUrl,
            authenticator = BasicAuth(
                username = username,
                password = password,
            ),
        )

        val file = File(filePathLarge)
        fileHandler.uploadFile(file, targetPath)

        println(
            fileHandler.getDownloadReference(
                targetPath, shouldExpire = false
            ).getShareLink(
                baseUrl = baseUrl
            )
        )
    }
}
