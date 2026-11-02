package zhiqiu.qimen

import com.tyme.solar.SolarDay
import com.tyme.solar.SolarTerm
import com.tyme.solar.SolarTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QimenTest {

    @Test
    fun 局数歌() {
        assertEquals(1, juOf("冬至", Yuan.Shang))
        assertEquals(7, juOf("惊蛰", Yuan.Zhong))
        assertEquals(4, juOf("冬至", Yuan.Xia))
        assertEquals(2, juOf("小寒", Yuan.Shang))
        assertEquals(8, juOf("小寒", Yuan.Zhong))
        assertEquals(5, juOf("小寒", Yuan.Xia))
        assertEquals(3, juOf("大寒", Yuan.Shang))
        assertEquals(9, juOf("春分", Yuan.Zhong))
        assertEquals(6, juOf("春分", Yuan.Xia))
        assertEquals(9, juOf("雨水", Yuan.Shang))
        assertEquals(6, juOf("雨水", Yuan.Zhong))
        assertEquals(3, juOf("雨水", Yuan.Xia))
        assertEquals(4, juOf("清明", Yuan.Shang))
        assertEquals(1, juOf("立夏", Yuan.Zhong))
        assertEquals(7, juOf("立夏", Yuan.Xia))
        assertEquals(8, juOf("立春", Yuan.Shang))
        assertEquals(5, juOf("立春", Yuan.Zhong))
        assertEquals(2, juOf("立春", Yuan.Xia))
        assertEquals(5, juOf("谷雨", Yuan.Shang))
        assertEquals(2, juOf("小满", Yuan.Zhong))
        assertEquals(8, juOf("小满", Yuan.Xia))
        assertEquals(6, juOf("芒种", Yuan.Shang))
        assertEquals(3, juOf("芒种", Yuan.Zhong))
        assertEquals(9, juOf("芒种", Yuan.Xia))
        assertEquals(9, juOf("夏至", Yuan.Shang))
        assertEquals(3, juOf("白露", Yuan.Zhong))
        assertEquals(6, juOf("夏至", Yuan.Xia))
        assertEquals(8, juOf("小暑", Yuan.Shang))
        assertEquals(2, juOf("小暑", Yuan.Zhong))
        assertEquals(5, juOf("小暑", Yuan.Xia))
        assertEquals(7, juOf("大暑", Yuan.Shang))
        assertEquals(1, juOf("秋分", Yuan.Zhong))
        assertEquals(4, juOf("秋分", Yuan.Xia))
        assertEquals(2, juOf("立秋", Yuan.Shang))
        assertEquals(5, juOf("立秋", Yuan.Zhong))
        assertEquals(8, juOf("立秋", Yuan.Xia))
        assertEquals(6, juOf("寒露", Yuan.Shang))
        assertEquals(9, juOf("立冬", Yuan.Zhong))
        assertEquals(3, juOf("立冬", Yuan.Xia))
        assertEquals(1, juOf("处暑", Yuan.Shang))
        assertEquals(4, juOf("处暑", Yuan.Zhong))
        assertEquals(7, juOf("处暑", Yuan.Xia))
        assertEquals(5, juOf("霜降", Yuan.Shang))
        assertEquals(8, juOf("小雪", Yuan.Zhong))
        assertEquals(2, juOf("小雪", Yuan.Xia))
        assertEquals(4, juOf("大雪", Yuan.Shang))
        assertEquals(1, juOf("大雪", Yuan.Zhong))
        assertEquals(7, juOf("大雪", Yuan.Xia))
    }

    @Test
    fun 符头地支定元() {
        assertEquals(Yuan.Shang, yuanOf(Branch.Zi))
        assertEquals(Yuan.Shang, yuanOf(Branch.Wu))
        assertEquals(Yuan.Shang, yuanOf(Branch.Mao))
        assertEquals(Yuan.Shang, yuanOf(Branch.You))
        assertEquals(Yuan.Zhong, yuanOf(Branch.Yin))
        assertEquals(Yuan.Zhong, yuanOf(Branch.Shen))
        assertEquals(Yuan.Zhong, yuanOf(Branch.Si))
        assertEquals(Yuan.Zhong, yuanOf(Branch.Hai))
        assertEquals(Yuan.Xia, yuanOf(Branch.Chen))
        assertEquals(Yuan.Xia, yuanOf(Branch.Xu))
        assertEquals(Yuan.Xia, yuanOf(Branch.Chou))
        assertEquals(Yuan.Xia, yuanOf(Branch.Wei))
    }

    @Test
    fun 符头跨交节仍用上一节气() {
        val sample = findCrossTerm()
        val chart = cast(
            sample.day.year,
            sample.day.month,
            sample.day.day,
            sample.hour,
            0,
            PlateKind.Zhuan,
            DayBoundary.Midnight,
        )
        assertEquals(sample.previous, chart.termName, "符头${sample.futou}跨过${sample.current}，局仍属上一节气")
        assertNotEquals(sample.current, chart.termName)
    }

    @Test
    fun 夏至后阴遁冬至后阳遁() {
        val summer = cast(2026, 7, 15, 12, 0, PlateKind.Zhuan, DayBoundary.Midnight)
        val winter = cast(2026, 1, 15, 12, 0, PlateKind.Zhuan, DayBoundary.Midnight)
        assertEquals(Dun.Yin, summer.dun)
        assertEquals(Dun.Yang, winter.dun)
    }

    @Test
    fun 正午四柱与节气历一致() {
        val time = SolarTime(2026, 6, 15, 10, 0, 0)
        val chart = cast(2026, 6, 15, 10, 0, PlateKind.Zhuan, DayBoundary.Hour23)
        val hour = time.getSixtyCycleHour()
        assertEquals(hour.getYear().getName(), chart.pillars[0].label)
        assertEquals(hour.getMonth().getName(), chart.pillars[1].label)
        assertEquals(hour.getDay().getName(), chart.pillars[2].label)
        assertEquals(hour.getSixtyCycle().getName(), chart.pillars[3].label)
        val midnight = cast(2026, 6, 15, 10, 0, PlateKind.Zhuan, DayBoundary.Midnight)
        assertEquals(chart.pillarText, midnight.pillarText)
    }

    @Test
    fun 二十三点换日与零点换日的日柱不同() {
        val time = SolarTime(2026, 6, 15, 23, 30, 0)
        val early = cast(2026, 6, 15, 23, 30, PlateKind.Zhuan, DayBoundary.Hour23)
        val late = cast(2026, 6, 15, 23, 30, PlateKind.Zhuan, DayBoundary.Midnight)
        val shifted = time.getSixtyCycleHour()
        assertEquals(shifted.getDay().getName(), early.pillars[2].label)
        assertEquals(shifted.getSixtyCycle().getName(), early.pillars[3].label)
        assertEquals(time.getSolarDay().getSixtyCycleDay().getSixtyCycle().getName(), late.pillars[2].label)
        assertNotEquals(early.pillars[2].label, late.pillars[2].label)
    }

    @Test
    fun 阳遁一局地盘从坎起戊() {
        val plate = arrange(1, true, Stem.Bing, Branch.Yin, PlateKind.Zhuan)
        assertEquals(
            listOf(Stem.Wu, Stem.Ji, Stem.Geng, Stem.Xin, Stem.Ren, Stem.Gui, Stem.Ding, Stem.Bing, Stem.Yi),
            plate.earth,
        )
        assertEquals(listOf(Stem.Ji, Stem.Ren), plate.cell(Palace.Kun).earthStems)
        assertEquals(Stem.Ren, plate.centerEarth)
        assertTrue(plate.cell(Palace.Zhong).earthStems.isEmpty())
    }

    @Test
    fun 阳遁一局丙寅转盘禽芮同宫() {
        val plate = arrange(1, true, Stem.Bing, Branch.Yin, PlateKind.Zhuan)
        assertEquals(Star.Peng, plate.zhiFu)
        assertEquals(Door.Xiu, plate.zhiShi)
        assertEquals(listOf(Star.Peng), plate.cell(Palace.Gen).stars)
        assertEquals(listOf(Stem.Wu), plate.cell(Palace.Gen).heavenStems)
        assertEquals(listOf(Star.Rui, Star.Qin), plate.cell(Palace.Dui).stars)
        assertEquals(listOf(Stem.Ji, Stem.Ren), plate.cell(Palace.Dui).heavenStems)
        assertEquals(Door.Xiu, plate.cell(Palace.Zhen).door)
        assertEquals(Deity.ZhiFu, plate.cell(Palace.Gen).deity)
        assertNull(plate.cell(Palace.Zhong).door)
        assertNull(plate.cell(Palace.Zhong).deity)
        assertEquals(listOf(Mark.Kong), plate.cell(Palace.Qian).marks)
        assertEquals(listOf(Mark.Ma, Mark.Mu, Mark.Po), plate.cell(Palace.Kun).marks)
        assertEquals(listOf(Mark.Mu), plate.cell(Palace.Xun).marks)
        assertEquals(listOf(Mark.Xing), plate.cell(Palace.Li).marks)
        assertEquals(listOf(Mark.Po), plate.cell(Palace.Dui).marks)
        assertTrue(plate.cell(Palace.Kan).marks.isEmpty())
    }

    @Test
    fun 甲子时以戊寻宫天蓬留在坎() {
        val plate = arrange(1, true, Stem.Jia, Branch.Zi, PlateKind.Zhuan)
        assertEquals(Star.Peng, plate.zhiFu)
        assertEquals(listOf(Star.Peng), plate.cell(Palace.Kan).stars)
        assertEquals(listOf(Star.Rui, Star.Qin), plate.cell(Palace.Kun).stars)
        assertEquals(listOf(Stem.Ji, Stem.Ren), plate.cell(Palace.Kun).heavenStems)
    }

    @Test
    fun 阴遁九局丙寅飞盘中宫有星且空一宫门() {
        val plate = arrange(9, false, Stem.Bing, Branch.Yin, PlateKind.Fei)
        assertEquals(
            listOf(Stem.Yi, Stem.Bing, Stem.Ding, Stem.Gui, Stem.Ren, Stem.Xin, Stem.Geng, Stem.Ji, Stem.Wu),
            plate.earth,
        )
        assertEquals(listOf(Star.Xin), plate.cell(Palace.Zhong).stars)
        assertEquals(listOf(Stem.Ren), plate.cell(Palace.Zhong).earthStems)
        assertEquals(listOf(Star.Ying), plate.cell(Palace.Kun).stars)
        assertEquals(Door.Scene, plate.cell(Palace.Dui).door)
        assertNull(plate.cell(Palace.Gen).door)
        assertEquals(1, plate.cells.count { it.door == null })
        assertEquals(1, plate.cells.count { it.deity == null })
        assertEquals(listOf(Mark.Kong), plate.cell(Palace.Qian).marks.filter { it == Mark.Kong })
        assertTrue(plate.cell(Palace.Kun).marks.contains(Mark.Ma))
    }

    @Test
    fun 阴遁转盘星沿八宫环逆行() {
        val plate = arrange(9, false, Stem.Bing, Branch.Yin, PlateKind.Zhuan)
        assertEquals(listOf(Star.Ying), plate.cell(Palace.Kun).stars)
        assertEquals(listOf(Star.Rui, Star.Qin), plate.cell(Palace.Dui).stars)
        assertEquals(listOf(Stem.Bing, plate.centerEarth), plate.cell(Palace.Kun).earthStems)
    }

    @Test
    fun 同一时辰换盘式局与地盘不变星门变() {
        val zhuan = cast(2026, 1, 15, 10, 30, PlateKind.Zhuan, DayBoundary.Hour23)
        val fei = cast(2026, 1, 15, 10, 30, PlateKind.Fei, DayBoundary.Hour23)
        assertEquals(zhuan.ju, fei.ju)
        assertEquals(zhuan.dun, fei.dun)
        assertEquals(zhuan.termName, fei.termName)
        assertEquals(zhuan.pillarText, fei.pillarText)
        assertEquals(
            listOf(fei.cell(Palace.Kun).earthStems.single(), fei.cell(Palace.Zhong).earthStems.single()),
            zhuan.cell(Palace.Kun).earthStems,
        )
        Palace.entries.filter { it != Palace.Kun && it != Palace.Zhong }.forEach { palace ->
            assertEquals(fei.cell(palace).earthStems, zhuan.cell(palace).earthStems, palace.shortName)
        }
        assertNotEquals(zhuan.cells.map { it.stars }, fei.cells.map { it.stars })
        assertTrue(zhuan.cell(Palace.Zhong).stars.isEmpty())
        assertTrue(fei.cell(Palace.Zhong).stars.isNotEmpty())
        assertEquals(1, fei.cells.count { it.door == null })
    }
}

private data class CrossTerm(
    val day: SolarDay,
    val hour: Int,
    val previous: String,
    val current: String,
    val futou: String,
)

/** 用节气时刻和甲己日自行找出「符头在交节前、排盘在交节后」的一天，不经过排盘函数。 */
private fun findCrossTerm(): CrossTerm {
    for (year in 2024..2027) {
        for (index in 0..23) {
            val term = SolarTerm(year, index)
            val change = term.getJulianDay().getSolarTime()
            val start = SolarDay(change.year, change.month, change.day)
            for (delta in 0..12) {
                val day = start.next(delta)
                val noon = SolarTime(day.year, day.month, day.day, 12, 0, 0)
                if (!noon.isAfter(change)) continue
                val futou = walkToFuTou(day)
                val futouStart = SolarTime(futou.year, futou.month, futou.day, 0, 0, 0)
                if (!futouStart.isBefore(change)) continue
                return CrossTerm(
                    day = day,
                    hour = 12,
                    previous = term.next(-1).getName(),
                    current = term.getName(),
                    futou = futou.getSixtyCycleDay().getSixtyCycle().getName(),
                )
            }
        }
    }
    error("2024–2027 里没有找到符头跨节的日子")
}

private fun walkToFuTou(day: SolarDay): SolarDay {
    var cursor = day
    while (cursor.getSixtyCycleDay().getSixtyCycle().getHeavenStem().getName() != "甲" &&
        cursor.getSixtyCycleDay().getSixtyCycle().getHeavenStem().getName() != "己"
    ) {
        cursor = cursor.next(-1)
    }
    return cursor
}
