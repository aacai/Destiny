package zhiqiu.xiaoliuren.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.xiaoliuren.EARTHLY_BRANCHES
import zhiqiu.xiaoliuren.MAX_COUNT
import zhiqiu.xiaoliuren.Palace
import zhiqiu.xiaoliuren.XiaoLiuRenChart
import zhiqiu.xiaoliuren.branchHourRange
import zhiqiu.xiaoliuren.cast
import zhiqiu.xiaoliuren.castCounts
import zhiqiu.xiaoliuren.castNow
import zhiqiu.xiaoliuren.lunarDayLabel

private enum class TimeMode(val label: String) {
    NOW("此刻"),
    LUNAR("农历"),
    COUNTS("报数"),
}

/**
 * 小六壬（马前课）起课页：农历月日起时辰，三步顺数定落宫。
 * 起课法与断语依倪海厦《天纪》口授，配左手三指六宫掌诀图。
 */
@Composable
fun XiaoLiuRenScreen(
    onBack: () -> Unit,
    darkTheme: Boolean = false,
    themeConfig: XlrThemeConfig = XlrThemeConfig.Default,
) {
    XlrThemeProvider(darkTheme = darkTheme, config = themeConfig) {
        ScreenContent(onBack = onBack)
    }
}

@Composable
private fun ScreenContent(onBack: () -> Unit) {
    val now = remember { castNow() }
    val seed = requireNotNull(now.lunar)
    var mode by remember { mutableStateOf(TimeMode.NOW) }
    var question by remember { mutableStateOf("") }
    var lunarMonth by remember { mutableIntStateOf(seed.month) }
    var lunarDay by remember { mutableIntStateOf(seed.day) }
    var branch by remember { mutableIntStateOf(seed.hourBranchIndex) }
    var leap by remember { mutableStateOf(seed.isLeapMonth) }
    var countMonth by remember { mutableStateOf(now.monthCount.toString()) }
    var countDay by remember { mutableStateOf(now.dayCount.toString()) }
    var countHour by remember { mutableStateOf(now.hourCount.toString()) }
    // 「此刻」每次进入或点刷新都重新取设备时间
    var snapshot by remember { mutableStateOf(now) }
    var showRule by remember { mutableStateOf(false) }

    val reported = listOf(countMonth, countDay, countHour).map { reportedNumber(it) }
    val chart = when (mode) {
        TimeMode.NOW -> snapshot
        TimeMode.LUNAR -> cast(lunarMonth, lunarDay, branch, leap)
        TimeMode.COUNTS -> castCounts(reported[0], reported[1], reported[2])
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XlrTheme.page)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "‹ 返回",
                color = XlrTheme.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text("小六壬 · 马前课", color = XlrTheme.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(XlrTheme.panel)
                    .border(1.dp, XlrTheme.line, CircleShape)
                    .clickable { showRule = true },
                contentAlignment = Alignment.Center,
            ) {
                Text("？", color = XlrTheme.muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SectionCard("占问") {
            BasicTextField(
                value = question,
                onValueChange = { question = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = XlrTheme.ink),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(XlrTheme.panel)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    Box {
                        if (question.isEmpty()) {
                            Text("此行如何／何时有回音……（可留空）", color = XlrTheme.muted, fontSize = 14.sp)
                        }
                        inner()
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SectionCard("起课方式") {
            SegmentedControl(
                options = TimeMode.entries.map { it.label },
                selectedIndex = TimeMode.entries.indexOf(mode),
                onSelect = { i ->
                    val next = TimeMode.entries[i]
                    if (next == TimeMode.NOW) {
                        val fresh = castNow()
                        val freshLunar = requireNotNull(fresh.lunar)
                        snapshot = fresh
                        lunarMonth = freshLunar.month
                        lunarDay = freshLunar.day
                        branch = freshLunar.hourBranchIndex
                        leap = freshLunar.isLeapMonth
                    }
                    // 切到报数时把当前课式的三数带过去，省得重填
                    if (next == TimeMode.COUNTS) {
                        countMonth = chart.monthCount.toString()
                        countDay = chart.dayCount.toString()
                        countHour = chart.hourCount.toString()
                    }
                    mode = next
                },
            )
            Spacer(modifier = Modifier.height(10.dp))

            when (mode) {
                TimeMode.NOW -> {
                    Text(snapshot.solarText, color = XlrTheme.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(snapshot.castText, color = XlrTheme.muted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "↻ 重新取此刻",
                        color = XlrTheme.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { snapshot = castNow() }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }

                TimeMode.LUNAR -> {
                    FieldCaption("农历月")
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        MONTH_LABELS.forEachIndexed { i, label ->
                            Chip(label, selected = lunarMonth == i + 1) { lunarMonth = i + 1 }
                        }
                        Chip("闰", selected = leap) { leap = !leap }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldCaption("农历日")
                    Spacer(modifier = Modifier.height(6.dp))
                    Stepper(value = lunarDay, range = 1..30, display = lunarDayLabel(lunarDay), onChange = { lunarDay = it })
                    Spacer(modifier = Modifier.height(12.dp))
                    FieldCaption("时辰")
                    Spacer(modifier = Modifier.height(6.dp))
                    EARTHLY_BRANCHES.chunked(6).forEachIndexed { rowIdx, names ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            names.forEachIndexed { colIdx, name ->
                                val idx = rowIdx * 6 + colIdx
                                BranchChip(
                                    name = name,
                                    range = branchHourRange(idx),
                                    selected = branch == idx,
                                ) { branch = idx }
                            }
                        }
                    }
                }

                TimeMode.COUNTS -> {
                    Text("随口报三个数，依次作月、日、时之数；顺数掌诀，不问农历。", color = XlrTheme.muted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ReportedField("第一数", "月", countMonth) { countMonth = it }
                        ReportedField("第二数", "日", countDay) { countDay = it }
                        ReportedField("第三数", "时", countHour) { countHour = it }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "所报 ${reported[0]}、${reported[1]}、${reported[2]}（1–$MAX_COUNT）",
                        color = XlrTheme.muted,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SectionCard("左手掌诀") {
            HandBoard(chart = chart)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "掌上 ①→⑥ 自大安顺时针一周：大安起数至月，月落宫数至日，日落宫数至时，只断最终落宫。",
                color = XlrTheme.muted,
                fontSize = 11.sp,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SectionCard("三步推演") {
            chart.steps.forEachIndexed { i, step ->
                if (i > 0) {
                    HorizontalDivider(color = XlrTheme.line, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(step.title, color = XlrTheme.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(34.dp))
                    Text(step.countText, color = XlrTheme.muted, fontSize = 13.sp, modifier = Modifier.width(84.dp))
                    Text("自 ${step.start.label} 顺数 ${step.count}", color = XlrTheme.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("→ ${step.land.label}", color = XlrTheme.palaceColor(step.land.luck.auspicious), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        VerdictCard(chart = chart)
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showRule) {
        RuleDialog(chart = chart, onDismiss = { showRule = false })
    }
}

/** 取数来源、推演法则与六宫速查；文案与 core 的 castCounts／advanceFrom 同式。 */
@Composable
private fun RuleDialog(chart: XiaoLiuRenChart, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 520.dp),
        containerColor = XlrTheme.card,
        tonalElevation = 0.dp,
        titleContentColor = XlrTheme.ink,
        textContentColor = XlrTheme.ink,
        title = { Text("取数与规则", color = XlrTheme.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                RuleSection("取数") {
                    Bullet("此刻", "取设备时间的农历月、农历日与时辰。")
                    Bullet("农历", "手选农历月日与时辰；闰月按本月计，只标注、不改落宫。")
                    Bullet("报数", "随口报三个数，依次作月、日、时之数，推演与农历同式。")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("时辰之数：子时为一，顺至亥时为十二，故辰时数五位。", color = XlrTheme.muted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "本课 ${chart.castText} → 三数 ${chart.monthCount}・${chart.dayCount}・${chart.hourCount}",
                        color = XlrTheme.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                RuleSection("推演") {
                    Text(
                        "掌诀六位顺时针一周：大安 → 留连 → 速喜 → 赤口 → 小吉 → 空亡，再回大安。",
                        color = XlrTheme.ink,
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "自起算宫起记「一」，顺数 N 位即落第 N 宫；满六位回绕，故落宫序 =（起算序 + N − 1）mod 6。",
                        color = XlrTheme.ink,
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    chart.steps.forEachIndexed { i, step ->
                        Text(
                            "${i + 1}. ${step.start.label}起，数至${step.countText}（${step.count} 位）→ ${step.land.label}",
                            color = XlrTheme.muted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "只以第三步落宫断事，月宫、日宫是中途立足点，不单独论吉凶。",
                        color = XlrTheme.muted,
                        fontSize = 12.sp,
                    )
                }
                RuleSection("六宫") {
                    Palace.ORDER.forEachIndexed { i, palace ->
                        val tone = XlrTheme.palaceColor(palace.luck.auspicious)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(ORDER_MARKS[i], color = XlrTheme.muted, fontSize = 12.sp, modifier = Modifier.width(18.dp))
                            Text(
                                palace.label,
                                color = tone,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(52.dp),
                            )
                            Text(
                                "${palace.wuxing}·${palace.beast} ${palace.direction}",
                                color = XlrTheme.muted,
                                fontSize = 11.sp,
                                modifier = Modifier.weight(1f),
                            )
                            Text(palace.state, color = XlrTheme.muted, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                            Tag(palace.luck.label, tone)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "谋事之数取各宫本数：大安、小吉一五七，留连二八十，速喜、空亡三六九，赤口四七十。",
                        color = XlrTheme.muted,
                        fontSize = 11.sp,
                    )
                }
                RuleSection("出处") {
                    Text(
                        "起课法与断语依倪海厦《天纪》口授，断曰取自通行《小六壬》口诀；推演一按原式，未增删吉凶。",
                        color = XlrTheme.muted,
                        fontSize = 11.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = XlrTheme.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

@Composable
private fun RuleSection(title: String, content: @Composable () -> Unit) {
    Text(title, color = XlrTheme.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    content()
    Spacer(modifier = Modifier.height(14.dp))
}

@Composable
private fun Bullet(term: String, detail: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Text(term, color = XlrTheme.ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(42.dp))
        Text(detail, color = XlrTheme.muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun VerdictCard(chart: XiaoLiuRenChart) {
    val p = chart.palace
    val tone = XlrTheme.palaceColor(p.luck.auspicious)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(XlrTheme.card)
            .border(1.dp, tone.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("落宫", color = XlrTheme.muted, fontSize = 12.sp, modifier = Modifier.width(40.dp))
            Text(p.label, color = tone, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(10.dp))
            Tag(p.luck.label, tone)
        }
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = XlrTheme.line, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Meta("五行", p.wuxing)
            Meta("六神", p.beast)
            Meta("方位", p.direction)
            Meta("谋事", p.matterNumbers)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("状态", color = XlrTheme.muted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(p.state, color = XlrTheme.ink, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Text("倪师断", color = XlrTheme.muted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(p.niMaster, color = tone, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        Text("断曰", color = XlrTheme.muted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        p.poem.forEach { line ->
            Text(line, color = XlrTheme.ink, fontSize = 13.sp, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

/** 三指六宫：食指、中指、无名指各两节，指尖在上、指根在下，食指根起大安顺时针一周。 */
@Composable
private fun HandBoard(chart: XiaoLiuRenChart) {
    val fingers = listOf(
        Finger("食指", tip = Palace.LIU_LIAN, root = Palace.DA_AN),
        Finger("中指", tip = Palace.SU_XI, root = Palace.KONG_WANG),
        Finger("无名指", tip = Palace.CHI_KOU, root = Palace.XIAO_JI),
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            fingers.forEach { finger ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Slot(finger.tip, chart, TIP_SHAPE)
                    Spacer(modifier = Modifier.height(3.dp))
                    Slot(finger.root, chart, ROOT_SHAPE)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(XlrTheme.line.copy(alpha = 0.6f)),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            fingers.forEach { finger ->
                Text(
                    finger.name,
                    color = XlrTheme.muted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private class Finger(val name: String, val tip: Palace, val root: Palace)

private val TIP_SHAPE = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
private val ROOT_SHAPE = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
private val ORDER_MARKS = listOf("①", "②", "③", "④", "⑤", "⑥")

@Composable
private fun Slot(palace: Palace, chart: XiaoLiuRenChart, shape: RoundedCornerShape) {
    val tone = XlrTheme.palaceColor(palace.luck.auspicious)
    val landed = palace == chart.hourPalace
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (landed) tone.copy(alpha = 0.22f) else XlrTheme.panel)
            .border(
                width = if (landed) 2.dp else 1.dp,
                color = if (landed) tone else XlrTheme.line,
                shape = shape,
            )
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "${ORDER_MARKS[palace.ordinal]} ${palace.label}",
            color = if (landed) tone else XlrTheme.ink,
            fontSize = 15.sp,
            fontWeight = if (landed) FontWeight.Bold else FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text("${palace.wuxing}·${palace.beast}", color = XlrTheme.muted, fontSize = 10.sp)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(XlrTheme.card)
            .border(1.dp, XlrTheme.line.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(3.dp).height(15.dp).clip(RoundedCornerShape(2.dp)).background(XlrTheme.accent))
            Spacer(modifier = Modifier.width(7.dp))
            Text(title, color = XlrTheme.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(XlrTheme.panel)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val active = i == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) XlrTheme.card else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (active) XlrTheme.accent else XlrTheme.muted,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) XlrTheme.accent.copy(alpha = 0.14f) else XlrTheme.panel)
            .border(1.dp, if (selected) XlrTheme.accent else XlrTheme.line, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
    ) {
        Text(
            text,
            color = if (selected) XlrTheme.accent else XlrTheme.muted,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun RowScope.ReportedField(caption: String, role: String, text: String, onChange: (String) -> Unit) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(caption, color = XlrTheme.muted, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(4.dp))
        BasicTextField(
            value = text,
            onValueChange = { onChange(it.filter(Char::isDigit).take(3)) },
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = XlrTheme.ink,
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(XlrTheme.accent),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(XlrTheme.panel)
                .border(1.dp, XlrTheme.line, RoundedCornerShape(10.dp))
                .padding(vertical = 8.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(role, color = XlrTheme.muted, fontSize = 10.sp)
    }
}

/** 报数只取数字，空或越界时回落到 1，保证任何时候都有课式。 */
private fun reportedNumber(text: String): Int =
    text.toIntOrNull()?.coerceIn(1, MAX_COUNT) ?: 1

@Composable
private fun BranchChip(name: String, range: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) XlrTheme.accent.copy(alpha = 0.14f) else XlrTheme.panel)
            .border(1.dp, if (selected) XlrTheme.accent else XlrTheme.line, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 0.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            name,
            color = if (selected) XlrTheme.accent else XlrTheme.ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Text(range, color = XlrTheme.muted, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 12.dp))
    }
}

@Composable
private fun Stepper(value: Int, range: IntRange, display: String, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StepButton("−") { onChange(if (value > range.first) value - 1 else range.last) }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(XlrTheme.panel)
                .border(1.dp, XlrTheme.line, RoundedCornerShape(10.dp))
                .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Text(display, color = XlrTheme.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        StepButton("＋") { onChange(if (value < range.last) value + 1 else range.first) }
        Text(
            "$value／$display",
            color = XlrTheme.muted,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(XlrTheme.panel)
            .border(1.dp, XlrTheme.line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = XlrTheme.ink, fontSize = 16.sp)
    }
}

@Composable
private fun Tag(text: String, tone: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(tone.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(text, color = tone, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Meta(label: String, value: String) {
    Column {
        Text(label, color = XlrTheme.muted, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = XlrTheme.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FieldCaption(text: String) {
    Text(text, color = XlrTheme.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
}

private val MONTH_LABELS = listOf("正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月")
