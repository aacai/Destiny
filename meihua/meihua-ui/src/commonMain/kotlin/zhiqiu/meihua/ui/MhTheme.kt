package zhiqiu.meihua.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import zhiqiu.applewidgets.kmp.oink.theme.oinkDarkColors
import zhiqiu.applewidgets.kmp.oink.theme.oinkLightColors

/** 梅花盘色板：与全局统一走 oink 中性色，吉凶用 oink 语义色。 */
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

private val oinkLight = oinkLightColors()
private val oinkDark = oinkDarkColors()

val LightMhColors = MhColors(
    page = oinkLight.bg,
    card = oinkLight.card,
    panel = oinkLight.well,
    ink = oinkLight.text,
    muted = oinkLight.text2,
    line = oinkLight.line2,
    accent = oinkLight.accent,
    lucky = oinkLight.successText,
    unlucky = oinkLight.dangerText,
    moving = oinkLight.warningText,
)

val DarkMhColors = MhColors(
    page = oinkDark.bg,
    card = oinkDark.card,
    panel = oinkDark.well,
    ink = oinkDark.text,
    muted = oinkDark.text2,
    line = oinkDark.line2,
    accent = oinkDark.accent,
    lucky = oinkDark.successText,
    unlucky = oinkDark.dangerText,
    moving = oinkDark.warningText,
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
