package zhiqiu.liuyao.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.liuyao.DayInfo
import zhiqiu.liuyao.YaoKind
import zhiqiu.liuyao.ZhanShi
import zhiqiu.liuyao.currentDayInfo
import zhiqiu.liuyao.dayInfoFromSolar

private val PageBg = Color(0xFFF5F3EE)

/** 起卦确认快照：六爻定稿。其余（占问/占类/卦主/时间）结果页仍可改，活算。 */
data class LiuYaoResult(
    val kinds: List<YaoKind>,
)

/**
 * 六爻流程：起卦页（问事＋指定六爻）→ 点排盘 → 结果页。
 * 结果页隐藏全部输入，只展示题头、卦盘与完整经文译文。
 */
@Composable
fun LiuYaoScreen(onBack: () -> Unit, onOpenYijing: () -> Unit = {}) {
    var question by remember { mutableStateOf("") }
    var guaZhu by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("男") }
    var topic by remember { mutableStateOf(ZhanShi.NONE) }
    var sub by remember { mutableStateOf(ZhanShi.NONE.subs[0]) }
    var dayInfo by remember { mutableStateOf(runCatching { currentDayInfo() }.getOrNull()) }
    // index 0=初爻 … 5=上爻；界面自上而下展示上爻→初爻
    var kinds by remember { mutableStateOf<List<YaoKind?>>(List(6) { null }) }
    var result by remember { mutableStateOf<LiuYaoResult?>(null) }

    val r = result
    if (r == null) {
        InputPage(
            onBack = onBack,
            onOpenYijing = onOpenYijing,
            question = question,
            onQuestion = { question = it },
            guaZhu = guaZhu,
            onGuaZhu = { guaZhu = it },
            gender = gender,
            onGender = { gender = it },
            topic = topic,
            onTopic = {
                topic = it
                sub = it.subs[0]
            },
            sub = sub,
            onSub = { sub = it },
            dayInfo = dayInfo,
            onDayInfo = { dayInfo = it },
            kinds = kinds,
            onKind = { pos, kind ->
                kinds = kinds.toMutableList().also { it[pos - 1] = kind }
            },
            onRestartKinds = { kinds = List(6) { null } },
            onRandomKinds = { kinds = List(6) { YaoKind.entries.random() } },
            onPaiPan = {
                dayInfo ?: return@InputPage
                val ks = kinds.filterNotNull()
                if (ks.size == 6) result = LiuYaoResult(ks)
            },
        )
    } else {
        ResultPage(
            kinds = r.kinds,
            question = question,
            onQuestion = { question = it },
            topic = topic,
            onTopic = {
                topic = it
                sub = it.subs[0]
            },
            sub = sub,
            onSub = { sub = it },
            guaZhu = guaZhu,
            onGuaZhu = { guaZhu = it },
            gender = gender,
            onGender = { gender = it },
            dayInfo = dayInfo,
            onDayInfo = { dayInfo = it },
            onBackToInput = { result = null },
            onRestart = {
                kinds = List(6) { null }
                result = null
            },
        )
    }
}

@Composable
private fun InputPage(
    onBack: () -> Unit,
    onOpenYijing: () -> Unit,
    question: String,
    onQuestion: (String) -> Unit,
    guaZhu: String,
    onGuaZhu: (String) -> Unit,
    gender: String,
    onGender: (String) -> Unit,
    topic: ZhanShi,
    onTopic: (ZhanShi) -> Unit,
    sub: String,
    onSub: (String) -> Unit,
    dayInfo: DayInfo?,
    onDayInfo: (DayInfo?) -> Unit,
    kinds: List<YaoKind?>,
    onKind: (pos: Int, kind: YaoKind?) -> Unit,
    onRestartKinds: () -> Unit,
    onRandomKinds: () -> Unit,
    onPaiPan: () -> Unit,
) {
    val unsetCount = kinds.count { it == null }
    Column(modifier = Modifier.fillMaxSize().background(PageBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("‹ 返回", color = PanAccent, fontSize = 15.sp) }
            Text("六爻起卦", color = PanInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onOpenYijing) { Text("易经", color = PanAccent, fontSize = 13.sp) }
            TextButton(
                onClick = onRestartKinds,
                enabled = kinds.any { it != null },
            ) { Text("重起", color = PanAccent, fontSize = 13.sp) }
        }
        HorizontalDivider(thickness = 0.5.dp, color = PanLine)

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(PanCardBg, RoundedCornerShape(12.dp))
                    .border(1.dp, PanLine, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LabeledRow(label = "占问") {
                    BasicTextField(
                        value = question,
                        onValueChange = onQuestion,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 14.sp, color = PanInk),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (question.isEmpty()) Text("如：此次求财可成否", color = PanMuted, fontSize = 14.sp)
                            inner()
                        },
                    )
                }
                LabeledRow(label = "占类") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            ZhanShi.entries.forEach { zs ->
                                Chip(text = zs.label, selected = topic == zs, onClick = { onTopic(zs) })
                            }
                        }
                        if (topic.subs.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                topic.subs.forEach { s ->
                                    Chip(text = s, selected = sub == s, onClick = { onSub(s) })
                                }
                            }
                        }
                    }
                }
                LabeledRow(label = "卦主") {
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
                        Spacer(Modifier.width(8.dp))
                        Chip(text = "男", selected = gender == "男", onClick = { onGender("男") })
                        Spacer(Modifier.width(6.dp))
                        Chip(text = "女", selected = gender == "女", onClick = { onGender("女") })
                    }
                }
                LabeledRow(label = "占时") {
                    DayEditor(dayInfo = dayInfo, onChange = onDayInfo)
                }
                (6 downTo 1).forEach { pos ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            yaoPlaceLabel(pos),
                            color = PanInk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(36.dp),
                        )
                        YaoKindDropdown(
                            selected = kinds[pos - 1],
                            onSelect = { onKind(pos, it) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onRandomKinds,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PanLine, contentColor = PanInk),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("随机取卦", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onPaiPan,
                        enabled = unsetCount == 0 && dayInfo != null,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PanAccent),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            if (unsetCount == 0) "排盘" else "还差 $unsetCount 爻",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LabeledRow(label: String, content: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = PanMuted, fontSize = 13.sp, modifier = Modifier.width(40.dp))
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

@Composable
internal fun DayEditor(dayInfo: DayInfo?, onChange: (DayInfo?) -> Unit) {
    var y by remember(dayInfo) { mutableStateOf(dayInfo?.year?.toString() ?: "") }
    var mo by remember(dayInfo) { mutableStateOf(dayInfo?.month?.toString() ?: "") }
    var d by remember(dayInfo) { mutableStateOf(dayInfo?.day?.toString() ?: "") }
    var h by remember(dayInfo) { mutableStateOf(dayInfo?.hour?.toString() ?: "") }
    var mi by remember(dayInfo) { mutableStateOf(dayInfo?.minute?.toString() ?: "") }
    var err by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }

    fun apply(ny: String, nmo: String, nd: String, nh: String, nmi: String) {
        val vi = listOf(ny, nmo, nd, nh, nmi).map { it.toIntOrNull() }
        if (vi.any { it == null }) {
            err = "时间不完整，沿用原占时"
            return
        }
        val (yy, mm, dd, hh, mmi) = vi.map { it!! }
        if (mm !in 1..12 || dd !in 1..31 || hh !in 0..23 || mmi !in 0..59) {
            err = "时间不合法，沿用原占时"
            return
        }
        val next = runCatching { dayInfoFromSolar(yy, mm, dd, hh, mmi) }.getOrNull()
        if (next == null) {
            err = "干支换算失败，沿用原占时"
        } else {
            err = ""
            onChange(next)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MiniField(value = y, label = "年", width = 58) { y = it; apply(it, mo, d, h, mi) }
            MiniField(value = mo, label = "月", width = 44) { mo = it; apply(y, it, d, h, mi) }
            MiniField(value = d, label = "日", width = 44) { d = it; apply(y, mo, it, h, mi) }
            // 时刻：滚轮选择（借鉴档案页「具体时刻」）
            TimeTrigger(
                hour = h.toIntOrNull() ?: 0,
                minute = mi.toIntOrNull() ?: 0,
                onClick = { showTimePicker = true },
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                runCatching { currentDayInfo() }.getOrNull()?.let {
                    onChange(it)
                    err = ""
                }
            }) { Text("今", color = PanAccent, fontSize = 13.sp) }
        }
        if (dayInfo != null) {
            Text(
                "${dayInfo.dateTimeLabel}　${dayInfo.monthLabel}月 ${dayInfo.dayLabel}日",
                color = PanAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        if (err.isNotBlank()) Text(err, color = PanMuted, fontSize = 12.sp)
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = h.toIntOrNull() ?: 0,
            initialMinute = mi.toIntOrNull() ?: 0,
            onDismiss = { showTimePicker = false },
            onConfirm = { nh, nmi ->
                h = nh.toString()
                mi = nmi.toString()
                apply(y, mo, d, h, mi)
                showTimePicker = false
            },
        )
    }
}

@Composable
internal fun MiniField(
    value: String,
    label: String,
    width: Int,
    onValueChange: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = value,
            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) onValueChange(it) },
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp, color = PanInk),
            modifier = Modifier.width(width.dp)
                .border(1.dp, PanLine, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
        )
        Spacer(Modifier.width(2.dp))
        Text(label, color = PanMuted, fontSize = 12.sp)
    }
}

/** 爻态说明文字。 */
private fun yaoKindLabel(kind: YaoKind): String = when (kind) {
    YaoKind.LAO_YANG -> "老阳 · 动 ○"
    YaoKind.SHAO_YANG -> "少阳"
    YaoKind.SHAO_YIN -> "少阴"
    YaoKind.LAO_YIN -> "老阴 · 动 ×"
}

/**
 * 单爻下拉选择：平时只占一行，显示爻画图标＋文字；
 * 未选时显示「未指定爻」，点开展开四个带图标的选项。
 */
@Composable
private fun YaoKindDropdown(
    selected: YaoKind?,
    onSelect: (YaoKind?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable { expanded = true }
                .background(PanCardBg, RoundedCornerShape(8.dp))
                .border(
                    1.dp,
                    if (selected == null) PanLine else PanAccent,
                    RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected == null) {
                Text(
                    "未指定爻",
                    color = PanMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
            } else {
                YaoGlyph(isYang = selected.isYang, mark = selected.mark, width = 36.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    yaoKindLabel(selected),
                    color = PanInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
            }
            Text("▾", color = PanMuted, fontSize = 13.sp)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(PanCardBg),
        ) {
            YaoKind.entries.forEach { kind ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            YaoGlyph(isYang = kind.isYang, mark = kind.mark, width = 36.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(yaoKindLabel(kind), color = PanInk, fontSize = 13.sp)
                        }
                    },
                    onClick = {
                        onSelect(kind)
                        expanded = false
                    },
                )
            }
            if (selected != null) {
                DropdownMenuItem(
                    text = { Text("清除，恢复未指定", color = PanMuted, fontSize = 13.sp) },
                    onClick = {
                        onSelect(null)
                        expanded = false
                    },
                )
            }
        }
    }
}
