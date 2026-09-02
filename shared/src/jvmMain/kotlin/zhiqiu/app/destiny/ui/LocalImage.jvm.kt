package zhiqiu.app.destiny.ui

import java.io.File

actual fun localImageModel(absolutePath: String): Any = File(absolutePath)
