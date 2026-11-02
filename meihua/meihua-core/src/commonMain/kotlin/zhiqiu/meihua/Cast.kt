package zhiqiu.meihua

import com.tyme.solar.SolarTime
import kotlin.random.Random
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import zhiqiu.liuyao.Trigram

const val MAX_NUMBER = 9999

/**
 * 时间起卦（农历口径）：
 * 上卦 = (年支序+月+日) ÷8 余；下卦 = (年+月+日+时) ÷8 余；动爻 = 同总和 ÷6 余。
 * 年/时：子=1…亥=12；月日：农历数。
 */
fun castFromLunar(
    yearBranchNum: Int,
    lunarMonth: Int,
    lunarDay: Int,
    hourBranchNum: Int,
    isLeapMonth: Boolean = false,
    yearBranchLabel: String = branchLabelOfNum(yearBranchNum),
    hourBranchLabel: String = branchLabelOfNum(hourBranchNum),
    solarText: String = "",
    question: String = "",
): MeiHuaChart {
    require(yearBranchNum in 1..12)
    require(lunarMonth in 1..12)
    require(lunarDay in 1..30)
    require(hourBranchNum in 1..12)

    val sumUpper = yearBranchNum + lunarMonth + lunarDay
    val sumAll = sumUpper + hourBranchNum
    val upperN = rem8(sumUpper)
    val lowerN = rem8(sumAll)
    val moving = rem6(sumAll)
    val upper = trigramOfInnate(upperN)
    val lower = trigramOfInnate(lowerN)

    val stamp = LunarStamp(
        yearBranchNum = yearBranchNum,
        month = lunarMonth,
        day = lunarDay,
        hourBranchNum = hourBranchNum,
        isLeapMonth = isLeapMonth,
        yearBranchLabel = yearBranchLabel,
        hourBranchLabel = hourBranchLabel,
        solarText = solarText,
    )

    val leapNote = if (isLeapMonth) "（闰月按本月 $lunarMonth 计）" else ""
    val trace = CastTrace(
        method = CastMethod.TIME,
        summary = "农历${stamp.lunarText} → 上${upper.label}下${lower.label} ${yaoLabel(moving)}动",
        lines = listOf(
            TraceLine("年支", "$yearBranchLabel=$yearBranchNum"),
            TraceLine("农历月", "$lunarMonth$leapNote"),
            TraceLine("农历日", "$lunarDay"),
            TraceLine("上卦和", "$yearBranchNum+$lunarMonth+$lunarDay=$sumUpper，÷8 余 $upperN → ${upper.label}"),
            TraceLine("时支", "$hourBranchLabel=$hourBranchNum"),
            TraceLine("下卦和", "$sumUpper+$hourBranchNum=$sumAll，÷8 余 $lowerN → ${lower.label}"),
            TraceLine("动爻", "$sumAll÷6 余 $moving → ${yaoLabel(moving)}"),
        ),
    )
    return assemble(
        upper = upper,
        lower = lower,
        movingYao = moving,
        trace = trace,
        stamp = stamp,
        lunarMonthForWang = lunarMonth,
        question = question,
    )
}

/** 由阳历时刻起卦：年支取农历年（正月初一换年），非立春八字年。 */
fun castFromSolar(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int = 0,
    question: String = "",
): MeiHuaChart {
    val solar = SolarTime(year, month, day, hour, minute, 0)
    val lunarHour = solar.getLunarHour()
    val lunarDay = lunarHour.getLunarDay()
    val lunarMonthObj = lunarDay.getLunarMonth()
    val lunarYearCycle = lunarMonthObj.getLunarYear().getSixtyCycle()
    val yearBranchName = lunarYearCycle.getEarthBranch().getName()
    val yearBranchNum = branchNumOfLabel(yearBranchName)
    // 子=0…亥=11 → 梅花序 +1；夜子时仍归子
    val hourIdx0 = lunarHour.getIndexInDay() % 12
    val hourBranchNum = hourIdx0 + 1
    val lunarMonth = kotlin.math.abs(lunarMonthObj.month)
    return castFromLunar(
        yearBranchNum = yearBranchNum,
        lunarMonth = lunarMonth,
        lunarDay = lunarDay.day,
        hourBranchNum = hourBranchNum,
        isLeapMonth = lunarMonthObj.month < 0,
        yearBranchLabel = yearBranchName,
        hourBranchLabel = BRANCH_LABELS[hourIdx0],
        solarText = formatSolar(year, month, day, hour, minute),
        question = question,
    )
}

fun castNow(question: String = ""): MeiHuaChart {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    @Suppress("DEPRECATION")
    return castFromSolar(now.year, now.monthNumber, now.dayOfMonth, now.hour, now.minute, question)
}

/**
 * 数字起卦：上数÷8→上卦，下数÷8→下卦，两数之和÷6→动爻（不加时辰）。
 */
fun castFromNumbers(
    upperNumber: Int,
    lowerNumber: Int,
    lunarMonthForWang: Int? = null,
    question: String = "",
): MeiHuaChart {
    require(upperNumber in 1..MAX_NUMBER) { "上数须在 1..$MAX_NUMBER" }
    require(lowerNumber in 1..MAX_NUMBER) { "下数须在 1..$MAX_NUMBER" }

    val upperN = rem8(upperNumber)
    val lowerN = rem8(lowerNumber)
    val sum = upperNumber + lowerNumber
    val moving = rem6(sum)
    val upper = trigramOfInnate(upperN)
    val lower = trigramOfInnate(lowerN)

    val trace = CastTrace(
        method = CastMethod.NUMBERS,
        summary = "报数 $upperNumber、$lowerNumber → 上${upper.label}下${lower.label} ${yaoLabel(moving)}动",
        lines = listOf(
            TraceLine("上数", "$upperNumber÷8 余 $upperN → ${upper.label}"),
            TraceLine("下数", "$lowerNumber÷8 余 $lowerN → ${lower.label}"),
            TraceLine("动爻", "$upperNumber+$lowerNumber=$sum，÷6 余 $moving → ${yaoLabel(moving)}"),
        ),
    )
    return assemble(
        upper = upper,
        lower = lower,
        movingYao = moving,
        trace = trace,
        lunarMonthForWang = lunarMonthForWang,
        question = question,
    )
}

/**
 * 端法占：上=物所属卦，下=方位卦，动爻=(上先天数+下先天数+时支序)÷6。
 * [hourBranchNum] 子=1…亥=12。
 */
fun castFromXiang(
    upper: Trigram,
    lower: Trigram,
    hourBranchNum: Int,
    posture: Posture = Posture.STANDING,
    stamp: LunarStamp? = null,
    question: String = "",
): MeiHuaChart {
    require(hourBranchNum in 1..12)
    val u = innateNumber(upper)
    val l = innateNumber(lower)
    val cheng = u + l + hourBranchNum
    val moving = rem6(cheng)
    val hourLabel = branchLabelOfNum(hourBranchNum)

    val trace = CastTrace(
        method = CastMethod.XIANG,
        summary = "端法 物${upper.label}/方${lower.label}/$hourLabel 时 → ${yaoLabel(moving)}动",
        lines = listOf(
            TraceLine("物卦", "${upper.label}=先天$u"),
            TraceLine("方位卦", "${lower.label}=先天$l"),
            TraceLine("时支", "$hourLabel=$hourBranchNum"),
            TraceLine("成卦之数", "$u+$l+$hourBranchNum=$cheng"),
            TraceLine("动爻", "$cheng÷6 余 $moving → ${yaoLabel(moving)}"),
            TraceLine("应期", "${posture.label} → ${yingQiDaysOf(cheng, posture)} 日"),
        ),
    )
    return assemble(
        upper = upper,
        lower = lower,
        movingYao = moving,
        trace = trace,
        stamp = stamp,
        lunarMonthForWang = stamp?.month,
        chengGuaShu = cheng,
        posture = posture,
        question = question,
    )
}

/** 手动指定上卦、下卦与动爻。 */
fun castManual(
    upper: Trigram,
    lower: Trigram,
    movingYao: Int,
    lunarMonthForWang: Int? = null,
    question: String = "",
): MeiHuaChart {
    require(movingYao in 1..6)
    val trace = CastTrace(
        method = CastMethod.MANUAL,
        summary = "手选 上${upper.label}下${lower.label} ${yaoLabel(movingYao)}动",
        lines = listOf(
            TraceLine("上卦", "${upper.label}（先天${innateNumber(upper)}）"),
            TraceLine("下卦", "${lower.label}（先天${innateNumber(lower)}）"),
            TraceLine("动爻", yaoLabel(movingYao)),
        ),
    )
    return assemble(
        upper = upper,
        lower = lower,
        movingYao = movingYao,
        trace = trace,
        lunarMonthForWang = lunarMonthForWang,
        question = question,
    )
}

/** 随机两正整数，走数字起卦。 */
fun castRandom(
    random: Random = Random.Default,
    lunarMonthForWang: Int? = null,
    question: String = "",
): MeiHuaChart {
    val a = random.nextInt(1, 64)
    val b = random.nextInt(1, 64)
    val chart = castFromNumbers(a, b, lunarMonthForWang, question)
    return chart.copy(
        method = CastMethod.RANDOM,
        trace = chart.trace.copy(
            method = CastMethod.RANDOM,
            summary = "随机 $a、$b → " + chart.trace.summary.substringAfter(" → "),
            lines = listOf(TraceLine("随机数", "$a、$b")) + chart.trace.lines,
        ),
    )
}

private fun yaoLabel(n: Int): String = when (n) {
    1 -> "初爻"
    2 -> "二爻"
    3 -> "三爻"
    4 -> "四爻"
    5 -> "五爻"
    6 -> "上爻"
    else -> "${n}爻"
}

private fun formatSolar(year: Int, month: Int, day: Int, hour: Int, minute: Int): String =
    "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-" +
        "${day.toString().padStart(2, '0')} ${hour.toString().padStart(2, '0')}:" +
        minute.toString().padStart(2, '0')
