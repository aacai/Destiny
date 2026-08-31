package zhiqiu.qizheng

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 多盘制自测：验证每个分歧点**确实生效**，且默认盘制与黄金样例口径一致。
 *
 * 断言尽量取「数学上可推导」的量（如古度/今度之比、罗计对调），
 * 不依赖外部参考盘，避免把流派待考之处写死成断言。
 */
class QizhengPanZhiTest {

    private fun build(config: QizhengConfig) = QizhengBuilder.build(
        year = 2026, month = 8, day = 29, hour = 14, minute = 5, gender = "男",
        config = config,
    )

    /** 角距（0..180） */
    private fun arc(a: Double, b: Double): Double {
        var d = abs(a - b) % 360.0
        if (d > 180.0) d = 360.0 - d
        return d
    }

    // ---------------------------------------------------------------- 冒烟

    @Test
    fun all_presets_build_without_crash() {
        for ((name, cfg) in PanZhiPresets.all) {
            val c = build(cfg)
            assertTrue(c.stars.isNotEmpty(), "$name 应有星曜")
            assertEquals(12, c.palaces.size, "$name 应有 12 宫")
            assertTrue(c.mingBranch in MingGong.Branches, "$name 命宫=${c.mingBranch}")
            assertTrue(c.xiuZeroDeg > 0.0, "$name 宿零点应已算出")
            assertTrue(c.panZhi.isNotBlank(), "$name 应有盘制名")
        }
    }

    @Test
    fun default_preset_matches_golden_name() {
        // 默认盘制名必须与改造前 QizhengDefaults.PanZhiName 完全一致
        assertEquals("回归今宿·黄道", PanZhiPresets.GuoLao.displayName)
        assertEquals("回归今宿·黄道", build(PanZhiPresets.GuoLao).panZhi)
    }

    // ---------------------------------------------------------------- 宿度制

    @Test
    fun xiuSystem_ancient_vs_modern_reading_ratio() {
        val zero = XiuTable.zeroDeg(0.266474)
        // 扫一圈黄经：宿名必须一致（宿界同源），读数之比恒为 365.25/360
        var checked = 0
        var lon = 0.0
        while (lon < 360.0) {
            val a = XiuTable.locate(lon, zero, XiuSystem.ANCIENT)
            val m = XiuTable.locate(lon, zero, XiuSystem.MODERN)
            assertEquals(a.name, m.name, "lon=$lon 宿名应一致")
            assertEquals(a.element, m.element, "lon=$lon 宿五行应一致")
            if (m.degreeInXiu > 0.5) {
                val ratio = a.degreeInXiu / m.degreeInXiu
                assertTrue(
                    abs(ratio - XiuRatio) < 1e-9,
                    "lon=$lon 古度/今度 = $ratio，应为 $XiuRatio",
                )
                checked++
            }
            lon += 7.3
        }
        assertTrue(checked > 30, "有效采样点太少: $checked")
    }

    @Test
    fun xiuSystem_affects_reading_not_palace() {
        val guDu = build(PanZhiPresets.GuoLao)          // 古度读数（默认）
        val jinDu = build(PanZhiPresets.ModernReading)  // 今度读数
        // 宿界同源 → 宿名、命宫都不变，只有宿内读数变
        assertEquals(guDu.mingBranch, jinDu.mingBranch, "读数制不应改变命宫")
        for (i in guDu.stars.indices) {
            assertEquals(guDu.stars[i].xiu, jinDu.stars[i].xiu, "星曜 ${guDu.stars[i].key} 宿名应一致")
        }
        val sunG = guDu.stars.first { it.key == "日" }.xiuDegree
        val sunJ = jinDu.stars.first { it.key == "日" }.xiuDegree
        assertTrue(sunG > sunJ, "古度读数应大于今度: $sunG vs $sunJ")
        assertTrue(abs(sunG / sunJ - XiuRatio) < 1e-9, "两者之比应为 $XiuRatio，实际 ${sunG / sunJ}")
        // 默认盘制必须保持传统古度口径：「箕四立」「斗 19.4」
        assertTrue(guDu.mingCenterTop.startsWith("箕"), "默认立命=${guDu.mingCenterTop}")
        assertTrue(Regex("斗\\s*([\\d.]+)").find(guDu.limits.daXian) != null, "默认大限应保持斗宿读数: ${guDu.limits.daXian}")
    }

    private companion object {
        const val XiuRatio = 365.25 / 360.0
    }

    // ---------------------------------------------------------------- 黄道基准

    @Test
    fun sidereal_offsets_zodiac_and_palace() {
        val trop = build(PanZhiPresets.GuoLao)
        val side = build(PanZhiPresets.Sidereal)
        assertEquals(0.0, trop.zodiacOffset, 1e-9, "回归制偏移应为 0")
        // 2026 年 ≈ J2000 + 0.266 世纪，Lahiri 23.8563 + 1.397×0.266 ≈ 24.23
        assertTrue(
            side.zodiacOffset > 23.5 && side.zodiacOffset < 25.0,
            "恒星偏移应在 24° 上下，实际 ${side.zodiacOffset}",
        )
        assertEquals(side.zodiacOffset, side.config.ayanamsa.offset(0.2665), 0.05)
        assertNotEquals(trop.mingBranch, side.mingBranch, "恒星制下命宫应西移一宫")
    }

    @Test
    fun ayanamsa_values_match_published_reference() {
        // J2000.0 权威值（Learn Jyotish / dekhopanchang）：
        // Lahiri 23.853°、KP(Krishnamurti) 23.749°、Raman 22.370°、Fagan-Bradley 24.042°
        assertEquals(23.85306, Ayanamsa.LAHIRI.offsetAtJ2000, 1e-5, "Lahiri")
        assertEquals(23.749, Ayanamsa.KRISHNAMURTI.offsetAtJ2000, 1e-5, "KP")
        assertEquals(22.370, Ayanamsa.RAMAN.offsetAtJ2000, 1e-5, "Raman")
        assertEquals(24.042, Ayanamsa.FAGAN_BRADLEY.offsetAtJ2000, 1e-5, "Fagan/Bradley")

        // Lahiri 官方多项式 A(T)=23.85306+1.39722T+0.00018T²−0.000005T³
        // T=0.26（2026 年）时应 ≈ 24.2164°
        assertEquals(24.2164, Ayanamsa.LAHIRI.offset(0.26), 0.001, "Lahiri@2026")
    }

    @Test
    fun ayanamsa_models_differ_by_about_1_5_degrees() {
        val lahiri = build(PanZhiPresets.Sidereal)
        val raman = build(PanZhiPresets.Sidereal.copy(ayanamsa = Ayanamsa.RAMAN))
        val gap = abs(lahiri.zodiacOffset - raman.zodiacOffset)
        // 权威值：Lahiri 与 Raman 相差 1.483°，足以让边界星曜换宫
        assertTrue(gap > 1.4 && gap < 1.6, "Lahiri 与 Raman 应相差约 1.48°，实际 $gap")
    }

    // ---------------------------------------------------------------- 罗计

    @Test
    fun nodeConvention_swaps_rahu_and_ketu() {
        val t = build(PanZhiPresets.GuoLao)
        val i = build(PanZhiPresets.GuoLao.copy(nodeConvention = NodeConvention.INDIAN))
        val rahuT = t.stars.first { it.key == "罗" }.longitude
        val ketuT = t.stars.first { it.key == "计" }.longitude
        val rahuI = i.stars.first { it.key == "罗" }.longitude
        val ketuI = i.stars.first { it.key == "计" }.longitude

        // 传统：罗=降交点；印度：罗=升交点 → 两派罗睺正好差 180°
        assertTrue(arc(rahuT, rahuI) > 179.9, "两派罗睺应对宫: ${arc(rahuT, rahuI)}")
        // 传统罗睺 == 印度计都（同为降交点）
        assertTrue(arc(rahuT, ketuI) < 0.01, "传统罗睺应等于印度计都")
        assertTrue(arc(ketuT, rahuI) < 0.01, "传统计都应等于印度罗睺")
        // 罗计恒为对宫
        assertTrue(arc(rahuT, ketuT) > 179.9, "罗计应对宫")
    }

    // ---------------------------------------------------------------- 紫炁

    @Test
    fun ziQi_toggle_and_period() {
        val base = build(PanZhiPresets.GuoLao)
        assertEquals(11, base.stars.size, "默认 11 曜")

        val off = build(PanZhiPresets.GuoLao.copy(useZiQi = false))
        assertEquals(10, off.stars.size, "关闭紫炁后 10 曜")
        assertTrue(off.stars.none { it.key == "炁" }, "不应再有紫炁")

        val p29 = build(PanZhiPresets.GuoLao.copy(ziQiMode = ZiQiMode.YEARS_29))
        val q28 = base.stars.first { it.key == "炁" }.longitude
        val q29 = p29.stars.first { it.key == "炁" }.longitude
        assertTrue(arc(q28, q29) > 1.0, "28 年与 29 年周期位置应不同: ${arc(q28, q29)}")

        val rev = build(PanZhiPresets.GuoLao.copy(ziQiMode = ZiQiMode.YEARS_28_REV))
        assertTrue(rev.stars.first { it.key == "炁" }.retro, "逆行派紫炁应标记为逆")
        assertTrue(base.stars.first { it.key == "炁" }.speed > 0, "顺行派速度应为正")
    }

    // ---------------------------------------------------------------- 立命 / 身宫

    @Test
    fun mingGongMethod_shifts_by_six() {
        val g = build(PanZhiPresets.GuoLao)
        val q = build(PanZhiPresets.GuoLao.copy(mingGongMethod = MingGongMethod.SHEN_TO_YOU))
        // 顺至卯(3) vs 顺至酉(9)：命宫相差 6 支
        val d = (q.mingBranchIndex - g.mingBranchIndex + 12) % 12
        assertEquals(6, d, "两种立命法应相差 6 支，实际 $d")
    }

    @Test
    fun shenGong_guolao_equals_moon_house() {
        // 果老派 / 耶律派：身宫 = 太阴所在宫（等同印度 Chandra Chart），与生时无关
        val c = build(PanZhiPresets.GuoLao)
        val moonBranch = c.stars.first { it.key == "月" }.branch
        assertEquals(moonBranch, c.shenBranch, "果老派身宫应等于月亮所在宫")
        assertTrue(c.shenBranch in MingGong.Branches)

        // 同一天换时辰：命宫会变（用太阳），身宫若月亮未跨宫则不变
        val otherHour = QizhengBuilder.build(2026, 8, 29, 2, 5, config = PanZhiPresets.GuoLao)
        assertEquals(
            otherHour.stars.first { it.key == "月" }.branch,
            otherHour.shenBranch,
            "身宫始终跟随月亮",
        )
    }

    @Test
    fun shenGong_qintang_reverses_to_you() {
        // 琴堂「逢酉安身」：太阴宫起生时逆数至酉。
        // 对称性自检 —— 酉时(支 9)生人步数为 0，身宫即太阴所在宫
        for (moonIdx in 0 until 12) {
            val shen = MingGong.shenBranchIndex(
                moonLon = MingGong.branchIndexToLonStart(moonIdx) + 15.0,
                hourBranchIndex = 9,
                method = ShenGongMethod.QINTANG_YOU,
            )
            assertEquals(moonIdx, shen, "酉时生人身宫应等于太阴宫 moonIdx=$moonIdx")
        }
        val qt = build(PanZhiPresets.GuoLao.copy(shenGongMethod = ShenGongMethod.QINTANG_YOU))
        val moonIdx = MingGong.Branches.indexOf(qt.stars.first { it.key == "月" }.branch)
        val hb = MingGong.hourToBranchIndex(14, 5)
        val expected = (moonIdx - (9 - hb + 12) % 12 + 12) % 12
        assertEquals(MingGong.Branches[expected], qt.shenBranch, "琴堂身宫公式")
    }

    @Test
    fun shenGong_toggle_off() {
        val off = build(PanZhiPresets.GuoLao.copy(useShenGong = false))
        assertNull(off.shenBranch, "关闭后身宫应为 null")
    }

    // ---------------------------------------------------------------- 神煞

    @Test
    fun shenSha_set_none_and_cap() {
        val full = build(PanZhiPresets.GuoLao)
        assertTrue(full.yearShenShaByBranch.any { it.isNotEmpty() }, "果老神煞不应为空")

        val none = build(PanZhiPresets.Minimal)
        assertTrue(none.yearShenShaByBranch.all { it.isEmpty() }, "NONE 时年环应全空")
        assertTrue(none.dayShenShaByBranch.all { it.isEmpty() }, "NONE 时日环应全空")

        val capped = build(PanZhiPresets.GuoLao.copy(shenShaMaxPerBranch = 1))
        val uncapped = build(PanZhiPresets.GuoLao.copy(shenShaMaxPerBranch = 0))
        for (i in 0 until 12) {
            assertTrue(capped.yearShenShaByBranch[i].size <= 1, "限 1 个时第 $i 支超额")
            assertTrue(
                uncapped.yearShenShaByBranch[i].size >= capped.yearShenShaByBranch[i].size,
                "不限时第 $i 支不应更少",
            )
        }
    }

    // ---------------------------------------------------------------- 庙旺

    @Test
    fun dignity_off_by_default() {
        val off = build(PanZhiPresets.GuoLao)
        assertTrue(off.dignity.isEmpty(), "默认不显示庙旺")

        val on = build(PanZhiPresets.WithDignity)
        assertTrue(on.dignity.isNotEmpty(), "开启后应有命中的曜")
        assertTrue(
            on.dignity.values.all { it in setOf("庙", "旺", "乐", "喜") },
            "等级标签异常: ${on.dignity.values}",
        )
        // 命中曜的地支必须与该曜表中的条目一致
        for (star in on.stars) {
            val label = on.dignity[star.key] ?: continue
            assertEquals(label, Dignity.levelOf(star.key, star.branch)?.label, "${star.key} 庙旺不符")
        }
    }

    // ---------------------------------------------------------------- 真太阳时

    @Test
    fun trueSolar_longitude_correction_is_4min_per_degree() {
        // 经度每偏西 15° → 校正 −60 分钟（均时差相同，相减抵消）
        val at120 = QizhengBuilder.applyTrueSolar(2026, 8, 29, 12, 0, lon = 120.0)
        val at105 = QizhengBuilder.applyTrueSolar(2026, 8, 29, 12, 0, lon = 105.0)
        val diff = (at105.hour * 60 + at105.minute) - (at120.hour * 60 + at120.minute)
        assertEquals(-60, diff, "偏西 15° 应慢 60 分钟，实际 $diff")
    }

    @Test
    fun plusDays_handles_month_and_year_rollover() {
        assertEquals(zhiqiu.qizheng.ClockParts(2026, 3, 1, 0, 0), QizhengBuilder.plusDays(2026, 2, 28, 1))
        assertEquals(zhiqiu.qizheng.ClockParts(2026, 2, 28, 0, 0), QizhengBuilder.plusDays(2026, 3, 1, -1))
        assertEquals(zhiqiu.qizheng.ClockParts(2027, 1, 1, 0, 0), QizhengBuilder.plusDays(2026, 12, 31, 1))
        assertEquals(zhiqiu.qizheng.ClockParts(2025, 12, 31, 0, 0), QizhengBuilder.plusDays(2026, 1, 1, -1))
        // 闰年：2024 年 2 月有 29 天
        assertEquals(29, QizhengBuilder.daysInMonth(2024, 2))
        assertEquals(28, QizhengBuilder.daysInMonth(2026, 2))
        assertEquals(zhiqiu.qizheng.ClockParts(2024, 2, 29, 0, 0), QizhengBuilder.plusDays(2024, 2, 28, 1))
        // 同月内加减不应改变月份
        assertEquals(zhiqiu.qizheng.ClockParts(2026, 8, 20, 0, 0), QizhengBuilder.plusDays(2026, 8, 29, -9))
    }

    @Test
    fun equationOfTime_in_reasonable_range() {
        // 均时差全年约 −14..+16 分钟，近似公式放宽到 −20..+25
        for (m in 1..12) {
            val eot = QizhengBuilder.equationOfTimeMinutes(2026, m, 15)
            assertTrue(eot in -20.0..25.0, "month=$m EoT=$eot 超出合理范围")
        }
    }

    @Test
    fun trueSolar_changes_chart_and_stays_same_by_default() {
        val civil = build(PanZhiPresets.GuoLao)
        assertEquals(14, civil.hour)
        assertEquals(5, civil.minute)

        val solar = build(PanZhiPresets.TrueSolar)
        // 北京 116.4074° 比东经 120° 偏西 3.59° → 约 −14.4 分钟，再加均时差
        val diff = (solar.hour * 60 + solar.minute) - (civil.hour * 60 + civil.minute)
        assertTrue(diff in -30..5, "北京真太阳时校正应在 −30..+5 分钟内，实际 $diff")
        assertEquals(solar.config.clockMode, ClockMode.TRUE_SOLAR)
    }

    // ---------------------------------------------------------------- 无全局状态

    @Test
    fun no_global_state_leak_between_builds() {
        // 连续用不同盘制排盘，后者不得受前者影响（原先两个全局 var 正是此处会串味）
        val a = build(PanZhiPresets.Sidereal)
        val b = build(PanZhiPresets.GuoLao)
        val c = build(PanZhiPresets.GuoLao)
        assertEquals(b.mingBranch, c.mingBranch, "同盘制两次排盘应完全一致")
        assertEquals(b.xiuZeroDeg, c.xiuZeroDeg, 1e-12)
        assertEquals(b.stars.map { it.longitude }, c.stars.map { it.longitude })
        assertNotEquals(a.zodiacOffset, b.zodiacOffset, "不同盘制偏移应不同")
    }

    // ---------------------------------------------------------------- dump

    @Test
    fun dump_all_presets_for_manual_review() {
        val out = StringBuilder()
        for ((name, cfg) in PanZhiPresets.all) {
            val c = build(cfg)
            val sun = c.stars.first { it.key == "日" }
            out.append(
                "%-16s %s\n".format(name, c.panZhi) +
                    "   命宫=${c.mingBranch} 立命=${c.mingCenterTop} 身宫=${c.shenBranch ?: "—"}\n" +
                    "   日=${"%.2f".format(sun.longitude)}° ${sun.xiu}${("%.2f".format(sun.xiuDegree))} " +
                    "偏移=${"%.2f".format(c.zodiacOffset)}° 曜数=${c.stars.size}\n" +
                    "   ${c.panZhiDetail}\n",
            )
        }
        println(out)
        java.io.File("/tmp/qizheng-panzhi.txt").writeText(out.toString())
    }
}
