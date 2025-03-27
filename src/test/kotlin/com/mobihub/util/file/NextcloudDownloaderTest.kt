package com.mobihub.util.file

import com.mobihub.utils.file.BasicAuth
import com.mobihub.utils.file.NextcloudDownloader
import com.mobihub.utils.file.exceptions.UnexpectedHttpResponse
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.net.http.HttpClient
import kotlin.test.Ignore
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.time.measureTime
import io.mockk.*
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.util.concurrent.Flow
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

/**
 * Test the Nextcloud downloader.
 *
 * @author Team-MobiHub
 */
class NextcloudDownloaderTest {

    private val targetPath = "user/file.zip"
    private val baseUrl = "https://nextcloud.example.com"
    private val username = "TestUser"
    val password = "TestPassword"

    @Test
    fun mockGetDownloadReference() {
        val request1 = mockGetDownloadReference(false)
        println(request1[0])

        assert(
            request1[0].uri().toString() == "${baseUrl}/ocs/v2.php/apps/files_sharing/api/v1/shares"
        )

        assert(request1[0].method() == "POST")
        assert(request1[0].headers().firstValue("Content-Type").orElse("") == "application/json")
        assert(request1[0].headers().firstValue("OCS-APIRequest").orElse("") == "true")
        assert(request1[0].headers().firstValue("Authorization").orElse("").startsWith("Basic "))
        assert(request1[0].bodyPublisher().get().contentLength() > 0)
        assert(
            extractBody(request1[0].bodyPublisher().get()).trimIndent() == """
                        {
                            "path": "/$targetPath",
                            "shareType": 3,
                            "permissions": 1
                        }
                    """.trimIndent()
        )

        val request2 = mockGetDownloadReference(true)
        assert(
            request2[0].uri().toString() == "${baseUrl}/ocs/v2.php/apps/files_sharing/api/v1/shares"
        )

        assert(request2[0].method() == "POST")
        assert(request2[0].headers().firstValue("Content-Type").orElse("") == "application/json")
        assert(request2[0].headers().firstValue("OCS-APIRequest").orElse("") == "true")
        assert(request2[0].headers().firstValue("Authorization").orElse("").startsWith("Basic "))
        assert(request2[0].bodyPublisher().get().contentLength() > 0)
        assert(
            extractBody(request2[0].bodyPublisher().get()).trimIndent().matches(
                """
                        \{
                         {4}"path": "/$targetPath",
                         {4}"shareType": 3,
                         {4}"permissions": 1,
                         {4}"expireDate": "\d{4}-\d{2}-\d{2}"
                        }""".trimIndent().toRegex()
            )
        )
    }


    private fun mockGetDownloadReference(shouldExpire: Boolean): List<HttpRequest> {
        val mockClient = mockk<HttpClient>()
        val mockResponse = mockk<HttpResponse<String>>()

        every { mockResponse.body() } returns """
            <?xml version="1.0"?>
            <ocs>
             <meta>
              <status>ok</status>
              <statuscode>200</statuscode>
              <message>OK</message>
             </meta>
             <data>
              <id>1</id>
              <share_type>3</share_type>
              <uid_owner>owner123</uid_owner>
              <displayname_owner>veryCoolName</displayname_owner>
              <permissions>17</permissions>
              <can_edit>1</can_edit>
              <can_delete>1</can_delete>
              <stime>1735299203</stime>
              <parent/>
              <expiration>2025-05-01 00: 00: 00</expiration>
              <token>xXGaHeGNmSQkCtn</token>
              <uid_file_owner>owner123</uid_file_owner>
              <note></note>
              <label></label>
              <displayname_file_owner>veryCoolName</displayname_file_owner>
              <path>${targetPath}</path>
              <item_type>file</item_type>
              <item_permissions>27</item_permissions>
              <is-mount-root></is-mount-root>
              <mount-type></mount-type>
              <mimetype>text/markdown</mimetype>
              <has_preview>1</has_preview>
              <storage_id>home: :ncp</storage_id>
              <storage>1</storage>
              <item_source>52</item_source>
              <file_source>52</file_source>
              <file_parent>50</file_parent>
              <file_target>/Readme.md</file_target>
              <item_size>136</item_size>
              <item_mtime>1734889833</item_mtime>
              <share_with/>
              <share_with_displayname>(Shared link)</share_with_displayname>
              <password/>
              <send_password_by_talk></send_password_by_talk>
              <url>${baseUrl}/index.php/s/xXGaHeGNmSQkCtn</url>
              <mail_send>1</mail_send>
              <hide_download>0</hide_download>
              <attributes/>
             </data>
            </ocs>
        """.trimIndent()

        val slots = mutableListOf<HttpRequest>()

        every { mockResponse.statusCode() } returns 200
        every { mockClient.send(capture(slots), any<HttpResponse.BodyHandler<String>>()) } returns mockResponse

        val nextcloudDownloader = NextcloudDownloader(
            baseUrl = baseUrl,
            authenticator = BasicAuth(
                username = username,
                password = password,
            ),
            client = mockClient,
        )

        val result = nextcloudDownloader.getDownloadReference(targetPath, shouldExpire)

        assertNotNull(result)
        assertEquals(
            "${baseUrl}/s/xXGaHeGNmSQkCtn/download/${targetPath.substringAfterLast("/")}", result.getShareLink(baseUrl)
        )

        verify(exactly = 1) { mockClient.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<String>>()) }

        confirmVerified(mockClient)

        return slots
    }

    private fun extractBody(bodyPublisher: HttpRequest.BodyPublisher): String {
        val byteArrayOutputStream = java.io.ByteArrayOutputStream()
        object : Flow.Subscription {
            override fun request(n: Long) {}
            override fun cancel() {}
        }
        val subscriber = object : Flow.Subscriber<ByteBuffer> {
            override fun onSubscribe(s: Flow.Subscription) {
                s.request(Long.MAX_VALUE)
            }

            override fun onNext(item: ByteBuffer) {
                byteArrayOutputStream.write(item.array(), item.arrayOffset(), item.remaining())
            }

            override fun onError(t: Throwable) {
                throw RuntimeException(t)
            }

            override fun onComplete() {}
        }
        bodyPublisher.subscribe(subscriber)
        return byteArrayOutputStream.toString(StandardCharsets.UTF_8)
    }

    @Test
    fun mockDownloadFile() {
        val mockClient = mockk<HttpClient>()
        val mockResponse = mockk<HttpResponse<Path>>()
        val mockPath = mockk<Path>()

        every { mockResponse.body() } returns mockPath
        every { mockResponse.statusCode() } returns 200
        every { mockClient.send(any(), any<HttpResponse.BodyHandler<Path>>()) } returns mockResponse
        every { mockPath.toFile() } returns File("mocked-file.zip")

        val nextcloudDownloader = NextcloudDownloader(
            baseUrl = baseUrl,
            authenticator = BasicAuth(
                username = username,
                password = password,
            ),
            client = mockClient,
        )

        val resultFile = nextcloudDownloader.downloadFile(targetPath)

        assertNotNull(resultFile)
        assertEquals("mocked-file.zip", resultFile.name)

        verify {
            mockClient.send(match {
                it.uri()
                    .toString() == "${baseUrl}/remote.php/dav/files/${username}/${targetPath}" && it.method() == "GET" && it.headers()
                    .firstValue("Authorization").orElse("").startsWith("Basic ")
            }, any<HttpResponse.BodyHandler<Path>>())
        }
        confirmVerified(mockClient)
    }

    @Test
    fun `test getDownloadReference with unexpected response`() {
        val mockClient = mockk<HttpClient>()
        val mockResponse = mockk<HttpResponse<String>>()
        val filePath = "user/file.zip"

        val slots = mutableListOf<HttpRequest>()

        every { mockClient.send(capture(slots), any<HttpResponse.BodyHandler<String>>()) } returns mockResponse
        every { mockResponse.statusCode() } returns 500
        every { mockResponse.body() } returns "Internal Server Error"

        val downloader = NextcloudDownloader(
            baseUrl = baseUrl, authenticator = BasicAuth(username, password), client = mockClient
        )

        assertThrows<UnexpectedHttpResponse> { downloader.getDownloadReference(filePath, false) }

        verify(exactly = 1) { mockClient.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<String>>()) }
    }

    @Test
    fun `test downloadFile with unexpected response`() {
        val mockClient = mockk<HttpClient>()
        val mockResponse = mockk<HttpResponse<Path>>()
        val filePath = "user/file.zip"

        val slots = mutableListOf<HttpRequest>()

        every { mockClient.send(capture(slots), any<HttpResponse.BodyHandler<Path>>()) } returns mockResponse
        every { mockResponse.statusCode() } returns 404
        every { mockResponse.body() } returns mockk()

        val downloader = NextcloudDownloader(
            baseUrl = baseUrl, authenticator = BasicAuth(username, password), client = mockClient
        )

        assertThrows<UnexpectedHttpResponse> { downloader.downloadFile(filePath) }

        verify(exactly = 1) { mockClient.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<Path>>()) }
    }

    /**
     * Tests the retrieval of a download reference for a file on the Nextcloud server.
     */
    @Ignore
    @Test
    fun timeGetDownloadReference() = runTest {
        val time = measureTime {
            val nextcloudChunkedUploader = NextcloudDownloader(
                baseUrl = baseUrl,
                authenticator = BasicAuth(
                    username = username,
                    password = password,
                ),
                client = HttpClient.newBuilder().build(),
            )
            val result = nextcloudChunkedUploader.getDownloadReference(
                targetPath, shouldExpire = false
            )

            assertNotNull(result)

            assertTrue(
                result.getShareLink(
                    baseUrl = baseUrl
                ).matches(Regex("${baseUrl}/s/.*"))
            )

            println(
                "Download reference: ${
                    result.getShareLink(
                        baseUrl = baseUrl
                    )
                }"
            )
        }
        println("Got Download reference in $time")
    }

    /**
     * Tests the download of a file from the Nextcloud server.
     */
    @Ignore
    @Test
    fun timeDownloadFile() = runTest {
        val time = measureTime {
            val nextcloudDownloader = NextcloudDownloader(
                baseUrl = baseUrl,
                authenticator = BasicAuth(
                    username = username,
                    password = password,
                ),
                client = HttpClient.newBuilder().build(),
            )

            val file = nextcloudDownloader.downloadFile(targetPath)

            println(file.absolutePath)

            assertTrue { file.exists() && file.isFile && file.canRead() && file.length() > 0 }
        }
        println("Downloaded file in $time")
    }
}