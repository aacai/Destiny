package zhiqiu.xiaoliuren

import com.tyme.solar.SolarTime
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** 起课三步中的一步：从哪一宫起、数几位、落在哪一宫。 */
data class Step(
    val title: String,
    val countText: String,
    val start: Palace,
    val land: Palace,
    val count: Int,
)

/** 农历课式的日期背景；报数起课没有。 */
data class LunarStamp(
    val month: Int,
    val day: Int,
    val isLeapMonth: Boolean,
    /** 地支序号，子=0 … 亥=11 */
    val hourBranchIndex: Int,
) {
    val monthText: String
        get() = lunarMonthLabel(month, isLeapMonth)

    val dayText: String
        get() = lunarDayLabel(day)

    val hourText: String
        get() = branchName(hourBranchIndex) + "时"

    /** 如「农历九月初四酉时」 */
    val dateText: String
        get() = "农历$monthText$dayText$hourText"
}

/**
 * 小六壬课式：三步逐层顺数，最终以第三步落宫断事。
 *
 * [monthCount] / [dayCount] / [hourCount] 是真正参与顺数的三个数，
 * 农历起课与报数起课只差在这三个数的来源，推演完全同式。
 */
data class XiaoLiuRenChart(
    val monthCount: Int,
    val dayCount: Int,
    val hourCount: Int,
    val monthPalace: Palace,
    val dayPalace: Palace,
    val hourPalace: Palace,
    val steps: List<Step>,
    /** 农历起课时的日期，报数起课为 null */
    val lunar: LunarStamp? = null,
    /** 阳历时间原文，仅按时刻起课时有值 */
    val solarText: String = "",
) {
    /** 落宫即断事之宫 */
    val palace: Palace
        get() = hourPalace

    val luck: Luck
        get() = palace.luck

    /** 课式来源的一句话描述：农历日期或所报三数 */
    val castText: String
        get() = lunar?.dateText ?: "报数 $monthCount、$dayCount、$hourCount"
}

/**
 * 从 [start] 宫起计「一」，顺数 [count] 位后所落宫的序号（0..5）。
 *
 * 掌诀顺序固定：大安(0) → 留连(1) → 速喜(2) → 赤口(3) → 小吉(4) → 空亡(5)，再回大安。
 */
fun advanceFrom(start: Int, count: Int): Int = floorMod(start + count - 1, Palace.ORDER_SIZE)

/** 顺数过程中依次经过的宫位序号，供掌诀图画行进路线。 */
fun countPath(start: Int, count: Int): List<Int> = (0 until count).map { floorMod(start + it, Palace.ORDER_SIZE) }

/** 报数上限，够用且不让掌诀图上的顺数失去意义。 */
const val MAX_COUNT = 999

/**
 * 三步推演：大安起数至月数，月落宫起数至日数，日落宫起数至时数。
 * 三个数依次作月、日、时之用，故农历起课与报数起课共用此式。
 */
fun castCounts(
    monthCount: Int,
    dayCount: Int,
    hourCount: Int,
    monthText: String = "第一数",
    dayText: String = "第二数",
    hourText: String = "第三数",
    lunar: LunarStamp? = null,
    solarText: String = "",
): XiaoLiuRenChart {
    require(monthCount in 1..MAX_COUNT) { "月数须在 1..$MAX_COUNT，实际为 $monthCount" }
    require(dayCount in 1..MAX_COUNT) { "日数须在 1..$MAX_COUNT，实际为 $dayCount" }
    require(hourCount in 1..MAX_COUNT) { "时数须在 1..$MAX_COUNT，实际为 $hourCount" }

    // 第一步：大安起正月，顺数到当月
    val monthIdx = advanceFrom(Palace.DA_AN.ordinal, monthCount)
    // 第二步：月落宫起初一，顺数到当日
    val dayIdx = advanceFrom(monthIdx, dayCount)
    // 第三步：日落宫起子时，顺数到当时
    val hourIdx = advanceFrom(dayIdx, hourCount)

    val monthPalace = Palace.of(monthIdx)
    val dayPalace = Palace.of(dayIdx)
    val hourPalace = Palace.of(hourIdx)

    return XiaoLiuRenChart(
        monthCount = monthCount,
        dayCount = dayCount,
        hourCount = hourCount,
        monthPalace = monthPalace,
        dayPalace = dayPalace,
        hourPalace = hourPalace,
        steps = listOf(
            Step("月", monthText, Palace.DA_AN, monthPalace, monthCount),
            Step("日", dayText, monthPalace, dayPalace, dayCount),
            Step("时", hourText, dayPalace, hourPalace, hourCount),
        ),
        lunar = lunar,
        solarText = solarText,
    )
}

/**
 * 农历起课：农历月 1..12（闰月按本月计，由 [isLeapMonth] 标注）、农历日 1..30、时辰地支序号 子=0..亥=11。
 *
 * 时辰用「子时为一」的序数参与顺数，故地支序号 4（辰）数五位。
 */
fun cast(
    lunarMonth: Int,
    lunarDay: Int,
    hourBranchIndex: Int,
    isLeapMonth: Boolean = false,
    solarText: String = "",
): XiaoLiuRenChart {
    require(lunarMonth in 1..12) { "农历月须在 1..12，实际为 $lunarMonth" }
    require(lunarDay in 1..30) { "农历日须在 1..30，实际为 $lunarDay" }
    require(hourBranchIndex in 0..11) { "时辰地支须在 0..11，实际为 $hourBranchIndex" }

    val lunar = LunarStamp(lunarMonth, lunarDay, isLeapMonth, hourBranchIndex)
    return castCounts(
        monthCount = lunarMonth,
        dayCount = lunarDay,
        hourCount = hourBranchIndex + 1,
        monthText = lunar.monthText,
        dayText = lunar.dayText,
        hourText = lunar.hourText,
        lunar = lunar,
        solarText = solarText,
    )
}

/** 由阳历时刻起课：取该时刻所在农历月日与时辰地支。 */
fun castFromSolar(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): XiaoLiuRenChart {
    val lunarHour = SolarTime(year, month, day, hour, minute, 0).getLunarHour()
    val lunarDay = lunarHour.getLunarDay()
    return cast(
        lunarMonth = kotlin.math.abs(lunarDay.month),
        lunarDay = lunarDay.day,
        // 23 点后为夜子时，仍属当日子时序，取模后归 0
        hourBranchIndex = lunarHour.getIndexInDay() % 12,
        isLeapMonth = lunarDay.month < 0,
        solarText = formatSolar(year, month, day, hour, minute),
    )
}

/** 以设备当前时间起课。 */
fun castNow(): XiaoLiuRenChart {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return castFromSolar(now.year, now.monthNumber, now.dayOfMonth, now.hour, now.minute)
}

private fun formatSolar(year: Int, month: Int, day: Int, hour: Int, minute: Int): String =
    "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-" +
        "${day.toString().padStart(2, '0')} ${hour.toString().padStart(2, '0')}:" +
        minute.toString().padStart(2, '0')

private val LUNAR_MONTH_LABELS = listOf("正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月")

fun lunarMonthLabel(month: Int, leap: Boolean): String =
    (if (leap) "闰" else "") + LUNAR_MONTH_LABELS[month - 1]

fun lunarDayLabel(day: Int): String = when {
    day == 10 -> "初十"
    day == 20 -> "二十"
    day == 30 -> "三十"
    day < 10 -> "初" + "一二三四五六七八九"[day - 1]
    day < 20 -> "十" + "一二三四五六七八九"[day - 11]
    else -> "廿" + "一二三四五六七八九"[day - 21]
}
