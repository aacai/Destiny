package zhiqiu.app.destiny.sharing

import okio.Buffer

/**
 * 极简 ZIP 写入器（STORE 模式，不压缩）。
 *
 * 存在的理由：Okio 的 `openZip` 返回的 zip 文件系统是**只读**的（写入会抛
 * `zip file systems are read-only`），无法用它创建备份包。
 *
 * 采用 STORE（不压缩）而非 DEFLATE：备份包里的图片（JPEG/PNG）本身已是压缩格式，
 * 再压缩几乎无收益，而 STORE 无需实现 deflate，格式简单、行为可预测。
 * 产出的是标准 zip，系统自带解压工具与 Okio 均可正常读取。
 */
internal object ZipWriter {

    private const val LOCAL_HEADER_SIG = 0x04034b50
    private const val CENTRAL_HEADER_SIG = 0x02014b50
    private const val EOCD_SIG = 0x06054b50

    private const val VERSION = 20
    private const val METHOD_STORE = 0
    /** DOS 日期：1980-01-01（(year-1980)<<9 | month<<5 | day） */
    private const val DOS_DATE_1980_01_01 = 0x0021

    /**
     * 打包为 zip 字节。
     *
     * @param entries 条目列表：(zip 内相对路径, 内容)。路径用 `/` 分隔，可含多级目录。
     */
    fun create(entries: List<Pair<String, ByteArray>>): ByteArray {
        val out = Buffer()
        val central = Buffer()
        var entryCount = 0
        var localOffset = 0L

        for ((name, data) in entries) {
            val nameBytes = name.encodeToByteArray()
            val crc = crc32(data)

            out.writeIntLe(LOCAL_HEADER_SIG)
            out.writeShortLe(VERSION)
            out.writeShortLe(0) // flags
            out.writeShortLe(METHOD_STORE)
            out.writeShortLe(0) // mod time
            out.writeShortLe(DOS_DATE_1980_01_01)
            out.writeIntLe(crc)
            out.writeIntLe(data.size) // compressed size == size (STORE)
            out.writeIntLe(data.size) // uncompressed size
            out.writeShortLe(nameBytes.size)
            out.writeShortLe(0) // extra length
            out.write(nameBytes)
            out.write(data)

            central.writeIntLe(CENTRAL_HEADER_SIG)
            central.writeShortLe(VERSION) // version made by
            central.writeShortLe(VERSION) // version needed
            central.writeShortLe(0) // flags
            central.writeShortLe(METHOD_STORE)
            central.writeShortLe(0) // mod time
            central.writeShortLe(DOS_DATE_1980_01_01)
            central.writeIntLe(crc)
            central.writeIntLe(data.size)
            central.writeIntLe(data.size)
            central.writeShortLe(nameBytes.size)
            central.writeShortLe(0) // extra length
            central.writeShortLe(0) // comment length
            central.writeShortLe(0) // disk number start
            central.writeShortLe(0) // internal attrs
            central.writeIntLe(0) // external attrs
            central.writeIntLe(localOffset.toInt())
            central.write(nameBytes)

            localOffset += 30L + nameBytes.size + data.size
            entryCount++
        }

        val centralSize = central.size
        val centralOffset = out.size
        out.writeAll(central)

        out.writeIntLe(EOCD_SIG)
        out.writeShortLe(0) // disk number
        out.writeShortLe(0) // disk with central dir
        out.writeShortLe(entryCount)
        out.writeShortLe(entryCount)
        out.writeIntLe(centralSize.toInt())
        out.writeIntLe(centralOffset.toInt())
        out.writeShortLe(0) // comment length

        return out.readByteArray()
    }

    /** CRC-32（zip 使用的标准多项式 0xEDB88320）。 */
    private fun crc32(data: ByteArray): Int {
        var crc = -1 // 0xFFFFFFFF
        for (b in data) {
            crc = crc xor (b.toInt() and 0xFF)
            repeat(8) {
                crc = if (crc and 1 != 0) (crc ushr 1) xor 0xEDB88320.toInt() else crc ushr 1
            }
        }
        return crc.inv()
    }
}
