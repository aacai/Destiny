package zhiqiu.app.destiny.ui.xingmingref

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import zhiqiu.app.destiny.oinkcompat.Checkbox
import zhiqiu.app.destiny.oinkcompat.CheckboxDefaults
import zhiqiu.app.destiny.oinkcompat.HorizontalDivider
import zhiqiu.app.destiny.oinkcompat.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zhiqiu.app.destiny.chart.computeBaziSummary
import zhiqiu.app.destiny.chart.resolvedSolarDate
import zhiqiu.app.destiny.profile.CaseType
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.qizheng.PanZhiPresets
import zhiqiu.qizheng.QizhengBuilder
import zhiqiu.qizheng.QizhengChart
import zhiqiu.qizheng.ui.QizhengWheel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 星命排盘 Demo：默认打开即排「黄金样例」政余盘，并与星命排盘 web 参考对照。
 * 命例持久化走 [Profile] / ProfileRepository（与首页档案同一库）。
 */
@OptIn(ExperimentalTime::class)
@Composable
fun XingmingRefScreen(
    profiles: List<Profile>,
    onBack: () -> Unit,
    /**
     * 持久化命例；宿主应 [ProfileRepository.upsert] 后把**带 id 的结果**回传 [onSaved]。
     */
    onSaveProfile: (draft: Profile, onSaved: (Profile) -> Unit) -> Unit,
    /** 打开完整档案排盘页（八字/紫微/七政） */
    onOpenFullChart: (Profile) -> Unit = {},
) {
    var tab by remember { mutableIntStateOf(0) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("黄金样例") }
    var male by remember { mutableStateOf(true) }
    var place by remember { mutableStateOf("中国/北京/真太阳时") }
    var rememberPlace by remember { mutableStateOf(true) }
    var year by remember { mutableStateOf("2026") }
    var month by remember { mutableStateOf("8") }
    var day by remember { mutableStateOf("29") }
    var hour by remember { mutableStateOf("14") }
    var minute by remember { mutableStateOf("5") }
    var search by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf<String?>(null) }
    /** 打开即展示盘面，便于 Demo 一眼可见 */
    var committed by remember { mutableStateOf(true) }

    val y = year.toIntOrNull() ?: 2026
    val mo = month.toIntOrNull()?.coerceIn(1, 12) ?: 8
    val d = day.toIntOrNull()?.coerceIn(1, 31) ?: 29
    val h = hour.toIntOrNull()?.coerceIn(0, 23) ?: 14
    val mi = minute.toIntOrNull()?.coerceIn(0, 59) ?: 5
    val timeLabel = "%04d-%02d-%02d %02d:%02d".format(y, mo, d, h, mi)
    val birthdayStr = "%04d-%02d-%02d".format(y, mo, d)

    val filtered = remember(profiles, search) {
        val q = search.trim()
        val list = profiles.sortedByDescending { it.updatedAt }
        if (q.isEmpty()) list
        else list.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.birthday.contains(q) ||
                it.solarDateDisplay.contains(q) ||
                it.baziSummary.contains(q) ||
                it.groupName.contains(q)
        }
    }

    fun loadProfile(p: Profile) {
        editingId = p.id
        name = p.name
        male = p.gender != "女"
        val parts = (p.solarDateDisplay.ifBlank { p.birthday }).split("-", "/", ".")
        if (parts.size >= 3) {
            year = parts[0]
            month = parts[1].trimStart('0').ifBlank { parts[1] }
            day = parts[2].trimStart('0').ifBlank { parts[2] }
            // 规范化：保留原数字
            year = parts[0]
            month = parts[1].toIntOrNull()?.toString() ?: month
            day = parts[2].toIntOrNull()?.toString() ?: day
        }
        hour = (p.clockHour ?: hourFromTimeIndex(p.timeIndex)).toString()
        minute = (p.clockMinute ?: 0).toString()
        committed = false
        toast = "已载入：${p.name}"
    }

    fun buildDraft(id: String = editingId.orEmpty()): Profile {
        val now = Clock.System.now().toEpochMilliseconds()
        val displayName = name.trim().ifBlank { "命例 $timeLabel" }
        val draft = Profile(
            id = id,
            name = displayName,
            gender = if (male) "男" else "女",
            birthdayType = "solar",
            birthday = birthdayStr,
            timeIndex = hourToTimeIndex(h),
            clockHour = h,
            clockMinute = mi,
            longitude = 116.4074,
            latitude = 39.9042,
            groupName = "案例",
            caseType = CaseType.BAZI,
            note = if (rememberPlace) place else "",
            createdAt = profiles.firstOrNull { it.id == id }?.createdAt ?: now,
            updatedAt = now,
        )
        return runCatching {
            draft.copy(
                solarDateDisplay = draft.resolvedSolarDate(),
                baziSummary = draft.computeBaziSummary(),
            )
        }.getOrElse {
            draft.copy(solarDateDisplay = birthdayStr, baziSummary = "")
        }
    }

    val chart = remember(committed, y, mo, d, h, mi, male) {
        if (!committed) null
        else runCatching {
            QizhengBuilder.build(
                year = y, month = mo, day = d, hour = h, minute = mi,
                gender = if (male) "男" else "女",
                lon = 116.4074, lat = 39.9042,
                // 对齐星命排盘默认：回归今宿·黄道 + 真太阳时
                config = PanZhiPresets.TrueSolar,
            )
        }.getOrNull()
    }

    fun applyGoldenSample() {
        editingId = null
        name = "黄金样例"
        male = true
        year = "2026"; month = "8"; day = "29"
        hour = "14"; minute = "5"
        place = "中国/北京/真太阳时"
        committed = true
        tab = 0
        toast = "已排黄金样例政余盘"
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(Xm.Page),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = 420.dp)
                .fillMaxHeight()
                .fillMaxWidth()
                .safeDrawingPadding(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Xm.Card)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("← 返回", color = Xm.Accent, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onBack))
                Spacer(Modifier.weight(1f))
                Text("星命排盘 Demo", fontWeight = FontWeight.Bold, color = Xm.Ink, fontSize = 16.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    "样例",
                    color = Xm.Accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { applyGoldenSample() },
                )
            }

            when (tab) {
                0 -> if (committed && chart != null) {
                    ZhengYuResult(
                        chart = chart,
                        profileName = name.ifBlank { "命例 $timeLabel" },
                        onBackInput = { committed = false },
                        onOpenFull = {
                            onSaveProfile(buildDraft()) { saved ->
                                editingId = saved.id
                                name = saved.name
                                onOpenFullChart(saved)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    InputHome(
                        name = name,
                        onName = { name = it },
                        male = male,
                        onMale = { male = it },
                        place = place,
                        rememberPlace = rememberPlace,
                        onRemember = { rememberPlace = it },
                        timeLabel = timeLabel,
                        year = year, month = month, day = day, hour = hour, minute = minute,
                        onYear = { year = it },
                        onMonth = { month = it },
                        onDay = { day = it },
                        onHour = { hour = it },
                        onMinute = { minute = it },
                        search = search,
                        onSearch = { search = it },
                        cases = filtered,
                        selectedId = editingId,
                        onSelectCase = { loadProfile(it) },
                        onSave = {
                            onSaveProfile(buildDraft()) { saved ->
                                editingId = saved.id
                                name = saved.name
                                toast = "已保存命例：${saved.name}"
                            }
                        },
                        onDone = {
                            // 完成：先落库再排盘，保证案例列表可回选
                            onSaveProfile(buildDraft()) { saved ->
                                editingId = saved.id
                                name = saved.name
                                committed = true
                                toast = null
                            }
                        },
                        onNew = {
                            editingId = null
                            name = ""
                            male = true
                            year = "2026"; month = "8"; day = "29"
                            hour = "14"; minute = "5"
                            committed = false
                            toast = "新建命例"
                        },
                        onGolden = { applyGoldenSample() },
                        onBaziReverse = { toast = "四柱反推（占位）" },
                        onCloud = { toast = "$it（占位）" },
                        modifier = Modifier.weight(1f),
                    )
                }
                1 -> PlaceholderPane("八字", "选案例后点「完成」可排政余；完整八字请点结果页「完整排盘」。", Modifier.weight(1f))
                else -> PlaceholderPane("我的", "命例与首页档案共用同一库，分组为「案例」。", Modifier.weight(1f))
            }

            HorizontalDivider(color = Xm.Line)
            Row(
                Modifier.fillMaxWidth().background(Xm.Card).padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                listOf("政余", "八字", "我的").forEachIndexed { i, label ->
                    Text(
                        label,
                        color = if (tab == i) Xm.Accent else Xm.Muted,
                        fontWeight = if (tab == i) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clickable {
                                tab = i
                                if (i != 0) committed = false
                            }
                            .padding(horizontal = 20.dp, vertical = 2.dp),
                    )
                }
            }
        }

        toast?.let { msg ->
            Text(
                msg,
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC333333))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clickable { toast = null },
            )
        }
    }
}

@Composable
private fun InputHome(
    name: String,
    onName: (String) -> Unit,
    male: Boolean,
    onMale: (Boolean) -> Unit,
    place: String,
    rememberPlace: Boolean,
    onRemember: (Boolean) -> Unit,
    timeLabel: String,
    year: String, month: String, day: String, hour: String, minute: String,
    onYear: (String) -> Unit,
    onMonth: (String) -> Unit,
    onDay: (String) -> Unit,
    onHour: (String) -> Unit,
    onMinute: (String) -> Unit,
    search: String,
    onSearch: (String) -> Unit,
    cases: List<Profile>,
    selectedId: String?,
    onSelectCase: (Profile) -> Unit,
    onSave: () -> Unit,
    onDone: () -> Unit,
    onNew: () -> Unit,
    onGolden: () -> Unit,
    onBaziReverse: () -> Unit,
    onCloud: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Xm.Accent)
                .clickable(onClick = onGolden)
                .padding(vertical = 12.dp, horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "一键黄金样例 · 2026-08-29 14:05 男 · 北京",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        XmCard {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("输入信息", fontWeight = FontWeight.Bold, color = Xm.Ink, fontSize = 17.sp)
                Spacer(Modifier.width(8.dp))
                Text("默认真太阳时", color = Xm.Muted, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    "新建",
                    color = Xm.Accent,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onNew),
                )
            }
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("姓名：", color = Xm.Ink, fontSize = 15.sp)
                BasicTextField(
                    value = name,
                    onValueChange = onName,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 15.sp, color = Xm.Ink),
                    cursorBrush = SolidColor(Xm.Accent),
                    decorationBox = { inner ->
                        if (name.isEmpty()) Text("未命名命例", color = Xm.Muted, fontSize = 15.sp)
                        inner()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Xm.FieldBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("性别：", color = Xm.Ink, fontSize = 15.sp)
                Spacer(Modifier.width(8.dp))
                SexRadio(selected = male, label = "男") { onMale(true) }
                Spacer(Modifier.width(20.dp))
                SexRadio(selected = !male, label = "女") { onMale(false) }
            }
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "出生地点：$place",
                    color = Xm.Ink,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Checkbox(
                    checked = rememberPlace,
                    onCheckedChange = onRemember,
                    colors = CheckboxDefaults.colors(checkedColor = Xm.Accent, uncheckedColor = Xm.Muted),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("记住", color = Xm.Ink, fontSize = 14.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text("选择时间：$timeLabel", color = Xm.Ink, fontSize = 15.sp)
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("手动输入：", color = Xm.Ink, fontSize = 15.sp)
                NumBox(year, onYear, Modifier.width(56.dp), maxLen = 4)
                Unit("年")
                NumBox(month, onMonth, Modifier.width(40.dp), maxLen = 2)
                Unit("月")
                NumBox(day, onDay, Modifier.width(40.dp), maxLen = 2)
                Unit("日")
                NumBox(hour, onHour, Modifier.width(40.dp), maxLen = 2)
                Unit("时")
                NumBox(minute, onMinute, Modifier.width(40.dp), maxLen = 2)
                Unit("分")
            }
            Spacer(Modifier.height(12.dp))
            Text("四柱反推", color = Xm.Ink, fontSize = 15.sp, modifier = Modifier.clickable(onClick = onBaziReverse))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OutlinePill("保存", onSave)
            Spacer(Modifier.width(10.dp))
            OutlinePill("完成", onDone)
        }

        // 搜索 + 案例列表
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFFEEEEEE))
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🔍", fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = search,
                onValueChange = onSearch,
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = Xm.Ink),
                cursorBrush = SolidColor(Xm.Accent),
                decorationBox = { inner ->
                    if (search.isEmpty()) {
                        Text("案例集合${cases.size}—请输入搜索信息", color = Xm.Muted, fontSize = 14.sp)
                    }
                    inner()
                },
                modifier = Modifier.weight(1f),
            )
        }

        XmCard {
            Text(
                "命例列表（点选载入再排盘）",
                fontWeight = FontWeight.Bold,
                color = Xm.Ink,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(8.dp))
            if (cases.isEmpty()) {
                Text("暂无命例。填写上方信息后点「保存」。", color = Xm.Muted, fontSize = 13.sp)
            } else {
                Column(
                    Modifier.heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    cases.take(50).forEach { p ->
                        val selected = p.id == selectedId
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Color(0xFFFFEBEE) else Xm.Page)
                                .border(
                                    1.dp,
                                    if (selected) Xm.Accent else Xm.Line,
                                    RoundedCornerShape(8.dp),
                                )
                                .clickable { onSelectCase(p) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.SemiBold, color = Xm.Ink, fontSize = 14.sp)
                                Text(
                                    "${p.solarDateDisplay.ifBlank { p.birthday }} · ${p.gender}" +
                                        (p.clockHour?.let { " · %02d:%02d".format(it, p.clockMinute ?: 0) } ?: ""),
                                    color = Xm.Muted,
                                    fontSize = 11.sp,
                                )
                                if (p.baziSummary.isNotBlank()) {
                                    Text(p.baziSummary, color = Xm.Muted, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                            if (p.groupName.isNotBlank()) {
                                Text(p.groupName, color = Xm.Muted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        XmCard {
            Text("云端数据", fontWeight = FontWeight.Bold, color = Xm.Ink, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "温馨提示：备份会覆盖云端数据，下载则合并云端数据到本地",
                color = Xm.Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                OutlinePill("备份") { onCloud("备份") }
                Spacer(Modifier.width(10.dp))
                OutlinePill("下载") { onCloud("下载") }
                Spacer(Modifier.width(10.dp))
                OutlinePill("清空") { onCloud("清空") }
            }
        }
    }
}

/** 星命排盘 web「回归今宿·黄道」黄金样例参考（黄经 / 宫 / 宿） */
private val XingmingppGoldenRef = mapOf(
    "日" to Triple(155.9397, "巳", "星"),
    "月" to Triple(348.7667, "亥", "危"),
    "水" to Triple(157.4572, "巳", "张"),
    "金" to Triple(200.9342, "辰", "轸"),
    "火" to Triple(101.6467, "未", "井"),
    "木" to Triple(133.1206, "午", "柳"),
    "土" to Triple(13.8303, "戌", "壁"),
    "计" to Triple(329.8358, "子", "虚"),
    "罗" to Triple(149.7547, "午", "星"),
    "孛" to Triple(268.0639, "寅", "尾"),
    "炁" to Triple(172.1486, "巳", "张"),
)

@Composable
private fun ZhengYuResult(
    chart: QizhengChart,
    profileName: String,
    onBackInput: () -> Unit,
    onOpenFull: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isGolden = chart.year == 2026 && chart.month == 8 && chart.day == 29 &&
        chart.hour == 14 && chart.minute == 5 && chart.gender == "男"
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(profileName, fontWeight = FontWeight.Bold, color = Xm.Ink, fontSize = 15.sp)
                Text(
                    "盘制 ${chart.panZhi}",
                    color = Xm.Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(chart.baziLabel, color = Xm.Muted, fontSize = 12.sp)
                Text(
                    "命宫 ${chart.mingBranch} · 身宫 ${chart.shenBranch ?: "—"}" +
                        if (isGolden) "（期望 丑 / 亥）" else "",
                    color = Xm.Muted,
                    fontSize = 12.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                OutlinePill("重新输入", onBackInput)
                Spacer(Modifier.height(6.dp))
                OutlinePill("完整排盘", onOpenFull)
            }
        }
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(2.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(Xm.Card),
            contentAlignment = Alignment.Center,
        ) {
            // 盘面铺满卡片宽度（卡片为正方形，宽度即边长），最大化盘面以缓解文字挤压
            val wheelSize = maxWidth - 8.dp
            QizhengWheel(chart = chart, size = wheelSize, enableZoom = true)
        }
        Spacer(Modifier.height(10.dp))
        if (isGolden) {
            XmCard {
                Text("对照星命排盘（web）", fontWeight = FontWeight.Bold, color = Xm.Ink, fontSize = 14.sp)
                Text(
                    "黄经Δ<0.5° · 宫/宿须全对",
                    color = Xm.Muted,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text("星", Modifier.width(28.dp), color = Xm.Muted, fontSize = 11.sp)
                    Text("Destiny", Modifier.weight(1f), color = Xm.Muted, fontSize = 11.sp)
                    Text("web", Modifier.weight(1f), color = Xm.Muted, fontSize = 11.sp)
                    Text("Δ°", Modifier.width(52.dp), color = Xm.Muted, fontSize = 11.sp)
                    Text("验", Modifier.width(28.dp), color = Xm.Muted, fontSize = 11.sp)
                }
                HorizontalDivider(color = Xm.Line, modifier = Modifier.padding(vertical = 4.dp))
                XingmingppGoldenRef.forEach { (key, ref) ->
                    val (wLon, wBranch, wXiu) = ref
                    val s = chart.stars.firstOrNull { it.key == key }
                    val dLon = s?.longitude?.minus(wLon)
                    val ok = s != null && s.branch == wBranch && s.xiu == wXiu
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(key, Modifier.width(28.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Xm.Ink)
                        Text(
                            if (s != null) "${"%.2f".format(s.longitude)} ${s.branch}${s.xiu}" else "—",
                            Modifier.weight(1f),
                            fontSize = 11.sp,
                            color = Xm.Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${"%.2f".format(wLon)} $wBranch$wXiu",
                            Modifier.weight(1f),
                            fontSize = 11.sp,
                            color = Xm.Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            dLon?.let { "%+.3f".format(it) } ?: "—",
                            Modifier.width(52.dp),
                            fontSize = 11.sp,
                            color = Xm.Ink,
                        )
                        Text(
                            if (ok) "✓" else "✗",
                            Modifier.width(28.dp),
                            fontSize = 13.sp,
                            color = if (ok) Color(0xFF2E7D32) else Xm.Accent,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        XmCard {
            Text("十一曜", fontWeight = FontWeight.Bold, color = Xm.Ink, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            chart.stars.forEach { s ->
                Text(
                    "${s.label}  ${"%.2f".format(s.longitude)}°  ${s.branch}·${s.xiu}${
                        "%.1f".format(s.xiuDegree)
                    }  ${s.motion}",
                    fontSize = 12.sp,
                    color = Xm.Ink,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun PlaceholderPane(title: String, hint: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.Bold, color = Xm.Ink, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(hint, color = Xm.Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun XmCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(Xm.Card)
            .border(1.dp, Xm.Line, RoundedCornerShape(10.dp))
            .padding(14.dp),
    ) { content() }
}

@Composable
private fun SexRadio(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .size(18.dp)
                .border(2.dp, if (selected) Xm.Accent else Xm.Muted, CircleShape)
                .padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Xm.Accent))
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = if (selected) Xm.Accent else Xm.Ink,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun NumBox(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, maxLen: Int = 2) {
    BasicTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(maxLen)) },
        singleLine = true,
        textStyle = TextStyle(fontSize = 14.sp, color = Xm.Ink),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        cursorBrush = SolidColor(Xm.Accent),
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, Xm.FieldBorder, RoundedCornerShape(4.dp))
            .background(Color.White)
            .padding(horizontal = 6.dp, vertical = 5.dp),
    )
}

@Composable
private fun Unit(text: String) {
    Text(text, fontSize = 13.sp, color = Xm.Muted, modifier = Modifier.padding(horizontal = 2.dp))
}

@Composable
private fun OutlinePill(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Xm.Ink,
        fontSize = 14.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Xm.Line, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
    )
}

private fun hourToTimeIndex(hour: Int): Int = when {
    hour == 0 -> 0
    hour == 23 -> 12
    else -> (hour + 1) / 2
}

private fun hourFromTimeIndex(timeIndex: Int): Int = when (timeIndex) {
    0 -> 0
    12 -> 23
    else -> timeIndex * 2 - 1
}

private object Xm {
    val Page = Color(0xFFF2F2F2)
    val Card = Color.White
    val Ink = Color(0xFF222222)
    val Muted = Color(0xFF9E9E9E)
    val Line = Color(0xFFE6E6E6)
    val FieldBorder = Color(0xFFD0D0D0)
    val Accent = Color(0xFFE53935)
}
