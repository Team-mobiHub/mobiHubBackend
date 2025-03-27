package com.mobihub.utils.file

import com.mobihub.utils.file.exceptions.UnexpectedHttpResponse
import io.ktor.http.parsing.*
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.io.StringReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

private const val COULD_NOT_GET_DOWNLOAD_REFERENCE_ERROR = """
Could not get download reference
Response code was: %s
Response body: 
%s
"""

private const val COULD_NOT_DOWNLOAD_FILE_ERROR = """
Could not download file
Response code was: %s
Response body: 
%s
"""

private const val XML_PARSER_ERROR = "Could not parse XML response."

/**
 * JSON template for the request to get a download reference.
 *
 * Usage: GET_DOWNLOAD_REFERENCE_JSON.format(reference, expirationDate)
 */
private const val GET_DOWNLOAD_REFERENCE_JSON = """
{
    "path": "/%s",
    "shareType": 3,
    "permissions": 1
}
"""

/**
 * JSON template for the request to get a download reference with an expiration date.
 *
 * Usage: GET_DOWNLOAD_REFERENCE_JSON.format(reference, expirationDate)
 */
private const val GET_DOWNLOAD_REFERENCE_EXPIRE_JSON = """
{
    "path": "/%s",
    "shareType": 3,
    "permissions": 1,
    "expireDate": "%s"
}
"""

private const val GOT_DOWNLOAD_REFERENCE_INFO = "Got download reference for file %s"
private const val DOWNLOADED_FILE_INFO = "Downloaded file %s"

private const val CONTENT_TYPE_KEY = "Content-Type"
private const val CONTENT_TYPE_JSON_VALUE = "application/json"
private const val OCS_API_REQUEST_KEY = "OCS-APIRequest"
private const val OCS_API_REQUEST_VALUE = "true"
private const val OCS_SHARE_TEMPLATE = "%s/ocs/v2.php/apps/files_sharing/api/v1/shares"
private const val DOWNLOAD_TEMPLATE = "%s/remote.php/dav/files/%s/%s"

private const val DATE_FORMAT = "yyyy-MM-dd"
private const val XML_PATH_PROPERTY = "//path"
private const val XML_TOKEN_PROPERTY = "//token"
private const val XML_EXPIRATION_PROPERTY = "//expiration"

/**
 * The Nextcloud downloader. Downloads files from a Nextcloud instance.
 * Implements the [Downloader] interface.
 *
 * @property baseUrl The base URL of the Nextcloud instance.
 * @property authenticator The authenticator to use for authentication.
 * @property client The HTTP client to use for the requests.
 *
 * @author Team-MobiHub
 */
class NextcloudDownloader(
    val baseUrl: String, val authenticator: Authenticator, val client: HttpClient
) : Downloader {
    private val log = LoggerFactory.getLogger(NextcloudDownloader::class.java)

    override fun getDownloadReference(reference: String, shouldExpire: Boolean): ShareLink {
        val request = HttpRequest.newBuilder().uri(URI.create(OCS_SHARE_TEMPLATE.format(baseUrl))).POST(
            HttpRequest.BodyPublishers.ofString(
                if (shouldExpire) GET_DOWNLOAD_REFERENCE_EXPIRE_JSON.format(
                    reference, getExpirationDate()
                ) else GET_DOWNLOAD_REFERENCE_JSON.format(
                    reference
                )
            )
        ).header(CONTENT_TYPE_KEY, CONTENT_TYPE_JSON_VALUE).header(OCS_API_REQUEST_KEY, OCS_API_REQUEST_VALUE)
            .apply { authenticator.authenticate(this) }.build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        require(response.statusCode() in RESPONSE_SUCCESSFUL) {
            log.error(
                COULD_NOT_GET_DOWNLOAD_REFERENCE_ERROR.format(
                    response.statusCode(), response.body()
                )
            )
            throw UnexpectedHttpResponse(
                COULD_NOT_GET_DOWNLOAD_REFERENCE_ERROR.format(
                    response.statusCode(), response.body()
                )
            )
        }

        log.info(GOT_DOWNLOAD_REFERENCE_INFO.format(reference))

        return getShareLink(response.body())
    }

    override fun downloadFile(reference: String): java.io.File {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(DOWNLOAD_TEMPLATE.format(baseUrl, authenticator.getUsername(), reference))).GET()
            .apply { authenticator.authenticate(this) }.build()
        val response = client.send(
            request, HttpResponse.BodyHandlers.ofFile(Paths.get("file-${java.util.UUID.randomUUID()}.zip"))
        )
        require(response.statusCode() in RESPONSE_SUCCESSFUL) {
            log.error(
                COULD_NOT_DOWNLOAD_FILE_ERROR.format(
                    response.statusCode(), response.body()
                )
            )
            throw UnexpectedHttpResponse(
                COULD_NOT_DOWNLOAD_FILE_ERROR.format(
                    response.statusCode(), response.body()
                )
            )
        }
        log.info(DOWNLOADED_FILE_INFO.format(reference))

        return response.body().toFile()
    }

    /**
     * Parses the XML response from the Nextcloud server to get the share link.
     *
     * @param xml The XML response from the Nextcloud server.
     * @return The share link.
     */
    private fun getShareLink(xml: String): ShareLink {
        val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val inputSource = InputSource(StringReader(xml))
        val document: Document = documentBuilder.parse(inputSource)

        val xPath = XPathFactory.newInstance().newXPath()
        val expirationNodeList =
            xPath.evaluate(XML_EXPIRATION_PROPERTY, document, XPathConstants.NODESET) as org.w3c.dom.NodeList
        val tokenNodeList = xPath.evaluate(XML_TOKEN_PROPERTY, document, XPathConstants.NODESET) as org.w3c.dom.NodeList
        val pathNodeList = xPath.evaluate(XML_PATH_PROPERTY, document, XPathConstants.NODESET) as org.w3c.dom.NodeList

        require(expirationNodeList.length > 0 && tokenNodeList.length > 0 && pathNodeList.length > 0) {
            log.error(XML_PARSER_ERROR)
            throw ParseException(XML_PARSER_ERROR)
        }

        return ShareLink(
            shareToken = tokenNodeList.item(0).textContent,
            fileName = pathNodeList.item(0).textContent,
        )
    }

    /**
     * Gets the expiration date for the download reference. The expiration date is two days from the current date.
     *
     * @return The expiration date.
     */
    private fun getExpirationDate(): String {
        val currentDate = LocalDate.now()
        val futureDate = currentDate.plusDays(2)
        val formatter = DateTimeFormatter.ofPattern(DATE_FORMAT)
        return futureDate.format(formatter)
    }
}
