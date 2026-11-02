package zhiqiu.liuyao.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/**
 * 仅按窗口宽度划分，用于小屏适配（与平台/安全区无关）。
 * 断点沿用 Material 规范：<600dp 紧凑，600~839dp 中等，>=840dp 展开。
 */
enum class WindowWidthSizeClass { Compact, Medium, Expanded }

@Composable
fun rememberWindowWidthSizeClass(): WindowWidthSizeClass {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val widthDp = with(density) { windowInfo.containerSize.width.toDp() }
    return remember(widthDp) {
        when {
            widthDp < 600.dp -> WindowWidthSizeClass.Compact
            widthDp < 840.dp -> WindowWidthSizeClass.Medium
            else -> WindowWidthSizeClass.Expanded
        }
    }
}
