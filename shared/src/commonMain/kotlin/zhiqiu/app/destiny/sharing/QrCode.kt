package zhiqiu.app.destiny.sharing

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
    QRCode.ofSquares()
        .withSize(cellSize)
        .build(content)
        .renderToBytes(format = "PNG")
