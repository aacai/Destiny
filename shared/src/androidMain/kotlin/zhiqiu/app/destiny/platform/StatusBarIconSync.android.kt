package zhiqiu.app.destiny.platform

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import zhiqiu.app.destiny.ui.theme.DestinyColorMode

@Composable
actual fun StatusBarIconSync(mode: DestinyColorMode) {
    val context = LocalContext.current
    LaunchedEffect(mode, context) {
        val activity = context as? Activity ?: return@LaunchedEffect
        val controller: WindowInsetsControllerCompat? =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        // 浅色主题 → 深色图标（出现深色字）；夜间主题 → 浅色图标
        controller?.isAppearanceLightStatusBars = mode == DestinyColorMode.Light
        controller?.isAppearanceLightNavigationBars = mode == DestinyColorMode.Light
    }
}
