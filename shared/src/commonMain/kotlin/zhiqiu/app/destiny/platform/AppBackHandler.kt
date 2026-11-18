package zhiqiu.app.destiny.platform

import androidx.compose.runtime.Composable

/**
 * 拦截系统/手势返回键。[enabled] 为 true 时消费返回事件并回调 [onBack]。
 * 主页应始终 enabled 且不 pop，避免把根页面弹出导致白屏。
 */
@Composable
expect fun AppBackHandler(enabled: Boolean, onBack: () -> Unit)
