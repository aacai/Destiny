package zhiqiu.app.destiny.sharing

import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

/**
 * 临时文件服务的真实上传/下载闭环（需要联网）。
 *
 * 备份分享完全依赖这条链路：上传拿到直链 → 对方按直链下载还原 zip。
 */
class FileHostRoundTripTest {

    @Test
    fun uploadThenDownloadKeepsBytes() = runBlocking {
        val client = FileIoClient(createSharedHttpClient())
        val payload = Random.nextBytes(64 * 1024)

        val link = client.upload(BackupSharing.BACKUP_FILE_NAME, payload).link
        assertTrue(link.startsWith("https://"), "应返回 https 直链，实际=$link")

        val downloaded = client.download(link)
        assertContentEquals(payload, downloaded, "下载内容应与上传内容一致")
    }
}
