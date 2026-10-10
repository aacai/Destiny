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

/** 桌面 macOS 全屏内容模式下，顶栏需要为红绿灯按钮让出的水平内缩；其他平台 0.dp。 */
expect val TrafficLightInsetStart: Dp
