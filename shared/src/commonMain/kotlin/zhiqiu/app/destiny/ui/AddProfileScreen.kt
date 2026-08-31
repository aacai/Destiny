package zhiqiu.app.destiny.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ChevronDown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import zhiqiu.app.destiny.chart.computeBaziSummary
import zhiqiu.app.destiny.chart.resolvedSolarDate
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.app.destiny.time.TIME_INDEX_LABELS
import zhiqiu.iztro.bazi.lookup.BaziCandidate
import zhiqiu.iztro.bazi.lookup.BaziPillars
import zhiqiu.iztro.bazi.lookup.StemBranch
import zhiqiu.iztro.bazi.lookup.reverseLookup
import zhiqiu.iztro.bazi.lookup.validateHourPillar
import zhiqiu.iztro.bazi.lookup.validateMonthPillar
import zhiqiu.iztro.bazi.lookup.validatePillars
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.minusMonths
import com.kizitonwose.calendar.core.plusMonths
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.YearMonth
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val PageBg = Color(0xFFF5F3EE)
private val CardBg = Color(0xFFFFFFFF)
private val Ink = Color(0xFF222222)
private val Muted = Color(0xFF8A8578)
private val Line = Color(0xFFE2DED2)
private val Accent = Color(0xFF26A6A6)
private val MaleDot = Color(0xFF4A90D9)
private val FemaleDot = Color(0xFFE57373)

private enum class InputMode(val label: String) { Solar("公历"), Lunar("农历"), Bazi("八字") }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun AddProfileScreen(
    initial: Profile? = null,
    onSave: (Profile) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var groupName by remember { mutableStateOf(initial?.groupName?.takeIf { it.isNotBlank() } ?: "默认") }
    var gender by remember { mutableStateOf(initial?.gender ?: "男") }
    var mode by remember {
        mutableStateOf(
            when (initial?.birthdayType) {
                "lunar" -> InputMode.Lunar
                else -> InputMode.Solar
            },
        )
    }
    var birthday by remember { mutableStateOf(initial?.birthday ?: "2026-8-29") }
    var timeIndex by remember { mutableIntStateOf(initial?.timeIndex ?: 7) } // 未时 ≈ 14:05
    var isLeapMonth by remember { mutableStateOf(initial?.isLeapMonth ?: false) }
    var bazi by remember { mutableStateOf(BaziInput()) }
    var editingSlot by remember { mutableStateOf(PillarSlot.YearStem) }
    var yearFrom by remember { mutableStateOf("1801") }
    var yearTo by remember { mutableStateOf("2099") }
    var clockHour by remember { mutableIntStateOf(initial?.clockHour ?: 12) }
    var clockMinute by remember { mutableIntStateOf(initial?.clockMinute ?: 0) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    // 输入框显示「日期 + 时刻」，手输时自动解析出日期/时刻/时辰
    var dateText by remember {
        mutableStateOf("${initial?.birthday ?: "2026-8-29"} " + "%02d:%02d".format(initial?.clockHour ?: 12, initial?.clockMinute ?: 0))
    }
    var candidates by remember { mutableStateOf<List<BaziCandidate>>(emptyList()) }
    var lookupError by remember { mutableStateOf<String?>(null) }
    var lookingUp by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 候选字点选：填入当前槽位 → 五虎遁/五鼠遁联动 → 跳到下一未填槽位
    val onPickCandidate: (String) -> Unit = { v ->
        val picked = when (editingSlot) {
            PillarSlot.YearStem -> bazi.copy(ys = v, ms = if (bazi.mb == null) null else bazi.ms)
            PillarSlot.YearBranch -> bazi.copy(yb = v)
            PillarSlot.MonthStem -> bazi.copy(ms = v)
            PillarSlot.MonthBranch -> bazi.copy(mb = v)
            PillarSlot.DayStem -> bazi.copy(ds = v, hs = if (bazi.hb == null) null else bazi.hs)
            PillarSlot.DayBranch -> bazi.copy(db = v)
            PillarSlot.HourStem -> bazi.copy(hs = v)
            PillarSlot.HourBranch -> bazi.copy(hb = v)
        }
        var out = picked
        val ys = out.ys
        val mb = out.mb
        if (ys != null && mb != null) out = out.copy(ms = expectedMonthStem(ys, mb))
        val ds = out.ds
        val hb = out.hb
        if (ds != null && hb != null) out = out.copy(hs = expectedHourStem(ds, hb))
        bazi = out
        editingSlot = nextEditing(out, editingSlot)
    }

    // 下拉选择日期：在现有生日基础上替换年/月/日（日按当月天数收敛）
    fun applyDatePick(year: String? = null, month: String? = null, day: String? = null) {
        val (cy, cm, cd) = parseBirthdayParts(birthday)
        val y = (year ?: cy)?.toIntOrNull() ?: return
        val m = (month ?: cm)?.toIntOrNull() ?: return
        val dRaw = (day ?: cd)?.toIntOrNull() ?: return
        val maxD = if (mode == InputMode.Lunar) 30 else solarDaysInMonth(y, m)
        val d = dRaw.coerceIn(1, maxD)
        birthday = "$y-$m-$d"
        dateText = "$birthday " + "%02d:%02d".format(clockHour, clockMinute)
    }

    // 手输「日期 时刻」：支持 2026-8-29 14:05、202608291405 等格式，并按时辰换算 timeIndex
    fun syncDateTimeText(text: String) {
        val tokens = text.trim().split(Regex("\\s+"))
        val datePart = tokens.getOrNull(0) ?: return

        // 紧凑格式：yyyyMMddHHmm
        if (datePart.matches(Regex("\\d{14}"))) {
            val yy = datePart.substring(0, 4).toInt()
            val mo = datePart.substring(4, 6).toInt()
            val dd = datePart.substring(6, 8).toInt()
            val h = datePart.substring(8, 10).toInt()
            val mi = datePart.substring(10, 12).toInt()
            if (mo in 1..12 && h in 0..23 && mi in 0..59) {
                birthday = "$yy-$mo-${dd.coerceIn(1, solarDaysInMonth(yy, mo))}"
                clockHour = h
                clockMinute = mi
                timeIndex = hourToTimeIndex(h)
            }
            return
        }
        // 紧凑格式：yyyyMMdd
        if (datePart.matches(Regex("\\d{8}"))) {
            val yy = datePart.substring(0, 4).toInt()
            val mo = datePart.substring(4, 6).toInt()
            val dd = datePart.substring(6, 8).toInt()
            if (mo in 1..12) {
                birthday = "$yy-$mo-${dd.coerceIn(1, solarDaysInMonth(yy, mo))}"
            }
            return
        }

        val (py, pm, pd) = parseBirthdayParts(datePart)
        val y = py?.toIntOrNull()
        val m = pm?.toIntOrNull() ?: 1
        val dRaw = pd?.toIntOrNull() ?: 1
        if (y != null) {
            birthday = "$y-$m-${dRaw.coerceIn(1, solarDaysInMonth(y, m))}"
        }
        val timePart = tokens.getOrNull(1) ?: return
        val hm = timePart.split(":")
        val h = hm.getOrNull(0)?.trim()?.toIntOrNull()?.takeIf { it in 0..23 } ?: return
        clockHour = h
        timeIndex = hourToTimeIndex(h)
        hm.getOrNull(1)?.trim()?.toIntOrNull()?.takeIf { it in 0..59 }?.let { clockMinute = it }
    }

    Scaffold(
        containerColor = PageBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (initial == null) "添加档案" else "编辑档案",
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = { IosBackButton(onClick = onCancel) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PageBg),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── 基本信息 ──
            SectionCard("基本信息") {
                FieldBox(label = null, value = name, onValueChange = { name = it }, placeholder = "姓名 · 如：张三")
                Spacer(modifier = Modifier.height(12.dp))
                FieldBox(label = "备注", value = note, onValueChange = { note = it }, placeholder = "备注（可选）")
                Spacer(modifier = Modifier.height(12.dp))
                Text("分组", color = Muted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("默认", "家人", "朋友", "案例", "名人").forEach { g ->
                        val selected = groupName == g
                        Text(
                            g,
                            color = if (selected) Color.White else Ink,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Accent else PageBg)
                                .border(1.dp, if (selected) Accent else Line, RoundedCornerShape(8.dp))
                                .clickable { groupName = g }
                                .padding(horizontal = 11.dp, vertical = 7.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("性别", color = Muted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("男" to MaleDot, "女" to FemaleDot).forEach { (g, color) ->
                        val selected = gender == g
                        Row(
                            modifier = Modifier
                                .clickable { gender = g }
                                .background(
                                    if (selected) color.copy(alpha = 0.12f) else PageBg,
                                    RoundedCornerShape(10.dp),
                                )
                                .border(
                                    1.dp,
                                    if (selected) color else Line,
                                    RoundedCornerShape(10.dp),
                                )
                                .padding(horizontal = 22.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(color, CircleShape),
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                g,
                                color = if (selected) color else Muted,
                                fontSize = 14.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }

            // ── 出生时间 ──
            SectionCard("出生时间") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InputMode.entries.forEach { m ->
                        val selected = mode == m
                        Text(
                            m.label,
                            color = if (selected) Color.White else Ink,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier
                                .clickable { mode = m }
                                .background(if (selected) Accent else PageBg, RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp, vertical = 7.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (mode != InputMode.Bazi) {
                    FieldBox(
                        label = if (mode == InputMode.Solar) {
                            "公历日期 · 时刻（2026-8-29 14:05 或 202608291405，也可点箭头选择）"
                        } else {
                            "农历日期 · 时刻（手输，或点右侧箭头选择）"
                        },
                        value = dateText,
                        onValueChange = {
                            dateText = it
                            syncDateTimeText(it)
                        },
                        placeholder = "YYYY-M-D HH:MM",
                        onPickerClick = { showDatePicker = true },
                    )
                } else {
                    // 四柱点选器（参考问真八字）：干/支圆圈 + 候选字联动过滤
                    BaziPillarRow(bazi, editingSlot, onEdit = { editingSlot = it })
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("查找范围", color = Muted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        YearField(value = yearFrom, placeholder = "1801") { yearFrom = it }
                        Text("~", color = Muted, fontSize = 12.sp)
                        YearField(value = yearTo, placeholder = "2099") { yearTo = it }
                        Text("年", color = Muted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "清除",
                            color = Muted,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable {
                                    bazi = BaziInput()
                                    editingSlot = PillarSlot.YearStem
                                    candidates = emptyList()
                                    lookupError = null
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                val from = yearFrom.toIntOrNull()
                                val to = yearTo.toIntOrNull()
                                if (from == null || to == null || from <= 0 || to > 9999 || from > to) {
                                    lookupError = "查找范围格式如 1801 ~ 2099"
                                    candidates = emptyList()
                                    return@Button
                                }
                                val y = bazi.ys?.let { s -> bazi.yb?.let { b -> StemBranch(s, b) } }
                                val m = bazi.ms?.let { s -> bazi.mb?.let { b -> StemBranch(s, b) } }
                                val d = bazi.ds?.let { s -> bazi.db?.let { b -> StemBranch(s, b) } }
                                val h = bazi.hs?.let { s -> bazi.hb?.let { b -> StemBranch(s, b) } }
                                if (y == null || m == null || d == null || h == null) {
                                    lookupError = "四柱还没选完"
                                    candidates = emptyList()
                                    return@Button
                                }
                                val pillars = BaziPillars(y, m, d, h)
                                val err = validatePillars(pillars)
                                if (err != null) {
                                    lookupError = err
                                    candidates = emptyList()
                                    return@Button
                                }
                                lookingUp = true
                                lookupError = null
                                scope.launch {
                                    val found = withContext(Dispatchers.Default) {
                                        reverseLookup(pillars, from, to)
                                    }
                                    candidates = found
                                    lookingUp = false
                                    if (found.isEmpty()) lookupError = "$from ~ $to 年内未找到匹配日期"
                                }
                            },
                            enabled = !lookingUp && bazi.isComplete(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Ink,
                                contentColor = Color.White,
                                disabledContainerColor = Line,
                            ),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp),
                        ) {
                            Text(
                                if (lookingUp) "反查中…" else "确定",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    CandidateGrid(
                        label = if (editingSlot.isStem) "候选天干 · ${editingSlot.label}" else "候选地支 · ${editingSlot.label}",
                        options = editingOptions(bazi, editingSlot),
                        selected = bazi[editingSlot],
                        onPick = onPickCandidate,
                    )
                    lookupError?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, color = Color(0xFFC62828), fontSize = 13.sp)
                    }
                    if (candidates.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "选择候选日期（${candidates.size} 个）",
                            color = Muted,
                            fontSize = 12.sp,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        candidates.take(12).forEach { c ->
                            Text(
                                "${c.solarDate} · ${TIME_INDEX_LABELS[c.timeIndex]}",
                                color = Ink,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        birthday = c.solarDate
                                        timeIndex = c.timeIndex
                                        clockHour = if (c.timeIndex == 0) 0 else c.timeIndex * 2 - 1
                                        clockMinute = 30
                                        dateText = "${c.solarDate} " + "%02d:%02d".format(clockHour, clockMinute)
                                        mode = InputMode.Solar
                                        isLeapMonth = false
                                    }
                                    .background(PageBg, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                            )
                        }
                    }
                }
            }

            // ── 保存 ──
            Button(
                onClick = {
                    val now = Clock.System.now().toEpochMilliseconds()
                    val draft = Profile(
                        id = initial?.id.orEmpty(),
                        name = name.trim().ifBlank { "未命名" },
                        gender = gender,
                        birthdayType = if (mode == InputMode.Lunar) "lunar" else "solar",
                        birthday = birthday.trim(),
                        timeIndex = timeIndex,
                        isLeapMonth = isLeapMonth && mode == InputMode.Lunar,
                        fixLeap = true,
                        clockHour = clockHour,
                        clockMinute = clockMinute,
                        note = note.trim(),
                        groupName = groupName,
                        createdAt = initial?.createdAt ?: now,
                        updatedAt = now,
                    )
                    val saved = runCatching {
                        draft.copy(
                            solarDateDisplay = draft.resolvedSolarDate(),
                            baziSummary = draft.computeBaziSummary(),
                        )
                    }.getOrElse {
                        draft.copy(solarDateDisplay = draft.birthday, baziSummary = "")
                    }
                    onSave(saved)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = mode != InputMode.Bazi || birthday.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Color.White,
                    disabledContainerColor = Line,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (initial == null) "保存档案" else "保存修改", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // 具体时刻选择（滚轮式，时 + 分）
    if (showTimePicker) {
        var selHour by remember { mutableIntStateOf(clockHour.coerceIn(0, 23)) }
        var selMinute by remember { mutableIntStateOf(clockMinute.coerceIn(0, 59)) }
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    clockHour = selHour
                    clockMinute = selMinute
                    showTimePicker = false
                }) { Text("确定", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消", color = Muted) }
            },
            title = { Text("具体时刻", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    WheelColumn(
                        items = (0..23).map { "%02d".format(it) },
                        startIndex = selHour,
                        onCenterChange = { selHour = it },
                    )
                    WheelColumn(
                        items = (0..59).map { "%02d".format(it) },
                        startIndex = selMinute,
                        onCenterChange = { selMinute = it },
                    )
                }
            },
        )
    }
    // 日期时间选择弹窗：公历=日历，农历=滚轮；时辰/具体时刻=滚轮
    if (showDatePicker) {
        val (py, pm, pd) = parseBirthdayParts(birthday)
        val y = py?.toIntOrNull() ?: 2000
        val m = pm?.toIntOrNull() ?: 1
        val d = pd?.toIntOrNull() ?: 1
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("确定", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消", color = Muted) }
            },
            title = {
                Text(
                    if (mode == InputMode.Solar) "选择公历日期" else "选择农历日期",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (mode == InputMode.Solar) {
                        SolarCalendarPicker(y = y, m = m, d = d) { ny, nm, nd -> birthday = "$ny-$nm-$nd" }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            WheelColumn((1900..2100).map { it.toString() }, y - 1900) { applyDatePick(year = (1900 + it).toString()) }
                            WheelColumn((1..12).map { it.toString() }, m - 1) { applyDatePick(month = (it + 1).toString()) }
                            WheelColumn((1..30).map { it.toString() }, d - 1) { applyDatePick(day = (it + 1).toString()) }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("闰月", color = Ink, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(checked = isLeapMonth, onCheckedChange = { isLeapMonth = it })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WheelColumn((0..23).map { "%02d".format(it) }, clockHour) { clockHour = it }
                        WheelColumn((0..59).map { "%02d".format(it) }, clockMinute) { clockMinute = it }
                    }
                    Text("时 · 分", color = Muted, fontSize = 11.sp)
                }
            },
        )
    }
}

/** 分组卡片：标题 + 内容 */
@Composable
private fun SectionCard(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

/** 白底圆角输入框（label 可空；可带下拉箭头，点箭头打开选择器） */
@Composable
private fun FieldBox(
    label: String?,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    onPickerClick: (() -> Unit)? = null,
) {
    Column {
        if (label != null) {
            Text(label, color = Muted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(6.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp, color = Ink),
            modifier = Modifier
                .fillMaxWidth()
                .background(PageBg, RoundedCornerShape(10.dp))
                .border(1.dp, Line, RoundedCornerShape(10.dp))
                .padding(start = 12.dp, end = if (onPickerClick != null) 38.dp else 12.dp, top = 11.dp, bottom = 11.dp),
            decorationBox = { inner ->
                Box {
                    // 占位文字直接叠在输入行上，始终单行高度
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            placeholder,
                            color = Muted,
                            fontSize = 15.sp,
                            maxLines = 1,
                            modifier = Modifier.align(Alignment.CenterStart),
                        )
                    }
                    inner()
                    if (onPickerClick != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onPickerClick)
                                .padding(4.dp),
                        ) {
                            Icon(
                                FeatherIcons.ChevronDown,
                                contentDescription = "打开选择器",
                                tint = Accent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            },
        )
    }
}

/** 四柱点选器：年/月/日/时 四列，干支圆圈，点击选中编辑槽位 */
@Composable
private fun BaziPillarRow(
    input: BaziInput,
    editing: PillarSlot,
    onEdit: (PillarSlot) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        PillarColumn("年", input.ys, input.yb, PillarSlot.YearStem, PillarSlot.YearBranch, editing, onEdit, Modifier.weight(1f))
        PillarColumn("月", input.ms, input.mb, PillarSlot.MonthStem, PillarSlot.MonthBranch, editing, onEdit, Modifier.weight(1f))
        PillarColumn("日", input.ds, input.db, PillarSlot.DayStem, PillarSlot.DayBranch, editing, onEdit, Modifier.weight(1f))
        PillarColumn("时", input.hs, input.hb, PillarSlot.HourStem, PillarSlot.HourBranch, editing, onEdit, Modifier.weight(1f))
    }
}

@Composable
private fun PillarColumn(
    label: String,
    stem: String?,
    branch: String?,
    stemSlot: PillarSlot,
    branchSlot: PillarSlot,
    editing: PillarSlot,
    onEdit: (PillarSlot) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(label, color = Muted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        GlyphCircle(stem, editing == stemSlot) { onEdit(stemSlot) }
        Spacer(modifier = Modifier.height(8.dp))
        GlyphCircle(branch, editing == branchSlot) { onEdit(branchSlot) }
    }
}

/** 干支圆圈：五行淡色底 + 编辑中红圈（参考问真八字） */
@Composable
private fun GlyphCircle(char: String?, active: Boolean, onClick: () -> Unit) {
    val color = char?.let { glyphColor(it) } ?: Muted
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (char != null) color.copy(alpha = 0.10f) else PageBg)
            .border(if (active) 2.dp else 1.dp, if (active) SelectRing else Line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(char ?: "－", color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

/** 候选字网格：仅显示与已选条件相容的字 */
@Composable
private fun CandidateGrid(
    label: String,
    options: List<String>,
    selected: String?,
    onPick: (String) -> Unit,
) {
    Column {
        Text(label, color = Muted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        val perRow = if (options.size > 10) 6 else 5
        options.chunked(perRow).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { ch ->
                    val isSel = ch == selected
                    val color = glyphColor(ch)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSel) color.copy(alpha = 0.15f) else PageBg)
                            .border(1.dp, if (isSel) color else Line, RoundedCornerShape(9.dp))
                            .clickable { onPick(ch) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(ch, color = color, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** 查找范围年份小输入框（仅数字，最多 4 位） */
@Composable
private fun YearField(value: String, placeholder: String, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = { s -> onChange(s.filter { it.isDigit() }.take(4)) },
        singleLine = true,
        textStyle = TextStyle(fontSize = 13.sp, color = Ink, textAlign = TextAlign.Center),
        modifier = Modifier
            .width(54.dp)
            .background(PageBg, RoundedCornerShape(8.dp))
            .border(1.dp, Line, RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isEmpty()) {
                    Text(placeholder, color = Muted.copy(alpha = 0.55f), fontSize = 13.sp)
                }
                inner()
            }
        },
    )
}

// ---------- 四柱联动规则 ----------

private enum class PillarSlot(val label: String, val isStem: Boolean) {
    YearStem("年干", true), YearBranch("年支", false),
    MonthStem("月干", true), MonthBranch("月支", false),
    DayStem("日干", true), DayBranch("日支", false),
    HourStem("时干", true), HourBranch("时支", false),
}

private data class BaziInput(
    val ys: String? = null, val yb: String? = null,
    val ms: String? = null, val mb: String? = null,
    val ds: String? = null, val db: String? = null,
    val hs: String? = null, val hb: String? = null,
) {
    operator fun get(slot: PillarSlot): String? = when (slot) {
        PillarSlot.YearStem -> ys
        PillarSlot.YearBranch -> yb
        PillarSlot.MonthStem -> ms
        PillarSlot.MonthBranch -> mb
        PillarSlot.DayStem -> ds
        PillarSlot.DayBranch -> db
        PillarSlot.HourStem -> hs
        PillarSlot.HourBranch -> hb
    }

    fun isComplete(): Boolean =
        ys != null && yb != null && ms != null && mb != null && ds != null && db != null && hs != null && hb != null
}

private val SLOT_ORDER = listOf(
    PillarSlot.YearStem, PillarSlot.YearBranch,
    PillarSlot.MonthStem, PillarSlot.MonthBranch,
    PillarSlot.DayStem, PillarSlot.DayBranch,
    PillarSlot.HourStem, PillarSlot.HourBranch,
)

/** 选完一个字后跳到下一个未填槽位（已自动遁出的槽位直接跳过） */
private fun nextEditing(input: BaziInput, after: PillarSlot): PillarSlot {
    for (j in SLOT_ORDER.indexOf(after) + 1 until SLOT_ORDER.size) {
        if (input[SLOT_ORDER[j]] == null) return SLOT_ORDER[j]
    }
    return after
}

/** 当前槽位的候选字：只留与已选条件相容的 */
private fun editingOptions(input: BaziInput, slot: PillarSlot): List<String> = when (slot) {
    // 年干受年支奇偶约束（六十甲子只有同奇偶配对）
    PillarSlot.YearStem -> input.yb?.let { parityStems(it) } ?: STEMS
    PillarSlot.YearBranch -> input.ys?.let { parityBranches(it) } ?: BRANCHES
    // 月干受五虎遁约束：年干定 → 十二个月可用的月干
    PillarSlot.MonthStem ->
        input.ys?.let { ys -> BRANCHES.mapNotNull { b -> expectedMonthStem(ys, b) }.distinct() } ?: STEMS
    // 月支受已选月干反推约束
    PillarSlot.MonthBranch -> {
        val ys = input.ys
        val ms = input.ms
        if (ys != null && ms != null) BRANCHES.filter { expectedMonthStem(ys, it) == ms } else BRANCHES
    }
    // 日干受日支奇偶约束
    PillarSlot.DayStem -> input.db?.let { parityStems(it) } ?: STEMS
    PillarSlot.DayBranch -> input.ds?.let { parityBranches(it) } ?: BRANCHES
    // 时干受五鼠遁约束：日干定 → 十二时辰可用的时干
    PillarSlot.HourStem ->
        input.ds?.let { ds -> BRANCHES.mapNotNull { b -> expectedHourStem(ds, b) }.distinct() } ?: STEMS
    // 时支受已选时干反推约束
    PillarSlot.HourBranch -> {
        val ds = input.ds
        val hs = input.hs
        if (ds != null && hs != null) BRANCHES.filter { expectedHourStem(ds, it) == hs } else BRANCHES
    }
}

/** 五虎遁：年干 + 月支 → 唯一月干（复用 bazi-core 校验规则） */
private fun expectedMonthStem(yearStem: String, monthBranch: String): String? =
    STEMS.firstOrNull { validateMonthPillar(yearStem, StemBranch(it, monthBranch)) }

/** 五鼠遁：日干 + 时支 → 唯一时干 */
private fun expectedHourStem(dayStem: String, hourBranch: String): String? =
    STEMS.firstOrNull { validateHourPillar(dayStem, StemBranch(it, hourBranch)) }

/** 与干同奇偶的支（六十甲子配对规则） */
private fun parityBranches(stem: String): List<String> {
    val i = STEMS.indexOf(stem)
    return BRANCHES.filterIndexed { j, _ -> j % 2 == i % 2 }
}

/** 与支同奇偶的干 */
private fun parityStems(branch: String): List<String> {
    val j = BRANCHES.indexOf(branch)
    return STEMS.filterIndexed { i, _ -> i % 2 == j % 2 }
}

private val STEMS = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
private val BRANCHES = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")

// 五行配色（问真式）
private val WoodGreen = Color(0xFF2E9E4F)
private val FireRed = Color(0xFFD84334)
private val EarthYellow = Color(0xFFB8860B)
private val MetalGray = Color(0xFF8A8F98)
private val WaterBlue = Color(0xFF2E6FB7)
private val SelectRing = Color(0xFFD32F2F)

private fun glyphColor(ch: String): Color = when (ch) {
    "甲", "乙", "寅", "卯" -> WoodGreen
    "丙", "丁", "巳", "午" -> FireRed
    "戊", "己", "辰", "未", "戌", "丑" -> EarthYellow
    "庚", "辛", "申", "酉" -> MetalGray
    else -> WaterBlue // 壬癸子亥
}

/** 公历日历（kizitonwose/Calendar）：点选日期，‹ › 翻月 */
@Composable
private fun SolarCalendarPicker(
    y: Int,
    m: Int,
    d: Int,
    onPick: (Int, Int, Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val selMonth = remember(y, m) { YearMonth(y, m) }
    val state = rememberCalendarState(
        startMonth = selMonth.minusMonths(1300),
        endMonth = selMonth.plusMonths(1300),
        firstVisibleMonth = selMonth,
        firstDayOfWeek = DayOfWeek.MONDAY,
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(308.dp)) {
            Text(
                "‹",
                color = Accent,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { scope.launch { state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.minusMonths(1)) } }
                    .padding(horizontal = 12.dp, vertical = 2.dp),
            )
            Text(
                "${state.firstVisibleMonth.yearMonth.year}年${state.firstVisibleMonth.yearMonth.month.ordinal + 1}月",
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Text(
                "›",
                color = Accent,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { scope.launch { state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.plusMonths(1)) } }
                    .padding(horizontal = 12.dp, vertical = 2.dp),
            )
        }
        HorizontalCalendar(
            state = state,
            modifier = Modifier.width(308.dp).height(240.dp),
            dayContent = { day ->
                val inMonth = day.position == DayPosition.MonthDate
                val selected = inMonth && day.date.year == y && day.date.month.ordinal + 1 == m && day.date.dayOfMonth == d
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(CircleShape)
                        .background(if (selected) Accent else Color.Transparent)
                        .clickable(enabled = inMonth) {
                            onPick(day.date.year, day.date.monthNumber, day.date.dayOfMonth)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        day.date.dayOfMonth.toString(),
                        color = when {
                            selected -> Color.White
                            !inMonth -> Muted.copy(alpha = 0.4f)
                            else -> Ink
                        },
                        fontSize = 13.sp,
                    )
                }
            },
            monthHeader = { month ->
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    month.weekDays.first().forEach { wd ->
                        Text(
                            dayOfWeekCn(wd.date.dayOfWeek),
                            color = Muted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            },
        )
    }
}

private fun dayOfWeekCn(d: kotlinx.datetime.DayOfWeek): String = when (d) {
    kotlinx.datetime.DayOfWeek.MONDAY -> "一"
    kotlinx.datetime.DayOfWeek.TUESDAY -> "二"
    kotlinx.datetime.DayOfWeek.WEDNESDAY -> "三"
    kotlinx.datetime.DayOfWeek.THURSDAY -> "四"
    kotlinx.datetime.DayOfWeek.FRIDAY -> "五"
    kotlinx.datetime.DayOfWeek.SATURDAY -> "六"
    kotlinx.datetime.DayOfWeek.SUNDAY -> "日"
}

/** 滚轮选择列（开源时间选择器常见样式）：滑动吸附，居中项为当前值 */
@Composable
private fun WheelColumn(
    items: List<String>,
    startIndex: Int,
    onCenterChange: (Int) -> Unit,
) {
    val itemHeight = 34.dp
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = startIndex.coerceIn(0, items.size - 1),
    )
    // 视口垂直中点所在的项即当前值
    val centerIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) {
                startIndex
            } else {
                val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2
                info.visibleItemsInfo
                    .minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - mid) }
                    ?.index ?: startIndex
            }
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { centerIndex }.collect { onCenterChange(it) }
    }
    // 固定宽高：避免 AlertDialog intrinsic 测量穿透到 LazyColumn
    LazyColumn(
        state = listState,
        modifier = Modifier.width(92.dp).height(itemHeight * 5),
        contentPadding = PaddingValues(vertical = itemHeight * 2),
        flingBehavior = rememberSnapFlingBehavior(listState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(items.size) { i ->
            val selected = centerIndex == i
            Box(modifier = Modifier.height(itemHeight), contentAlignment = Alignment.Center) {
                Text(
                    items[i],
                    color = if (selected) Ink else Muted,
                    fontSize = if (selected) 17.sp else 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

// ---------- 日期解析辅助 ----------

/** 拆 YYYY-M-D（容忍 . / / 分隔）；非法部分返回 null */
private fun parseBirthdayParts(birthday: String): Triple<String?, String?, String?> {
    val p = birthday.trim().split('-', '.', '/')
    val y = p.getOrNull(0)?.trim()?.takeIf { it.length == 4 && it.all { c -> c.isDigit() } }
    val m = p.getOrNull(1)?.trim()?.toIntOrNull()?.takeIf { it in 1..12 }?.toString()
    val d = p.getOrNull(2)?.trim()?.toIntOrNull()?.takeIf { it in 1..31 }?.toString()
    return Triple(y, m, d)
}

private fun solarDaysInMonth(year: Int, month: Int): Int = when (month) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    else -> 31
}

/** 小时 → 时辰序号（0 早子 / 1 丑 … 11 亥 / 12 晚子） */
private fun hourToTimeIndex(hour: Int): Int = when {
    hour == 0 -> 0
    hour == 23 -> 12
    else -> (hour + 1) / 2
}
