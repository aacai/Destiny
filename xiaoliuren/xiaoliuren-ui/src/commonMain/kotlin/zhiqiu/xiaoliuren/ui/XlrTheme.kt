package zhiqiu.xiaoliuren.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 小六壬掌盘色板：暖纸底 + 吉凶双色。 */
data class XlrColors(
    val page: Color,
    val card: Color,
    val panel: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val lucky: Color,
    val unlucky: Color,
) {
    /** 吉宫用吉色、凶宫用凶色着色。 */
    fun palaceColor(auspicious: Boolean): Color = if (auspicious) lucky else unlucky
}

val LightXlrColors = XlrColors(
    page = Color(0xFFF5F3EE),
    card = Color(0xFFFFFFFF),
    panel = Color(0xFFF5F0ED),
    ink = Color(0xFF222222),
    muted = Color(0xFF8A8578),
    line = Color(0xFFE2DED2),
    accent = Color(0xFF26A6A6),
    lucky = Color(0xFF1B5E20),
    unlucky = Color(0xFFB3261E),
)

val DarkXlrColors = XlrColors(
    page = Color(0xFF1C1B19),
    card = Color(0xFF2A2825),
    panel = Color(0xFF211F1D),
    ink = Color(0xFFE8E4DC),
    muted = Color(0xFF9E988C),
    line = Color(0xFF3F3C36),
    accent = Color(0xFF4DB6B6),
    lucky = Color(0xFF81C784),
    unlucky = Color(0xFFEF9A9A),
)

data class XlrThemeConfig(
    val lightTheme: XlrColors = LightXlrColors,
    val darkTheme: XlrColors = DarkXlrColors,
) {
    fun colors(darkTheme: Boolean): XlrColors = if (darkTheme) this.darkTheme else this.lightTheme

    companion object {
        val Default = XlrThemeConfig()
    }
}

val LocalXlrColors = staticCompositionLocalOf { LightXlrColors }

/** 当前小六壬色板（须在 [XlrThemeProvider] 内）。 */
val XlrTheme: XlrColors
    @Composable
    @ReadOnlyComposable
    get() = LocalXlrColors.current

@Composable
fun XlrThemeProvider(
    darkTheme: Boolean,
    config: XlrThemeConfig = XlrThemeConfig.Default,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalXlrColors provides config.colors(darkTheme),
        content = content,
    )
}
