package zhiqiu.app.destiny.sharing

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * 临时文件服务客户端逻辑测试（用 MockEngine，不依赖真实网络）。
 * 覆盖：主服务纯文本直链、备用服务 JSON 直链解析、主服务失败自动降级、下载取回字节。
 */
class FileHostRoundTripTest {

    private fun mockClient(handler: (String) -> Pair<ByteArray, HttpStatusCode>) =
        HttpClient(
            MockEngine { request ->
                val (body, status) = handler(request.url.host)
                if (status == HttpStatusCode.OK) {
                    respond(body, status)
                } else {
                    respondError(status, body.decodeToString())
                }
            },
        )

    @Test
    fun x0atUploadReturnsPlainLink() = runBlocking {
        val client = FileIoClient(mockClient { host ->
            if (host.contains("x0.at")) "https://x0.at/AbCd.bin".encodeToByteArray() to HttpStatusCode.OK
            else ByteArray(0) to HttpStatusCode.InternalServerError
        })
        val link = client.upload("destiny-backup.zip", Random.nextBytes(32)).link
        assertEquals("https://x0.at/AbCd.bin", link)
    }

    @Test
    fun uguuUploadParsesJsonLink() = runBlocking {
        val json = """{"success":true,"files":[{"url":"https://d.uguu.se/XyZ.bin","size":32}]}"""
        val client = FileIoClient(mockClient { host ->
            if (host.contains("uguu")) json.encodeToByteArray() to HttpStatusCode.OK
            else ByteArray(0) to HttpStatusCode.InternalServerError
        })
        val link = client.upload("destiny-backup.zip", Random.nextBytes(32)).link
        assertEquals("https://d.uguu.se/XyZ.bin", link)
    }

    @Test
    fun fallsBackWhenPrimaryFails() = runBlocking {
        val json = """{"success":true,"files":[{"url":"https://d.uguu.se/XyZ.bin"}]}"""
        val client = FileIoClient(mockClient { host ->
            if (host.contains("x0.at")) ByteArray(0) to HttpStatusCode.Forbidden
            else json.encodeToByteArray() to HttpStatusCode.OK
        })
        val link = client.upload("destiny-backup.zip", Random.nextBytes(32)).link
        assertEquals("https://d.uguu.se/XyZ.bin", link)
    }

    @Test
    fun downloadReturnsBytes() = runBlocking {
        val payload = Random.nextBytes(128)
        val client = FileIoClient(mockClient { _ -> payload to HttpStatusCode.OK })
        assertContentEquals(payload, client.download("https://x0.at/AbCd.bin"))
    }
}
