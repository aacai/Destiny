package zhiqiu.meihua.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import zhiqiu.app.destiny.oinkcompat.AlertDialog
import zhiqiu.app.destiny.oinkcompat.HorizontalDivider
import zhiqiu.app.destiny.oinkcompat.Button
import zhiqiu.app.destiny.oinkcompat.Text
import zhiqiu.app.destiny.oinkcompat.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.liuyao.Trigram
import zhiqiu.meihua.BRANCH_LABELS
import zhiqiu.meihua.DEFAULT_CONVENTIONS
import zhiqiu.meihua.MAX_NUMBER
import zhiqiu.meihua.MeiHuaChart
import zhiqiu.meihua.Posture
import zhiqiu.meihua.castFromLunar
import zhiqiu.meihua.castFromNumbers
import zhiqiu.meihua.castFromXiang
import zhiqiu.meihua.castManual
import zhiqiu.meihua.castNow
import zhiqiu.meihua.castRandom
import zhiqiu.meihua.innateNumber

private enum class CastTab(val label: String) {
    NOW(MeiHuaStrings.t("now")),
    LUNAR(MeiHuaStrings.t("lunar")),
    NUMBERS(MeiHuaStrings.t("numbers")),
    XIANG("端法"),
    MANUAL("手选"),
    RANDOM("随机"),
}

/**
 * 梅花易数起卦页：时间 / 数字 / 端法 / 手选 / 随机；结果区一本变互错综点选详情。
 */
/** 梅花的一次存档：中性字段，由宿主组装成档案。 */
data class MeiHuaSave(
    val id: String,
    val name: String,
    val note: String,
    val groupName: String,
    val tab: String = "NOW",
    val question: String = "",
    val lunarMonth: Int = 1,
    val lunarDay: Int = 1,
    val hourBranch: Int = 1,
    val yearBranch: Int = 1,
    val leap: Boolean = false,
    val upperNum: String = "3",
    val lowerNum: String = "5",
    val summary: String = "",
)

@Composable
fun MeiHuaScreen(
    onBack: () -> Unit,
    darkTheme: Boolean = false,
    themeConfig: MhThemeConfig = MhThemeConfig.Default,
    onSave: (MeiHuaSave) -> Unit = {},
) {
    MhThemeProvider(darkTheme = darkTheme, config = themeConfig) {
        ScreenContent(onBack = onBack, onSave = onSave)
    }
}

/** 从存档的结构化数据只读重播梅花卦例，展示与起卦页同款结果板。 */
@Composable
fun MeiHuaCaseViewer(
    tab: String,
    question: String,
    lunarMonth: Int,
    lunarDay: Int,
    hourBranch: Int,
    yearBranch: Int,
    leap: Boolean,
    upperNum: String,
    lowerNum: String,
    summary: String,
    onBack: () -> Unit = {},
    onEdit: (() -> Unit)? = null,
    noteSection: (@Composable () -> Unit)? = null,
    darkTheme: Boolean = false,
    themeConfig: MhThemeConfig = MhThemeConfig.Default,
) {
    MhThemeProvider(darkTheme = darkTheme, config = themeConfig) {
        val rebuildable = tab == "NOW" || tab == "LUNAR" || tab == "NUMBERS"
        val chart = remember(tab, question, lunarMonth, lunarDay, hourBranch, yearBranch, leap, upperNum, lowerNum) {
            if (!rebuildable) null else runCatching {
                if (tab == "NUMBERS") {
                    castFromNumbers(parseNum(upperNum), parseNum(lowerNum), lunarMonth, question)
                } else {
                    castFromLunar(yearBranch, lunarMonth, lunarDay, hourBranch, leap, question)
                }
            }.getOrNull()
        }
        Column(modifier = Modifier.fillMaxSize().background(MhTheme.page)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text(MeiHuaStrings.backShort, color = MhTheme.accent, fontSize = 15.sp) }
                Text("卦例详情", color = MhTheme.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (onEdit != null) {
                    TextButton(onClick = onEdit) { Text(MeiHuaStrings.edit, color = MhTheme.accent, fontSize = 13.sp) }
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = MhTheme.line)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                if (question.isNotBlank()) {
                    Text("占问：$question", color = MhTheme.ink, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (chart != null) {
                    ResultBoard(chart = chart, expandTrace = true, onToggleTrace = {})
                } else if (summary.isNotBlank()) {
                    Text(summary, color = MhTheme.ink, fontSize = 14.sp)
                }
                if (noteSection != null) noteSection()
            }
        }
    }
}

@Composable
private fun ScreenContent(onBack: () -> Unit, onSave: (MeiHuaSave) -> Unit) {
    val seed = remember { castNow() }
    val seedStamp = seed.stamp
    var tab by remember { mutableStateOf(CastTab.NOW) }
    var question by remember { mutableStateOf("") }
    var noteDraft by remember { mutableStateOf<String?>(null) }
    var showRule by remember { mutableStateOf(false) }
    var expandTrace by remember { mutableStateOf(true) }

    var snapshot by remember { mutableStateOf(seed) }
    var yearBranch by remember { mutableIntStateOf(seedStamp?.yearBranchNum ?: 1) }
    var lunarMonth by remember { mutableIntStateOf(seedStamp?.month ?: 1) }
    var lunarDay by remember { mutableIntStateOf(seedStamp?.day ?: 1) }
    var hourBranch by remember { mutableIntStateOf(seedStamp?.hourBranchNum ?: 1) }
    var leap by remember { mutableStateOf(seedStamp?.isLeapMonth ?: false) }

    var upperNum by remember { mutableStateOf("3") }
    var lowerNum by remember { mutableStateOf("5") }

    var xiangUpper by remember { mutableStateOf(Trigram.QIAN) }
    var xiangLower by remember { mutableStateOf(Trigram.XUN) }
    var xiangHour by remember { mutableIntStateOf(seedStamp?.hourBranchNum ?: 4) }
    var posture by remember { mutableStateOf(Posture.STANDING) }

    var manualUpper by remember { mutableStateOf(Trigram.LI) }
    var manualLower by remember { mutableStateOf(Trigram.XUN) }
    var manualYao by remember { mutableIntStateOf(2) }

    var randomChart by remember { mutableStateOf(castRandom(question = question)) }

    val chart = when (tab) {
        CastTab.NOW -> snapshot
        CastTab.LUNAR -> castFromLunar(
            yearBranchNum = yearBranch,
            lunarMonth = lunarMonth,
            lunarDay = lunarDay,
            hourBranchNum = hourBranch,
            isLeapMonth = leap,
            question = question,
        )
        CastTab.NUMBERS -> castFromNumbers(
            upperNumber = parseNum(upperNum),
            lowerNumber = parseNum(lowerNum),
            lunarMonthForWang = lunarMonth,
            question = question,
        )
        CastTab.XIANG -> castFromXiang(
            upper = xiangUpper,
            lower = xiangLower,
            hourBranchNum = xiangHour,
            posture = posture,
            stamp = seedStamp?.copy(hourBranchNum = xiangHour, hourBranchLabel = BRANCH_LABELS[xiangHour - 1]),
            question = question,
        )
        CastTab.MANUAL -> castManual(
            upper = manualUpper,
            lower = manualLower,
            movingYao = manualYao,
            lunarMonthForWang = lunarMonth,
            question = question,
        )
        CastTab.RANDOM -> randomChart
    }
    val autoNote = buildString {
        if (question.isNotBlank()) appendLine("【占问】${question.trim()}")
        appendLine("【起卦】${chart.trace.summary}")
        appendLine("【体用】体${chart.ti.label} 用${chart.yong.label} · ${chart.relation.label}")
        appendLine("【卦象】${chart.upper.label}上 ${chart.lower.label}下")
    }
    val noteText = noteDraft ?: autoNote

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MhTheme.page),
    ) {
        // 顶栏固定，不随正文滚动
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            TopBar(onBack = onBack, onRule = { showRule = true })
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
                .padding(bottom = 10.dp),
        ) {
        Spacer(modifier = Modifier.height(4.dp))

        SectionCard(MeiHuaStrings.question) {
            BasicTextField(
                value = question,
                onValueChange = { question = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = MhTheme.ink),
                cursorBrush = SolidColor(MhTheme.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MhTheme.panel)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    Box {
                        if (question.isEmpty()) {
                            Text("所问何事（可留空）", color = MhTheme.muted, fontSize = 14.sp)
                        }
                        inner()
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        SectionCard("起卦方式") {
            SegmentedControl(
                options = CastTab.entries.map { it.label },
                selectedIndex = CastTab.entries.indexOf(tab),
                onSelect = { i ->
                    val next = CastTab.entries[i]
                    if (next == CastTab.NOW) {
                        snapshot = castNow(question)
                        snapshot.stamp?.let { s ->
                            yearBranch = s.yearBranchNum
                            lunarMonth = s.month
                            lunarDay = s.day
                            hourBranch = s.hourBranchNum
                            leap = s.isLeapMonth
                        }
                    }
                    if (next == CastTab.RANDOM) {
                        randomChart = castRandom(lunarMonthForWang = lunarMonth, question = question)
                    }
                    tab = next
                },
            )
            Spacer(modifier = Modifier.height(10.dp))
            when (tab) {
                CastTab.NOW -> {
                    Text(snapshot.stamp?.solarText.orEmpty(), color = MhTheme.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(snapshot.stamp?.lunarText.orEmpty(), color = MhTheme.muted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    AccentAction("↻ 重新取此刻") {
                        snapshot = castNow(question)
                    }
                }
                CastTab.LUNAR -> LunarEditors(
                    yearBranch = yearBranch,
                    onYear = { yearBranch = it },
                    month = lunarMonth,
                    onMonth = { lunarMonth = it },
                    day = lunarDay,
                    onDay = { lunarDay = it },
                    hour = hourBranch,
                    onHour = { hourBranch = it },
                    leap = leap,
                    onLeap = { leap = it },
                )
                CastTab.NUMBERS -> {
                    Text("报两个正整数：上数定上卦，下数定下卦，两数之和定动爻。", color = MhTheme.muted, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumField("上数", upperNum) { upperNum = it }
                        NumField("下数", lowerNum) { lowerNum = it }
                    }
                }
                CastTab.XIANG -> XiangEditors(
                    upper = xiangUpper,
                    onUpper = { xiangUpper = it },
                    lower = xiangLower,
                    onLower = { xiangLower = it },
                    hour = xiangHour,
                    onHour = { xiangHour = it },
                    posture = posture,
                    onPosture = { posture = it },
                )
                CastTab.MANUAL -> ManualEditors(
                    upper = manualUpper,
                    onUpper = { manualUpper = it },
                    lower = manualLower,
                    onLower = { manualLower = it },
                    yao = manualYao,
                    onYao = { manualYao = it },
                )
                CastTab.RANDOM -> {
                    Text(randomChart.trace.summary, color = MhTheme.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(10.dp))
                    AccentAction("↻ 再摇一次") {
                        randomChart = castRandom(lunarMonthForWang = lunarMonth, question = question)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        ResultBoard(chart = chart, expandTrace = expandTrace, onToggleTrace = { expandTrace = !expandTrace })
        Spacer(modifier = Modifier.height(12.dp))
        Text(MeiHuaStrings.note, color = MhTheme.muted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(6.dp))
        BasicTextField(
            value = noteText,
            onValueChange = { noteDraft = it },
            textStyle = TextStyle(fontSize = 13.sp, color = MhTheme.ink, lineHeight = 19.sp),
            cursorBrush = SolidColor(MhTheme.accent),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 110.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MhTheme.panel)
                .padding(12.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val id = buildString { repeat(32) { append("abcdefghijklmnopqrstuvwxyz0123456789"[kotlin.random.Random.nextInt(36)]) } }
            onSave(
                MeiHuaSave(
                    id = id,
                    name = question.trim().ifBlank { "梅花·${chart.upper.label}上${chart.lower.label}下" },
                    note = noteText,
                    groupName = "梅花",
                    tab = tab.name, question = question,
                    lunarMonth = lunarMonth, lunarDay = lunarDay,
                    hourBranch = hourBranch, yearBranch = yearBranch, leap = leap,
                    upperNum = upperNum, lowerNum = lowerNum, summary = chart.trace.summary,
                ),
            )
        }) {
            Text(MeiHuaStrings.saveGua, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(24.dp))
        } // scroll body
    }

    if (showRule) {
        RuleDialog(onDismiss = { showRule = false })
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, onRule: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            MeiHuaStrings.backShort,
            color = MhTheme.accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onBack)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Spacer(modifier = Modifier.weight(1f))
        Text("梅花易数", color = MhTheme.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            MeiHuaStrings.rule,
            color = MhTheme.accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onRule)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/** 结果页五卦：本 / 变 / 互 / 错 / 综 */
private enum class GuaKind(val label: String) {
    BEN("本"),
    BIAN("变"),
    HU("互"),
    CUO("错"),
    ZONG("综"),
}

@Composable
private fun ResultBoard(chart: MeiHuaChart, expandTrace: Boolean, onToggleTrace: () -> Unit) {
    var selected by remember(chart) { mutableStateOf(GuaKind.BEN) }

    SectionCard("取数算式") {
        Text(
            if (expandTrace) "收起明细 ▴" else "展开明细 ▾",
            color = MhTheme.accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onToggleTrace),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(chart.trace.summary, color = MhTheme.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        if (expandTrace) {
            Spacer(modifier = Modifier.height(8.dp))
            chart.trace.lines.forEach { line ->
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    Text(line.title, color = MhTheme.muted, fontSize = 12.sp, modifier = Modifier.width(64.dp))
                    Text(line.detail, color = MhTheme.ink, fontSize = 12.sp, modifier = Modifier.weight(1f))
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))

    // 一区五卦：点选下方出详情
    SectionCard("卦象") {
        Text(
            "${chart.movingYaoName}动　上${chart.upper.label}下${chart.lower.label}",
            color = MhTheme.muted,
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GuaKind.entries.forEach { kind ->
                GuaPickCell(
                    kind = kind,
                    chart = chart,
                    selected = selected == kind,
                    onClick = { selected = kind },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = MhTheme.line)
        Spacer(modifier = Modifier.height(12.dp))
        GuaDetailPane(kind = selected, chart = chart)
    }

    val cheng = chart.chengGuaShu
    val yingQi = chart.yingQiDays
    if (cheng != null && yingQi != null) {
        Spacer(modifier = Modifier.height(12.dp))
        SectionCard("端法应期") {
            Text(
                "成卦之数 $cheng　${chart.posture?.label.orEmpty()} → ${formatDays(yingQi)} 日",
                color = MhTheme.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun GuaPickCell(
    kind: GuaKind,
    chart: MeiHuaChart,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entry = chart.entryOf(kind)
    val stroke = if (selected) MhTheme.accent else MhTheme.line
    val bg = if (selected) MhTheme.panel else MhTheme.card
    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.5.dp, stroke, RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            kind.label,
            color = if (selected) MhTheme.accent else MhTheme.muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        CompactYaoColumn(
            bits = entry.bitsBottomUp,
            moving = if (kind == GuaKind.BEN || kind == GuaKind.BIAN) chart.movingYao else -1,
            markChanged = kind == GuaKind.BIAN,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            entry.fullName,
            color = MhTheme.ink,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun GuaDetailPane(kind: GuaKind, chart: MeiHuaChart) {
    val entry = chart.entryOf(kind)
    val text = chart.textOf(kind)
    val tone = MhTheme.tone(chart.relation.auspicious)

    Text(
        "${kind.label}卦 · ${entry.fullName}",
        color = MhTheme.ink,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        "上${entry.upper.label}　下${entry.lower.label}",
        color = MhTheme.muted,
        fontSize = 12.sp,
    )
    Spacer(modifier = Modifier.height(10.dp))
    YaoColumn(
        bits = entry.bitsBottomUp,
        moving = if (kind == GuaKind.BEN || kind == GuaKind.BIAN) chart.movingYao else -1,
        markChanged = kind == GuaKind.BIAN,
    )

    when (kind) {
        GuaKind.BEN -> {
            Spacer(modifier = Modifier.height(12.dp))
            Text("体用", color = MhTheme.muted, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Meta("体", "${chart.ti.label}·${chart.ti.element.label}（${chart.tiWang.label}）")
                Meta("用", "${chart.yong.label}·${chart.yong.element.label}（${chart.yongWang.label}）")
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(chart.relation.label, color = tone, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(chart.relation.verdict, color = tone, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            XiangBlock("体·${chart.ti.label}", chart.tiXiang.nature, chart.tiXiang.images)
            Spacer(modifier = Modifier.height(8.dp))
            XiangBlock("用·${chart.yong.label}", chart.yongXiang.nature, chart.yongXiang.images)
        }
        GuaKind.BIAN -> {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "变后用=${chart.bianYong.label}（${chart.bianRelation.label}）· ${chart.changeSummary}",
                color = MhTheme.ink,
                fontSize = 13.sp,
            )
        }
        GuaKind.HU -> {
            Spacer(modifier = Modifier.height(12.dp))
            if (chart.huFromBian) {
                Text("乾坤无互，已取变卦之互", color = MhTheme.moving, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                "上互${chart.hu.upper.label}　下互${chart.hu.lower.label}　体互${chart.tiHu.label}　用互${chart.yongHu.label}",
                color = MhTheme.ink,
                fontSize = 13.sp,
            )
        }
        GuaKind.CUO -> {
            Spacer(modifier = Modifier.height(12.dp))
            Text("错卦：本卦六爻阴阳皆反", color = MhTheme.muted, fontSize = 12.sp)
        }
        GuaKind.ZONG -> {
            Spacer(modifier = Modifier.height(12.dp))
            Text("综卦：本卦上下颠倒", color = MhTheme.muted, fontSize = 12.sp)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    Text("卦辞", color = MhTheme.muted, fontSize = 11.sp)
    Spacer(modifier = Modifier.height(4.dp))
    Text(text.guaCi, color = MhTheme.ink, fontSize = 13.sp)
    if (kind == GuaKind.BEN || kind == GuaKind.BIAN) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(if (kind == GuaKind.BEN) "动爻爻辞" else "对应爻辞", color = MhTheme.muted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text.yaoCi.getOrNull(chart.movingYao - 1).orEmpty(),
            color = if (kind == GuaKind.BEN) MhTheme.moving else MhTheme.ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun MeiHuaChart.entryOf(kind: GuaKind) = when (kind) {
    GuaKind.BEN -> ben
    GuaKind.BIAN -> bian
    GuaKind.HU -> hu
    GuaKind.CUO -> cuo
    GuaKind.ZONG -> zong
}

private fun MeiHuaChart.textOf(kind: GuaKind) = when (kind) {
    GuaKind.BEN -> benText
    GuaKind.BIAN -> bianText
    GuaKind.HU -> huText
    GuaKind.CUO -> cuoText
    GuaKind.ZONG -> zongText
}

/** 爻画总宽；阴爻中间留缝，两段等宽。 */
private val YaoBarWidth = 120.dp
private val YaoBarHeight = 9.dp
private val YaoGap = 12.dp
private val CompactYaoBarWidth = 36.dp
private val CompactYaoBarHeight = 4.dp
private val CompactYaoGap = 4.dp

/** 五卦选择格用的迷你六爻（无序号）。 */
@Composable
private fun CompactYaoColumn(
    bits: List<Int>,
    moving: Int,
    markChanged: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for (pos in 6 downTo 1) {
            val yang = bits[pos - 1] == 1
            val isMoving = pos == moving
            val stroke = when {
                isMoving && markChanged -> MhTheme.moving
                isMoving -> MhTheme.moving
                else -> MhTheme.ink
            }
            MeiHuaYaoGlyph(
                yang = yang,
                color = stroke,
                barWidth = CompactYaoBarWidth,
                barHeight = CompactYaoBarHeight,
                gap = CompactYaoGap,
            )
        }
    }
}

/**
 * 六爻列：上→初。
 * 阳爻一横实线；阴爻两段中空（不再用「整条挖白块」）。
 * 动爻用 ○（阳）/ ×（阴），变卦同位标「变」。
 */
@Composable
private fun YaoColumn(
    bits: List<Int>,
    moving: Int,
    markChanged: Boolean = false,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        for (pos in 6 downTo 1) {
            val yang = bits[pos - 1] == 1
            val isMoving = pos == moving
            val stroke = if (isMoving) MhTheme.moving else MhTheme.ink
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    when (pos) {
                        1 -> "初"
                        6 -> "上"
                        else -> "$pos"
                    },
                    color = MhTheme.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.width(28.dp),
                )
                MeiHuaYaoGlyph(yang = yang, color = stroke)
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier.width(28.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    when {
                        isMoving && markChanged -> Text(
                            "变",
                            color = MhTheme.moving,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        isMoving && yang -> Box(
                            Modifier
                                .size(12.dp)
                                .border(1.6.dp, MhTheme.moving, CircleShape),
                        )
                        isMoving && !yang -> Text(
                            "×",
                            color = MhTheme.moving,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

/** 单爻：阳一横，阴两截。 */
@Composable
private fun MeiHuaYaoGlyph(
    yang: Boolean,
    color: Color,
    barWidth: Dp = YaoBarWidth,
    barHeight: Dp = YaoBarHeight,
    gap: Dp = YaoGap,
) {
    if (yang) {
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(barHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
    } else {
        val seg = (barWidth - gap) / 2
        Row(
            modifier = Modifier.width(barWidth),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(seg)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
            Box(
                modifier = Modifier
                    .width(seg)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
        }
    }
}

@Composable
private fun XiangBlock(title: String, nature: String, images: List<String>) {
    Text("$title（$nature）", color = MhTheme.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(4.dp))
    Text(images.joinToString("、"), color = MhTheme.muted, fontSize = 12.sp)
}

@Composable
private fun LunarEditors(
    yearBranch: Int,
    onYear: (Int) -> Unit,
    month: Int,
    onMonth: (Int) -> Unit,
    day: Int,
    onDay: (Int) -> Unit,
    hour: Int,
    onHour: (Int) -> Unit,
    leap: Boolean,
    onLeap: (Boolean) -> Unit,
) {
    FieldCaption("年支")
    Spacer(modifier = Modifier.height(6.dp))
    BranchRow(selected = yearBranch, onSelect = onYear)
    Spacer(modifier = Modifier.height(10.dp))
    FieldCaption("农历月")
    Spacer(modifier = Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..12).forEach { m ->
            Chip("${m}月", selected = month == m) { onMonth(m) }
        }
        Chip("闰", selected = leap) { onLeap(!leap) }
    }
    Spacer(modifier = Modifier.height(10.dp))
    FieldCaption("农历日")
    Spacer(modifier = Modifier.height(6.dp))
    Stepper(value = day, range = 1..30, onChange = onDay)
    Spacer(modifier = Modifier.height(10.dp))
    FieldCaption("时辰")
    Spacer(modifier = Modifier.height(6.dp))
    BranchRow(selected = hour, onSelect = onHour)
}

@Composable
private fun XiangEditors(
    upper: Trigram,
    onUpper: (Trigram) -> Unit,
    lower: Trigram,
    onLower: (Trigram) -> Unit,
    hour: Int,
    onHour: (Int) -> Unit,
    posture: Posture,
    onPosture: (Posture) -> Unit,
) {
    Text("物所属卦为上，所在方位卦为下；动爻加时支序。", color = MhTheme.muted, fontSize = 11.sp)
    Spacer(modifier = Modifier.height(8.dp))
    FieldCaption("物卦（上）")
    Spacer(modifier = Modifier.height(6.dp))
    TrigramRow(selected = upper, onSelect = onUpper)
    Spacer(modifier = Modifier.height(8.dp))
    FieldCaption("方位卦（下）")
    Spacer(modifier = Modifier.height(6.dp))
    TrigramRow(selected = lower, onSelect = onLower)
    Spacer(modifier = Modifier.height(8.dp))
    FieldCaption("时辰")
    Spacer(modifier = Modifier.height(6.dp))
    BranchRow(selected = hour, onSelect = onHour)
    Spacer(modifier = Modifier.height(8.dp))
    FieldCaption("姿态（应期）")
    Spacer(modifier = Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Posture.entries.forEach { p ->
            Chip(p.label, selected = posture == p) { onPosture(p) }
        }
    }
}

@Composable
private fun ManualEditors(
    upper: Trigram,
    onUpper: (Trigram) -> Unit,
    lower: Trigram,
    onLower: (Trigram) -> Unit,
    yao: Int,
    onYao: (Int) -> Unit,
) {
    FieldCaption("上卦")
    Spacer(modifier = Modifier.height(6.dp))
    TrigramRow(selected = upper, onSelect = onUpper)
    Spacer(modifier = Modifier.height(8.dp))
    FieldCaption("下卦")
    Spacer(modifier = Modifier.height(6.dp))
    TrigramRow(selected = lower, onSelect = onLower)
    Spacer(modifier = Modifier.height(8.dp))
    FieldCaption("动爻")
    Spacer(modifier = Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..6).forEach { n ->
            val label = when (n) {
                1 -> "初"
                6 -> "上"
                else -> "$n"
            }
            Chip(label, selected = yao == n) { onYao(n) }
        }
    }
}

@Composable
private fun RuleDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 520.dp),
        containerColor = MhTheme.card,
        title = { Text("梅花易数口径", color = MhTheme.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                DEFAULT_CONVENTIONS.forEach { line ->
                    Text("· $line", color = MhTheme.ink, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "端法应期：行取成卦数之半，立取成卦数，坐加倍；单位为日。仅端法适用。",
                    color = MhTheme.muted,
                    fontSize = 12.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = MhTheme.accent)
            }
        },
    )
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MhTheme.card)
            .border(1.dp, MhTheme.line, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(title, color = MhTheme.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEachIndexed { i, label ->
            Chip(label, selected = i == selectedIndex) { onSelect(i) }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) MhTheme.card else MhTheme.ink,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MhTheme.accent else MhTheme.panel)
            .border(1.dp, if (selected) MhTheme.accent else MhTheme.line, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun BranchRow(selected: Int, onSelect: (Int) -> Unit) {
    BRANCH_LABELS.chunked(6).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            row.forEach { name ->
                val num = BRANCH_LABELS.indexOf(name) + 1
                Chip(name, selected = selected == num) { onSelect(num) }
            }
        }
    }
}

@Composable
private fun TrigramRow(selected: Trigram, onSelect: (Trigram) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Trigram.entries.forEach { t ->
            Chip("${t.label}${innateNumber(t)}", selected = selected == t) { onSelect(t) }
        }
    }
}

@Composable
private fun Stepper(value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Chip("−", selected = false) { if (value > range.first) onChange(value - 1) }
        Text("$value", color = MhTheme.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Chip("+", selected = false) { if (value < range.last) onChange(value + 1) }
    }
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit) {
    Column(modifier = Modifier.width(120.dp)) {
        Text(label, color = MhTheme.muted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = { raw ->
                onChange(raw.filter { it.isDigit() }.take(4))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(fontSize = 16.sp, color = MhTheme.ink, fontWeight = FontWeight.SemiBold),
            cursorBrush = SolidColor(MhTheme.accent),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MhTheme.panel)
                .padding(horizontal = 10.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun FieldCaption(text: String) {
    Text(text, color = MhTheme.muted, fontSize = 11.sp)
}

@Composable
private fun Meta(label: String, value: String) {
    Column {
        Text(label, color = MhTheme.muted, fontSize = 11.sp)
        Text(value, color = MhTheme.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AccentAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = MhTheme.accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

private fun parseNum(raw: String): Int {
    val n = raw.toIntOrNull() ?: 1
    return n.coerceIn(1, MAX_NUMBER)
}

private fun formatDays(days: Double): String =
    if (days == days.toLong().toDouble()) days.toLong().toString() else days.toString()
