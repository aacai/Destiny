package zhiqiu.meihua

import kotlin.test.Test
import kotlin.test.assertEquals
import zhiqiu.liuyao.Trigram

/** 对照原书观梅占与阳历换算口径的复核用例。 */
class MeiHuaVerifyDumpTest {

    @Test
    fun `观梅占体用为用克体`() {
        val chart = castFromLunar(5, 12, 17, 9, yearBranchLabel = "辰", hourBranchLabel = "申")
        assertEquals(TiYongRelation.YONG_KE_TI, chart.relation)
        assertEquals("用克体，诸事凶", chart.relation.verdict)
        // 变后用=艮，体兑；土生金=用生体
        assertEquals(Trigram.GEN, chart.bianYong)
        assertEquals(TiYongRelation.YONG_SHENG_TI, chart.bianRelation)
        assertEquals("用凶变吉，先凶后吉", chart.changeSummary)
    }

    @Test
    fun `阳历跨正月初一换农历年支`() {
        // 2024-02-09 腊月三十 仍属癸卯年；2024-02-10 正月初一 甲辰年
        val before = castFromSolar(2024, 2, 9, 12, 0)
        val after = castFromSolar(2024, 2, 10, 12, 0)
        assertEquals("卯", before.stamp!!.yearBranchLabel)
        assertEquals("辰", after.stamp!!.yearBranchLabel)
        assertEquals(1, after.stamp!!.month)
        assertEquals(1, after.stamp!!.day)
    }

    @Test
    fun `申时序为9 夜子时序为1`() {
        // 任意日 15:30 为申时（15-17）
        val shen = castFromSolar(2024, 3, 1, 15, 30)
        assertEquals(9, shen.stamp!!.hourBranchNum)
        assertEquals("申", shen.stamp!!.hourBranchLabel)
        // 23:30 夜子
        val zi = castFromSolar(2024, 3, 1, 23, 30)
        assertEquals(1, zi.stamp!!.hourBranchNum)
        assertEquals("子", zi.stamp!!.hourBranchLabel)
    }
}
