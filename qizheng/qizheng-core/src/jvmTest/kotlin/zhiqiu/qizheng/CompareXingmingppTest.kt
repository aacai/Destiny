package zhiqiu.qizheng

import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 与权威在线排盘「星命排盘 V1.25」(https://xingmingpp.xyz/) 逐项核对。
 * 同一生辰：2026-08-29 14:05，男，默认北京。
 * 网站口径：回归今宿·黄道，默认真太阳时（界面显示「真太阳时14:05」）。
 *
 * 参考值（web）从网站 DOM 原文提取（黄经度 / 落宫 / 宿）：
 *   日 155.9397 巳 星 | 月 348.7667 亥 危 | 水 157.4572 巳 张
 *   金 200.9342 辰 轸 | 火 101.6467 未 井 | 木 133.1206 午 柳
 *   土 013.8303 戌 壁 | 计 329.8358 子 虚 | 罗 149.7547 午 星
 *   孛 268.0639 寅 尾 | 炁 172.1486 巳 张
 * 立命 丑5.94 箕4.30 | 安身 亥18.77 危15.03
 */
class CompareXingmingppTest {

    private val ref = mapOf(
        "日" to Triple(155.9397, "巳", "星"),
        "月" to Triple(348.7667, "亥", "危"),
        "水" to Triple(157.4572, "巳", "张"),
        "金" to Triple(200.9342, "辰", "轸"),
        "火" to Triple(101.6467, "未", "井"),
        "木" to Triple(133.1206, "午", "柳"),
        "土" to Triple(13.8303, "戌", "壁"),
        "计" to Triple(329.8358, "子", "虚"),
        "罗" to Triple(149.7547, "午", "星"),
        "孛" to Triple(268.0639, "寅", "尾"),
        "炁" to Triple(172.1486, "巳", "张"),
    )

    @Test
    fun compare_to_xingmingpp() {
        val out = StringBuilder()
        for ((cfg, label) in listOf(QizhengConfig() to "CIVIL", PanZhiPresets.TrueSolar to "TRUE_SOLAR")) {
            val c = QizhengBuilder.build(2026, 8, 29, 14, 5, "男", config = cfg)
            out.appendLine(
                "===== $label =====  bazi=${c.baziLabel} ming=${c.mingBranch} " +
                    "shen=${c.shenBranch} zero=${"%.4f".format(c.xiuZeroDeg)}"
            )
            ref.forEach { (k, t) ->
                val (wl, wb, wx) = t
                val s = c.stars.firstOrNull { it.key == k }
                if (s != null) {
                    val dLon = s.longitude - wl
                    val branchOk = if (s.branch == wb) "✓" else "✗"
                    val xiuOk = if (s.xiu == wx) "✓" else "✗"
                    out.appendLine(
                        "%-2s web lon=%8.4f %s %s | mine lon=%8.4f %s %s%s dLon=%+7.4f  %s%s motion=%s".format(
                            k, wl, wb, wx, s.longitude, s.branch, s.xiu,
                            "%.2f".format(s.xiuDegree), dLon, branchOk, xiuOk, s.motion
                        )
                    )
                } else {
                    out.appendLine("$k  —— 未找到")
                }
            }
            out.appendLine("ming=${c.mingBranch}(web 丑) shen=${c.shenBranch}(web 亥)")
            out.appendLine()
        }
        println(out)
        File("/tmp/compare-xingmingpp.txt").writeText(out.toString())
    }

    /** 平太阳时：落宫落宿必须与网站参考全对；黄经允许 0.5° 内 */
    @Test
    fun assert_civil_matches_xingmingpp_branch_xiu() {
        val c = QizhengBuilder.build(2026, 8, 29, 14, 5, "男")
        assertEquals("丙午 丙申 乙亥 癸未", c.baziLabel)
        assertEquals("丑", c.mingBranch)
        assertEquals("亥", c.shenBranch)
        ref.forEach { (k, t) ->
            val (wl, wb, wx) = t
            val s = c.stars.first { it.key == k }
            assertEquals(wb, s.branch, "$k 落宫")
            assertEquals(wx, s.xiu, "$k 落宿")
            assertTrue(abs(s.longitude - wl) < 0.5, "$k 黄经差 ${s.longitude - wl}")
        }
    }

    /** 神煞丰富度：黄金样例必验 + 新增岁煞/空亡/长生 */
    @Test
    fun assert_shensha_richness_on_golden_sample() {
        val c = QizhengBuilder.build(2026, 8, 29, 14, 5, "男")
        val wu = MingGong.Branches.indexOf("午")
        val shen = MingGong.Branches.indexOf("申")
        val xu = MingGong.Branches.indexOf("戌")
        val zi = MingGong.Branches.indexOf("子")
        val you = MingGong.Branches.indexOf("酉")
        assertTrue("太岁" in c.yearShenShaByBranch[wu])
        assertTrue("驿马" in c.yearShenShaByBranch[shen])
        assertTrue("华盖" in c.yearShenShaByBranch[xu])
        assertTrue("岁破" in c.yearShenShaByBranch[zi] || "天哭" in c.yearShenShaByBranch[zi])
        assertTrue("飞廉" in c.yearShenShaByBranch[you] || "卷舌" in c.yearShenShaByBranch[you])
        // 乙亥日 → 甲戌旬空申酉
        assertTrue(c.dayShenShaByBranch[shen].any { it == "空亡" })
        assertTrue(c.dayShenShaByBranch[you].any { it == "空亡" })
        // 十二长生应铺满十二支各至少一字
        val cs = setOf("长生", "沐浴", "冠带", "临官", "帝旺", "衰", "病", "死", "墓", "绝", "胎", "养")
        val hit = c.dayShenShaByBranch.flatten().count { it in cs }
        assertTrue(hit >= 12, "十二长生应落满，实际命中 $hit")
    }

    /** 五星须带迟留伏逆状态字 */
    @Test
    fun assert_five_planets_have_motion_state() {
        val c = QizhengBuilder.build(2026, 8, 29, 14, 5, "男")
        listOf("水", "金", "火", "木", "土").forEach { k ->
            val m = c.stars.first { it.key == k }.motion
            assertTrue(m in setOf("顺", "迟", "留", "伏", "逆"), "$k motion=$m")
        }
        // 本盘水金距日很近，至少一颗应为伏或迟/顺（不允许空串）
        val shui = c.stars.first { it.key == "水" }
        assertTrue(shui.motion.isNotBlank())
    }

    /** 多生辰冒烟：能排出盘且十一曜齐全 */
    @Test
    fun smoke_multiple_births() {
        val samples = listOf(
            listOf(1990, 1, 15, 8, 0),
            listOf(2000, 6, 1, 12, 30),
            listOf(2010, 12, 21, 23, 45),
            listOf(2024, 2, 10, 6, 15),
        )
        samples.forEach { (y, m, d, h, mi) ->
            val c = QizhengBuilder.build(y, m, d, h, mi, "女")
            assertEquals(11, c.stars.size, "$y-$m-$d")
            assertTrue(c.mingBranch in MingGong.Branches)
            assertTrue(c.shenBranch in MingGong.Branches)
            assertTrue(c.baziLabel.split(" ").size == 4)
        }
    }

    /** 七政四柱与 tyme 八字一致（可与紫微/八字模块交叉核对） */
    @Test
    fun bazi_matches_tyme_eight_char() {
        val c = QizhengBuilder.build(2026, 8, 29, 14, 5, "男")
        val ec = com.tyme.solar.SolarTime(2026, 8, 29, 14, 5, 0).getLunarHour().getEightChar()
        val expect = listOf(
            ec.getYear().getName(),
            ec.getMonth().getName(),
            ec.getDay().getName(),
            ec.getHour().getName(),
        ).joinToString(" ")
        assertEquals(expect, c.baziLabel)
    }

    @Test
    fun sunrise_local_beijing_august_is_morning() {
        val sr = QizhengBuilder.sunriseSunsetLocal(2026, 8, 29, 116.4, 39.9, rising = true)
        val ss = QizhengBuilder.sunriseSunsetLocal(2026, 8, 29, 116.4, 39.9, rising = false)
        assertTrue(sr.hour in 4..7, "北京 8 月日出应在清晨，实际 ${sr.hour}:${sr.minute}")
        assertTrue(ss.hour in 17..20, "北京 8 月日落应在傍晚，实际 ${ss.hour}:${ss.minute}")
        assertTrue(ss.hour > sr.hour)
    }
}
