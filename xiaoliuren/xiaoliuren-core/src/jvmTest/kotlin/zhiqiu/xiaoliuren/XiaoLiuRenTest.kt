package zhiqiu.xiaoliuren

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class XiaoLiuRenTest {

    @Test
    fun `掌诀顺序固定不变`() {
        assertEquals(
            listOf("大安", "留连", "速喜", "赤口", "小吉", "空亡"),
            Palace.ORDER.map { it.label },
        )
    }

    /** 倪海厦《天纪》口授例：农历四月初五辰时，三步顺数终落空亡，此事不宜。 */
    @Test
    fun `天纪例 四月初五辰时落空亡`() {
        val chart = cast(lunarMonth = 4, lunarDay = 5, hourBranchIndex = 4) // 辰=4
        assertEquals(Palace.CHI_KOU, chart.monthPalace, "四月自大安顺数 4 位落赤口")
        assertEquals(Palace.LIU_LIAN, chart.dayPalace, "赤口起 1 顺数 5 位落留连")
        assertEquals(Palace.KONG_WANG, chart.hourPalace, "留连起 1 顺数 5 位（辰为第 5 时）落空亡")
        assertEquals(Luck.GREAT_MALEFIC, chart.luck)
        assertFalse(chart.luck.auspicious)
    }

    @Test
    fun `三步推演的起算点逐层衔接`() {
        val chart = cast(lunarMonth = 4, lunarDay = 5, hourBranchIndex = 4)
        assertEquals(Palace.DA_AN, chart.steps[0].start)
        assertEquals(chart.monthPalace, chart.steps[1].start)
        assertEquals(chart.dayPalace, chart.steps[2].start)
        assertEquals(chart.hourPalace, chart.steps[2].land)
    }

    @Test
    fun `正月初一子时归大安`() {
        val chart = cast(lunarMonth = 1, lunarDay = 1, hourBranchIndex = 0)
        assertEquals(Palace.DA_AN, chart.palace)
        assertEquals(Luck.GREAT_AUSPICIOUS, chart.luck)
    }

    @Test
    fun `月宫按六月一循环`() {
        val expected = listOf("大安", "留连", "速喜", "赤口", "小吉", "空亡", "大安", "留连", "速喜", "赤口", "小吉", "空亡")
        val actual = (1..12).map { cast(lunarMonth = it, lunarDay = 1, hourBranchIndex = 0).monthPalace.label }
        assertEquals(expected, actual)
    }

    /** 日落宫固定时，十二时辰恰覆盖六宫各两次。 */
    @Test
    fun `十二时辰遍历六宫各两次`() {
        val lands = (0..11).map { cast(lunarMonth = 6, lunarDay = 11, hourBranchIndex = it).hourPalace }
        assertEquals(2, lands.count { it == Palace.DA_AN })
        assertEquals(2, lands.count { it == Palace.LIU_LIAN })
        assertEquals(2, lands.count { it == Palace.SU_XI })
        assertEquals(2, lands.count { it == Palace.CHI_KOU })
        assertEquals(2, lands.count { it == Palace.XIAO_JI })
        assertEquals(2, lands.count { it == Palace.KONG_WANG })
    }

    @Test
    fun `顺数路径末位与落宫一致`() {
        val path = countPath(start = 3, count = 5)
        assertEquals(listOf(3, 4, 5, 0, 1), path)
        assertEquals(advanceFrom(start = 3, count = 5), path.last())
    }

    @Test
    fun `三吉三凶`() {
        assertEquals(3, Palace.entries.count { it.luck.auspicious })
        assertEquals(3, Palace.entries.count { !it.luck.auspicious })
        assertTrue(Palace.DA_AN.luck.auspicious)
        assertTrue(Palace.SU_XI.luck.auspicious)
        assertTrue(Palace.XIAO_JI.luck.auspicious)
        assertFalse(Palace.LIU_LIAN.luck.auspicious)
        assertFalse(Palace.CHI_KOU.luck.auspicious)
        assertFalse(Palace.KONG_WANG.luck.auspicious)
    }

    @Test
    fun `闰月标注不改变落宫`() {
        val normal = cast(lunarMonth = 5, lunarDay = 12, hourBranchIndex = 7)
        val leap = cast(lunarMonth = 5, lunarDay = 12, hourBranchIndex = 7, isLeapMonth = true)
        assertEquals(normal.palace, leap.palace)
        assertEquals("五月十二", normal.lunar!!.monthText + normal.lunar.dayText)
        assertEquals("闰五月十二", leap.lunar!!.monthText + leap.lunar.dayText)
    }

    @Test
    fun `农历日读写`() {
        assertEquals("初一", lunarDayLabel(1))
        assertEquals("初十", lunarDayLabel(10))
        assertEquals("十一", lunarDayLabel(11))
        assertEquals("二十", lunarDayLabel(20))
        assertEquals("廿一", lunarDayLabel(21))
        assertEquals("三十", lunarDayLabel(30))
        assertEquals("五月", lunarMonthLabel(5, false))
        assertEquals("闰五月", lunarMonthLabel(5, true))
        assertEquals("冬月", lunarMonthLabel(11, false))
        assertEquals("腊月", lunarMonthLabel(12, false))
    }

    @Test
    fun `时辰名称`() {
        assertEquals("子时", cast(1, 1, 0).lunar!!.hourText)
        assertEquals("辰时", cast(1, 1, 4).lunar!!.hourText)
        assertEquals("亥时", cast(1, 1, 11).lunar!!.hourText)
    }

    @Test
    fun `非法入参拒绝`() {
        assertFailsWith<IllegalArgumentException> { cast(0, 1, 0) }
        assertFailsWith<IllegalArgumentException> { cast(13, 1, 0) }
        assertFailsWith<IllegalArgumentException> { cast(1, 0, 0) }
        assertFailsWith<IllegalArgumentException> { cast(1, 31, 0) }
        assertFailsWith<IllegalArgumentException> { cast(1, 1, -1) }
        assertFailsWith<IllegalArgumentException> { cast(1, 1, 12) }
    }
}

/** 报数起课：三个数依次作月、日、时之用，与农历起课同式。 */
class CountCastTest {

    @Test
    fun `报数与农历起课等价`() {
        val lunar = cast(lunarMonth = 4, lunarDay = 5, hourBranchIndex = 4) // 辰为第 5 时
        val counted = castCounts(4, 5, 5)
        assertEquals(lunar.monthPalace, counted.monthPalace)
        assertEquals(lunar.dayPalace, counted.dayPalace)
        assertEquals(lunar.hourPalace, counted.hourPalace)
        assertEquals(lunar.steps.map { it.land }, counted.steps.map { it.land })
    }

    @Test
    fun `报数三步落宫`() {
        val chart = castCounts(6, 6, 6)
        assertEquals(Palace.KONG_WANG, chart.monthPalace, "大安起 1 顺数 6 位落空亡")
        assertEquals(Palace.XIAO_JI, chart.dayPalace, "空亡起 1 顺数 6 位落小吉")
        assertEquals(Palace.CHI_KOU, chart.hourPalace, "小吉起 1 顺数 6 位落赤口")
        assertEquals(Luck.MEDIUM_MALEFIC, chart.luck)
    }

    /** 六宫一循环，故三数各加 6 落宫不变。 */
    @Test
    fun `报数每满六宫回到同落宫`() {
        val base = castCounts(3, 7, 9)
        val shifted = castCounts(3 + 6, 7 + 12, 9 + 18)
        assertEquals(base.palace, shifted.palace)
        assertEquals(base.steps.map { it.land }, shifted.steps.map { it.land })
    }

    @Test
    fun `报数三步皆一归大安`() {
        val chart = castCounts(1, 1, 1)
        assertEquals(Palace.DA_AN, chart.palace)
        assertEquals(listOf(Palace.DA_AN, Palace.DA_AN, Palace.DA_AN), chart.steps.map { it.land })
    }

    @Test
    fun `报数无农历背景`() {
        val chart = castCounts(3, 7, 9)
        assertEquals(null, chart.lunar)
        assertEquals("报数 3、7、9", chart.castText)
        assertEquals(listOf("第一数", "第二数", "第三数"), chart.steps.map { it.countText })
        assertEquals("", chart.solarText)
    }

    @Test
    fun `农历课式带日期描述`() {
        val chart = cast(9, 4, 9)
        assertEquals("农历九月初四酉时", assertNotNull(chart.lunar).dateText)
        assertEquals("农历九月初四酉时", chart.castText)
        assertEquals(listOf(9, 4, 10), listOf(chart.monthCount, chart.dayCount, chart.hourCount))
    }

    @Test
    fun `报数越界拒绝`() {
        assertFailsWith<IllegalArgumentException> { castCounts(0, 1, 1) }
        assertFailsWith<IllegalArgumentException> { castCounts(-3, 1, 1) }
        assertFailsWith<IllegalArgumentException> { castCounts(1, 0, 1) }
        assertFailsWith<IllegalArgumentException> { castCounts(1, 1, 0) }
        assertFailsWith<IllegalArgumentException> { castCounts(MAX_COUNT + 1, 1, 1) }
    }

    @Test
    fun `报数上限可起课`() {
        val chart = castCounts(MAX_COUNT, MAX_COUNT, MAX_COUNT)
        assertTrue(chart.palace in Palace.entries)
    }
}

class SolarCastTest {

    /** 时辰边界：23 点与 0 点同为子时，21–22 点为亥时。 */
    @Test
    fun `阳历小时映射到时辰`() {
        assertEquals(0, castFromSolar(2026, 10, 13, 23).lunar!!.hourBranchIndex)
        assertEquals(0, castFromSolar(2026, 10, 13, 0).lunar!!.hourBranchIndex)
        assertEquals(1, castFromSolar(2026, 10, 13, 1).lunar!!.hourBranchIndex)
        assertEquals(1, castFromSolar(2026, 10, 13, 2).lunar!!.hourBranchIndex)
        assertEquals(3, castFromSolar(2026, 10, 13, 5).lunar!!.hourBranchIndex)
        assertEquals(11, castFromSolar(2026, 10, 13, 22).lunar!!.hourBranchIndex)
    }

    @Test
    fun `阳历起课与纯数字起课同式`() {
        val solar = castFromSolar(2026, 10, 13, 14, 5)
        val lunar = assertNotNull(solar.lunar)
        val pure = cast(lunar.month, lunar.day, lunar.hourBranchIndex, lunar.isLeapMonth)
        assertEquals(pure.palace, solar.palace)
        assertEquals(7, lunar.hourBranchIndex, "14:05 为未时")
        assertEquals("2026-10-13 14:05", solar.solarText)
    }

    @Test
    fun `阳历起课落在合法农历范围内`() {
        val lunar = assertNotNull(castFromSolar(2026, 10, 13, 14).lunar)
        assertTrue(lunar.month in 1..12, "month=${lunar.month}")
        assertTrue(lunar.day in 1..30, "day=${lunar.day}")
        assertTrue(lunar.dateText.contains(lunarDayLabel(lunar.day)))
        assertTrue(lunar.dateText.endsWith(lunar.hourText))
    }

    /** 夜子时（23 点后）仍算当日，换日以零点为界；时辰记作子时。 */
    @Test
    fun `夜子时归当日`() {
        val noon = requireNotNull(castFromSolar(2026, 10, 13, 12).lunar)
        val lateNight = requireNotNull(castFromSolar(2026, 10, 13, 23).lunar)
        val earlyMorning = requireNotNull(castFromSolar(2026, 10, 14, 0).lunar)
        assertEquals(noon.day, lateNight.day, "23 点仍属当日")
        assertEquals(0, lateNight.hourBranchIndex)
        assertTrue(earlyMorning.day != lateNight.day, "过零点即换日")
    }

    @Test
    fun `此刻起课可用`() {        val chart = castNow()
        assertTrue(assertNotNull(chart.lunar).month in 1..12)
        assertTrue(chart.steps.size == 3)
    }
}
