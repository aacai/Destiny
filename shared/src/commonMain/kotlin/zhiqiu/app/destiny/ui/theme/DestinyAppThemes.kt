package zhiqiu.app.destiny.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import zhiqiu.iztro.bazi.ui.BaziThemeConfig
import zhiqiu.iztro.ui.IztroThemeConfig
import zhiqiu.liuyao.ui.PanThemeConfig
import zhiqiu.qizheng.ui.QizhengThemeConfig

/**
 * 宿主成套主题：一次注入各盘 lightTheme / darkTheme。
 *
 * ```
 * App(
 *   themes = DestinyAppThemes.Default.copy(
 *     app = DestinyThemeConfig.Default.copy(
 *       lightTheme = LightDestinyColors.copy(accent = Color(0xFF8B5A2B)),
 *     ),
 *     iztro = IztroThemeConfig.Default.copy(
 *       darkTheme = DarkIztroColors.copy(boardBg = Color(0xFF121212)),
 *     ),
 *   ),
 * )
 * ```
 */
data class DestinyAppThemes(
    val app: DestinyThemeConfig = DestinyThemeConfig.Default,
    val iztro: IztroThemeConfig = IztroThemeConfig.Default,
    val bazi: BaziThemeConfig = BaziThemeConfig.Default,
    val pan: PanThemeConfig = PanThemeConfig.Default,
    val qizheng: QizhengThemeConfig = QizhengThemeConfig.Default,
) {
    companion object {
        val Default = DestinyAppThemes()
    }
}

val LocalDestinyAppThemes = staticCompositionLocalOf { DestinyAppThemes.Default }
