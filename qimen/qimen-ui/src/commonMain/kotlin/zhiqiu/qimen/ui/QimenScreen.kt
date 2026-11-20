package zhiqiu.qimen.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import zhiqiu.app.destiny.oinkcompat.AlertDialog
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import zhiqiu.qimen.DayBoundary
import zhiqiu.qimen.Palace
import zhiqiu.qimen.PlateKind
import zhiqiu.qimen.QimenChart
import zhiqiu.qimen.cast

private enum class TimeMode(val label: String) {
    Now(QimenStrings.t("now")),
    Pick(QimenStrings.t("pick")),
}

/** 奇门的一次存档：中性字段，由宿主组装成档案。 */
data class QimenSave(
    val id: String,
    val name: String,
    val note: String,
    val groupName: String,
    val mode: String = "Now",
    val question: String = "",
    val year: Int = 0,
    val month: Int = 0,
    val day: Int = 0,
    val hour: Int = 0,
    val minute: Int = 0,
    val plate: String = "Zhuan",
    val boundary: String = "Hour23",
    val solarText: String = "",
    val pillarText: String = "",
    val yuanText: String = "",
    val dunText: String = "",
)

@Composable
fun QimenScreen(
    onBack: () -> Unit,
    darkTheme: Boolean = false,
    onSave: (QimenSave) -> Unit = {},
) {
    QimenThemeProvider(darkTheme = darkTheme) {
        ScreenContent(onBack = onBack, onSave = onSave)
    }
}

@Composable
private fun ScreenContent(onBack: () -> Unit, onSave: (QimenSave) -> Unit) {
    val seed = remember { deviceNow() }
    var mode by remember { mutableStateOf(TimeMode.Now) }
    var plate by remember { mutableStateOf(PlateKind.Zhuan) }
    var boundary by remember { mutableStateOf(DayBoundary.Hour23) }
    var question by remember { mutableStateOf("") }
    var snapshot by remember { mutableStateOf(seed) }
    var year by remember { mutableIntStateOf(seed.year) }
    @Suppress("DEPRECATION")
    var month by remember { mutableIntStateOf(seed.monthNumber) }
    var day by remember { mutableIntStateOf(seed.day) }
    var hour by remember { mutableIntStateOf(seed.hour) }
    var minute by remember { mutableIntStateOf(seed.minute) }
    var selected by remember { mutableStateOf<Palace?>(null) }
    var showRule by remember { mutableStateOf(false) }

    val whenYear = if (mode == TimeMode.Now) snapshot.year else year
    @Suppress("DEPRECATION")
    val whenMonth = if (mode == TimeMode.Now) snapshot.monthNumber else month
    val whenDay = if (mode == TimeMode.Now) snapshot.day else day
    val whenHour = if (mode == TimeMode.Now) snapshot.hour else hour
    val whenMinute = if (mode == TimeMode.Now) snapshot.minute else minute
    val casted = runCatching {
        cast(whenYear, whenMonth, whenDay, whenHour, whenMinute, plate, boundary)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QimenTheme.page),
    ) {
        // 顶栏固定
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                QimenStrings.backShort,
                color = QimenTheme.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Text(
                "奇门遁甲",
                color = QimenTheme.ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(QimenTheme.panel)
                    .border(1.dp, QimenTheme.line, CircleShape)
                    .clickable { showRule = true },
                contentAlignment = Alignment.Center,
            ) {
                Text("？", color = QimenTheme.muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
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
        SectionCard(QimenStrings.question) {
            BasicTextField(
                value = question,
                onValueChange = { question = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = QimenTheme.ink),
                cursorBrush = SolidColor(QimenTheme.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(QimenTheme.panel)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                decorationBox = { inner ->
                    Box {
                        if (question.isEmpty()) {
                            Text("所问之事（可留空）", color = QimenTheme.muted, fontSize = 14.sp)
                        }
                        inner()
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        SectionCard("起局") {
            SegmentedControl(
                options = TimeMode.entries.map { it.label },
                selectedIndex = mode.ordinal,
                onSelect = { index ->
                    val next = TimeMode.entries[index]
                    if (next == TimeMode.Now) snapshot = deviceNow()
                    mode = next
                },
            )
            if (mode == TimeMode.Pick) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Stepper("年", year, 1900, 2100) {
                        year = it
                        day = day.coerceAtMost(daysInMonth(it, month))
                    }
                    Stepper("月", month, 1, 12) {
                        month = it
                        day = day.coerceAtMost(daysInMonth(year, it))
                    }
                    Stepper("日", day, 1, daysInMonth(year, month)) { day = it }
                    Stepper("时", hour, 0, 23) { hour = it }
                    Stepper("分", minute, 0, 59) { minute = it }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            SegmentedControl(
                options = listOf("转盘", "飞盘"),
                selectedIndex = if (plate == PlateKind.Zhuan) 0 else 1,
                onSelect = { plate = if (it == 0) PlateKind.Zhuan else PlateKind.Fei },
            )
            Spacer(modifier = Modifier.height(10.dp))
            SegmentedControl(
                options = listOf("日界 23:00", "日界 00:00"),
                selectedIndex = if (boundary == DayBoundary.Hour23) 0 else 1,
                onSelect = { boundary = if (it == 0) DayBoundary.Hour23 else DayBoundary.Midnight },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        casted.fold(
            onSuccess = { chart ->
                if (question.isNotBlank()) {
                    Text(question, color = QimenTheme.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                InfoCard(chart)
                Spacer(modifier = Modifier.height(10.dp))
                QimenBoard(chart = chart, selected = selected, onSelect = { selected = it })
                selected?.let { palace ->
                    Spacer(modifier = Modifier.height(10.dp))
                    PalaceDetail(chart, palace)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(QimenTheme.accent)
                        .clickable {
                            val id = buildString { repeat(32) { append("abcdefghijklmnopqrstuvwxyz0123456789"[kotlin.random.Random.nextInt(36)]) } }
                            val summary = buildString {
                                if (question.isNotBlank()) appendLine("【占问】${question.trim()}")
                                appendLine("【时间】${chart.solarText}")
                                appendLine("【四柱】${chart.pillarText}")
                                appendLine("【局式】${chart.termName}${chart.yuanText} · ${chart.dunText}")
                                appendLine("【值符】${chart.zhiFu.label}　【值使】${chart.zhiShi.full}")
                            }
                            onSave(
                                QimenSave(
                                    id = id,
                                    name = question.trim().ifBlank { chart.solarText },
                                    note = summary,
                                    groupName = "奇门",
                                    mode = mode.name, question = question,
                                    year = whenYear, month = whenMonth, day = whenDay,
                                    hour = whenHour, minute = whenMinute,
                                    plate = plate.name, boundary = boundary.name,
                                    solarText = chart.solarText, pillarText = chart.pillarText,
                                    yuanText = chart.yuanText, dunText = chart.dunText,
                                ),
                            )
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(QimenStrings.saveJu, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            },
            onFailure = { error ->
                Text(
                    "这个时间排不出来",
                    color = QimenTheme.unlucky,
                    fontSize = 13.sp,
                )
                val detail = error.message?.takeIf { it.isNotBlank() && !it.startsWith("index", ignoreCase = true) }
                if (detail != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(detail, color = QimenTheme.muted, fontSize = 11.sp)
                }
            },
        )
        Spacer(modifier = Modifier.height(16.dp))
        } // scroll body
    }

    if (showRule) {
        RuleDialog(onDismiss = { showRule = false })
    }
}

@Composable
private fun InfoCard(chart: QimenChart) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(QimenTheme.card)
            .border(1.dp, QimenTheme.line.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(chart.solarText, color = QimenTheme.muted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(chart.pillarText, color = QimenTheme.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "${chart.termName}${chart.yuanText} · ${chart.dunText}",
            color = QimenTheme.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "旬首${chart.xunLabel}遁${chart.dunStem.label}  空${chart.kongText}",
            color = QimenTheme.ink,
            fontSize = 12.sp,
        )
        Text(
            "值符${chart.zhiFu.label}  值使${chart.zhiShi.full}  马${chart.horse.label}",
            color = QimenTheme.ink,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun PalaceDetail(chart: QimenChart, palace: Palace) {
    val cell = chart.cell(palace)
    SectionCard("${palace.shortName} · ${palace.direction}") {
        if (chart.plate == PlateKind.Zhuan && palace == Palace.Zhong) {
            Text(
                "转盘中宫不留星门神。天禽随天芮，中宫地盘${chart.centerEarth.label}寄在坤二。",
                color = QimenTheme.ink,
                fontSize = 13.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("${chart.dunText} ${chart.yuanText}", color = QimenTheme.accent, fontSize = 13.sp)
            Text("值符 ${chart.zhiFu.label}", color = QimenTheme.ink, fontSize = 13.sp)
            Text("值使 ${chart.zhiShi.full}", color = QimenTheme.ink, fontSize = 13.sp)
            return@SectionCard
        }
        DetailLine("神", cell.deity?.label(chart.yang) ?: "—")
        DetailLine(
            "星",
            cell.stars.joinToString(" ") { star ->
                if (star == chart.zhiFu) "${star.label}（值符）" else star.label
            }.ifEmpty { "—" },
        )
        DetailLine(
            "门",
            cell.door?.let { door ->
                if (door == chart.zhiShi) "${door.full}（值使）" else door.full
            } ?: "—",
        )
        DetailLine("天盘", cell.heavenStems.joinToString("") { it.label }.ifEmpty { "—" })
        DetailLine("地盘", cell.earthStems.joinToString("") { it.label }.ifEmpty { "—" })
        DetailLine("角标", cell.marks.joinToString(" ") { it.label }.ifEmpty { "—" })
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Text(label, color = QimenTheme.muted, fontSize = 12.sp, modifier = Modifier.width(36.dp))
        Text(value, color = QimenTheme.ink, fontSize = 13.sp)
    }
}

@Composable
private fun RuleDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = QimenTheme.card,
        title = { Text("时家奇门", color = QimenTheme.ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                RuleLine("定局", "拆补。符头取甲己日，用符头日界所在节气定上中下元，交节后、下一符头前仍用上一节气。")
                RuleLine("转盘", "星门神沿八宫环转，阳顺阴逆。天禽随天芮，中五寄坤，坤宫可有两干。")
                RuleLine("飞盘", "星按蓬芮冲辅禽心柱任英、门按休死伤杜开惊生景，沿洛书飞。中宫入局，八门八神各空一宫。")
                RuleLine("日界", "23:00 子初换日柱。00:00 则二十三点仍用当日日柱，时辰仍是子时。")
                RuleLine("角标", "空是时旬空亡，马是时支驿马，墓、刑看天盘干与地盘干，迫是门克宫。")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = QimenTheme.accent, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}

@Composable
private fun RuleLine(title: String, detail: String) {
    Text(title, color = QimenTheme.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(4.dp))
    Text(detail, color = QimenTheme.muted, fontSize = 12.sp)
    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(QimenTheme.card)
            .border(1.dp, QimenTheme.line.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(15.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(QimenTheme.accent),
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(title, color = QimenTheme.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
            .background(QimenTheme.panel)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) QimenTheme.card else QimenTheme.panel)
                    .clickable { onSelect(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (active) QimenTheme.ink else QimenTheme.muted,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun RowScope.Stepper(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = QimenTheme.muted, fontSize = 10.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton("−") { if (value > min) onChange(value - 1) }
            Text(
                value.toString(),
                color = QimenTheme.ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            StepButton("+") { if (value < max) onChange(value + 1) }
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = QimenTheme.accent,
        fontSize = 16.sp,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp),
    )
}

private fun deviceNow(): LocalDateTime =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

private fun daysInMonth(year: Int, month: Int): Int {
    val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    return when (month) {
        2 -> if (leap) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
}
