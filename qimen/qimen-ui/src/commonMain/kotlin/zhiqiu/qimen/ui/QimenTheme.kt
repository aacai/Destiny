package zhiqiu.qimen.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 奇门盘色：沿用掌盘的暖纸底，只加三奇色和中平门色。 */
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

val LightQimenColors = QimenColors(
    page = Color(0xFFF5F3EE),
    card = Color(0xFFFFFFFF),
    panel = Color(0xFFF5F0ED),
    ink = Color(0xFF222222),
    muted = Color(0xFF8A8578),
    line = Color(0xFFE2DED2),
    accent = Color(0xFF26A6A6),
    lucky = Color(0xFF1B5E20),
    unlucky = Color(0xFFB3261E),
    neutral = Color(0xFF5C6B7A),
    sanQi = Color(0xFFC47D0E),
)

val DarkQimenColors = QimenColors(
    page = Color(0xFF1C1B19),
    card = Color(0xFF2A2825),
    panel = Color(0xFF211F1D),
    ink = Color(0xFFE8E4DC),
    muted = Color(0xFF9E988C),
    line = Color(0xFF3F3C36),
    accent = Color(0xFF4DB6B6),
    lucky = Color(0xFF81C784),
    unlucky = Color(0xFFEF9A9A),
    neutral = Color(0xFFB0BEC5),
    sanQi = Color(0xFFE0A84A),
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
