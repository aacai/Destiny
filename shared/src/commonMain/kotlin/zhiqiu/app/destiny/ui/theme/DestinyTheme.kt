package zhiqiu.app.destiny.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 应用日/夜间模式（手动切换，不跟系统） */
enum class DestinyColorMode {
    Light,
    Dark,
    ;

    fun toggle(): DestinyColorMode = if (this == Light) Dark else Light

    val prefValue: String
        get() = when (this) {
            Light -> "light"
            Dark -> "dark"
        }

    companion object {
        const val PrefKey = "app.colorMode"

        fun fromPref(value: String?): DestinyColorMode =
            if (value.equals("dark", ignoreCase = true)) Dark else Light
    }
}

/**
 * 暖纸色板 token。日间对齐档案页；夜间柔和深暖底（非纯黑）。
 * 自定义时复制 [LightDestinyColors] / [DarkDestinyColors] 再改需要的字段即可。
 */
data class DestinyColors(
    val isDark: Boolean,
    val page: Color,
    val card: Color,
    val panel: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val danger: Color,
    val male: Color,
    val female: Color,
) {
    fun toMaterialColorScheme(): ColorScheme =
        if (isDark) {
            darkColorScheme(
                primary = accent,
                onPrimary = Color(0xFF102A2A),
                secondary = accent,
                onSecondary = page,
                background = page,
                onBackground = ink,
                surface = card,
                onSurface = ink,
                surfaceVariant = panel,
                onSurfaceVariant = muted,
                outline = line,
                error = danger,
                onError = Color.White,
            )
        } else {
            lightColorScheme(
                primary = accent,
                onPrimary = Color.White,
                secondary = accent,
                onSecondary = Color.White,
                background = page,
                onBackground = ink,
                surface = card,
                onSurface = ink,
                surfaceVariant = panel,
                onSurfaceVariant = muted,
                outline = line,
                error = danger,
                onError = Color.White,
            )
        }
}

/** 内置日间主题色板 */
val LightDestinyColors = DestinyColors(
    isDark = false,
    page = Color(0xFFF5F3EE),
    card = Color(0xFFFFFFFF),
    panel = Color(0xFFF5F0ED),
    ink = Color(0xFF222222),
    muted = Color(0xFF8A8578),
    line = Color(0xFFE2DED2),
    accent = Color(0xFF26A6A6),
    danger = Color(0xFFD9534F),
    male = Color(0xFF4A90D9),
    female = Color(0xFFE57373),
)

/** 内置夜间主题色板 */
val DarkDestinyColors = DestinyColors(
    isDark = true,
    page = Color(0xFF1C1B19),
    card = Color(0xFF2A2825),
    panel = Color(0xFF2A2825),
    ink = Color(0xFFE8E4DC),
    muted = Color(0xFF9E988C),
    line = Color(0xFF3F3C36),
    accent = Color(0xFF4DB6B6),
    danger = Color(0xFFE57373),
    male = Color(0xFF64B5F6),
    female = Color(0xFFEF9A9A),
)

/**
 * 成套主题配置：同时持有 lightTheme / darkTheme。
 * 宿主或插件可传入自定义 [DestinyThemeConfig] 覆盖默认纸色，而无需改业务 UI。
 *
 * 例：
 * ```
 * val custom = DestinyThemeConfig.Default.copy(
 *     lightTheme = LightDestinyColors.copy(accent = Color(0xFF8B5A2B)),
 *     darkTheme = DarkDestinyColors.copy(accent = Color(0xFFD4A574)),
 * )
 * DestinyThemeProvider(mode, config = custom) { ... }
 * ```
 */
data class DestinyThemeConfig(
    val lightTheme: DestinyColors = LightDestinyColors,
    val darkTheme: DestinyColors = DarkDestinyColors,
) {
    fun colors(mode: DestinyColorMode): DestinyColors = when (mode) {
        DestinyColorMode.Light -> lightTheme
        DestinyColorMode.Dark -> darkTheme
    }

    companion object {
        val Default = DestinyThemeConfig()
    }
}

val LocalDestinyColors = staticCompositionLocalOf { LightDestinyColors }

val LocalDestinyColorMode = staticCompositionLocalOf { DestinyColorMode.Light }

val LocalDestinyThemeConfig = staticCompositionLocalOf { DestinyThemeConfig.Default }

/** 当前解析后的色板（须在 [DestinyThemeProvider] 内） */
val DestinyTheme: DestinyColors
    @Composable
    @ReadOnlyComposable
    get() = LocalDestinyColors.current

@Composable
fun DestinyThemeProvider(
    mode: DestinyColorMode,
    config: DestinyThemeConfig = DestinyThemeConfig.Default,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalDestinyThemeConfig provides config,
        LocalDestinyColorMode provides mode,
        LocalDestinyColors provides config.colors(mode),
    ) {
        content()
    }
}
