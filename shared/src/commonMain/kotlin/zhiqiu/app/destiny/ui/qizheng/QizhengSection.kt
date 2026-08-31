package zhiqiu.app.destiny.ui.qizheng

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.qizheng.HuaYaoSchool
import zhiqiu.qizheng.PanZhiPresets
import zhiqiu.app.destiny.qizheng.buildFromProfile
import zhiqiu.qizheng.PatternView
import zhiqiu.qizheng.QizhengBuilder
import zhiqiu.qizheng.QizhengChart
import zhiqiu.qizheng.ZodiacMode
import zhiqiu.qizheng.ui.QizhengSettingsPage
import zhiqiu.qizheng.ui.QizhengWheel

private val Page = Color(0xFFFFFFFF)
private val Ink = Color(0xFF222222)
private val Muted = Color(0xFF757575)
private val Line = Color(0xFFE0E0E0)
private val ChipBg = Color(0xFFF5F5F5)
private val Accent = Color(0xFF1B5E20)

/**
 * 二十八宿染色：取 7 个高对比基色（色相均匀分散），按步长 3 循环映射到 28 宿。
 * 任意相邻宿（含环首尾）色相差 ≥150°，冷暖强对比、绝不连片同色；每 7 宿才重复同一基色。
 */
private val XIU_BASE: List<Color> = listOf(
    Color(0xFFE53935), // 红
    Color(0xFFFDD835), // 黄
    Color(0xFF7CB342), // 黄绿
    Color(0xFF26A69A), // 青绿
    Color(0xFF1E88E5), // 蓝
    Color(0xFF8E24AA), // 紫
    Color(0xFFD81B60), // 品红
)
private val XIU_PALETTE: List<Color> = List(28) { XIU_BASE[(it * 3) % XIU_BASE.size] }

private enum class QzTimeMode { Natal, Demo, Custom }

@Composable
fun QizhengSection(
    profile: Profile,
    onSaveNote: ((String) -> Unit)? = null,
    /** 切换盘制时回调（盘制名），用于持久化到档案 */
    onSavePanZhi: ((String) -> Unit)? = null,
) {
    var timeMode by remember { mutableStateOf(QzTimeMode.Demo) }
    var subTab by remember { mutableIntStateOf(0) }
    var noteDraft by remember(profile.id) { mutableStateOf(profile.qizhengNote) }
    var cy by remember { mutableIntStateOf(2026) }
    var cm by remember { mutableIntStateOf(8) }
    var cd by remember { mutableIntStateOf(29) }
    var ch by remember { mutableIntStateOf(14) }
    var cmi by remember { mutableIntStateOf(5) }

    var xiuTintEnabled by remember { mutableStateOf(false) }
    var xiuColors by remember { mutableStateOf(XIU_PALETTE) }
    // 独立设置页开关
    var showSettings by remember { mutableStateOf(false) }

    // 固定命宫：-1 = 自动（时加太阳数至卯），0..11 = 子..亥（MOIRA 式手动安命）
    var fixedMing by rememberSaveable(profile.id) { mutableIntStateOf(-1) }
    // 化曜流派：0 = 果老化曜，1 = 天官化曜
    var huaYaoSchoolIdx by rememberSaveable(profile.id) { mutableIntStateOf(0) }

    // 盘制：坐标（0黄道回归 1黄道恒星 2赤道恒星）× 宿制（0果老 1回归今宿 2回归古宿 3古宿岁差 4郑案今宿）
    var coordIdx by rememberSaveable(profile.id) { mutableIntStateOf(initialPanZhiIdxs(profile.qizhengPanZhi).first) }
    var xiuIdx by rememberSaveable(profile.id) { mutableIntStateOf(initialPanZhiIdxs(profile.qizhengPanZhi).second) }

    val chart = remember(profile, timeMode, noteDraft, cy, cm, cd, ch, cmi, coordIdx, xiuIdx, fixedMing, huaYaoSchoolIdx) {
        runCatching {
            val base = when (xiuIdx) {
                1 -> PanZhiPresets.ModernReading
                2 -> PanZhiPresets.Ancient
                3 -> PanZhiPresets.AncientJ2000
                4 -> PanZhiPresets.ZhengAn
                else -> PanZhiPresets.GuoLao
            }
            val cfg = base.copy(
                zodiac = if (coordIdx == 0) ZodiacMode.TROPICAL else ZodiacMode.SIDEREAL,
                equatorial = coordIdx == 2,
                fixedMingBranch = fixedMing.takeIf { it >= 0 },
                huaYaoSchool = if (huaYaoSchoolIdx == 1) HuaYaoSchool.TIANGUAN else HuaYaoSchool.GUOLAO,
            )
            when (timeMode) {
                QzTimeMode.Demo -> QizhengBuilder.build(
                    year = 2026, month = 8, day = 29, hour = 14, minute = 5,
                    gender = profile.gender.ifBlank { "男" },
                    lon = profile.longitude ?: 116.4074,
                    lat = profile.latitude ?: 39.9042,
                    note = noteDraft,
                    config = cfg,
                )
                QzTimeMode.Natal -> buildFromProfile(
                    profile, note = noteDraft, config = cfg,
                )
                QzTimeMode.Custom -> buildFromProfile(
                    profile, cy, cm, cd, ch, cmi, note = noteDraft, config = cfg,
                )
            }
        }.getOrNull()
    }

    if (chart == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("七政排盘失败", color = Ink)
        }
        return
    }

    // 独立设置页：整页替换排盘内容，返回后按新设置重排
    if (showSettings) {
        QizhengSettingsPage(
            coordIdx = coordIdx,
            xiuIdx = xiuIdx,
            fixedMing = fixedMing,
            huaYaoIdx = huaYaoSchoolIdx,
            xiuTint = xiuTintEnabled,
            onCoordChange = {
                coordIdx = it
                onSavePanZhi?.invoke(persistPanZhiName(coordIdx, xiuIdx))
            },
            onXiuChange = {
                xiuIdx = it
                onSavePanZhi?.invoke(persistPanZhiName(coordIdx, xiuIdx))
            },
            onFixedMingChange = { fixedMing = it },
            onHuaYaoChange = { huaYaoSchoolIdx = it },
            onXiuTintChange = { xiuTintEnabled = it },
            onBack = { showSettings = false },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Page)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        // 顶栏：盘制（必须标明）+ 四柱干支
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(
                    "盘制 ${chart.panZhi}",
                    color = Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(chart.panZhiDetail, color = Muted, fontSize = 10.sp)
                if (chart.shenBranch != null) {
                    Text("身宫 ${chart.shenBranch}", color = Muted, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    chart.stemLine.forEach { c ->
                        Text(c.toString(), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(if (chart.gender == "女") "坤" else "乾", color = Muted, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    chart.branchLine.forEach { c ->
                        Text(c.toString(), color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Text(
                "⚙ 设置",
                color = Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { showSettings = true },
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 左：限运资料卡；右：盘面（略偏右）
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val limitW = 88.dp
            val gap = 6.dp
            val baseSize = min(maxWidth - limitW - gap, 560.dp) * 0.88f
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val transformState = rememberTransformableState { zoom, pan, _ ->
                scale = (scale * zoom).coerceIn(1f, 3.2f)
                if (scale > 1.01f) offset += pan else offset = Offset.Zero
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                LimitGrid(
                    chart = chart,
                    modifier = Modifier
                        .width(limitW)
                        .padding(top = 4.dp),
                    columns = 1,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(baseSize)
                        .clip(RectangleShape)
                        .background(Page)
                        .transformable(state = transformState)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.15f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 1.85f
                                    }
                                },
                            )
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            },
                    ) {
                        QizhengWheel(
                            chart = chart,
                            size = baseSize,
                            xiuColors = if (xiuTintEnabled) xiuColors else null,
                            onXiuClick = { idx ->
                                val cur = xiuColors[idx]
                                val baseIdx = XIU_BASE.indexOf(cur).takeIf { it >= 0 } ?: 0
                                xiuColors = xiuColors.toMutableList().apply {
                                    this[idx] = XIU_BASE[(baseIdx + 1) % XIU_BASE.size]
                                }
                            },
                        )
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (xiuTintEnabled) Color(0xFFE8F5E9) else ChipBg,
                                    RoundedCornerShape(4.dp),
                                )
                                .border(
                                    1.dp,
                                    if (xiuTintEnabled) Accent else Line,
                                    RoundedCornerShape(4.dp),
                                )
                                .clickable { xiuTintEnabled = !xiuTintEnabled }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        ) {
                            Text(
                                "染二十八宿",
                                color = if (xiuTintEnabled) Accent else Ink,
                                fontSize = 10.sp,
                            )
                        }
                        ZoomBtn("−") {
                            scale = (scale - 0.25f).coerceAtLeast(1f)
                            if (scale <= 1.01f) offset = Offset.Zero
                        }
                        ZoomBtn("+") { scale = (scale + 0.25f).coerceAtMost(3.2f) }
                    }
                    Text(
                        "双指缩放 · 双击放大 · 点宿染色",
                        color = Muted,
                        fontSize = 9.sp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 时间条：编辑 | 时间 | 本命
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TimeChip(
                text = "编辑",
                selected = timeMode == QzTimeMode.Custom,
                onClick = { timeMode = QzTimeMode.Custom },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Line, RoundedCornerShape(4.dp))
                    .clickable { timeMode = QzTimeMode.Custom }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    buildString {
                        append(chart.year.toString().padStart(4, '0'))
                        append('-')
                        append(chart.month.toString().padStart(2, '0'))
                        append('-')
                        append(chart.day.toString().padStart(2, '0'))
                        append(' ')
                        append(chart.hour.toString().padStart(2, '0'))
                        append(':')
                        append(chart.minute.toString().padStart(2, '0'))
                    },
                    color = Ink,
                    fontSize = 13.sp,
                )
            }
            TimeChip(
                text = "本命",
                selected = timeMode == QzTimeMode.Natal,
                onClick = { timeMode = QzTimeMode.Natal },
            )
            TimeChip(
                text = "演示",
                selected = timeMode == QzTimeMode.Demo,
                onClick = { timeMode = QzTimeMode.Demo },
            )
        }

        if (timeMode == QzTimeMode.Custom) {
            Spacer(modifier = Modifier.height(6.dp))
            TimeEditRow(cy, cm, cd, ch, cmi) { y, m, d, h, mi ->
                cy = y; cm = m; cd = d; ch = h; cmi = mi
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 子 Tab
        val tabs = listOf("四柱", "化曜", "相位", "星格", "批注")
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { i, title ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { subTab = i }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        title,
                        color = if (subTab == i) Accent else Ink,
                        fontSize = 13.sp,
                        fontWeight = if (subTab == i) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(2.dp)
                            .background(if (subTab == i) Accent else Color.Transparent),
                    )
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Line))

        Spacer(modifier = Modifier.height(8.dp))
        when (subTab) {
            0 -> PillarsPane(chart)
            1 -> HuaYaoPane(
                chart = chart,
                school = if (huaYaoSchoolIdx == 1) HuaYaoSchool.TIANGUAN else HuaYaoSchool.GUOLAO,
                onSchoolChange = { huaYaoSchoolIdx = if (it == HuaYaoSchool.TIANGUAN) 1 else 0 },
            )
            2 -> AspectsPane(chart)
            3 -> PatternsPane(chart)
            else -> NotesPane(
                note = noteDraft,
                onNoteChange = { noteDraft = it },
                onSave = onSaveNote,
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun MingChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) Color(0xFFE8F5E9) else ChipBg, RoundedCornerShape(4.dp))
            .border(1.dp, if (selected) Accent else Line, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(text, color = if (selected) Accent else Ink, fontSize = 11.sp)
    }
}

@Composable
private fun TimeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) Color(0xFFE8F5E9) else ChipBg, RoundedCornerShape(4.dp))
            .border(1.dp, if (selected) Accent else Line, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(text, color = Ink, fontSize = 12.sp)
    }
}

@Composable
private fun TimeEditRow(
    year: Int, month: Int, day: Int, hour: Int, minute: Int,
    onChange: (Int, Int, Int, Int, Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TinyIntField(year, 4) { onChange(it, month, day, hour, minute) }
        Text("/", fontSize = 12.sp)
        TinyIntField(month, 2) { onChange(year, it.coerceIn(1, 12), day, hour, minute) }
        Text("/", fontSize = 12.sp)
        TinyIntField(day, 2) { onChange(year, month, it.coerceIn(1, 31), hour, minute) }
        Spacer(modifier = Modifier.width(8.dp))
        TinyIntField(hour, 2) { onChange(year, month, day, it.coerceIn(0, 23), minute) }
        Text(":", fontSize = 12.sp)
        TinyIntField(minute, 2) { onChange(year, month, day, hour, it.coerceIn(0, 59)) }
    }
}

@Composable
private fun TinyIntField(value: Int, maxLen: Int, onValue: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { raw ->
            val digits = raw.filter { it.isDigit() }.take(maxLen)
            if (digits.isNotEmpty()) onValue(digits.toInt())
        },
        modifier = Modifier.width(if (maxLen >= 4) 72.dp else 52.dp),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
    )
}

@Composable
private fun ZoomBtn(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(ChipBg, CircleShape)
            .border(1.dp, Line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 16.sp, color = Ink, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LimitGrid(
    chart: QizhengChart,
    modifier: Modifier = Modifier,
    columns: Int = 3,
) {
    val l = chart.limits
    val items = listOf(
        "大限" to l.daXian,
        "太岁" to l.taiSui,
        "小限" to l.xiaoXian,
        "月限" to l.yueXian,
        "缠木" to l.shanMu,
        "顶星" to l.dingXing,
    )
    Column(
        modifier = modifier
            .background(ChipBg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.chunked(columns.coerceAtLeast(1)).forEach { row ->
            if (columns <= 1) {
                row.forEach { (k, v) ->
                    Column {
                        Text(k, color = Muted, fontSize = 10.sp)
                        Text(v, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { (k, v) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.Start,
                        ) {
                            Text(k, color = Muted, fontSize = 10.sp)
                            Text(v, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    repeat(columns - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PillarsPane(chart: QizhengChart) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(chart.termLabel, fontSize = 12.sp, color = Ink)
        Text("阳历：${chart.solarLabel}", fontSize = 13.sp, color = Ink)
        Text("农历：${chart.lunarLabel}", fontSize = 13.sp, color = Ink)
        Text(
            "${if (chart.gender == "女") "坤造" else "乾造"}：${chart.baziLabel} ${chart.nayin}",
            fontSize = 13.sp,
            color = Ink,
            fontWeight = FontWeight.Medium,
        )
        Text("实宫：${chart.solidBranches}  虚宫：${chart.emptyBranches}", fontSize = 12.sp, color = Ink)
    }
}

@Composable
private fun HuaYaoPane(chart: QizhengChart, school: HuaYaoSchool, onSchoolChange: (HuaYaoSchool) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
        // 流派切换：果老化曜 / 天官化曜
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MingChip("果老化曜", school == HuaYaoSchool.GUOLAO) { onSchoolChange(HuaYaoSchool.GUOLAO) }
            MingChip("天官化曜", school == HuaYaoSchool.TIANGUAN) { onSchoolChange(HuaYaoSchool.TIANGUAN) }
        }
        Spacer(modifier = Modifier.height(6.dp))
        val cols = chart.huaYao
        if (cols.isEmpty()) {
            Text("无化曜标注", color = Muted)
        } else {
            // 仿参考盘「天星化曜」：每星一列，列头星名，列内该星所化的诸名
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                cols.forEach { col ->
                    Column(
                        modifier = Modifier
                            .background(ChipBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 4.dp)
                            .width(44.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            col.star,
                            color = Accent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        col.labels.forEach { label ->
                            Text(label, color = Ink, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 相位页：立命 / 安身 + 星曜位置表（宿度、宫度、逆速、庙旺利陷），版式仿参考盘。
 */
@Composable
private fun AspectsPane(chart: QizhengChart) {
    val fmt2 = { v: Double ->
        val t = kotlin.math.round(v * 100).toLong()
        val i = t / 100
        val f = kotlin.math.abs(t % 100)
        if (f == 0L) "$i.00" else if (f < 10) "$i.0$f" else "$i.$f"
    }
    val moon = chart.stars.firstOrNull { it.key == "月" }
    val mingLoc = runCatching {
        zhiqiu.qizheng.XiuTable.locate(
            chart.mingDuLon,
            chart.xiuZeroDeg,
            chart.config.xiuSystem,
            chart.config.xiuFrame,
            chart.config.equatorial,
        )
    }.getOrNull()
    val mingInPalace = ((chart.mingDuLon % 30.0) + 30.0) % 30.0

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        // 立命 / 安身
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(
                "立命：${chart.mingBranch}${fmt2(mingInPalace)} ${mingLoc?.name ?: "？"}${mingLoc?.degreeInXiu?.let { fmt2(it) } ?: "？"}",
                fontSize = 17.sp,
                color = Ink,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            moon?.let { m ->
                Text(
                    "安身：${m.branch}${fmt2(((m.longitude % 30.0) + 30.0) % 30.0)} ${m.xiu}${fmt2(m.xiuDegree)}",
                    fontSize = 17.sp,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        HorizontalDivider(color = Line)

        // 星曜位置表
        chart.stars.forEach { s ->
            val motion = when {
                s.retro -> "逆"
                kotlin.math.abs(s.speed) >= 1.0 -> "速"
                else -> ""
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(s.key, fontSize = 15.sp, color = Ink, modifier = Modifier.width(40.dp))
                Text("${s.xiu}${fmt2(s.xiuDegree)}°", fontSize = 15.sp, color = Ink, modifier = Modifier.width(120.dp))
                Text("${s.branch}${fmt2(((s.longitude % 30.0) + 30.0) % 30.0)}°", fontSize = 15.sp, color = Ink, modifier = Modifier.width(110.dp))
                Text(motion, fontSize = 13.sp, color = Muted, modifier = Modifier.width(36.dp))
                chart.dignity[s.key]?.let { level ->
                    Text(level, fontSize = 13.sp, color = Accent)
                }
            }
            HorizontalDivider(color = Line, thickness = 0.5.dp)
        }
    }
}

/**
 * 星格页：政余喜格 / 政余忌格 两列，只列出成格条目。
 */
@Composable
private fun PatternsPane(chart: QizhengChart) {
    val xi = chart.patterns.filter { it.auspicious && it.hit }
    val ji = chart.patterns.filter { !it.auspicious && it.hit }
    var selected by remember { mutableStateOf<PatternView?>(null) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("政余喜格", fontSize = 16.sp, color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("政余忌格", fontSize = 16.sp, color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            PatternColumn(xi, Modifier.weight(1f), onPick = { selected = it })
            PatternColumn(ji, Modifier.weight(1f), onPick = { selected = it })
        }
    }

    selected?.let { p ->
        Dialog(onDismissRequest = { selected = null }) {
            Column(
                modifier = Modifier
                    .background(Page, RoundedCornerShape(10.dp))
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.name, fontSize = 18.sp, color = Ink, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (p.auspicious) "喜格" else "忌格",
                        fontSize = 12.sp,
                        color = if (p.auspicious) Accent else Color(0xFFC62828),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("判定：${p.detail}", fontSize = 13.sp, color = Muted)
                if (p.desc.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(p.desc, fontSize = 14.sp, color = Ink, lineHeight = 20.sp)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "知道了",
                    color = Accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable { selected = null },
                )
            }
        }
    }
}

@Composable
private fun PatternColumn(
    items: List<PatternView>,
    modifier: Modifier,
    onPick: (PatternView) -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (items.isEmpty()) {
            Text("无", fontSize = 15.sp, color = Muted)
        } else {
            items.forEach { p ->
                Text(
                    p.name,
                    fontSize = 15.sp,
                    color = Ink,
                    modifier = Modifier.clickable { onPick(p) },
                )
            }
        }
    }
}

/** 批注：用户手写观察与断语，800ms 防抖自动保存到档案 */
@Composable
private fun NotesPane(
    note: String,
    onNoteChange: (String) -> Unit,
    onSave: ((String) -> Unit)?,
) {
    var saved by remember { mutableStateOf(true) }

    LaunchedEffect(note, onSave) {
        if (onSave != null) {
            delay(800)
            onSave(note)
            saved = true
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("批注", fontSize = 16.sp, color = Ink, fontWeight = FontWeight.Bold)
            Text(
                when {
                    onSave == null -> ""
                    saved -> "已自动保存"
                    else -> "编辑中…"
                },
                fontSize = 12.sp,
                color = Muted,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        BasicTextField(
            value = note,
            onValueChange = {
                onNoteChange(it)
                saved = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color.White, RoundedCornerShape(10.dp))
                .border(1.dp, Line, RoundedCornerShape(10.dp))
                .padding(12.dp),
            textStyle = TextStyle(fontSize = 14.sp, color = Ink, lineHeight = 21.sp),
            decorationBox = { inner ->
                Column {
                    if (note.isEmpty()) {
                        Text(
                            "在此写下这张盘的观察、断语与心得，会自动保存到该档案…",
                            fontSize = 14.sp,
                            color = Muted,
                            lineHeight = 21.sp,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

private fun fmt1(v: Double): String {
    val t = kotlin.math.round(v * 10).toInt()
    return "${t / 10}.${kotlin.math.abs(t % 10)}"
}

/** 档案持久化的盘制名 →（坐标 idx, 宿制 idx）；未知名按果老默认 */
private fun initialPanZhiIdxs(name: String): Pair<Int, Int> = when (name) {
    "回归今宿" -> 0 to 1
    "回归古宿" -> 0 to 2
    "古宿岁差" -> 0 to 3
    "郑案今宿" -> 1 to 4
    "赤道恒星" -> 2 to 1
    else -> 0 to 0
}

/** （坐标 idx, 宿制 idx）→ 持久化盘制名 */
private fun persistPanZhiName(coordIdx: Int, xiuIdx: Int): String =
    if (coordIdx == 2) "赤道恒星"
    else listOf("果老星宗", "回归今宿", "回归古宿", "古宿岁差", "郑案今宿")[xiuIdx.coerceIn(0, 4)]
