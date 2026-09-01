package zhiqiu.app.destiny.sharing

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Litterbox（catbox 临时文件托管）服务特性：
 * - 免注册、免 API key；
 * - 上传后返回直链，链接在指定时间内有效（默认 72 小时）；
 * - 到期后自动删除，不限制下载次数。
 */
private const val LITTERBOX_UPLOAD_URL = "https://litterbox.catbox.moe/resources/internals/api.php"

/** 上传接口返回的信封。 */
data class FileUploadResponse(
    val link: String,
)

/** 上传或下载失败。 */
class FileUploadException(message: String) : Exception(message)

/**
 * file.io 客户端。仅负责「上传得到链接」与「按链接取回字节」，不含业务与序列化逻辑。
 *
 * @param http 已配置好 [ContentNegotiation] 的 Ktor 客户端，由调用方注入（便于测试与复用连接池）
 */
class FileIoClient(private val http: HttpClient) {

    /**
     * 上传 [bytes]，返回分享链接。
     *
     * @param fileName 展示用的文件名（仅作元信息，不含路径）
     * @param expires 有效期，支持 Litterbox 的 `1h`/`12h`/`24h`/`72h`；其他值回退为 `72h`
     * @return 上传结果，[FileUploadResponse.link] 即分享链接
     */
    suspend fun upload(fileName: String, bytes: ByteArray, expires: String = "72h"): FileUploadResponse {
        val response = http.submitFormWithBinaryData(
            url = LITTERBOX_UPLOAD_URL,
            formData = formData {
                append("reqtype", "fileupload")
                append("time", expires.toLitterboxTime())
                append(
                    "fileToUpload",
                    bytes,
                    Headers.build {
                        append(HttpHeaders.ContentType, "application/octet-stream")
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"fileToUpload\"; filename=\"$fileName\"")
                    },
                )
            },
        )

        if (!response.status.isSuccess()) {
            throw FileUploadException(
                "Litterbox 返回 HTTP ${response.status.value}：${runCatching { response.bodyAsText() }.getOrDefault("")}",
            )
        }

        val link = response.bodyAsText().trim()
        if (link.isBlank()) {
            throw FileUploadException("上传响应缺少分享链接")
        }
        return FileUploadResponse(link = link)
    }

    /**
     * 按 [link] 取回文件内容。
     */
    suspend fun download(link: String): ByteArray {
        val response = http.get(link)
        if (response.status.isSuccess()) {
            return response.body<ByteArray>()
        }
        val message = runCatching { response.bodyAsText().take(200) }.getOrDefault("")
        throw FileUploadException("下载失败（HTTP ${response.status.value}）${message.ifBlank { "" }}")
    }
}

/** 创建供 [FileIoClient] 使用的 Ktor 客户端（也可供 Coil3 复用）。 */
fun createSharedHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true })
    }
}

/** 把任意有效期描述映射到 Litterbox 支持的档位。 */
private fun String.toLitterboxTime(): String = when (lowercase()) {
    "1h" -> "1h"
    "12h" -> "12h"
    "24h" -> "24h"
    "72h" -> "72h"
    else -> "72h"
}
