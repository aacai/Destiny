package zhiqiu.meihua

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import zhiqiu.liuyao.Trigram

class MeiHuaCoreTest {

    @Test
    fun `先天数与取余`() {
        assertEquals(1, innateNumber(Trigram.QIAN))
        assertEquals(8, innateNumber(Trigram.KUN))
        assertEquals(8, rem8(8))
        assertEquals(8, rem8(16))
        assertEquals(2, rem8(34))
        assertEquals(6, rem6(6))
        assertEquals(1, rem6(43))
        assertEquals(2, rem6(8))
    }

    /** 牡丹占口径：辰年十二月十七申时 → 泽火革初爻动变泽山咸，互乾巽。 */
    @Test
    fun `时间起卦 辰年十二月十七申时得革变咸`() {
        // 辰=5，申=9
        val chart = castFromLunar(
            yearBranchNum = 5,
            lunarMonth = 12,
            lunarDay = 17,
            hourBranchNum = 9,
            yearBranchLabel = "辰",
            hourBranchLabel = "申",
        )
        assertEquals("泽火革", chart.ben.fullName)
        assertEquals(1, chart.movingYao)
        assertEquals("泽山咸", chart.bian.fullName)
        assertEquals(Trigram.DUI, chart.upper)
        assertEquals(Trigram.LI, chart.lower)
        // 二三四巽、三四五乾 → 天风姤；原书常只说互见乾巽
        assertEquals(Trigram.QIAN, chart.hu.upper)
        assertEquals(Trigram.XUN, chart.hu.lower)
        // 初爻在下 → 下离为用，上兑为体
        assertEquals(Trigram.DUI, chart.ti)
        assertEquals(Trigram.LI, chart.yong)
        assertTrue(chart.tiIsUpper)
    }

    /** 数字 3、5 → 火风鼎二爻动 → 火山旅；互泽天夬。 */
    @Test
    fun `数字起卦 3与5得鼎变旅`() {
        val chart = castFromNumbers(3, 5)
        assertEquals("火风鼎", chart.ben.fullName)
        assertEquals(2, chart.movingYao)
        assertEquals("火山旅", chart.bian.fullName)
        assertEquals(Trigram.LI, chart.upper)
        assertEquals(Trigram.XUN, chart.lower)
        assertEquals("泽天夬", chart.hu.fullName)
        // 二爻在下 → 巽用、离体；用木生体火
        assertEquals(Trigram.LI, chart.ti)
        assertEquals(Trigram.XUN, chart.yong)
        assertEquals(TiYongRelation.YONG_SHENG_TI, chart.relation)
    }

    /** 字占例：8、5 → 地风升初爻动变泰；互震兑。 */
    @Test
    fun `数字起卦 8与5得升变泰`() {
        val chart = castFromNumbers(8, 5)
        assertEquals("地风升", chart.ben.fullName)
        assertEquals(1, chart.movingYao)
        assertEquals("地天泰", chart.bian.fullName)
        // 升：二三四兑、三四五震 → 互见震兑
        assertEquals(Trigram.DUI, chart.hu.lower)
        assertEquals(Trigram.ZHEN, chart.hu.upper)
    }

    @Test
    fun `端法五例动爻`() {
        // 老人忧色：乾1 巽5 卯4 → 10 → 四爻
        assertEquals(4, castFromXiang(Trigram.QIAN, Trigram.XUN, 4).movingYao)
        assertEquals(10, castFromXiang(Trigram.QIAN, Trigram.XUN, 4).chengGuaShu)
        // 少年喜色：艮7 离3 午7 → 17 → 五爻
        assertEquals(5, castFromXiang(Trigram.GEN, Trigram.LI, 7).movingYao)
        // 牛哀鸣：坤8 坎6 午7 → 21 → 三爻
        assertEquals(3, castFromXiang(Trigram.KUN, Trigram.KAN, 7).movingYao)
        // 鸡悲鸣：巽5 乾1 卯4 → 10 → 四爻
        assertEquals(4, castFromXiang(Trigram.XUN, Trigram.QIAN, 4).movingYao)
        // 枯枝坠地：离3 兑2 辰5 → 10 → 四爻
        assertEquals(4, castFromXiang(Trigram.LI, Trigram.DUI, 5).movingYao)
    }

    @Test
    fun `端法应期 行半立倍坐`() {
        val walk = castFromXiang(Trigram.QIAN, Trigram.XUN, 4, Posture.WALKING)
        assertEquals(5.0, walk.yingQiDays)
        val stand = castFromXiang(Trigram.QIAN, Trigram.XUN, 4, Posture.STANDING)
        assertEquals(10.0, stand.yingQiDays)
        val sit = castFromXiang(Trigram.QIAN, Trigram.XUN, 4, Posture.SITTING)
        assertEquals(20.0, sit.yingQiDays)
    }

    @Test
    fun `乾为天互卦改取变卦之互`() {
        // 五爻动：乾→火天大有；本卦互仍乾，改取大有之互（下乾上兑=泽天夬）
        val chart5 = castManual(Trigram.QIAN, Trigram.QIAN, 5)
        assertEquals("乾为天", chart5.ben.fullName)
        assertTrue(chart5.huFromBian)
        assertEquals("火天大有", chart5.bian.fullName)
        assertEquals("泽天夬", chart5.hu.fullName)
        assertEquals(Trigram.DUI, chart5.hu.upper)
        assertEquals(Trigram.QIAN, chart5.hu.lower)
    }

    @Test
    fun `坤为地互卦改取变卦之互`() {
        val chart = castManual(Trigram.KUN, Trigram.KUN, 2)
        assertEquals("坤为地", chart.ben.fullName)
        assertTrue(chart.huFromBian)
        assertFalse(chart.hu.fullName == "坤为地" && !chart.huFromBian)
    }

    @Test
    fun `体用五关系断语`() {
        assertEquals(TiYongRelation.BI_HE, tiYongRelationOf(Trigram.QIAN, Trigram.DUI))
        assertEquals(TiYongRelation.TI_KE_YONG, tiYongRelationOf(Trigram.QIAN, Trigram.XUN)) // 金克木
        assertEquals(TiYongRelation.YONG_SHENG_TI, tiYongRelationOf(Trigram.LI, Trigram.XUN)) // 木生火
        assertEquals(TiYongRelation.TI_SHENG_YONG, tiYongRelationOf(Trigram.XUN, Trigram.LI)) // 木生火=体生用
        assertEquals(TiYongRelation.YONG_KE_TI, tiYongRelationOf(Trigram.XUN, Trigram.QIAN)) // 金克木=用克体
        assertTrue(TiYongRelation.TI_KE_YONG.verdict.contains("吉"))
        assertTrue(TiYongRelation.YONG_KE_TI.verdict.contains("凶"))
    }

    @Test
    fun `手动与随机可成盘`() {
        val m = castManual(Trigram.LI, Trigram.KAN, 3)
        assertEquals("火水未济", m.ben.fullName)
        val r = castRandom(kotlin.random.Random(42))
        assertEquals(CastMethod.RANDOM, r.method)
        assertTrue(r.ben.fullName.isNotEmpty())
    }
}
