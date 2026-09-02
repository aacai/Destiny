package zhiqiu.app.destiny.sharing

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.random.Random

/**
 * 命例批注图片的本地磁盘存储。
 *
 * 文件按 `images/<案例id>/<模块>/<文件名>` 落盘（与备份包、数据库中的相对路径完全一致），
 * 因此导入导出时路径可直接对应，无需转换。
 *
 * @param imagesRoot 图片根目录（绝对路径）；实际文件落在 `<imagesRoot>/<案例id>/<模块>/<文件名>`
 * @param fs 文件系统，默认 [FileSystem.SYSTEM]；测试可注入 [okio.fakefilesystem.FakeFileSystem]
 */
class ImageStorage(
    private val imagesRoot: String,
    private val fs: FileSystem = FileSystem.SYSTEM,
) {
    /** 图片根目录（绝对路径），供上层拼接临时文件路径等 */
    val root: String get() = imagesRoot

    /** 保存图片字节，返回规范相对路径 `images/<案例id>/<模块>/<文件名>` */
    fun save(profileId: String, category: String, bytes: ByteArray, extension: String): String {
        val name = "${randomName()}${normalizeExt(extension)}"
        val relativePath = BackupLayout.imagePath(profileId, category, name)
        val file = "$imagesRoot/$profileId/$category/$name".toPath()
        fs.createDirectories(file.parent!!)
        fs.write(file) { write(bytes, 0, bytes.size) }
        return relativePath
    }

    /** 以指定相对路径写入（导入备份时使用，路径来自备份包内部） */
    fun writeAt(relativePath: String, bytes: ByteArray) {
        val file = resolve(relativePath)
        fs.createDirectories(file.parent!!)
        fs.write(file) { write(bytes, 0, bytes.size) }
    }

    /** 读取图片字节；文件不存在返回 null */
    fun read(relativePath: String): ByteArray? =
        runCatching { fs.read(resolve(relativePath)) { readByteArray() } }.getOrNull()

    /** 图片本地绝对路径，供 Coil 等直接加载 */
    fun absolutePath(relativePath: String): String = resolve(relativePath).toString()

    fun exists(relativePath: String): Boolean = fs.exists(resolve(relativePath))

    /** 删除单张图片（同时调用方负责删库记录） */
    fun delete(relativePath: String) {
        runCatching { fs.delete(resolve(relativePath)) }
    }

    /** 删除某案例的全部图片目录（案例被删时调用） */
    fun deleteByProfile(profileId: String) {
        runCatching { deleteRecursively("$imagesRoot/$profileId".toPath()) }
    }

    private fun deleteRecursively(path: Path) {
        if (fs.exists(path) && fs.metadata(path).isDirectory) {
            for (child in fs.list(path)) deleteRecursively(child)
        }
        runCatching { fs.delete(path) }
    }

    private fun resolve(relativePath: String): Path {
        val inner = relativePath.removePrefix("${BackupLayout.IMAGES_ROOT}/")
        return "$imagesRoot/$inner".toPath()
    }

    private fun normalizeExt(ext: String): String {
        val raw = ext.trim().lowercase()
        if (raw.isEmpty()) return ""
        return if (raw.startsWith(".")) raw else ".$raw"
    }

    private fun randomName(): String = buildString(16) {
        repeat(16) { append(HEX[Random.nextInt(HEX.size)]) }
    }

    private companion object {
        val HEX = "0123456789abcdef".toCharArray()
    }
}
