package zhiqiu.qimen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.qimen.Door
import zhiqiu.qimen.DoorTone
import zhiqiu.qimen.Palace
import zhiqiu.qimen.PlateKind
import zhiqiu.qimen.QimenChart
import zhiqiu.qimen.Stem

/** 上南下北、左东右西。 */
private val GRID = listOf(
    listOf(Palace.Xun, Palace.Li, Palace.Kun),
    listOf(Palace.Zhen, Palace.Zhong, Palace.Dui),
    listOf(Palace.Gen, Palace.Kan, Palace.Qian),
)

private val CellHeight = 108.dp

@Composable
fun QimenBoard(
    chart: QimenChart,
    selected: Palace?,
    onSelect: (Palace) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, QimenTheme.line, RoundedCornerShape(12.dp)),
    ) {
        GRID.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().height(CellHeight)) {
                row.forEach { palace ->
                    PalaceCellView(
                        chart = chart,
                        palace = palace,
                        selected = palace == selected,
                        onClick = { onSelect(palace) },
                        modifier = Modifier.weight(1f).fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PalaceCellView(
    chart: QimenChart,
    palace: Palace,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val summary = chart.plate == PlateKind.Zhuan && palace == Palace.Zhong
    Column(
        modifier = modifier
            .border(
                width = if (selected) 1.5.dp else 0.5.dp,
                color = if (selected) QimenTheme.accent else QimenTheme.line,
            )
            .background(if (selected) QimenTheme.accent.copy(alpha = 0.08f) else QimenTheme.card)
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 3.dp),
        verticalArrangement = if (summary) Arrangement.Center else Arrangement.Top,
        horizontalAlignment = if (summary) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        if (summary) {
            CenterSummary(chart)
        } else {
            OuterCell(chart, palace)
        }
    }
}

@Composable
private fun CenterSummary(chart: QimenChart) {
    Text(chart.dunText, color = QimenTheme.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Text(chart.yuanText, color = QimenTheme.muted, fontSize = 10.sp)
    Text("值符 ${chart.zhiFu.label}", color = QimenTheme.ink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    Text("值使 ${chart.zhiShi.full}", color = QimenTheme.ink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    Text("${chart.centerEarth.label}寄坤", color = stemColor(chart.centerEarth), fontSize = 10.sp)
}

@Composable
private fun OuterCell(chart: QimenChart, palace: Palace) {
    val cell = chart.cell(palace)
    val marks = cell.marks.joinToString("") { it.label }
    Line(
        left = cell.deity?.label(chart.yang) ?: "—",
        right = marks.ifEmpty { "" },
        leftColor = QimenTheme.ink,
        rightColor = QimenTheme.unlucky,
        leftBold = false,
        rightBold = false,
    )
    repeat(2) { index ->
        val star = cell.stars.getOrNull(index)
        val stem = cell.heavenStems.getOrNull(index)
        Line(
            left = star?.label ?: "",
            right = stem?.label ?: "",
            leftColor = QimenTheme.ink,
            rightColor = stem?.let { stemColor(it) } ?: QimenTheme.ink,
            leftBold = star == chart.zhiFu,
            rightBold = stem?.sanQi == true,
        )
    }
    Line(
        left = cell.door?.full ?: "—",
        right = cell.earthStems.getOrNull(0)?.label ?: "",
        leftColor = cell.door?.let { doorColor(it) } ?: QimenTheme.muted,
        rightColor = cell.earthStems.getOrNull(0)?.let { stemColor(it) } ?: QimenTheme.ink,
        leftBold = cell.door == chart.zhiShi,
        rightBold = cell.earthStems.getOrNull(0)?.sanQi == true,
    )
    Line(
        left = "",
        right = cell.earthStems.getOrNull(1)?.label ?: "",
        leftColor = QimenTheme.ink,
        rightColor = cell.earthStems.getOrNull(1)?.let { stemColor(it) } ?: QimenTheme.ink,
        leftBold = false,
        rightBold = cell.earthStems.getOrNull(1)?.sanQi == true,
    )
    Text(
        palace.shortName,
        color = QimenTheme.muted,
        fontSize = 9.sp,
        textAlign = TextAlign.End,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Line(
    left: String,
    right: String,
    leftColor: Color,
    rightColor: Color,
    leftBold: Boolean,
    rightBold: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            left,
            color = leftColor,
            fontSize = 10.sp,
            fontWeight = if (leftBold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.weight(1f),
        )
        Text(
            right,
            color = rightColor,
            fontSize = 10.sp,
            fontWeight = if (rightBold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@Composable
private fun stemColor(stem: Stem): Color = if (stem.sanQi) QimenTheme.sanQi else QimenTheme.ink

@Composable
private fun doorColor(door: Door): Color = when (door.tone) {
    DoorTone.Lucky -> QimenTheme.lucky
    DoorTone.Unlucky -> QimenTheme.unlucky
    DoorTone.Neutral -> QimenTheme.neutral
}
