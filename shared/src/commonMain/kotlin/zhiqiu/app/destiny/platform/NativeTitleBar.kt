package zhiqiu.app.destiny.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import zhiqiu.app.destiny.ui.theme.DestinyColorMode

/**
 * 将 macOS 原生标题栏融入应用主题（白色条消失）。
 * 非 JVM 平台或桌面无 JFrame 时 no-op。
 */
@Composable
expect fun NativeTitleBarSync(mode: DestinyColorMode)

/**
 * macOS 全屏内容模式下红绿灯按钮占用的顶部色带高度，由宿主统一留出，
 * 各页面顶栏因此不必再逐个避让；其他平台 0.dp。
 */
expect val TitleBarBandHeight: Dp
