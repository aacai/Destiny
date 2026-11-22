package zhiqiu.app.destiny.oinkcompat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.applewidgets.kmp.oink.basic.OinkText
import zhiqiu.applewidgets.kmp.oink.layout.OinkDivider
import zhiqiu.applewidgets.kmp.oink.overlay.OinkDialog
import zhiqiu.applewidgets.kmp.oink.theme.OinkAccent
import zhiqiu.applewidgets.kmp.oink.theme.OinkColors
import zhiqiu.applewidgets.kmp.oink.theme.OinkTheme
import zhiqiu.applewidgets.kmp.oink.theme.oinkDarkColors
import zhiqiu.applewidgets.kmp.oink.theme.oinkLightColors

/**
 * 高阶 MD3 容器兼容层：保持 Scaffold/TopAppBar/NavigationBar 等签名，
 * 内部用 foundation + Oink 令牌实现，业务侧改 import 即可。
 */

/** Oink 小标题 + 内容分区 */
@Composable
fun Section(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    zhiqiu.applewidgets.kmp.oink.layout.OinkSection(
        title = title,
        modifier = modifier,
        content = content,
    )
}

/** Oink 卡片（圆角 + 描边 + 顶部高光） */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    zhiqiu.applewidgets.kmp.oink.basic.OinkCard(
        modifier = modifier,
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    containerColor: Color = Color.Unspecified,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        topBar()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            content(androidx.compose.foundation.layout.PaddingValues(0.dp))
            Box(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp)) {
                floatingActionButton()
            }
        }
        bottomBar()
    }
}

@Composable
fun TopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarColors.Default,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(OinkTheme.colors.surface)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigationIcon()
        Box(Modifier.weight(1f).padding(horizontal = 8.dp)) { title() }
        Row(horizontalArrangement = Arrangement.End) { actions() }
    }
}

object TopAppBarDefaults {
    @Composable
    fun topAppBarColors(
        containerColor: Color = Color.Unspecified,
        titleContentColor: Color = Color.Unspecified,
        actionIconContentColor: Color = Color.Unspecified,
    ): TopAppBarColors = TopAppBarColors.Default
}

@Immutable
data class TopAppBarColors(val any: Any? = null) {
    companion object { val Default = TopAppBarColors() }
}

@Composable
fun NavigationBar(
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(OinkTheme.colors.surface)
            .height(60.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
fun RowScope.NavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable () -> Unit = {},
    alwaysShowLabel: Boolean = true,
) {
    val tint = if (selected) OinkTheme.colors.accentText else OinkTheme.colors.text2
    Column(
        modifier = modifier
            .weight(1f)
            .height(56.dp)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            zhiqiu.applewidgets.kmp.oink.theme.LocalOinkContentColor provides tint,
        ) { icon() }
        Box { label() }
    }
}

@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (containerColor == Color.Unspecified) OinkTheme.colors.accentFill else containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            zhiqiu.applewidgets.kmp.oink.theme.LocalOinkContentColor provides
                if (contentColor == Color.Unspecified) OinkTheme.colors.onAccent else contentColor,
        ) { content() }
    }
}

@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable () -> Unit = {},
    title: @Composable () -> Unit = {},
    text: @Composable () -> Unit = {},
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    containerColor: Color = Color.Unspecified,
    tonalElevation: androidx.compose.ui.unit.Dp = 0.dp,
    titleContentColor: Color = Color.Unspecified,
    textContentColor: Color = Color.Unspecified,
    iconContentColor: Color = Color.Unspecified,
) {
    // Oink 的对话框接收纯文本；这里用 foundation 布局承载自定义 @Composable 内容，
    // 但配色/圆角/按钮语义统一取 Oink 令牌。
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OinkTheme.colors.scrim)
            .clickable(onClick = onDismissRequest),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clickable(enabled = false, onClick = {})
                .clip(shape)
                .background(if (containerColor == Color.Unspecified) OinkTheme.colors.pop else containerColor)
                .padding(20.dp),
        ) {
            title()
            Box(Modifier.padding(top = 8.dp)) { text() }
            Row(
                Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                dismissButton()
                confirmButton()
            }
        }
    }
}

@Composable
fun DropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (expanded) {
        Box(modifier.clickable(onClick = onDismissRequest)) {
            Column(
                modifier = Modifier
                    .background(OinkTheme.colors.pop)
                    .padding(4.dp),
                content = content,
            )
        }
    }
}

@Composable
fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) leadingIcon()
        Box(Modifier.weight(1f).padding(horizontal = 8.dp)) { text() }
        if (trailingIcon != null) trailingIcon()
    }
}

@Composable
fun TabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    indicator: @Composable (List<Any>) -> Unit = { },
    tabs: @Composable () -> Unit,
) {
    Row(modifier.fillMaxWidth().background(OinkTheme.colors.surface)) { tabs() }
}

@Composable
fun SecondaryTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    tabs: @Composable () -> Unit,
) {
    Row(modifier.fillMaxWidth().background(OinkTheme.colors.surface)) { tabs() }
}

@Composable
fun Tab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: @Composable () -> Unit = {},
    icon: @Composable (() -> Unit)? = null,
) {
    val tint = if (selected) OinkTheme.colors.accentText else OinkTheme.colors.text2
    Column(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(zhiqiu.applewidgets.kmp.oink.theme.LocalOinkContentColor provides tint) {
            if (icon != null) icon()
            text()
        }
    }
}

object TabRowDefaults {
    @Composable
    fun SecondaryIndicator(modifier: Modifier = Modifier, color: Color = Color.Unspecified, height: Dp = 2.dp) {
        Box(modifier.fillMaxWidth().height(height).background(if (color == Color.Unspecified) OinkTheme.colors.accent else color))
    }

    fun Modifier.tabIndicatorOffset(currentTabPosition: Any): Modifier = this
}

fun Modifier.tabIndicatorOffset(currentTabPosition: Any): Modifier = this

/** MaterialTheme 兼容：仅提供 colorScheme/typography 取值，视觉由外层 OinkTheme 决定。 */
object MaterialTheme {
    val colorScheme: MaterialColorScheme
        @Composable @ReadOnlyComposable
        get() = MaterialColorScheme(
            primary = OinkTheme.colors.accent,
            surface = OinkTheme.colors.surface,
            onSurface = OinkTheme.colors.text,
        )

    val typography: MaterialTypography
        @Composable @ReadOnlyComposable
        get() = MaterialTypography(
            titleSmall = OinkTheme.textStyles.h4,
            bodyLarge = OinkTheme.textStyles.bodyLarge,
            bodyMedium = OinkTheme.textStyles.body,
        )

    @Composable
    operator fun invoke(
        colorScheme: ColorScheme = ColorScheme(),
        typography: Typography = Typography(),
        content: @Composable () -> Unit,
    ) = content()
}

@Immutable data class MaterialColorScheme(val primary: Color, val surface: Color, val onSurface: Color)
@Immutable data class MaterialTypography(val titleSmall: TextStyle, val bodyLarge: TextStyle, val bodyMedium: TextStyle)
@Immutable data class ColorScheme(
    val primary: Color = Color.Unspecified,
    val onPrimary: Color = Color.Unspecified,
    val secondary: Color = Color.Unspecified,
    val onSecondary: Color = Color.Unspecified,
    val background: Color = Color.Unspecified,
    val onBackground: Color = Color.Unspecified,
    val surface: Color = Color.Unspecified,
    val onSurface: Color = Color.Unspecified,
    val surfaceVariant: Color = Color.Unspecified,
    val onSurfaceVariant: Color = Color.Unspecified,
    val outline: Color = Color.Unspecified,
    val error: Color = Color.Unspecified,
    val onError: Color = Color.Unspecified,
)
@Immutable data class Typography(val any: Any? = null)

fun lightColorScheme(
    primary: Color = Color.Unspecified,
    onPrimary: Color = Color.Unspecified,
    secondary: Color = Color.Unspecified,
    onSecondary: Color = Color.Unspecified,
    background: Color = Color.Unspecified,
    onBackground: Color = Color.Unspecified,
    surface: Color = Color.Unspecified,
    onSurface: Color = Color.Unspecified,
    surfaceVariant: Color = Color.Unspecified,
    onSurfaceVariant: Color = Color.Unspecified,
    outline: Color = Color.Unspecified,
    error: Color = Color.Unspecified,
    onError: Color = Color.Unspecified,
): ColorScheme = ColorScheme(primary, onPrimary, secondary, onSecondary, background, onBackground, surface, onSurface, surfaceVariant, onSurfaceVariant, outline, error, onError)

fun darkColorScheme(
    primary: Color = Color.Unspecified,
    onPrimary: Color = Color.Unspecified,
    secondary: Color = Color.Unspecified,
    onSecondary: Color = Color.Unspecified,
    background: Color = Color.Unspecified,
    onBackground: Color = Color.Unspecified,
    surface: Color = Color.Unspecified,
    onSurface: Color = Color.Unspecified,
    surfaceVariant: Color = Color.Unspecified,
    onSurfaceVariant: Color = Color.Unspecified,
    outline: Color = Color.Unspecified,
    error: Color = Color.Unspecified,
    onError: Color = Color.Unspecified,
): ColorScheme = ColorScheme(primary, onPrimary, secondary, onSecondary, background, onBackground, surface, onSurface, surfaceVariant, onSurfaceVariant, outline, error, onError)

/** 主题容器：业务已有 DestinyThemeProvider/PanThemeProvider 等，这里再包一层 OinkTheme 提供令牌。 */
@Composable
fun MaterialThemeHost(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) oinkDarkColors() else oinkLightColors()
    OinkTheme(colors = colors) { content() }
}
