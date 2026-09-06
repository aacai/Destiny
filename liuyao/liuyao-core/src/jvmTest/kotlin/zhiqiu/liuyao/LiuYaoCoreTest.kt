package zhiqiu.liuyao

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LiuYaoCoreTest {

    /** 甲子日（旬空戌亥）的测试占时。 */
    private fun jiaziDay() = DayInfo(
        year = 2026, month = 9, day = 6, hour = 12, minute = 0,
        yearStem = Stem.BING, yearBranch = Branch.WU,
        monthStem = Stem.YI, monthBranch = Branch.YOU,
        dayStem = Stem.JIA, dayBranch = Branch.ZI,
        hourStem = Stem.GENG, hourBranch = Branch.WU,
        yearLabel = "丙午", monthLabel = "乙酉", dayLabel = "甲子", hourLabel = "庚午",
        xunKong = setOf(Branch.XU, Branch.HAI),
        lunarLabel = "八月初五", weekLabel = "日",
    )

    @Test
    fun catalog_has64UniqueHexagrams() {
        assertEquals(64, HEXAGRAM_CATALOG.size)
        assertEquals(64, HEXAGRAM_CATALOG.map { it.bitsKey }.toSet().size)
        Palace.entries.forEach { palace ->
            assertEquals(8, HEXAGRAM_CATALOG.count { it.palace == palace }, "宫 ${palace.label}")
        }
    }

    @Test
    fun catalog_upperLowerSpotCheck() {
        // 地天泰=上坤下乾，天地否=上乾下坤
        val tai = HEXAGRAM_CATALOG.first { it.fullName == "地天泰" }
        assertEquals(Trigram.KUN, tai.upper)
        assertEquals(Trigram.QIAN, tai.lower)
        assertEquals(Palace.KUN, tai.palace)
        val pi = HEXAGRAM_CATALOG.first { it.fullName == "天地否" }
        assertEquals(Trigram.QIAN, pi.upper)
        assertEquals(Trigram.KUN, pi.lower)
        assertEquals(Palace.QIAN, pi.palace)
        // 水火既济=上坎下离，火水未济=上离下坎
        val jiji = HEXAGRAM_CATALOG.first { it.fullName == "水火既济" }
        assertEquals(Trigram.KAN, jiji.upper)
        assertEquals(Trigram.LI, jiji.lower)
        // 泽雷随=上兑下震，山雷颐=上艮下震，泽风大过=上兑下巽
        val sui = HEXAGRAM_CATALOG.first { it.fullName == "泽雷随" }
        assertEquals(Trigram.DUI, sui.upper)
        assertEquals(Trigram.ZHEN, sui.lower)
        val yi = HEXAGRAM_CATALOG.first { it.fullName == "山雷颐" }
        assertEquals(Trigram.GEN, yi.upper)
        assertEquals(Trigram.ZHEN, yi.lower)
        // 雷地豫=上震下坤
        val yu = HEXAGRAM_CATALOG.first { it.fullName == "雷地豫" }
        assertEquals(Trigram.ZHEN, yu.upper)
        assertEquals(Trigram.KUN, yu.lower)
    }

    @Test
    fun shiYingAlwaysTwoApart() {
        HEXAGRAM_CATALOG.forEach {
            val diff = (it.yingPosition - it.shiPosition + 6) % 6
            assertEquals(3, diff, it.fullName)
        }
        // 本宫六世三应，游魂四世初应，归魂三世上应
        val qian = HEXAGRAM_CATALOG.first { it.fullName == "乾为天" }
        assertEquals(6, qian.shiPosition)
        assertEquals(3, qian.yingPosition)
        val jin = HEXAGRAM_CATALOG.first { it.fullName == "火地晋" }
        assertEquals(4, jin.shiPosition)
        assertEquals(1, jin.yingPosition)
        val dayou = HEXAGRAM_CATALOG.first { it.fullName == "火天大有" }
        assertEquals(3, dayou.shiPosition)
        assertEquals(6, dayou.yingPosition)
    }

    @Test
    fun najia_qianKun() {
        val qian = hexagramOf(listOf(1, 1, 1, 1, 1, 1))
        assertEquals("乾为天", qian.fullName)
        assertEquals(Ganzhi(Stem.JIA, Branch.ZI), najiaOf(qian, 1))
        assertEquals(Ganzhi(Stem.JIA, Branch.CHEN), najiaOf(qian, 3))
        assertEquals(Ganzhi(Stem.REN, Branch.WU), najiaOf(qian, 4))
        assertEquals(Ganzhi(Stem.REN, Branch.XU), najiaOf(qian, 6))
        val kun = hexagramOf(listOf(0, 0, 0, 0, 0, 0))
        assertEquals(Ganzhi(Stem.YI, Branch.WEI), najiaOf(kun, 1))
        assertEquals(Ganzhi(Stem.GUI, Branch.YOU), najiaOf(kun, 6))
    }

    @Test
    fun najia_otherTrigrams() {
        val li = hexagramOf(listOf(1, 0, 1, 1, 0, 1))
        assertEquals("离为火", li.fullName)
        assertEquals(Ganzhi(Stem.JI, Branch.MAO), najiaOf(li, 1))
        assertEquals(Ganzhi(Stem.JI, Branch.SI), najiaOf(li, 6))
        // 水天需=上坎下乾，初爻甲子，上爻戊子
        val xu = HEXAGRAM_CATALOG.first { it.fullName == "水天需" }
        assertEquals(Ganzhi(Stem.JIA, Branch.ZI), najiaOf(xu, 1))
        assertEquals(Ganzhi(Stem.WU, Branch.ZI), najiaOf(xu, 6))
        // 天泽履=上乾下兑，初爻丁巳
        val lv = HEXAGRAM_CATALOG.first { it.fullName == "天泽履" }
        assertEquals(Ganzhi(Stem.DING, Branch.SI), najiaOf(lv, 1))
        // 山天大畜=上艮下乾，三爻甲辰
        val dachu = HEXAGRAM_CATALOG.first { it.fullName == "山天大畜" }
        assertEquals(Ganzhi(Stem.JIA, Branch.CHEN), najiaOf(dachu, 3))
    }

    @Test
    fun chart_qianAllStatic() {
        val day = jiaziDay()
        val chart = buildChart(List(6) { YaoKind.SHAO_YANG }, day)!!
        assertEquals("乾为天", chart.original.entry.fullName)
        assertEquals(Palace.QIAN, chart.original.entry.palace)
        assertNull(chart.changed)
        assertTrue(chart.movingPositions.isEmpty())
        val y1 = chart.original.yaos[0]
        assertEquals("甲子", y1.ganzhi.label)
        // 乾宫金，子水为我生者→子孙
        assertEquals(SixKin.CHILD, y1.sixKin)
        // 甲日青龙起初爻
        assertEquals(SixGod.QING_LONG, y1.sixGod)
        assertEquals(SixGod.ZHU_QUE, chart.original.yaos[1].sixGod)
        // 世在上爻，应在三爻
        assertTrue(chart.original.yaos[5].isShi)
        assertTrue(chart.original.yaos[2].isYing)
        // 甲子日：桃花在酉。乾卦无酉支，无桃花临爻
        assertTrue(chart.original.yaos.none { ShenSha.TAO_HUA in it.shenSha })
    }

    @Test
    fun chart_gouChangesToQian() {
        // 天风姤=乾上巽下 bits: 下巽011 + 上乾111
        val day = jiaziDay().copy(dayStem = Stem.XIN, dayBranch = Branch.MAO, dayLabel = "辛卯")
        val kinds = listOf(
            YaoKind.LAO_YIN,
            YaoKind.SHAO_YANG,
            YaoKind.SHAO_YANG,
            YaoKind.SHAO_YANG,
            YaoKind.SHAO_YANG,
            YaoKind.SHAO_YANG,
        )
        val chart = buildChart(kinds, day)!!
        assertEquals("天风姤", chart.original.entry.fullName)
        assertEquals(listOf(1), chart.movingPositions)
        // 初爻老阴变阳→下卦变乾→乾为天
        assertNotNull(chart.changed)
        assertEquals("乾为天", chart.changed!!.entry.fullName)
        // 本卦初爻：下巽辛丑土；乾宫金，土生金→父母
        val y1 = chart.original.yaos[0]
        assertEquals("辛丑", y1.ganzhi.label)
        assertEquals(SixKin.PARENT, y1.sixKin)
        assertTrue(y1.isMoving)
        // 变卦初爻：乾下甲子水；六亲仍以本宫（乾金）→子孙
        val c1 = chart.changed!!.yaos[0]
        assertEquals("甲子", c1.ganzhi.label)
        assertEquals(SixKin.CHILD, c1.sixKin)
        // 变卦爻不装六神神煞，但按变卦自己的宫位标世应（乾本宫六世三应）
        assertNull(c1.sixGod)
        assertTrue(c1.shenSha.isEmpty())
        assertTrue(chart.changed!!.yaos[5].isShi)
        assertTrue(chart.changed!!.yaos[2].isYing)
    }

    @Test
    fun shensha_jiaziDay() {
        val m = shenShaBranches(Stem.JIA, Branch.ZI)
        assertEquals(setOf(Branch.YIN), m[ShenSha.YI_MA])
        assertEquals(setOf(Branch.YOU), m[ShenSha.TAO_HUA])
        assertEquals(setOf(Branch.MAO), m[ShenSha.YANG_REN])
        assertEquals(setOf(Branch.CHOU, Branch.WEI), m[ShenSha.TIAN_YI])
        assertEquals(setOf(Branch.CHEN), m[ShenSha.HUA_GAI])
        assertEquals(setOf(Branch.SI), m[ShenSha.JIE_SHA])
    }

    @Test
    fun yongshen_wealthForMan() {
        // 乾为天：乾宫金，金克木为妻财→二爻甲寅
        val day = jiaziDay()
        val chart = buildChart(
            List(6) { YaoKind.SHAO_YANG }, day, ZhanSelection(ZhanShi.WEALTH, "求财"), "男",
        )!!
        val yong = chart.original.yaos.filter { it.isYongShen }.map { it.position }
        assertEquals(listOf(2), yong)
        assertEquals("妻财·九二甲寅", chart.yongShenLine)
        // 感情/女 → 官鬼（午火：四爻壬午）
        val love = buildChart(
            List(6) { YaoKind.SHAO_YANG }, day, ZhanSelection(ZhanShi.LOVE, "结婚"), "女",
        )!!
        assertEquals(listOf(4), love.original.yaos.filter { it.isYongShen }.map { it.position })
        // 通用 → 无用神
        val none = buildChart(List(6) { YaoKind.SHAO_YANG }, day)!!
        assertTrue(none.original.yaos.none { it.isYongShen })
        assertEquals("", none.yongShenLine)
        // 出行/行人/比赛看世爻（乾为天世在上爻）
        val travel = buildChart(
            List(6) { YaoKind.SHAO_YANG }, day, ZhanSelection(ZhanShi.MISSING, "行人"), "男",
        )!!
        assertEquals(listOf(6), travel.original.yaos.filter { it.isYongShen }.map { it.position })
        // 学业取官鬼兼父母；寻物取父母
        val study = buildChart(
            List(6) { YaoKind.SHAO_YANG }, day, ZhanSelection(ZhanShi.STUDY, "考试"), "男",
        )!!
        assertEquals(
            listOf(3, 4, 6),
            study.original.yaos.filter { it.isYongShen }.map { it.position }.sorted(),
        )
    }

    @Test
    fun xunkong_rules() {
        // 甲子日本旬空戌亥；癸亥日属甲寅旬空子丑
        assertEquals(setOf(Branch.XU, Branch.HAI), xunKongOf(Stem.JIA, Branch.ZI))
        assertEquals(setOf(Branch.ZI, Branch.CHOU), xunKongOf(Stem.GUI, Branch.HAI))
        assertEquals(setOf(Branch.WEI, Branch.WU), xunKongOf(Stem.XIN, Branch.MAO))
    }
}
