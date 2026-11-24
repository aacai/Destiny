package zhiqiu.app.destiny.platform

import androidx.compose.runtime.Composable

/**
 * 拦截系统/手势返回键。[enabled] 为 true 时消费返回事件并回调 [onBack]。
 * 调用方在主页应直接退出应用，不要 pop 根导航页。
 */
@Composable
expect fun AppBackHandler(enabled: Boolean, onBack: () -> Unit)
