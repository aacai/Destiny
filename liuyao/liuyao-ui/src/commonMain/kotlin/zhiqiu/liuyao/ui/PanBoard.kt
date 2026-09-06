package zhiqiu.liuyao.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.liuyao.HexagramEntry
import zhiqiu.liuyao.InstalledHexagram
import zhiqiu.liuyao.InstalledYao
import zhiqiu.liuyao.LiuYaoChart
import zhiqiu.liuyao.yaoPlaceName

internal val PanCardBg = Color(0xFFFFFFFF)
internal val PanInk = Color(0xFF222222)
internal val PanMuted = Color(0xFF8A8578)
internal val PanLine = Color(0xFFE2DED2)
internal val PanAccent = Color(0xFF26A6A6)
internal val YongShenGold = Color(0xFFD9A441)
internal val ShiMark = Color(0xFFB3261E)
internal val PillarRed = Color(0xFFD32F2F)

/** 各列固定宽度（dp），保证每行、每卦都严格对齐 */
private val W_SHEN = 26.dp   // 六神
private val W_KIN = 28.dp    // 六亲
private val W_GZ = 36.dp     // 干支
private val W_WX = 16.dp     // 五行
private val W_YAO = 52.dp    // 卦象（爻画 + 动变标记）
private val W_SY = 22.dp     // 世 / 应 / 用

/** 单爻横画：阳爻一横，阴爻两段；动爻右侧附 ○/×。动变标记常驻占位，保证爻画列对齐。 */
@Composable
fun YaoGlyph(
    isYang: Boolean,
    mark: String?,
    modifier: Modifier = Modifier,
    width: Dp = 32.dp,
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
        Spacer(Modifier.width(3.dp))
        // 动变标记常驻占位：动爻显示 ○/×，静爻留白，使爻画横向位置恒定
        Box(Modifier.width(14.dp), contentAlignment = Alignment.CenterStart) {
            Text(mark ?: "", color = ShiMark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** 列头：卦名（宫·世系），如「地水师（坎宫·归魂卦）」。 */
private fun hexTitle(entry: HexagramEntry): String =
    "${entry.fullName}（${entry.palace.label}宫·${entry.type.label}卦）"

/**
 * 排盘卡片：本卦（及变卦）以固定列宽表格呈现，列标题为
 * 「六神 六亲 干支 五行 卦象 世应」，自上而下 上爻→初爻。
 *
 * 响应式：内容宽度足够时本卦与变卦左右并排；不足（手机）时上下堆叠，保证横向永不放不下。
 */
@Composable
fun ChartCard(chart: LiuYaoChart, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(PanCardBg, RoundedCornerShape(12.dp))
            .border(1.dp, PanLine, RoundedCornerShape(12.dp))
            .padding(10.dp),
    ) {
        val sideBySide = chart.changed != null && maxWidth >= 350.dp
        if (sideBySide && chart.changed != null) {
            Row(modifier = Modifier.fillMaxWidth()) {
                HexTable(
                    title = hexTitle(chart.original.entry),
                    hex = chart.original,
                    movingPositions = chart.movingPositions,
                    withShen = true,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(PanLine),
                )
                HexTable(
                    title = hexTitle(chart.changed!!.entry),
                    hex = chart.changed!!,
                    movingPositions = chart.movingPositions,
                    withShen = false,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HexTable(
                    title = hexTitle(chart.original.entry),
                    hex = chart.original,
                    movingPositions = chart.movingPositions,
                    withShen = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                chart.changed?.let { changed ->
                    HorizontalDivider(thickness = 0.5.dp, color = PanLine)
                    HexTable(
                        title = hexTitle(changed.entry),
                        hex = changed,
                        movingPositions = chart.movingPositions,
                        withShen = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } ?: Text("静卦无动爻，无变卦。", color = PanMuted, fontSize = 12.sp)
            }
        }
    }
}

/** 单卦表：标题 + 列头 + 六爻（上→初）。 */
@Composable
private fun HexTable(
    title: String,
    hex: InstalledHexagram,
    movingPositions: List<Int>,
    withShen: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(title, color = PanInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        HeaderRow(withShen)
        Spacer(Modifier.height(3.dp))
        hex.yaos.sortedByDescending { it.position }.forEach { yao ->
            DataRow(
                yao = yao,
                moved = yao.position in movingPositions,
                withShen = withShen,
            )
            Spacer(Modifier.height(3.dp))
        }
    }
}

@Composable
private fun HeaderRow(withShen: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (withShen) Cell("六神", W_SHEN, PanMuted, 11.sp, true)
        Cell("六亲", W_KIN, PanMuted, 11.sp, true)
        Cell("干支", W_GZ, PanMuted, 11.sp, true)
        Cell("五行", W_WX, PanMuted, 11.sp, true)
        Cell("卦象", W_YAO, PanMuted, 11.sp, true)
        Cell("世应", W_SY, PanMuted, 11.sp, true)
    }
}

@Composable
private fun DataRow(yao: InstalledYao, moved: Boolean, withShen: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (withShen) {
            Cell(yao.sixGod?.label ?: "　", W_SHEN, PanMuted, 11.sp)
        }
        val yong = yao.isYongShen
        Cell(
            text = yao.sixKin.label,
            width = W_KIN,
            color = if (yong) YongShenGold else PanInk,
            size = 12.sp,
            bold = yong,
        )
        Cell(yao.ganzhi.label, W_GZ, PanInk, 12.sp)
        Cell(yao.ganzhi.element.label, W_WX, PanMuted, 11.sp)
        Box(Modifier.width(W_YAO), contentAlignment = Alignment.CenterStart) {
            YaoGlyph(isYang = yao.isYang, mark = yao.kind?.mark)
        }
        Cell(shiYingMark(yao), W_SY, ShiMark, 12.sp, bold = yong)
    }
}

/** 固定列宽单元格：文字居中，跨行严格对齐。 */
@Composable
private fun Cell(
    text: String,
    width: Dp,
    color: Color,
    size: androidx.compose.ui.unit.TextUnit,
    bold: Boolean = false,
) {
    Box(Modifier.width(width), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = color,
            fontSize = size,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
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
