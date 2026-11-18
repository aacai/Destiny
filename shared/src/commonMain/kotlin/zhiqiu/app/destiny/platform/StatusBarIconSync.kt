package zhiqiu.app.destiny.platform

import androidx.compose.runtime.Composable
import zhiqiu.app.destiny.ui.theme.DestinyColorMode

/**
 * 让系统状态栏图标明暗匹配应用顶栏底色：浅色主题用深色图标，夜间主题用浅色图标。
 * 仅 Android 的 edge-to-edge 需要；其它平台 no-op。
 */
@Composable
expect fun StatusBarIconSync(mode: DestinyColorMode)
