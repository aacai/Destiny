package zhiqiu.xiaoliuren.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import zhiqiu.applewidgets.kmp.oink.theme.oinkDarkColors
import zhiqiu.applewidgets.kmp.oink.theme.oinkLightColors

/** 小六壬掌盘色板：与全局统一走 oink 中性色，吉凶用 oink 语义色。 */
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

private val oinkLight = oinkLightColors()
private val oinkDark = oinkDarkColors()

val LightXlrColors = XlrColors(
    page = oinkLight.bg,
    card = oinkLight.card,
    panel = oinkLight.well,
    ink = oinkLight.text,
    muted = oinkLight.text2,
    line = oinkLight.line2,
    accent = oinkLight.accent,
    lucky = oinkLight.successText,
    unlucky = oinkLight.dangerText,
)

val DarkXlrColors = XlrColors(
    page = oinkDark.bg,
    card = oinkDark.card,
    panel = oinkDark.well,
    ink = oinkDark.text,
    muted = oinkDark.text2,
    line = oinkDark.line2,
    accent = oinkDark.accent,
    lucky = oinkDark.successText,
    unlucky = oinkDark.dangerText,
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
