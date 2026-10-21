package zhiqiu.app.destiny.sharing

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 临时文件托管服务（均免注册、公开 API、国内网络可直连）：
 *
 * - 主服务 [X0AtHost]（x0.at）：`POST https://x0.at/`，multipart 字段 `file`，
 *   响应体是纯文本直链；单文件上限 1 GiB，文件保留 3~100 天。
 * - 备用服务 [UgUUHost]（uguu.se）：`POST https://uguu.se/upload.php`，multipart 字段 `files[]`，
 *   响应体是 JSON 直链；单文件上限 128 MiB，文件 3 小时后删除。
 *
 * 两者都只给出直链，下载时 GET 取原始字节即可，与具体是哪一家无关。
 */
private val FILE_HOSTS = listOf<FileHost>(X0AtHost, UgUUHost)

/** 上传接口返回的信封。 */
data class FileUploadResponse(
    val link: String,
)

/** 上传或下载失败。 */
class FileUploadException(message: String) : Exception(message)

/**
 * 临时文件服务客户端。仅负责「上传得到链接」与「按链接取回字节」，不含业务与序列化逻辑。
 *
 * @param http 已配置好 [ContentNegotiation] 的 Ktor 客户端，由调用方注入（便于测试与复用连接池）
 */
class FileIoClient(private val http: HttpClient) {

    /**
     * 上传 [bytes]，返回分享链接。
     *
     * 依次尝试各托管服务，第一个成功的结果即返回；全部失败时抛出 [FileUploadException]。
     *
     * @param fileName 展示用的文件名（仅作元信息，不含路径）
     * @return 上传结果，[FileUploadResponse.link] 即分享链接
     */
    suspend fun upload(fileName: String, bytes: ByteArray): FileUploadResponse {
        var lastError: Exception? = null
        for (host in FILE_HOSTS) {
            try {
                return FileUploadResponse(link = host.upload(http, fileName, bytes))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw FileUploadException("上传失败：${lastError?.message ?: "没有可用的文件服务"}")
    }

    /**
     * 按 [link] 取回文件内容。
     *
     * 国内网络偶尔会中途断流，因此校验 `Content-Length` 并在失败时重试一次。
     */
    suspend fun download(link: String): ByteArray {
        var lastError: Exception? = null
        repeat(2) {
            try {
                val response = http.get(link)
                val bytes = response.body<ByteArray>()
                if (!response.status.isSuccess()) {
                    throw FileUploadException("下载失败（HTTP ${response.status.value}）")
                }
                val expected = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
                if (expected != null && expected > 0 && bytes.size.toLong() != expected) {
                    throw FileUploadException("下载不完整（${bytes.size}/$expected 字节）")
                }
                return bytes
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw FileUploadException("下载失败：${lastError?.message ?: "未知错误"}")
    }
}

/** x0.at：主服务，保留 3~100 天。 */
private object X0AtHost : FileHost {
    override val label: String = "x0.at"
    private const val UPLOAD_URL = "https://x0.at/"

    override suspend fun upload(http: HttpClient, fileName: String, bytes: ByteArray): String {
        val response = http.submitFormWithBinaryData(
            url = UPLOAD_URL,
            formData = formData {
                append(
                    "file",
                    bytes,
                    Headers.build {
                        append(HttpHeaders.ContentType, "application/octet-stream")
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$fileName\"")
                    },
                )
                append("keep_name", "1")
            },
        )
        val text = response.bodyAsText().trim()
        if (!response.status.isSuccess()) {
            throw FileUploadException("$label 返回 HTTP ${response.status.value}：${text.take(200)}")
        }
        if (!text.startsWith("http")) {
            throw FileUploadException("$label 响应异常：${text.take(200)}")
        }
        return text
    }
}

/** uguu.se：备用服务，3 小时后删除。 */
private object UgUUHost : FileHost {
    override val label: String = "uguu.se"
    private const val UPLOAD_URL = "https://uguu.se/upload.php"

    override suspend fun upload(http: HttpClient, fileName: String, bytes: ByteArray): String {
        val response = http.submitFormWithBinaryData(
            url = UPLOAD_URL,
            formData = formData {
                append(
                    "files[]",
                    bytes,
                    Headers.build {
                        append(HttpHeaders.ContentType, "application/octet-stream")
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"files[]\"; filename=\"$fileName\"")
                    },
                )
            },
        )
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw FileUploadException("$label 返回 HTTP ${response.status.value}：${text.take(200)}")
        }
        val payload = FILE_HOST_JSON.decodeFromString<UgUUResponse>(text)
        val link = payload.files.firstOrNull()?.url.orEmpty()
        if (!link.startsWith("http")) {
            throw FileUploadException("$label 响应缺少直链：${text.take(200)}")
        }
        return link
    }
}

/** 临时文件服务的上传接口约定。 */
private interface FileHost {
    val label: String
    suspend fun upload(http: HttpClient, fileName: String, bytes: ByteArray): String
}

@Serializable
private data class UgUUResponse(
    val success: Boolean = false,
    val files: List<UgUUFile> = emptyList(),
)

@Serializable
private data class UgUUFile(
    val url: String = "",
)

private val FILE_HOST_JSON = Json { ignoreUnknownKeys = true; isLenient = true }

/** 创建供 [FileIoClient] 使用的 Ktor 客户端。 */
fun createSharedHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 300_000
        connectTimeoutMillis = 30_000
        socketTimeoutMillis = 120_000
    }
}
