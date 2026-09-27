package zhiqiu.qizheng.ui

import androidx.compose.ui.graphics.Color

/** 七政盘面纸色（行星语义色仍用模块内常量） */
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

val LightQizhengColors = QizhengColors(
    isDark = false,
    page = Color(0xFFFFFFFF),
    paper = Color(0xFFFFFFFF),
    cream = Color(0xFFFDFBF4),
    hairline = Color(0xFFC9C4B8),
    line = Color(0xFF212121),
    soft = Color(0xFF9E9E9E),
    tick = Color(0xFF424242),
    ink = Color(0xFF111111),
    muted = Color(0xFF757575),
    chipBg = Color(0xFFF5F5F5),
    chipOn = Color(0xFFE8F5E9),
    accent = Color(0xFF1B5E20),
)

val DarkQizhengColors = QizhengColors(
    isDark = true,
    page = Color(0xFF1C1B19),
    paper = Color(0xFF2A2825),
    cream = Color(0xFF1C1B19),
    hairline = Color(0xFF3F3C36),
    line = Color(0xFFE8E4DC),
    soft = Color(0xFF9E988C),
    tick = Color(0xFFB0A99C),
    ink = Color(0xFFE8E4DC),
    muted = Color(0xFF9E988C),
    chipBg = Color(0xFF2A2825),
    chipOn = Color(0xFF1E3A2F),
    accent = Color(0xFF81C784),
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
