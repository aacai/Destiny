package zhiqiu.liuyao

import com.tyme.solar.SolarTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 占类大类：决定用神规则；每个大类下挂小类，仅用于记录与展示。
 * 用神：财运→妻财，事业/学业/官司→官鬼，感情→男妻财女官鬼，
 * 健康/怀孕→子孙，家宅/寻物/天气→父母，出行/行人/比赛→世爻。
 */
enum class ZhanShi(val label: String, val subs: List<String>) {
    NONE("通用", listOf("通用")),
    LOVE("感情婚姻", listOf("恋爱", "结婚", "复合", "外遇", "离婚")),
    WEALTH("财运生意", listOf("求财", "投资", "合作", "讨债", "买卖", "开店")),
    CAREER("事业工作", listOf("求职", "升迁", "跳槽", "开业", "换岗")),
    STUDY("学业考试", listOf("考试", "升学", "留学", "面试")),
    HEALTH("健康疾病", listOf("疾病", "医药", "手术", "体检")),
    PREGNANCY("怀孕生育", listOf("备孕", "怀孕", "分娩")),
    TRAVEL("出行远行", listOf("远行", "搬迁", "旅游")),
    LAWSUIT("官司诉讼", listOf("诉讼", "仲裁", "调解")),
    HOME("家宅田宅", listOf("阳宅", "装修", "阴宅", "租房")),
    LOST("寻物失物", listOf("失物", "宠物")),
    MISSING("行人走失", listOf("行人", "走失")),
    MATCH("比赛竞争", listOf("比赛", "竞标", "竞争")),
    WEATHER("天气年运", listOf("天气", "年运")),
}

/** 占类选择：大类＋小类。 */
data class ZhanSelection(val topic: ZhanShi = ZhanShi.NONE, val sub: String = "") {
    val display: String get() = when {
        topic == ZhanShi.NONE -> "通用"
        sub.isBlank() -> topic.label
        else -> "${topic.label}·$sub"
    }
}

/** 以世爻为用神的大类（看双方旺衰消长）。 */
fun shiAsYongShen(topic: ZhanShi): Boolean = when (topic) {
    ZhanShi.TRAVEL, ZhanShi.MISSING, ZhanShi.MATCH -> true
    else -> false
}

fun yongShenKins(topic: ZhanShi, gender: String): Set<SixKin> = when (topic) {
    ZhanShi.NONE -> emptySet()
    ZhanShi.WEALTH -> setOf(SixKin.WEALTH)
    ZhanShi.CAREER, ZhanShi.LAWSUIT -> setOf(SixKin.GHOST)
    ZhanShi.STUDY -> setOf(SixKin.GHOST, SixKin.PARENT)
    ZhanShi.LOVE -> if (gender == "女") setOf(SixKin.GHOST) else setOf(SixKin.WEALTH)
    ZhanShi.HEALTH, ZhanShi.PREGNANCY -> setOf(SixKin.CHILD)
    ZhanShi.HOME, ZhanShi.LOST, ZhanShi.WEATHER -> setOf(SixKin.PARENT)
    ZhanShi.TRAVEL, ZhanShi.MISSING, ZhanShi.MATCH -> emptySet()
}

/** 占时信息：月建（月支）与日辰（日干支）是六神神煞的起例基准。 */
data class DayInfo(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val yearStem: Stem,
    val yearBranch: Branch,
    val monthStem: Stem,
    val monthBranch: Branch,
    val dayStem: Stem,
    val dayBranch: Branch,
    val hourStem: Stem,
    val hourBranch: Branch,
    val yearLabel: String,
    val monthLabel: String,
    val dayLabel: String,
    val hourLabel: String,
    /** 旬空两支。 */
    val xunKong: Set<Branch>,
    /** 如七月初五。 */
    val lunarLabel: String,
    /** 如一（即周一）。 */
    val weekLabel: String,
) {
    val dateTimeLabel: String
        get() = buildString {
            append(year.toString().padStart(4, '0'))
            append('-')
            append(month.toString().padStart(2, '0'))
            append('-')
            append(day.toString().padStart(2, '0'))
            append(' ')
            append(hour.toString().padStart(2, '0'))
            append(':')
            append(minute.toString().padStart(2, '0'))
        }
}

/**
 * 旬空：日柱所在旬，旬首前两位。
 * 如癸亥日属甲寅旬，空子丑；甲子日本旬，空戌亥。
 */
fun xunKongOf(dayStem: Stem, dayBranch: Branch): Set<Branch> {
    val all = Branch.entries
    val head = (dayBranch.ordinal - dayStem.ordinal + 12) % 12
    return setOf(all[(head + 11) % 12], all[(head + 10) % 12])
}

fun dayInfoFromSolar(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
): DayInfo {
    val solar = SolarTime(year, month, day, hour, minute, 0)
    val eight = solar.getLunarHour().getEightChar()
    val yearPillar = eight.getYear()
    val monthPillar = eight.getMonth()
    val dayPillar = eight.getDay()
    val hourPillar = eight.getHour()
    val dStem = stemOf(dayPillar.getHeavenStem().getName())
    val dBranch = branchOf(dayPillar.getEarthBranch().getName())
    val lunarDay = solar.getSolarDay().getLunarDay()
    val lunarMonthName = lunarDay.getLunarMonth().getName()
    return DayInfo(
        year = year,
        month = month,
        day = day,
        hour = hour,
        minute = minute,
        yearStem = stemOf(yearPillar.getHeavenStem().getName()),
        yearBranch = branchOf(yearPillar.getEarthBranch().getName()),
        monthStem = stemOf(monthPillar.getHeavenStem().getName()),
        monthBranch = branchOf(monthPillar.getEarthBranch().getName()),
        dayStem = dStem,
        dayBranch = dBranch,
        hourStem = stemOf(hourPillar.getHeavenStem().getName()),
        hourBranch = branchOf(hourPillar.getEarthBranch().getName()),
        yearLabel = yearPillar.getName(),
        monthLabel = monthPillar.getName(),
        dayLabel = dayPillar.getName(),
        hourLabel = hourPillar.getName(),
        xunKong = xunKongOf(dStem, dBranch),
        lunarLabel = (if (lunarMonthName.endsWith("月")) lunarMonthName else lunarMonthName + "月") +
            lunarDay.getName(),
        weekLabel = solar.getSolarDay().getWeek().getName(),
    )
}

/** 当前跨平台时间（kotlin.time.Clock + kotlinx-datetime 时区），默认占时。 */
fun currentDayInfo(): DayInfo {
    val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return dayInfoFromSolar(now.year, now.monthNumber, now.dayOfMonth, now.hour, now.minute)
}
