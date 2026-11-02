package zhiqiu.qimen

import com.tyme.sixtycycle.SixtyCycle
import com.tyme.solar.SolarDay
import com.tyme.solar.SolarTime

/** 上中下元对应局数表的下标。 */
fun yuanOf(branch: Branch): Yuan = when (branch) {
    Branch.Zi, Branch.Wu, Branch.Mao, Branch.You -> Yuan.Shang
    Branch.Yin, Branch.Shen, Branch.Si, Branch.Hai -> Yuan.Zhong
    Branch.Chen, Branch.Xu, Branch.Chou, Branch.Wei -> Yuan.Xia
}

/**
 * 拆补局数。数组顺序是上元、中元、下元。
 * 阳遁冬至到芒种，阴遁夏至到大雪。
 */
fun juOf(termName: String, yuan: Yuan): Int {
    val row = JU_TABLE[termName] ?: error("没有这个节气的局：$termName")
    return row[yuan.ordinal]
}

data class JuInfo(
    val termName: String,
    val dun: Dun,
    val yuan: Yuan,
    val ju: Int,
    val futouLabel: String,
)

/**
 * 拆补：符头是日柱当日或向前最近的甲、己日。
 * 元看符头地支，局看符头日界起点所在的节气，不看排盘时刻已经交节没有。
 */
fun resolveJu(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
    boundary: DayBoundary,
    method: JuMethod = JuMethod.ChaiBu,
): JuInfo {
    require(method == JuMethod.ChaiBu) { "第一期只排拆补" }
    val time = SolarTime(year, month, day, hour, minute, 0)
    val chartDay = chartSolarDay(time, boundary)
    val futou = futouDay(chartDay)
    val term = futouStart(futou, boundary).getTerm()
    val futouCycle = futou.getSixtyCycleDay().getSixtyCycle()
    val yuan = yuanOf(branchOf(futouCycle))
    val name = term.getName()
    return JuInfo(
        termName = name,
        dun = if (term.getIndex() < 12) Dun.Yang else Dun.Yin,
        yuan = yuan,
        ju = juOf(name, yuan),
        futouLabel = futouCycle.getName(),
    )
}

/** 年柱立春换、月柱交节换，取自 tyme 的节气四柱；日柱、时柱按日界另算。 */
fun pillarsOf(time: SolarTime, boundary: DayBoundary): List<Pillar> {
    val cycleHour = time.getSixtyCycleHour()
    val civil = time.getSolarDay().getSixtyCycleDay().getSixtyCycle()
    val dayCycle = if (boundary == DayBoundary.Hour23 && time.hour >= 23) civil.next(1) else civil
    val day = pillarOf(dayCycle)
    val branchIndex = if (time.hour == 23) 0 else (time.hour + 1) / 2
    val branch = Branch.entries[branchIndex]
    // 五鼠遁：时干由日干推出，日界改了日干，时干跟着改。
    val stem = Stem.entries[(day.stem.ordinal % 5) * 2 + branchIndex]
    return listOf(
        pillarOf(cycleHour.getYear()),
        pillarOf(cycleHour.getMonth()),
        day,
        Pillar(stem, branch),
    )
}

/** 阳顺阴逆，局数宫起戊，布戊己庚辛壬癸丁丙乙。下标是宫 ordinal。 */
fun earthStems(ju: Int, yang: Boolean): List<Stem> {
    require(ju in 1..9) { "局数须在 1..9，实际为 $ju" }
    val placed = arrayOfNulls<Stem>(9)
    var number = ju
    for (stem in EARTH_ORDER) {
        placed[number - 1] = stem
        number = stepLuo(number, yang)
    }
    return placed.map { requireNotNull(it) }
}

private fun chartSolarDay(time: SolarTime, boundary: DayBoundary): SolarDay {
    val day = time.getSolarDay()
    return if (boundary == DayBoundary.Hour23 && time.hour >= 23) day.next(1) else day
}

private fun futouDay(day: SolarDay): SolarDay {
    var cursor = day
    while (cursor.getSixtyCycleDay().getSixtyCycle().getHeavenStem().getIndex() !in FU_TOU_STEMS) {
        cursor = cursor.next(-1)
    }
    return cursor
}

/** 符头日的日界起点。交节若落在这一天的后半段，符头仍属上一节气。 */
private fun futouStart(futou: SolarDay, boundary: DayBoundary): SolarTime {
    return if (boundary == DayBoundary.Hour23) {
        val prev = futou.next(-1)
        SolarTime(prev.year, prev.month, prev.day, 23, 0, 0)
    } else {
        SolarTime(futou.year, futou.month, futou.day, 0, 0, 0)
    }
}

private fun pillarOf(cycle: SixtyCycle): Pillar = Pillar(stemOf(cycle), branchOf(cycle))

private fun stemOf(cycle: SixtyCycle): Stem = Stem.entries[cycle.getHeavenStem().getIndex()]

private fun branchOf(cycle: SixtyCycle): Branch = Branch.entries[cycle.getEarthBranch().getIndex()]

private fun stepLuo(number: Int, yang: Boolean): Int = when {
    yang -> if (number == 9) 1 else number + 1
    else -> if (number == 1) 9 else number - 1
}

private val FU_TOU_STEMS = intArrayOf(0, 5)

private val EARTH_ORDER = listOf(
    Stem.Wu, Stem.Ji, Stem.Geng, Stem.Xin, Stem.Ren, Stem.Gui, Stem.Ding, Stem.Bing, Stem.Yi,
)

private val JU_TABLE: Map<String, IntArray> = mapOf(
    "冬至" to intArrayOf(1, 7, 4),
    "惊蛰" to intArrayOf(1, 7, 4),
    "小寒" to intArrayOf(2, 8, 5),
    "大寒" to intArrayOf(3, 9, 6),
    "春分" to intArrayOf(3, 9, 6),
    "雨水" to intArrayOf(9, 6, 3),
    "清明" to intArrayOf(4, 1, 7),
    "立夏" to intArrayOf(4, 1, 7),
    "立春" to intArrayOf(8, 5, 2),
    "谷雨" to intArrayOf(5, 2, 8),
    "小满" to intArrayOf(5, 2, 8),
    "芒种" to intArrayOf(6, 3, 9),
    "夏至" to intArrayOf(9, 3, 6),
    "白露" to intArrayOf(9, 3, 6),
    "小暑" to intArrayOf(8, 2, 5),
    "大暑" to intArrayOf(7, 1, 4),
    "秋分" to intArrayOf(7, 1, 4),
    "立秋" to intArrayOf(2, 5, 8),
    "寒露" to intArrayOf(6, 9, 3),
    "立冬" to intArrayOf(6, 9, 3),
    "处暑" to intArrayOf(1, 4, 7),
    "霜降" to intArrayOf(5, 8, 2),
    "小雪" to intArrayOf(5, 8, 2),
    "大雪" to intArrayOf(4, 1, 7),
)
