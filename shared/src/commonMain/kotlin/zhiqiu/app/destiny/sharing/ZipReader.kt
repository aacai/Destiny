package zhiqiu.app.destiny.sharing

import okio.Buffer
import okio.FileSystem
import okio.Path
import okio.buffer

/**
 * 极简 ZIP 读取器（仅 STORE，与 [ZipWriter] 对称）。
 *
 * 浏览器 / wasmJs 没有 Okio 的 [FileSystem.openZip]，备份包又是我们自己用 STORE 写出的，
 * 因此在 common 里用同一套格式读写，避免平台差异。
 */
internal object ZipReader {

    private const val LOCAL_HEADER_SIG = 0x04034b50
    private const val METHOD_STORE = 0

    fun readEntries(bytes: ByteArray): Map<String, ByteArray> {
        val src = Buffer().write(bytes)
        val out = linkedMapOf<String, ByteArray>()
        while (src.size >= 4L) {
            val sig = src.readIntLe()
            if (sig != LOCAL_HEADER_SIG) break
            src.skip(2) // version
            src.skip(2) // flags
            val method = src.readShortLe().toInt() and 0xFFFF
            src.skip(2) // mod time
            src.skip(2) // mod date
            src.skip(4) // crc
            val compSize = src.readIntLe()
            val uncompSize = src.readIntLe()
            val nameLen = src.readShortLe().toInt() and 0xFFFF
            val extraLen = src.readShortLe().toInt() and 0xFFFF
            val name = src.readByteArray(nameLen.toLong()).decodeToString()
            if (extraLen > 0) src.skip(extraLen.toLong())
            require(method == METHOD_STORE) {
                "仅支持 STORE 压缩方式的备份包（method=$method）: $name"
            }
            require(compSize == uncompSize) { "STORE 条目大小不一致: $name" }
            out[name] = src.readByteArray(uncompSize.toLong())
        }
        return out
    }

    fun unpackFromFs(fs: FileSystem, zipPath: Path): UnpackedBackup {
        val bytes = fs.read(zipPath) { readByteArray() }
        val entries = readEntries(bytes)
        val json = entries[BackupLayout.BACKUP_JSON]?.decodeToString()
            ?: error("缺少 ${BackupLayout.BACKUP_JSON}")
        val images = entries.filterKeys { it.startsWith("${BackupLayout.IMAGES_ROOT}/") }
        return UnpackedBackup(json, images)
    }
}
