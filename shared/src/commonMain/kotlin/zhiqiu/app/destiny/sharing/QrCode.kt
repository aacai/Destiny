package zhiqiu.app.destiny.sharing

import okio.ByteString.Companion.decodeBase64
import qrcode.QRCode

/**
 * 将文本编码为二维码 PNG 字节。
 *
 * 采用本地生成（qrcode-kotlin，纯 Kotlin）而非调用在线二维码服务，原因是分享链接通常有
 * 有效期：若把链接交给第三方服务，对方可抢先下载导致链接失效（可用性风险），
 * 且链接会被记录在对方日志中。本地生成无网络往返、无泄露。
 *
 * @param content 待编码内容（通常是分享链接）
 * @param cellSize 每个模块的像素边长，越大越清晰；链接较长时可适当调小
 * @return PNG 图片字节，可直接用 Coil3 渲染或写入文件
 */
fun generateQrCodePng(content: String, cellSize: Int = 8): ByteArray =
    asPngBytes(
        QRCode.ofSquares()
            .withSize(cellSize)
            .build(content)
            .renderToBytes(format = "PNG")
    )

private const val PNG_FIRST_BYTE = 0x89.toByte()

/** wasm/js 变体没有光栅化后端，库返回 `data:image/png;base64,…` 文本而非图片字节 */
private fun asPngBytes(rendered: ByteArray): ByteArray {
    if (rendered.firstOrNull() == PNG_FIRST_BYTE) return rendered
    val base64 = rendered.decodeToString().substringAfter(',')
    return base64.decodeBase64()?.toByteArray()
        ?: throw IllegalStateException("二维码渲染结果不是 PNG，也不是 base64 data URI")
}
