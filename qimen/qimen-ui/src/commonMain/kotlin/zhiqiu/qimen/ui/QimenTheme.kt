package zhiqiu.qimen.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import zhiqiu.applewidgets.kmp.oink.theme.oinkDarkColors
import zhiqiu.applewidgets.kmp.oink.theme.oinkLightColors

/** 奇门盘色：与全局统一走 oink 中性色，吉凶/中平用 oink 语义色。 */
data class QimenColors(
    val page: Color,
    val card: Color,
    val panel: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val lucky: Color,
    val unlucky: Color,
    val neutral: Color,
    val sanQi: Color,
)

private val oinkLight = oinkLightColors()
private val oinkDark = oinkDarkColors()

val LightQimenColors = QimenColors(
    page = oinkLight.bg,
    card = oinkLight.card,
    panel = oinkLight.well,
    ink = oinkLight.text,
    muted = oinkLight.text2,
    line = oinkLight.line2,
    accent = oinkLight.accent,
    lucky = oinkLight.successText,
    unlucky = oinkLight.dangerText,
    neutral = oinkLight.text2,
    sanQi = oinkLight.warningText,
)

val DarkQimenColors = QimenColors(
    page = oinkDark.bg,
    card = oinkDark.card,
    panel = oinkDark.well,
    ink = oinkDark.text,
    muted = oinkDark.text2,
    line = oinkDark.line2,
    accent = oinkDark.accent,
    lucky = oinkDark.successText,
    unlucky = oinkDark.dangerText,
    neutral = oinkDark.text2,
    sanQi = oinkDark.warningText,
)

val LocalQimenColors = staticCompositionLocalOf { LightQimenColors }

val QimenTheme: QimenColors
    @Composable
    @ReadOnlyComposable
    get() = LocalQimenColors.current

@Composable
fun QimenThemeProvider(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalQimenColors provides if (darkTheme) DarkQimenColors else LightQimenColors,
        content = content,
    )
}
