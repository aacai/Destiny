package zhiqiu.app.destiny.sharing

/**
 * 备份分享：把导出的备份包（zip 字节，含图片与可选加密）上传到 Litterbox 换回一个链接，
 * 接收方凭链接下载后导入。
 *
 * 这是纯逻辑层，不涉及 UI：
 * - 发送方：[share] 上传 zip → 得到链接 → 再生成二维码供扫码；
 * - 接收方：[fetch] 取回 zip 字节 → 交给 `ProfileRepository.importBackupBytes` 还原。
 *
 * @param client Litterbox 客户端（持有 Ktor 实例）
 */
class BackupSharing(private val client: FileIoClient) {

    /**
     * 上传备份包并返回分享链接。
     *
     * @param backupZip 已导出的备份 zip 字节（[zhiqiu.app.destiny.profile.ProfileRepository.exportBackupBytes] 产出）
     * @param expires 链接有效期，支持 `1h`/`12h`/`24h`/`72h`（默认 72 小时）
     * @return 分享链接
     */
    suspend fun share(backupZip: ByteArray, expires: String = "72h"): String =
        client.upload(
            fileName = BACKUP_FILE_NAME,
            bytes = backupZip,
            expires = expires,
        ).link

    /**
     * 按链接取回备份包字节。
     */
    suspend fun fetch(link: String): ByteArray = client.download(link)

    companion object {
        const val BACKUP_FILE_NAME = "destiny-backup.zip"
    }
}
