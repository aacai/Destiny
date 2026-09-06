package zhiqiu.liuyao.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import zhiqiu.liuyao.DayInfo
import zhiqiu.liuyao.InstalledYao
import zhiqiu.liuyao.LiuYaoChart
import zhiqiu.liuyao.ShenSha
import zhiqiu.liuyao.Trigram
import zhiqiu.liuyao.YaoKind
import zhiqiu.liuyao.ZhanSelection
import zhiqiu.liuyao.ZhanShi
import zhiqiu.liuyao.buildChart
import zhiqiu.liuyao.shenShaBranches
import zhiqiu.liuyao.zhouyiOf
import zhiqiu.liuyao.zhouyiTranslationOf
import zhiqiu.liuyao.daXiangOf
import zhiqiu.liuyao.xiaoXiangOf
import zhiqiu.liuyao.yongXiangOf
import zhiqiu.liuyao.yongYiOf
import zhiqiu.liuyao.wenyanYiOf
import zhiqiu.liuyao.PINYIN
import zhiqiu.liuyao.SHUO_GUA
import zhiqiu.liuyao.tuanOf
import zhiqiu.liuyao.wenyanOf

/**
 * 结果页：题头（占问/占类/卦主/时间可改，干支神煞用神联动）＋
 * 双列卦盘＋经文 Tab（本卦/变卦）。
 */
@Composable
fun ResultPage(
    kinds: List<YaoKind>,
    question: String,
    onQuestion: (String) -> Unit,
    topic: ZhanShi,
    onTopic: (ZhanShi) -> Unit,
    sub: String,
    onSub: (String) -> Unit,
    guaZhu: String,
    onGuaZhu: (String) -> Unit,
    gender: String,
    onGender: (String) -> Unit,
    dayInfo: DayInfo?,
    onDayInfo: (DayInfo?) -> Unit,
    onBackToInput: () -> Unit,
    onRestart: () -> Unit,
) {
    val chart = remember(kinds, dayInfo, topic, sub, gender) {
        val d = dayInfo ?: return@remember null
        buildChart(kinds, d, ZhanSelection(topic, sub), gender)
    }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F3EE))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBackToInput) { Text("‹ 起卦", color = PanAccent, fontSize = 15.sp) }
            Text("排盘结果", color = PanInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onRestart) { Text("重起", color = PanAccent, fontSize = 13.sp) }
        }
        HorizontalDivider(thickness = 0.5.dp, color = PanLine)
        if (chart == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("排盘失败，请返回重起。", color = PanMuted, fontSize = 14.sp)
            }
            return
        }
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ResultHeader(
                chart = chart,
                question = question,
                onQuestion = onQuestion,
                topic = topic,
                onTopic = onTopic,
                sub = sub,
                onSub = onSub,
                guaZhu = guaZhu,
                onGuaZhu = onGuaZhu,
                gender = gender,
                onGender = onGender,
                onDayInfo = onDayInfo,
            )
            ChartCard(chart = chart)
            FullTextsSection(chart = chart)
            ShuoGuaSection()
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** 结果信息头：占问/占类/卦主/时间可改；干支/神煞/用神随改联动。 */
@Composable
private fun ResultHeader(
    chart: LiuYaoChart,
    question: String,
    onQuestion: (String) -> Unit,
    topic: ZhanShi,
    onTopic: (ZhanShi) -> Unit,
    sub: String,
    onSub: (String) -> Unit,
    guaZhu: String,
    onGuaZhu: (String) -> Unit,
    gender: String,
    onGender: (String) -> Unit,
    onDayInfo: (DayInfo?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val day = chart.dayInfo
    val shaBranches = shenShaBranches(day.dayStem, day.dayBranch)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PanCardBg, RoundedCornerShape(12.dp))
            .border(1.dp, PanLine, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        InfoLine(label = "占问") {
            BasicTextField(
                value = question,
                onValueChange = onQuestion,
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = PanInk),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (question.isEmpty()) Text("未填写", color = PanMuted, fontSize = 14.sp)
                    inner()
                },
            )
        }
        InfoLine(label = "占类") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MiniDropdown(
                    options = ZhanShi.entries.map { it.label },
                    selected = topic.label,
                    onSelect = { label -> ZhanShi.entries.firstOrNull { it.label == label }?.let(onTopic) },
                    modifier = Modifier.weight(1f),
                )
                MiniDropdown(
                    options = topic.subs,
                    selected = sub,
                    onSelect = onSub,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        InfoLine(label = "卦主") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = guaZhu,
                    onValueChange = onGuaZhu,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = PanInk),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (guaZhu.isEmpty()) Text("未填写", color = PanMuted, fontSize = 14.sp)
                        inner()
                    },
                )
                Spacer(Modifier.width(6.dp))
                Chip(text = "男", selected = gender == "男", onClick = { onGender("男") })
                Spacer(Modifier.width(4.dp))
                Chip(text = "女", selected = gender == "女", onClick = { onGender("女") })
            }
        }
        InfoLine(label = "时间") {
            DayEditor(dayInfo = day, onChange = { if (it != null) onDayInfo(it) })
        }
        InfoLine(label = "干支") {
            Text(
                buildAnnotatedString {
                    append("${day.yearLabel} ")
                    withStyle(SpanStyle(color = PillarRed)) { append("${day.monthLabel} ") }
                    withStyle(SpanStyle(color = PillarRed)) { append("${day.dayLabel} ") }
                    append("${day.hourLabel}")
                    append("(旬空 ${day.xunKong.sortedBy { it.ordinal }.joinToString(" ") { it.label }})")
                },
                color = PanInk,
                fontSize = 14.sp,
            )
        }
        InfoLine(label = "神煞") {
            Text(
                ShenSha.entries.joinToString("  ") { sha ->
                    "${shaShortName(sha)}--${shaBranches[sha].orEmpty().joinToString("") { it.label }}"
                },
                color = PanInk,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }
        if (chart.yongShenLine.isNotBlank()) {
            InfoLine(label = "用神", value = chart.yongShenLine)
        }
    }
}

private fun shaShortName(sha: ShenSha): String = when (sha) {
    ShenSha.YI_MA -> "驿马"
    ShenSha.TAO_HUA -> "咸池"
    ShenSha.YANG_REN -> "羊刃"
    ShenSha.TIAN_YI -> "天乙"
    ShenSha.HUA_GAI -> "华盖"
    ShenSha.JIE_SHA -> "劫煞"
}

/** 题头一行：标签固定列宽，值在右侧独立列内自动换行。 */
@Composable
private fun InfoLine(label: String, value: String, valueColor: Color = PanInk) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            "$label：",
            color = PanInk,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(52.dp),
        )
        Text(value, color = valueColor, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

/** 同上，但值为自定义内容（输入框/下拉/带颜色干支）。 */
@Composable
private fun InfoLine(label: String, content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            "$label：",
            color = PanInk,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(52.dp),
        )
        Box(Modifier.weight(1f)) { content() }
    }
}

/** 经文区：本卦/变卦 Tab 切换，原文配译文，紧凑排布。 */
@Composable
fun FullTextsSection(chart: LiuYaoChart, modifier: Modifier = Modifier) {
    var tab by remember { mutableIntStateOf(0) }
    var showPinyin by remember { mutableStateOf(false) }
    val hasChanged = chart.changed != null
    if (tab > 0 && !hasChanged) tab = 0
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PanCardBg, RoundedCornerShape(12.dp))
            .border(1.dp, PanLine, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (hasChanged) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = Color.Transparent,
                contentColor = PanAccent,
                indicator = { positions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(positions[tab]),
                        color = PanAccent,
                    )
                },
            ) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = {
                        Text(
                            "本卦·${chart.original.entry.fullName}",
                            fontSize = 13.sp,
                            fontWeight = if (tab == 0) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = {
                        Text(
                            "变卦·${chart.changed!!.entry.fullName}",
                            fontSize = 13.sp,
                            fontWeight = if (tab == 1) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }
        } else {
            Text(
                "本卦【${chart.original.entry.fullName}】",
                color = PanInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("拼音注音", color = PanMuted, fontSize = 12.sp)
            Spacer(Modifier.width(4.dp))
            Switch(checked = showPinyin, onCheckedChange = { showPinyin = it })
        }
        HexTextsBlock(chart = chart, isOriginal = tab == 0, showPinyin = showPinyin)
    }
}

@Composable
private fun HexTextsBlock(
    chart: LiuYaoChart,
    isOriginal: Boolean,
    showPinyin: Boolean,
) {
    val hexagram = if (isOriginal) chart.original else chart.changed!!
    val text = zhouyiOf(hexagram.entry)
    val yi = zhouyiTranslationOf(hexagram.entry)
    val daXiang = daXiangOf(hexagram.entry)
    val xiaoXiang = xiaoXiangOf(hexagram.entry)
    val tuan = tuanOf(hexagram.entry)
    val wenyan = wenyanOf(hexagram.entry)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        @Composable
        fun pt(s: String, color: Color, size: TextUnit, lh: TextUnit) {
            if (showPinyin) PinyinText(s, color, size, lh)
            else Text(s, color = color, fontSize = size, lineHeight = lh)
        }
        pt("卦辞：${text.guaCi}", PanInk, 14.sp, 19.sp)
        Text("译：${yi.guaYi}", color = PanMuted, fontSize = 13.sp, lineHeight = 18.sp)
        if (daXiang.isNotEmpty()) {
            pt("大象：$daXiang", PanAccent, 13.sp, 18.sp)
        }
        if (tuan.isNotEmpty()) {
            pt("彖曰：$tuan", PanAccent, 13.sp, 18.sp)
        }
        // 初→上逐爻展开
        hexagram.yaos.sortedBy { it.position }.forEach { yao ->
            val highlight = isOriginal && yao.position in chart.movingPositions
            YaoTextRow(
                yao = yao,
                wen = text.yaoCi[yao.position - 1],
                yi = yi.yaoYi[yao.position - 1],
                xiao = xiaoXiang.getOrNull(yao.position - 1),
                highlight = highlight,
                showGod = isOriginal,
                showPinyin = showPinyin,
            )
        }
        // 乾坤全变附用九/用六
        if (isOriginal && chart.movingPositions.size == 6 && text.yongCi != null) {
            val tag = if (hexagram.entry.upper == Trigram.QIAN) "用九" else "用六"
            pt("$tag：${text.yongCi}", PanInk, 14.sp, 18.sp)
            yongYiOf(hexagram.entry)?.let { yy ->
                Text("译：$yy", color = PanMuted, fontSize = 13.sp, lineHeight = 17.sp)
            }
            yongXiangOf(hexagram.entry)?.let { yx ->
                pt("象：$yx", PanAccent, 13.sp, 18.sp)
            }
        }
        wenyan?.let { wx ->
            pt("文言：$wx", PanInk, 13.sp, 18.sp)
            wenyanYiOf(hexagram.entry)?.let { wy ->
                wy.split("‖").filter { it.isNotBlank() }.forEach { line ->
                    Text("译：$line", color = PanMuted, fontSize = 13.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}

/** 说卦传卡片：八卦取象总纲，整篇独立于卦象，默认折叠，支持拼音注音。 */
@Composable
private fun ShuoGuaSection(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var showPinyin by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PanCardBg, RoundedCornerShape(12.dp))
            .border(1.dp, PanLine, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "说卦传·八卦取象",
                color = PanInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (expanded) {
                Text("拼音注音", color = PanMuted, fontSize = 12.sp)
                Spacer(Modifier.width(4.dp))
                Switch(checked = showPinyin, onCheckedChange = { showPinyin = it })
            }
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "收起" else "展开", color = PanAccent, fontSize = 13.sp)
            }
        }
        if (expanded) {
            SHUO_GUA.split("\n").filter { it.isNotBlank() }.forEach { para ->
                if (showPinyin) {
                    PinyinText(para, color = PanInk, fontSize = 13.sp, lineHeight = 18.sp)
                } else {
                    Text(para, color = PanInk, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun YaoTextRow(
    yao: InstalledYao,
    wen: String,
    yi: String,
    xiao: String? = null,
    highlight: Boolean,
    showGod: Boolean,
    showPinyin: Boolean = false,
) {
    val tags = buildString {
        if (yao.isMoving) append(if (yao.isYang) "动○" else "动×")
        if (yao.isShi) append("世")
        if (yao.isYing) append("应")
        if (yao.isYongShen) {
            if (isNotEmpty()) append("·")
            append("用")
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(
                if (highlight) Color(0xFFF1F8F8) else Color.Transparent,
                RoundedCornerShape(8.dp),
            )
            .border(
                1.dp,
                if (highlight) PanAccent.copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                yao.yaoName,
                color = PanInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                buildString {
                    if (showGod) append("${yao.sixGod?.label} ")
                    append("${yao.sixKin.label}${yao.ganzhi.label}${yao.ganzhi.element.label}")
                    if (tags.isNotEmpty()) append(" $tags")
                },
                color = if (yao.isYongShen) YongShenGold else PanMuted,
                fontSize = 12.sp,
            )
        }
        if (showPinyin) PinyinText(wen, color = PanInk, fontSize = 14.sp, lineHeight = 18.sp)
        else Text(wen, color = PanInk, fontSize = 14.sp, lineHeight = 18.sp)
        Text("译：$yi", color = PanMuted, fontSize = 13.sp, lineHeight = 17.sp)
        if (!xiao.isNullOrEmpty()) {
            if (showPinyin) PinyinText("象：$xiao", color = PanAccent, fontSize = 12.5.sp, lineHeight = 17.sp)
            else Text("象：$xiao", color = PanAccent, fontSize = 12.5.sp, lineHeight = 17.sp)
        }
    }
}
