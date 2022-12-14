package be.zvz.kotlininside.http

import be.zvz.kotlininside.http.HttpRequest.HttpRequestException
import org.apache.tika.Tika
import org.apache.tika.mime.MimeTypes
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

class DefaultHttpClient
/**
 * DefaultHttpClient의 constructor입니다.
 *
 * @param enableGzipCompression     GZIP 압축 사용 유무
 * @param enableCache 캐시 사용 유무
 * @param proxy    Proxy 설정
 */
@JvmOverloads constructor(
    private val enableGzipCompression: Boolean = true,
    private val enableCache: Boolean = true,
    private val proxy: Proxy? = null
) : HttpInterface {
    private val tika = Tika()

    private fun useGzipEncoding(request: HttpRequest): HttpRequest {
        return if (enableGzipCompression) {
            request.acceptGzipEncoding().uncompress(true)
        } else request
    }

    private fun useProxy(request: HttpRequest): HttpRequest {
        if (proxy != null) {
            request.useProxy(proxy.ip, proxy.port)
        }
        return request
    }

    private fun useCache(request: HttpRequest): HttpRequest {
        if (enableCache) {
            request.useCaches(true)
        }
        return request
    }

    private fun urlEncode(str: String): String {
        return try {
            URLEncoder.encode(str, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            str
        }
    }

    private fun optionToUrl(url: String, option: HttpInterface.Option?): String {
        if (option == null) return url
        var replacedUrl = url
        option.pathParameter.forEach { (key, value) ->
            replacedUrl = replacedUrl.replace("{$key}", urlEncode(value))
        }
        var firstKey = false

        return StringBuilder(replacedUrl).apply {
            option.queryParameter.forEach { (key, value) ->
                if (!firstKey) {
                    firstKey = true
                    append("?")
                } else {
                    append("&")
                }.append(urlEncode(key)).append("=").append(urlEncode(value))
            }
        }.toString()
    }

    private fun setPostRequestHeaders(option: HttpInterface.Option, request: HttpRequest, bodyData: StringBuilder) {
        request.headers(option.headers)
        if (option.userAgent != null) request.header("User-Agent", option.userAgent)
        if (option.body != null && option.contentType != null) {
            request.header("Content-Type", option.contentType)
            bodyData.append(option.body)
        } else {
            request.header("Content-Type", "application/x-www-form-urlencoded")
            var firstKey = false
            option.bodyParameter.forEach { (key, value) ->
                if (!firstKey) {
                    firstKey = true
                    bodyData
                } else {
                    bodyData.append("&")
                }.append(urlEncode(key)).append("=").append(urlEncode(value))
            }
        }
    }

    @Throws(HttpException::class)
    override fun get(url: String, option: HttpInterface.Option?): String? {
        val request = useCache(
            useGzipEncoding(
                useProxy(
                    HttpRequest.get(optionToUrl(url, option))
                )
            )
        )
            .acceptJson()
            .followRedirects(true)
        if (option != null) {
            request.headers(option.headers)
            if (option.userAgent != null) request.header("User-Agent", option.userAgent)
        }
        return try {
            // HTTP API로 변경될 경우 필요한 코드
            request.body()
        } catch (e: HttpRequestException) {
            throw HttpException(request.code(), e.message)
        }
    }

    @Throws(HttpException::class)
    override fun post(url: String, option: HttpInterface.Option?): String? {
        val request = useCache(
            useGzipEncoding(
                useProxy(
                    HttpRequest.post(optionToUrl(url, option))
                )
            )
        )
            .acceptJson()
            .followRedirects(true)
        val bodyData = StringBuilder()
        option?.let { setPostRequestHeaders(it, request, bodyData) }
        return try {
            request
                .send(bodyData.toString())
                .body()
        } catch (e: HttpRequestException) {
            throw HttpException(request.code(), e.message)
        }
    }

    @Throws(HttpException::class)
    override fun delete(url: String, option: HttpInterface.Option?): String? {
        throw UnsupportedOperationException("Not implemented yet")
    }

    @Throws(HttpException::class)
    override fun head(url: String, option: HttpInterface.Option?): String? {
        throw UnsupportedOperationException("Not implemented yet")
    }

    @Throws(HttpException::class)
    override fun put(url: String, option: HttpInterface.Option?): String? {
        throw UnsupportedOperationException("Not implemented yet")
    }

    @Throws(HttpException::class)
    override fun patch(url: String, option: HttpInterface.Option?): String? {
        throw UnsupportedOperationException("Not implemented yet")
    }

    private fun addToPart(request: HttpRequest, key: String, fileInfo: HttpInterface.Option.FileInfo, infix: Int) {
        if (fileInfo.mimeType == null) {
            val (contentType, fileName) = getFileInfoFromStream(fileInfo.stream, infix)
            request.part(key, fileName, contentType, fileInfo.stream)
        } else {
            request.part(
                key,
                "image$infix${fromMimeTypeToExtension(fileInfo.mimeType)}",
                fileInfo.mimeType,
                fileInfo.stream
            )
        }
    }

    @Throws(HttpException::class)
    override fun upload(url: String, option: HttpInterface.Option?): String? {
        val request = useCache(
            useGzipEncoding(
                useProxy(
                    HttpRequest.post(optionToUrl(url, option))
                        .setMultipartFormDataBoundary(HttpRequest.generateBoundary())
                )
            )
        )
            .acceptJson()
            .followRedirects(true)
        val bodyData = StringBuilder()
        if (option != null) {
            setPostRequestHeaders(option, request, bodyData)
            option.multipartParameter.forEach(request::part)
            var count = 0
            option.multipartFile.forEach { (key, fileInfo) ->
                addToPart(request, key, fileInfo, count)
                count++
            }
            count = 0
            option.multipartFileList.forEach { (key, value) ->
                value.forEach { fileInfo ->
                    addToPart(request, key, fileInfo, count)
                    count++
                }
            }
        }
        return try {
            request
                .send(bodyData.toString())
                .body()
        } catch (e: HttpRequestException) {
            throw HttpException(request.code(), e.message)
        }
    }

    private data class FileInfo(
        val contentType: String,
        val fileName: String
    )

    private fun fromMimeTypeToExtension(contentType: String?): String =
        MimeTypes.getDefaultMimeTypes().forName(contentType ?: DEFAULT_MIME_TYPE).extension
    private fun getFileInfoFromStream(inputStream: InputStream, infix: Int): FileInfo {
        val contentType = try {
            tika.detect(inputStream)
        } catch (_: IOException) {
            DEFAULT_MIME_TYPE
        }
        return FileInfo(
            contentType,
            "image$infix${fromMimeTypeToExtension(contentType)}"
        )
    }

    class Proxy internal constructor(var ip: String, var port: Int)

    companion object {
        const val DEFAULT_MIME_TYPE = "video/mp4"
    }
}
