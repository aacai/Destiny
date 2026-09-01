package zhiqiu.app.destiny.sharing

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.openZip

/**
 * 备份包内的目录布局约定。
 *
 * 图片按「案例 id → 模块」两级目录组织，避免所有图片平铺在一个目录里：
 * ```
 * backup.json
 * images/<案例id>/bazi/20260901-143022.jpg      # 八字模块配图
 * images/<案例id>/ziwei/20260901-143055.jpg     # 紫微模块配图
 * ```
 * 该相对路径在 zip 包内与应用本地图片根目录下保持一致，便于人工对照查找。
 */
object BackupLayout {
    const val BACKUP_JSON = "backup.json"
    const val IMAGES_ROOT = "images"

    /**
     * 图片的规范相对路径：`images/<profileId>/<category>/<fileName>`。
     *
     * @param profileId 案例 id
     * @param category 模块目录名（如 `bazi` 八字、`ziwei` 紫微、`qizheng` 七政）
     * @param fileName 文件名，建议使用可读时间戳，便于人工查找
     */
    fun imagePath(profileId: String, category: String, fileName: String): String =
        "$IMAGES_ROOT/$profileId/$category/$fileName"
}

/** 解包后的备份内容。 */
data class UnpackedBackup(
    val backupJson: String,
    /** 相对路径 → 图片字节，路径形如 `images/<案例id>/<模块>/xxx.jpg` */
    val images: Map<String, ByteArray>,
)

/**
 * 备份包（zip）的打包与解包。结构为 `backup.json` + `images/...`，单文件即可分享/上传。
 *
 * @param fs 文件系统，默认 [FileSystem.SYSTEM]；测试可注入 [okio.fakefilesystem.FakeFileSystem]
 */
class BackupBundle(private val fs: FileSystem = FileSystem.SYSTEM) {

    /**
     * 打包为 zip。
     *
     * @param zipPath 输出的 zip 文件路径，已存在则覆盖
     * @param backupJson 备份数据（明文或加密信封）
     * @param images 相对路径 → 图片字节，键应来自 [BackupLayout.imagePath]
     */
    fun pack(zipPath: Path, backupJson: String, images: Map<String, ByteArray>) {
        if (fs.exists(zipPath)) fs.delete(zipPath)
        val entries = buildList {
            add(BackupLayout.BACKUP_JSON to backupJson.encodeToByteArray())
            for ((relativePath, bytes) in images) add(relativePath to bytes)
        }
        writeBytes(fs, zipPath, ZipWriter.create(entries))
    }

    /**
     * 解包 zip。
     *
     * @throws okio.IOException zip 损坏，或缺少 [BackupLayout.BACKUP_JSON]
     */
    fun unpack(zipPath: Path): UnpackedBackup {
        val zip = fs.openZip(zipPath)
        return try {
            val json = readText(zip, BackupLayout.BACKUP_JSON.toPath())
            val images = mutableMapOf<String, ByteArray>()
            val imagesRoot = BackupLayout.IMAGES_ROOT.toPath()
            if (zip.exists(imagesRoot)) collectImages(zip, imagesRoot, images)
            UnpackedBackup(json, images)
        } finally {
            zip.close()
        }
    }

    /** 递归收集 [dir] 下的图片文件（相对路径 → 字节）。 */
    private fun collectImages(zip: FileSystem, dir: Path, out: MutableMap<String, ByteArray>) {
        for (entry in zip.list(dir)) {
            if (zip.metadata(entry).isDirectory) {
                collectImages(zip, entry, out)
            } else {
                // Okio 报告的 zip 内路径带前导 '/'，统一成打包时的相对路径形式
                out[entry.toString().removePrefix("/")] = readBytes(zip, entry)
            }
        }
    }

    private fun writeText(fs: FileSystem, path: Path, text: String) {
        val sink = fs.sink(path).buffer()
        try {
            sink.writeUtf8(text)
        } finally {
            sink.close() // 会先 flush 再关闭底层 sink
        }
    }

    private fun writeBytes(fs: FileSystem, path: Path, bytes: ByteArray) {
        val sink = fs.sink(path).buffer()
        try {
            sink.write(bytes, 0, bytes.size)
        } finally {
            sink.close()
        }
    }

    private fun readText(fs: FileSystem, path: Path): String {
        val source = fs.source(path).buffer()
        return try {
            source.readUtf8()
        } finally {
            source.close()
        }
    }

    private fun readBytes(fs: FileSystem, path: Path): ByteArray {
        val source = fs.source(path).buffer()
        return try {
            source.readByteArray()
        } finally {
            source.close()
        }
    }
}
