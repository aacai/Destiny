package zhiqiu.app.destiny.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.awt.Frame
import javax.swing.JFrame
import zhiqiu.app.destiny.ui.theme.DestinyColorMode

private val isMacOs: Boolean = System.getProperty("os.name", "").contains("Mac", ignoreCase = true)

@Composable
actual fun NativeTitleBarSync(mode: DestinyColorMode) {
    if (!isMacOs) return
    LaunchedEffect(mode) {
        Frame.getFrames().filterIsInstance<JFrame>().forEach { frame ->
            val root = frame.rootPane
            root.putClientProperty("apple.awt.fullWindowContent", true)
            root.putClientProperty("apple.awt.transparentTitleBar", true)
            root.putClientProperty(
                "apple.awt.windowAppearance",
                when (mode) {
                    DestinyColorMode.Dark -> "darkAqua"
                    DestinyColorMode.Light -> "aqua"
                },
            )
            frame.title = ""
            frame.repaint()
        }
    }
}

actual val TitleBarBandHeight: Dp
    get() = if (isMacOs) 28.dp else 0.dp
