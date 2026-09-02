package zhiqiu.app.destiny.ui

import platform.Foundation.NSURL

actual fun localImageModel(absolutePath: String): Any = NSURL.fileURLWithPath(absolutePath)
