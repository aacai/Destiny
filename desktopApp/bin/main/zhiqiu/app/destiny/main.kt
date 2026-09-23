package zhiqiu.app.destiny

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.vinceglb.filekit.FileKit

fun main() = application {
    FileKit.init(appId = "zhiqiu.app.destiny")
    Window(
        onCloseRequest = ::exitApplication,
        title = "Destiny",
    ) {
        App()
    }
}
