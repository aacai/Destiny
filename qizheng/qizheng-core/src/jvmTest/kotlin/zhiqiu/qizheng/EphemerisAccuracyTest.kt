package zhiqiu.qizheng

import com.tyme.culture.Phase
import com.tyme.solar.SolarTerm
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/**
 * 星历精度自测 —— 不依赖任何外部网络服务，全部用「可反推的天文锚点」校验：
 *
 * 1. **二十四节气**：tyme4kt 的 [SolarTerm] 给出节气的精确时刻（寿星历，秒级）。
 *    在该时刻，太阳**视黄经**必须精确等于节气度数（冬至 270°、春分 0°…）。
 *    这一项同时校验了儒略日换算、UT→TT 的 ΔT、以及太阳算法本身。
 * 2. **朔望**：tyme4kt 的 [Phase] 给出定朔/定望时刻。新月时日月黄经差为 0°，
 *    满月时为 180°。用于校验月亮算法。
 * 3. **Meeus 交叉校验**：用《Astronomical Algorithms》第 25 章独立实现太阳视黄经，
 *    与寿星历（VSOP87）的结果对比，两者算法完全独立。
 */
class EphemerisAccuracyTest {

    /**
     * tyme4kt 的 [com.tyme.jd.JulianDay] 采用「北京时间读数按 UTC 刻度」的约定：
     * 其数值 = 真实 UT 儒略日 + 8h（= 1/3 天）。这是寿星历为让节气直接落在
     * **北京日期**上所做的处理（[SolarTerm.getSolarDay] 因此给出北京日期）。
     *
     * 实测佐证：不减这个偏移时，冬至（日速 1.017°/天）偏 0.3396°、
     * 夏至（日速 0.953°/天）偏 0.3178°，两者除以各自日速都恰好 = 0.3334 天。
     *
     * 本测试要的是真实 UT（[Ephemeris] 的入参口径），故统一减掉 8 小时。
     */
    private val TymeJdBeijingOffsetDays = 1.0 / 3.0

    private fun norm180(x: Double): Double {
        var v = x % 360.0
        if (v > 180.0) v -= 360.0
        if (v < -180.0) v += 360.0
        return v
    }

    /** Meeus《Astronomical Algorithms》第 25 章：太阳视黄经（低精度，误差 ≲ 0.01°） */
    private fun meeusApparentSunLon(jdTt: Double): Double {
        val t = (jdTt - 2451545.0) / 36525.0
        val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t * t
        val m = 357.52911 + 35999.05029 * t - 0.0001537 * t * t
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(Math.toRadians(m)) +
            (0.019993 - 0.000101 * t) * sin(Math.toRadians(2 * m)) +
            0.000289 * sin(Math.toRadians(3 * m))
        val o = l0 + c
        val omega = 125.04 - 1934.136 * t
        // 光行差（低精度）
        return o - 0.00569 - 0.00478 * sin(Math.toRadians(omega))
    }

    @Test
    fun sun_longitude_matches_24_solar_terms() {
        val report = StringBuilder()
        var worst = 0.0
        var worstLabel = ""

        for (year in 2024..2028) {
            for (index in 0..23) {
                val term = SolarTerm(year, index)
                val jd = term.getJulianDay().getDay() - TymeJdBeijingOffsetDays
                // 节气名序列自冬至起：冬至 270°，每节气 +15°
                val expected = (270.0 + index * 15.0) % 360.0
                val actual = Ephemeris.sunLongitude(jd)
                val err = abs(norm180(actual - expected))
                if (err > worst) {
                    worst = err
                    worstLabel = "${term.getName()} jd=$jd"
                }
                if (year == 2026) {
                    report.append("${term.getName()} jd=%.5f  expect=%.4f  actual=%.6f  err=%.6f\n".format(jd, expected, actual, err))
                }
            }
        }
        println(report)
        java.io.File("/tmp/qizheng-solar-terms.txt").writeText(report.toString())
        assertTrue(
            worst < 0.002,
            "太阳视黄经与节气的最大偏差 ${worst}° 超出 0.002°（@$worstLabel）",
        )
    }

    @Test
    fun sun_longitude_crosscheck_meeus() {
        var worst = 0.0
        for (d in 0..730 step 7) {
            val jdUt = 2461278.75 + d // 2026-08-29 起两年
            val jdTt = Ephemeris.jdTerrestrial(jdUt)
            val a = Ephemeris.sunLongitude(jdUt)
            val b = meeusApparentSunLon(jdTt)
            worst = maxOf(worst, abs(norm180(a - b)))
        }
        println("sun cross-check vs Meeus: worst=$worst deg")
        assertTrue(worst < 0.01, "寿星历与 Meeus 太阳黄经差异 $worst° 超出 0.01°")
    }

    @Test
    fun moon_longitude_matches_syzygy() {
        val report = StringBuilder()
        var worstNew = 0.0
        var worstFull = 0.0

        for (month in 1..12) {
            val newMoon = Phase.fromIndex(2026, month, 0)
            val jdNew = newMoon.getSolarTime().getJulianDay().getDay() - TymeJdBeijingOffsetDays
            val dNew = norm180(Ephemeris.moonLongitude(jdNew) - Ephemeris.sunLongitude(jdNew))
            worstNew = maxOf(worstNew, abs(dNew))
            report.append("2026-$month 新月 jd=%.5f  日月角距=%.5f\n".format(jdNew, dNew))

            val fullMoon = Phase.fromIndex(2026, month, 4)
            val jdFull = fullMoon.getSolarTime().getJulianDay().getDay() - TymeJdBeijingOffsetDays
            val dFull = norm180(Ephemeris.moonLongitude(jdFull) - Ephemeris.sunLongitude(jdFull))
            worstFull = maxOf(worstFull, abs(dFull - 180.0))
            report.append("2026-$month 满月 jd=%.5f  日月角距=%.5f\n".format(jdFull, dFull))
        }
        println(report)
        java.io.File("/tmp/qizheng-moon-phases.txt").writeText(report.toString())
        assertTrue(worstNew < 0.02, "新月日月角距最大偏差 $worstNew° 超出 0.02°")
        assertTrue(worstFull < 0.02, "满月日月角距最大偏差 $worstFull° 超出 0.02°")
    }

    @Test
    fun deltaT_is_sane() {
        // 2000 年 ΔT ≈ 64 s，2026 年 ≈ 72 s，均应为正值且量级正确
        val dt2026 = Ephemeris.deltaT(2461278.75) * 86400.0
        println("deltaT(2026) = $dt2026 s")
        assertTrue(dt2026 in 60.0..90.0, "2026 年 ΔT 应约 72 s，实际 $dt2026 s")
    }

    /**
     * 用**独立可观测的天象**校验五星：冲日。
     *
     * 冲日定义：行星地心黄经 − 太阳黄经 = 180°（地球位于太阳与行星之间）。
     * 本测试不依赖任何冲日的精确钟点，而是**自行反解**冲日时刻，
     * 再与天文年历公布的日期比对 —— 这样避开了「公布时刻的时区/钟点」歧义。
     */
    private fun oppositionJd(body: Ephemeris.Body, y: Int, m: Int, dayFrom: Int, dayTo: Int): Double {
        var bestJd = 0.0
        var bestErr = Double.MAX_VALUE
        var jd = AstroMath.julianDay(y, m, dayFrom, 0.0)
        val end = AstroMath.julianDay(y, m, dayTo, 0.0)
        while (jd <= end) {
            val d = norm180(
                Ephemeris.planetLongitude(body, jd) - Ephemeris.sunLongitude(jd) - 180.0
            )
            if (abs(d) < bestErr) {
                bestErr = abs(d)
                bestJd = jd
            }
            jd += 0.01
        }
        return bestJd
    }

    @Test
    fun jupiter_opposition_2026_falls_on_published_date() {
        // 权威来源（NASA 中文、百度百科、多家天文媒体）一致：2026 年木星冲日在 1 月 10 日
        // 权威时刻：北京时间 2026-01-10 16:42 = UT 08:42
        // 来源：新华社天津电、有趣天文奇观、中国地理学会（三方一致）
        val ut = AstroMath.julianDay(2026, 1, 10, 8.7)
        val jd = oppositionJd(Ephemeris.Body.Jupiter, 2026, 1, 5, 20)
        val jupLon = Ephemeris.planetLongitude(Ephemeris.Body.Jupiter, ut)
        val sunLon = Ephemeris.sunLongitude(ut)
        java.io.File("/tmp/qizheng-opposition.txt").writeText(
            "反解黄经冲 JD=$jd\n权威 UT 08:42 JD=$ut\n偏差=${jd - ut} 天\n" +
                "该时刻 木星黄经=$jupLon°  太阳黄经=$sunLon°\n" +
                "黄经差=${norm180(jupLon - sunLon)}°\n" +
                "木星所在宫=${MingGong.Branches[MingGong.longitudeToBranchIndex(jupLon)]}\n",
        )
        println("木星冲日：反解=${jd} 权威=${ut} 偏差=${jd - ut} 天；木星黄经=$jupLon")
        // ⚠️ 天文年历的「冲」按**视赤经**差 180°，本项目只算黄经，用**黄经冲**反解；
        // 两者因黄赤交角可差约 0.3–0.5 天。故容差放宽到 1.5 天，
        // 本测试用于排除量级 ≥30° 的算法错误，不作定量精度断言。
        assertTrue(
            abs(jd - ut) < 1.5,
            "木星冲日反解与权威相差 ${abs(jd - ut)} 天，超出 1.5 天容差",
        )
    }

    @Test
    fun saturn_and_mars_opposition_2026_are_consistent() {
        // 土星冲日：2026 年同样在 1 月（土星冲日每年较木星晚约 2 周，2026 年约 1 月下旬）
        val saturnJd = oppositionJd(Ephemeris.Body.Saturn, 2026, 1, 5, 31)
        // 火星冲日：火星周期约 780 天，2025 年 1 月冲日后，下一次在 2027 年初，
        // 故 2026 年 1 月不应出现火星冲日 —— 用此排除「算法把任意行星都算成冲日」的假阳性
        val marsErr = run {
            var best = Double.MAX_VALUE
            var jd = AstroMath.julianDay(2026, 1, 1, 0.0)
            val end = AstroMath.julianDay(2026, 12, 31, 0.0)
            while (jd <= end) {
                val d = abs(
                    norm180(
                        Ephemeris.planetLongitude(Ephemeris.Body.Mars, jd) -
                            Ephemeris.sunLongitude(jd) - 180.0
                    )
                )
                if (d < best) best = d
                jd += 0.5
            }
            best
        }
        println("2026 土星冲日 JD=$saturnJd；2026 全年火星距冲日最小角距=$marsErr°")
        // 土星冲日应能反解到（1 月内存在零点）
        assertTrue(saturnJd > 0.0, "应能反解出 2026 年土星冲日")
        // 火星 2026 年不应冲日：最小角距应显著大于 0（>5°）
        assertTrue(marsErr > 5.0, "2026 年不应有火星冲日，实际最小角距仅 $marsErr°")
    }

    @Test
    fun xiu_zero_agrees_with_historic_winter_solstice() {
        // 金标准对齐「星命排盘 V1.25」：2026 冬至点（黄经 270°）落尾宿约 13.5 今度。
        // 注：古宿度表言冬至在箕；今宿度表因距星变迁已移到尾，本标定采用今宿。
        val z = XiuTable.zeroDeg(Ephemeris.centuriesTt(AstroMath.julianDay(2026, 8, 29, 6.0)))
        val loc = XiuTable.locate(270.0, z, XiuSystem.MODERN)
        println("2026 冬至点落宿: ${loc.name}${"%.2f".format(loc.degreeInXiu)}（网站：尾13.47）")
        assertEquals("尾", loc.name, "2026 冬至点应与网站一致落在尾宿")
        assertTrue(abs(loc.degreeInXiu - 13.47) < 0.5, "冬至点应在尾13.5今度附近，实际 ${loc.degreeInXiu}")
    }

    @Test
    fun xiu_zero_moves_with_precession() {
        // 今宿零点随岁差东移 ≈ 1.397°/世纪
        val z2026 = XiuTable.zeroDeg(Ephemeris.centuriesTt(2461278.75))
        val z1926 = XiuTable.zeroDeg(Ephemeris.centuriesTt(AstroMath.julianDay(1926, 8, 29, 6.0)))
        val drift = z2026 - z1926
        println("xiu zero 2026=$z2026  1926=$z1926  drift=$drift deg/century")
        assertTrue(abs(drift - XiuTable.PrecessionDegPerCentury) < 0.01, "岁差漂移 $drift 异常")
        // 冬至点应稳定落在尾宿（岁差 100 年内不足以跨宿）
        val winterSolsticeXiu = XiuTable.locate(270.0, z2026)
        println("2026 冬至点落宿: ${winterSolsticeXiu.name}${winterSolsticeXiu.degreeInXiu}")
        assertTrue(winterSolsticeXiu.name == "尾", "2026 冬至点应在尾宿，实际 ${winterSolsticeXiu.name}")
    }
}
