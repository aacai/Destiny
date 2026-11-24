package zhiqiu.qizheng.ui

import androidx.compose.ui.graphics.Color
import zhiqiu.applewidgets.kmp.oink.theme.oinkDarkColors
import zhiqiu.applewidgets.kmp.oink.theme.oinkLightColors

/** 七政盘面中性色统一走 oink（行星语义色仍用模块内常量） */
data class QizhengColors(
    val isDark: Boolean,
    val page: Color,
    val paper: Color,
    val cream: Color,
    val hairline: Color,
    val line: Color,
    val soft: Color,
    val tick: Color,
    val ink: Color,
    val muted: Color,
    val chipBg: Color,
    val chipOn: Color,
    val accent: Color,
)

private val oinkLight = oinkLightColors()
private val oinkDark = oinkDarkColors()

val LightQizhengColors = QizhengColors(
    isDark = false,
    page = oinkLight.bg,
    paper = oinkLight.card,
    cream = oinkLight.wellHi,
    hairline = oinkLight.line2,
    line = oinkLight.text,
    soft = oinkLight.text3,
    tick = oinkLight.text2,
    ink = oinkLight.text,
    muted = oinkLight.text2,
    chipBg = oinkLight.well,
    chipOn = oinkLight.accentSoft,
    accent = oinkLight.accent,
)

val DarkQizhengColors = QizhengColors(
    isDark = true,
    page = oinkDark.bg,
    paper = oinkDark.card,
    cream = oinkDark.wellHi,
    hairline = oinkDark.line2,
    line = oinkDark.text,
    soft = oinkDark.text3,
    tick = oinkDark.text2,
    ink = oinkDark.text,
    muted = oinkDark.text2,
    chipBg = oinkDark.well,
    chipOn = oinkDark.accentSoft,
    accent = oinkDark.accent,
)

/**
 * 成套七政主题：lightTheme / darkTheme 可整体替换。
 *
 * ```
 * QizhengWheel(
 *   darkTheme = true,
 *   themeConfig = QizhengThemeConfig.Default.copy(
 *     darkTheme = DarkQizhengColors.copy(accent = Color(0xFFA5D6A7)),
 *   ),
 * )
 * ```
 */
data class QizhengThemeConfig(
    val lightTheme: QizhengColors = LightQizhengColors,
    val darkTheme: QizhengColors = DarkQizhengColors,
) {
    fun colors(darkTheme: Boolean): QizhengColors = if (darkTheme) this.darkTheme else lightTheme

    companion object {
        val Default = QizhengThemeConfig()
    }
}
