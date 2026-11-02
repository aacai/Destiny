package zhiqiu.liuyao.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.liuyao.Branch
import zhiqiu.liuyao.InstalledHexagram
import zhiqiu.liuyao.InstalledYao
import zhiqiu.liuyao.LiuYaoChart
import zhiqiu.liuyao.panTitle
import zhiqiu.liuyao.yaoPlaceName

data class PanColors(
    val page: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val line: Color,
    val accent: Color,
    val yongShenGold: Color,
    val shiMark: Color,
    val pillarRed: Color,
)

/** 内置日间六爻色板 */
val LightPanColors = PanColors(
    page = Color(0xFFF5F3EE),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF222222),
    muted = Color(0xFF8A8578),
    line = Color(0xFFE2DED2),
    accent = Color(0xFF26A6A6),
    yongShenGold = Color(0xFFD9A441),
    shiMark = Color(0xFFB3261E),
    pillarRed = Color(0xFFD32F2F),
)

/** 内置夜间六爻色板 */
val DarkPanColors = PanColors(
    page = Color(0xFF1C1B19),
    card = Color(0xFF2A2825),
    ink = Color(0xFFE8E4DC),
    muted = Color(0xFF9E988C),
    line = Color(0xFF3F3C36),
    accent = Color(0xFF4DB6B6),
    yongShenGold = Color(0xFFFFD54F),
    shiMark = Color(0xFFEF9A9A),
    pillarRed = Color(0xFFEF5350),
)

/** 成套六爻主题：lightTheme / darkTheme 可整体替换 */
data class PanThemeConfig(
    val lightTheme: PanColors = LightPanColors,
    val darkTheme: PanColors = DarkPanColors,
) {
    fun colors(darkTheme: Boolean): PanColors = if (darkTheme) this.darkTheme else lightTheme

    companion object {
        val Default = PanThemeConfig()
    }
}

val LocalPanColors = staticCompositionLocalOf { LightPanColors }

val LocalPanThemeConfig = staticCompositionLocalOf { PanThemeConfig.Default }

@Composable
fun PanThemeProvider(
    darkTheme: Boolean,
    config: PanThemeConfig = PanThemeConfig.Default,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalPanThemeConfig provides config,
        LocalPanColors provides config.colors(darkTheme),
        content = content,
    )
}

internal val PanCardBg: Color
    @Composable @ReadOnlyComposable get() = LocalPanColors.current.card
internal val PanInk: Color
    @Composable @ReadOnlyComposable get() = LocalPanColors.current.ink
internal val PanMuted: Color
    @Composable @ReadOnlyComposable get() = LocalPanColors.current.muted
internal val PanLine: Color
    @Composable @ReadOnlyComposable get() = LocalPanColors.current.line
internal val PanAccent: Color
    @Composable @ReadOnlyComposable get() = LocalPanColors.current.accent
internal val YongShenGold: Color
    @Composable @ReadOnlyComposable get() = LocalPanColors.current.yongShenGold
internal val ShiMark: Color
    @Composable @ReadOnlyComposable get() = LocalPanColors.current.shiMark
internal val PillarRed: Color
    @Composable @ReadOnlyComposable get() = LocalPanColors.current.pillarRed

/** 列序：六神、伏神、本卦、变卦。行高统一，四列才齐。 */
private val W_GOD = 36.dp
private val W_LINE = 72.dp
private val W_FU = 72.dp
private val ROW_H = 22.dp
private val TITLE_H = 20.dp

/** 单爻横画：阳爻一横，阴爻两段；动爻右侧附 ○/×。 */
@Composable
fun YaoGlyph(
    isYang: Boolean,
    mark: String?,
    modifier: Modifier = Modifier,
    width: Dp = 32.dp,
    /** 静爻也留出 ○/× 空位，起卦下拉里各选项爻画才对齐。卦盘上不留，世应紧挨爻画。 */
    reserveMark: Boolean = true,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (isYang) {
            Box(
                modifier = Modifier
                    .width(width)
                    .height(10.dp)
                    .background(PanInk, RoundedCornerShape(2.dp)),
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    modifier = Modifier
                        .width((width - 5.dp) / 2)
                        .height(10.dp)
                        .background(PanInk, RoundedCornerShape(2.dp)),
                )
                Box(
                    modifier = Modifier
                        .width((width - 5.dp) / 2)
                        .height(10.dp)
                        .background(PanInk, RoundedCornerShape(2.dp)),
                )
            }
        }
        if (reserveMark || mark != null) {
            Spacer(Modifier.width(3.dp))
            Box(
                Modifier.then(if (reserveMark) Modifier.width(14.dp) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (mark != null) MovingMark(mark)
            }
        }
    }
}

/** 动爻：老阳空心圆，老阴叉。圆是描边，不跟世应抢红色。 */
@Composable
private fun MovingMark(mark: String) {
    when (mark) {
        "○" -> Box(
            Modifier
                .size(12.dp)
                .border(1.5.dp, PanInk, CircleShape),
        )
        else -> Text(mark, color = PanInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * 排盘：从左到右「六神、伏神、本卦、变卦」。
 * 有动爻时变卦固定在右侧，窄屏可横滑，不再折到下面。
 * [highlightBranches] 来自题头点选的神煞地支，本卦变卦同支爻一并高亮。
 */
@Composable
fun ChartCard(
    chart: LiuYaoChart,
    modifier: Modifier = Modifier,
    highlightBranches: Set<Branch> = emptySet(),
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(PanCardBg, RoundedCornerShape(12.dp))
            .border(1.dp, PanLine, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        val originalTopDown = chart.original.yaos.sortedByDescending { it.position }
        val changed = chart.changed
        val changedTopDown = changed?.yaos?.sortedByDescending { it.position }
        val showFu = chart.fuShen.isNotEmpty()
        val overhead = W_GOD + 4.dp + (if (showFu) W_FU + 4.dp else 0.dp) + (if (changed != null) 8.dp else 0.dp)
        val slots = if (changed != null) 2 else 1
        val each = (maxWidth - overhead) / slots
        val scroll = changed != null && each < 148.dp
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (scroll) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
            verticalAlignment = Alignment.Top,
        ) {
            SixGodColumn(yaos = originalTopDown)
            if (showFu) {
                Spacer(Modifier.width(4.dp))
                FuShenColumn(byPosition = chart.fuShen, yaos = originalTopDown)
            }
            Spacer(Modifier.width(4.dp))
            HexTable(
                hex = chart.original,
                yaos = originalTopDown,
                highlightBranches = highlightBranches,
                modifier = if (scroll) Modifier.width(156.dp) else Modifier.weight(1f),
            )
            if (changed != null && changedTopDown != null) {
                Spacer(Modifier.width(8.dp))
                HexTable(
                    hex = changed,
                    yaos = changedTopDown,
                    highlightBranches = highlightBranches,
                    modifier = if (scroll) Modifier.width(156.dp) else Modifier.weight(1f),
                )
            }
        }
        if (changed == null) {
            Text("静卦无动爻，无变卦。", color = PanMuted, fontSize = 12.sp)
        }
        }
    }
}

/** 左列：标题「六神」+ 上爻→初爻的玄武…青龙，行高与卦爻对齐。 */
@Composable
private fun SixGodColumn(yaos: List<InstalledYao>) {
    Column(Modifier.width(W_GOD)) {
        Box(
            Modifier.fillMaxWidth().height(TITLE_H),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text("六神", color = PanInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        yaos.forEach { yao ->
            Box(
                Modifier.fillMaxWidth().height(ROW_H),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    yao.sixGod?.label ?: "　",
                    color = PanInk,
                    fontSize = 13.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 伏神列：缺的六亲写在飞神同一行，没有的行留空。 */
@Composable
private fun FuShenColumn(
    byPosition: Map<Int, InstalledYao>,
    yaos: List<InstalledYao>,
) {
    Column(Modifier.width(W_FU)) {
        Box(
            Modifier.fillMaxWidth().height(TITLE_H),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text("伏神", color = PanInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        yaos.forEach { yao ->
            val fu = byPosition[yao.position]
            Box(
                Modifier.fillMaxWidth().height(ROW_H),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (fu != null) {
                    Text(
                        "${fu.sixKin.label}${fu.ganzhi.label}${fu.ganzhi.element.label}",
                        color = ShiMark,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** 单卦：标题 + 六爻（上→初），不含六神、伏神列。 */
@Composable
private fun HexTable(
    hex: InstalledHexagram,
    yaos: List<InstalledYao>,
    highlightBranches: Set<Branch>,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Box(
            Modifier.fillMaxWidth().height(TITLE_H),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                hex.entry.panTitle(),
                color = PanInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(4.dp))
        yaos.forEach { yao ->
            DataRow(
                yao = yao,
                highlighted = yao.ganzhi.branch in highlightBranches,
            )
        }
    }
}

@Composable
private fun DataRow(yao: InstalledYao, highlighted: Boolean) {
    val yong = yao.isYongShen
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_H)
            .background(
                if (highlighted) PanAccent.copy(alpha = 0.16f) else Color.Transparent,
                RoundedCornerShape(4.dp),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${yao.sixKin.label}${yao.ganzhi.label}${yao.ganzhi.element.label}",
            color = when {
                highlighted -> PanAccent
                yong -> YongShenGold
                else -> PanInk
            },
            fontSize = 12.sp,
            fontWeight = if (highlighted || yong) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            modifier = Modifier.width(W_LINE),
        )
        YaoGlyph(isYang = yao.isYang, mark = yao.kind?.mark, width = 28.dp, reserveMark = false)
        val mark = shiYingMark(yao)
        if (mark.isNotEmpty()) {
            Text(
                mark,
                color = ShiMark,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}

private fun shiYingMark(yao: InstalledYao): String = buildString {
    if (yao.isShi) append("世")
    if (yao.isYing) append("应")
    if (yao.isYongShen) {
        if (isNotEmpty()) append("·")
        append("用")
    }
}

/** 紧凑下拉框：结果页题头改占类用。 */
@Composable
internal fun MiniDropdown(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .border(1.dp, PanLine, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                selected,
                color = PanInk,
                fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(" ▾", color = PanMuted, fontSize = 11.sp)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(PanCardBg),
        ) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = { Text(o, fontSize = 13.sp, color = PanInk) },
                    onClick = {
                        onSelect(o)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun Chip(    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(
                if (selected) PanAccent.copy(alpha = 0.14f) else PanCardBg,
                RoundedCornerShape(8.dp),
            )
            .border(
                1.dp,
                if (selected) PanAccent else PanLine,
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) PanAccent else PanMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

internal fun yaoPlaceLabel(position: Int): String = yaoPlaceName(position)
