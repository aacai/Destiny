package zhiqiu.app.destiny.sharing

import kotlin.test.Test
import kotlin.test.assertTrue

class QrCodeTest {

    @Test
    fun generatesValidPng() {
        val png = generateQrCodePng("https://file.io/2ojE41")
        assertTrue(png.isNotEmpty(), "二维码不应为空")
        // PNG 文件头：89 50 4E 47
        val header = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte())
        assertTrue(
            png.copyOfRange(0, 4).contentEquals(header),
            "输出应为 PNG 图片，实际头部=${png.copyOfRange(0, 4).joinToString { it.toString(16) }}",
        )
    }

    @Test
    fun largerCellSizeProducesLargerImage() {
        val small = generateQrCodePng("https://file.io/abc123", cellSize = 4)
        val large = generateQrCodePng("https://file.io/abc123", cellSize = 12)
        assertTrue(large.size > small.size, "模块边长越大，图片体积应越大")
    }
}
