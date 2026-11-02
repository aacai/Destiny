package zhiqiu.meihua.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 梅花盘色板：竹青纸底，吉凶分明。 */
data class MhColors(
    val page: Color,
    val card: Color,
    val panel: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val lucky: Color,
    val unlucky: Color,
    val moving: Color,
) {
    fun tone(auspicious: Boolean): Color = if (auspicious) lucky else unlucky
}

val LightMhColors = MhColors(
    page = Color(0xFFF4F6F2),
    card = Color(0xFFFFFFFF),
    panel = Color(0xFFEEF2EC),
    ink = Color(0xFF1F2A1C),
    muted = Color(0xFF7A8574),
    line = Color(0xFFD7DFD2),
    accent = Color(0xFF2F6B4F),
    lucky = Color(0xFF1B5E20),
    unlucky = Color(0xFFB3261E),
    moving = Color(0xFFC45C26),
)

val DarkMhColors = MhColors(
    page = Color(0xFF171A16),
    card = Color(0xFF242822),
    panel = Color(0xFF1D211C),
    ink = Color(0xFFE4E8E0),
    muted = Color(0xFF9AA394),
    line = Color(0xFF3A4038),
    accent = Color(0xFF6FAF8E),
    lucky = Color(0xFF81C784),
    unlucky = Color(0xFFEF9A9A),
    moving = Color(0xFFE08A5A),
)

data class MhThemeConfig(
    val lightTheme: MhColors = LightMhColors,
    val darkTheme: MhColors = DarkMhColors,
) {
    fun colors(isDark: Boolean): MhColors = if (isDark) this.darkTheme else this.lightTheme

    companion object {
        val Default = MhThemeConfig()
    }
}

val LocalMhColors = staticCompositionLocalOf { LightMhColors }

val MhTheme: MhColors
    @Composable
    @ReadOnlyComposable
    get() = LocalMhColors.current

@Composable
fun MhThemeProvider(
    darkTheme: Boolean,
    config: MhThemeConfig = MhThemeConfig.Default,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalMhColors provides config.colors(darkTheme),
        content = content,
    )
}
