package zhiqiu.qizheng

import com.tyme.util.ShouXingUtil
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

object AstroMath {
    const val Deg2Rad = PI / 180.0
    const val Rad2Deg = 180.0 / PI

    /** 儒略日（J2000.0 = 2000-01-01 12:00 TT） */
    const val J2000 = 2451545.0

    fun norm360(x: Double): Double {
        var v = x % 360.0
        if (v < 0) v += 360.0
        return v
    }

    /** 归一化到 (-180, 180] */
    fun norm180(x: Double): Double {
        var v = norm360(x)
        if (v > 180.0) v -= 360.0
        return v
    }

    fun sind(d: Double) = sin(d * Deg2Rad)
    fun cosd(d: Double) = cos(d * Deg2Rad)

    /** 黄经 → 赤经（赤道恒星制布盘用），黄赤交角取 J2000 标准值 */
    fun rightAscension(lonDeg: Double, obliquityDeg: Double = 23.4367): Double =
        norm360(
            kotlin.math.atan2(
                sind(lonDeg) * cosd(obliquityDeg),
                cosd(lonDeg),
            ) * Rad2Deg
        )

    /** Gregorian → Julian Day (UT), hour as decimal. */
    fun julianDay(year: Int, month: Int, day: Int, hourUt: Double): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) +
            floor(30.6001 * (m + 1)) +
            day + hourUt / 24.0 + b - 1524.5
    }

    fun centuriesJ2000(jd: Double): Double = (jd - J2000) / 36525.0
}

/**
 * 七政四余星历。
 *
 * 精度分层：
 * - **日月**：走寿星天文历（VSOP87 + ELP 级数，tyme4kt 的 [ShouXingUtil]），
 *   视黄经含章动与光行差，误差 < 1″（约 0.0003°）。
 * - **五星**：Meeus《Astronomical Algorithms》第 31/32 章完整轨道要素
 *   （含 T²/T³ 项）＋椭圆运动，误差 ≲ 0.01°。
 * - **罗睺/计都**：Meeus 第 22 章月球平均交点，误差 ≲ 0.01°。
 * - **月孛**：Meeus 月球平均远地点（顺行，8.85 年一周）。
 * - **紫炁**：传统约定的 28 年一周虚星，非天文实点（见 [Ephemeris.ziQi]）。
 *
 * 时间基准：入参一律为 **UT 儒略日**，内部统一换算到 **TT**（力学时）后再算，
 * 由 [deltaT] 提供 ΔT。
 */
object Ephemeris {
    enum class Body {
        Sun, Moon, Mercury, Venus, Mars, Jupiter, Saturn,
        Rahu, // 罗睺 — 交点，具体取升/降由 [QizhengConfig.nodeConvention] 决定
        Ketu, // 计都 — 罗睺的对宫
        YueBei, // 月孛 — 月球平均远地点
        ZiQi, // 紫炁 — 传统虚星，周期/行向由 [QizhengConfig.ziQiMode] 决定
    }

    data class Position(
        val longitude: Double,
        val speedDegPerDay: Double,
    )

    /**
     * ΔT = TT − UT（天）。来自寿星天文历的 ΔT 表（含 2000 年后的外推）。
     * 2026 年约 +72 秒，对太阳影响可忽略，但对月亮约 0.011°（≈ 1.5′）。
     */
    fun deltaT(jdUt: Double): Double = ShouXingUtil.dtT(jdUt - AstroMath.J2000)

    /** UT 儒略日 → TT 儒略日 */
    fun jdTerrestrial(jdUt: Double): Double = jdUt + deltaT(jdUt)

    /** 以 TT 计的 J2000 起算世纪数 */
    fun centuriesTt(jdUt: Double): Double = AstroMath.centuriesJ2000(jdTerrestrial(jdUt))

    /**
     * 星曜位置。[config] 决定罗计取升/降交点、紫炁的周期与行向。
     * 不传时取 [PanZhiPresets.default]，与历史行为一致。
     */
    fun position(
        body: Body,
        jdUt: Double,
        config: QizhengConfig = PanZhiPresets.default,
    ): Position = when (body) {
        Body.Sun -> sun(jdUt)
        Body.Moon -> moon(jdUt)
        Body.Mercury -> planet(jdUt, Mercury)
        Body.Venus -> planet(jdUt, Venus)
        Body.Mars -> planet(jdUt, Mars)
        Body.Jupiter -> planet(jdUt, Jupiter)
        Body.Saturn -> planet(jdUt, Saturn)
        Body.Rahu -> lunarNode(jdUt, descending = config.nodeConvention == NodeConvention.TRADITIONAL)
        Body.Ketu -> lunarNode(jdUt, descending = config.nodeConvention != NodeConvention.TRADITIONAL)
        Body.YueBei -> meanApogee(jdUt)
        Body.ZiQi -> ziQi(jdUt, config.ziQiMode)
    }

    // ---------------------------------------------------------------- 日 / 月

    /** 太阳视黄经（度）：VSOP87 地球黄经 + 章动 + 光行差 */
    fun sunLongitude(jdUt: Double): Double {
        val t = centuriesTt(jdUt)
        return AstroMath.norm360(ShouXingUtil.saLon(t, -1) * AstroMath.Rad2Deg)
    }

    /** 月亮地心黄经（度）：ELP 级数，截断到项数 n */
    fun moonLongitude(jdUt: Double, terms: Int = -1): Double {
        val t = centuriesTt(jdUt)
        return AstroMath.norm360(ShouXingUtil.mLon(t, terms) * AstroMath.Rad2Deg)
    }

    private fun sun(jdUt: Double): Position {
        val lon = sunLongitude(jdUt)
        val spd = centerDiff(jdUt, 0.5) { sunLongitude(it) }
        return Position(lon, spd)
    }

    private fun moon(jdUt: Double): Position {
        val lon = moonLongitude(jdUt)
        val spd = centerDiff(jdUt, 0.25) { moonLongitude(it) }
        return Position(lon, spd)
    }

    /** 中心差分求速度（度/天），比单侧差分稳定得多 */
    private fun centerDiff(jdUt: Double, halfStepDays: Double, f: (Double) -> Double): Double {
        val a = f(jdUt - halfStepDays)
        val b = f(jdUt + halfStepDays)
        return AstroMath.norm180(b - a) / (2 * halfStepDays)
    }

    // ---------------------------------------------------------------- 五星

    /**
     * Meeus《Astronomical Algorithms》Table 31.a（适用 1800–2050）。
     * L：平黄经（度，含 T⁰..T³）；a：半长径（AU）；e：偏心率；
     * I：轨道倾角（度）；Pi(ϖ)：近日点黄经（度）；Om(Ω)：升交点黄经（度）。
     */
    internal data class Elements(
        val L: DoubleArray,
        val a0: Double, val a1: Double,
        val e0: Double, val e1: Double, val e2: Double,
        val i0: Double, val i1: Double, val i2: Double,
        val pi0: Double, val pi1: Double, val pi2: Double,
        val om0: Double, val om1: Double, val om2: Double, val om3: Double,
    )

    private val Mercury = Elements(
        doubleArrayOf(252.25032350, 149472.67411175, 0.00030397, 0.000000018),
        0.38709893, 0.00000066,
        0.20563569, 0.00002027, -0.000000028,
        7.00459499, -0.00595132, 0.000000081,
        77.45611904, 0.00005713, -0.000000131,
        48.33076593, -0.00534163, -0.000000118, 0.0,
    )
    private val Venus = Elements(
        doubleArrayOf(181.97909950, 58517.81538729, 0.00028656, 0.0000000011),
        0.72333199, 0.00000092,
        0.00677323, -0.00004938, -0.000000036,
        3.39461058, -0.00000718, 0.000000005,
        131.60246718, 0.00002807, -0.000000091,
        76.67984255, -0.00843582, -0.000000105, 0.0,
    )
    private val Mars = Elements(
        doubleArrayOf(355.43299958, 19140.30268499, 0.00031307, 0.000000017),
        1.52366231, -0.00007221,
        0.09341233, 0.00011902, -0.000000212,
        1.85032282, -0.00067504, 0.000000126,
        336.06023395, 0.00005888, -0.000000128,
        49.55742766, -0.00713300, -0.000000115, 0.0,
    )
    private val Jupiter = Elements(
        doubleArrayOf(34.35151874, 3034.90567464, -0.00008501, 0.000000004),
        5.20248019, -0.00002864,
        0.04853590, 0.00018026, -0.000000226,
        1.29861416, -0.00022699, 0.000000023,
        14.33120687, 0.00000000, -0.000000005,
        100.46457166, 0.17668143, 0.00090387, -0.000007032,
    )
    private val Saturn = Elements(
        doubleArrayOf(50.07744430, 1222.11384873, -0.00009002, 0.000000009),
        9.53707032, -0.00000311,
        0.05415060, -0.00034445, -0.000000729,
        2.49424102, 0.00451969, -0.000000026,
        93.05723748, 0.00000000, -0.000000018,
        113.66550252, -0.00213888, -0.000000469, 0.0,
    )

    private fun planet(jdUt: Double, el: Elements): Position {
        val lon = planetLongitude(jdUt, el)
        val spd = centerDiff(jdUt, 0.5) { planetLongitude(it, el) }
        return Position(lon, spd)
    }

    /** 五星地心黄经（度）。仅支持 [Body.Mercury]..[Body.Saturn]。 */
    fun planetLongitude(body: Body, jdUt: Double): Double = when (body) {
        Body.Mercury -> planetLongitude(jdUt, Mercury)
        Body.Venus -> planetLongitude(jdUt, Venus)
        Body.Mars -> planetLongitude(jdUt, Mars)
        Body.Jupiter -> planetLongitude(jdUt, Jupiter)
        Body.Saturn -> planetLongitude(jdUt, Saturn)
        else -> throw IllegalArgumentException("不是五星: $body")
    }

    /** 行星地心黄经（度）。未做光行时修正，误差 ≲ 0.01°。 */
    internal fun planetLongitude(jdUt: Double, el: Elements): Double {
        val t = centuriesTt(jdUt)
        val t2 = t * t
        val t3 = t2 * t

        val l = el.L[0] + el.L[1] * t + el.L[2] * t2 + el.L[3] * t3
        val a = el.a0 + el.a1 * t
        val e = el.e0 + el.e1 * t + el.e2 * t2
        val inc = el.i0 + el.i1 * t + el.i2 * t2
        val pi = el.pi0 + el.pi1 * t + el.pi2 * t2
        val om = el.om0 + el.om1 * t + el.om2 * t2 + el.om3 * t3

        val w = pi - om // 近日点幅角
        val m = AstroMath.norm180(l - pi)

        // 解开普勒方程（牛顿迭代）
        val mr = m * AstroMath.Deg2Rad
        var ecc = mr + e * sin(mr)
        repeat(20) {
            val d = (ecc - e * sin(ecc) - mr) / (1 - e * cos(ecc))
            ecc -= d
            if (abs(d) < 1e-13) return@repeat
        }

        // 轨道平面内的日心黄道坐标
        val xv = a * (cos(ecc) - e)
        val yv = a * sqrt(1 - e * e) * sin(ecc)

        // 旋转到黄道坐标系
        val cw = AstroMath.cosd(w); val sw = AstroMath.sind(w)
        val co = AstroMath.cosd(om); val so = AstroMath.sind(om)
        val ci = AstroMath.cosd(inc); val si = AstroMath.sind(inc)

        val xh = (cw * co - sw * so * ci) * xv + (-sw * co - cw * so * ci) * yv
        val yh = (cw * so + sw * co * ci) * xv + (-sw * so + cw * co * ci) * yv

        // 转到地心：减去地球的日心位置（= 太阳地心位置取反）
        val sunLon = sunLongitude(jdUt)
        val r = sunRadiusAu(jdUt)
        val xe = r * AstroMath.cosd(sunLon + 180.0)
        val ye = r * AstroMath.sind(sunLon + 180.0)

        return AstroMath.norm360(atan2(yh - ye, xh - xe) * AstroMath.Rad2Deg)
    }

    /** 太阳地心距（AU），Meeus 第 25 章。仅用于五星的地心化，精度要求不高。 */
    private fun sunRadiusAu(jdUt: Double): Double {
        val t = centuriesTt(jdUt)
        val m = AstroMath.norm360(357.52911 + 35999.05029 * t - 0.0001537 * t * t)
        val e = 0.016708634 - 0.000042037 * t - 0.0000001267 * t * t
        val c = (1.914602 - 0.004817 * t) * AstroMath.sind(m) +
            0.019993 * AstroMath.sind(2 * m) +
            0.000289 * AstroMath.sind(3 * m)
        return 1.000001018 * (1 - e * e) / (1 + e * AstroMath.cosd(m + c))
    }

    // ---------------------------------------------------------------- 四余

    /** 月球平均交点（Meeus 第 22 章），[descending] = true 时取对宫（降交点）。 */
    fun lunarNodeLongitude(jdUt: Double, descending: Boolean): Double {
        val t = centuriesTt(jdUt)
        val up = AstroMath.norm360(125.0445479 - 1934.1362891 * t + 0.0020754 * t * t)
        return if (descending) AstroMath.norm360(up + 180.0) else up
    }

    private fun lunarNode(jdUt: Double, descending: Boolean): Position {
        val lon = lunarNodeLongitude(jdUt, descending)
        val spd = centerDiff(jdUt, 0.5) { lunarNodeLongitude(it, descending) }
        return Position(lon, spd)
    }

    /**
     * 月孛：月球**远地点**（Meeus 47 章），顺行，约 8.85 年一周。
     *
     * ⚠️ Meeus 第 47 章公式 `83.3532465 + 4069.0137287·t` 给出的是**近地点（perigee）**；
     * 远地点（apogee）与近地点相差 180°，故此处显式 +180°。
     *
     * 核对「星命排盘 V1.25」：2026-08-29 月孛实测在 268.06°（寅宫），
     * 修正前算得 88.06°（差整整 180°），修正后吻合。
     */
    fun apogeeLongitude(jdUt: Double): Double {
        val t = centuriesTt(jdUt)
        val perigee = 83.3532465 + 4069.0137287 * t - 0.0103200 * t * t
        return AstroMath.norm360(perigee + 180.0)
    }

    private fun meanApogee(jdUt: Double): Position {
        val lon = apogeeLongitude(jdUt)
        val spd = centerDiff(jdUt, 0.5) { apogeeLongitude(it) }
        return Position(lon, spd)
    }

    /**
     * 紫炁：七政四余的传统虚星，**非天文实点，周期与行向各家不一**。
     *
     * 本项目历元：`1975-03-13 16:00 UT`（JD 2442485.1667）时紫炁在 `230.5°`，
     * 该组参数与「星命排盘」参考盘在 2026 年落翼宿一致。
     * TODO: 若换参考盘，需同步重标此历元。
     */
    fun ziQiLongitude(jdUt: Double, mode: ZiQiMode = ZiQiMode.YEARS_28): Double {
        val epoch = 2442485.1667
        val start = 230.5
        val periodDays = mode.periodYears * 365.2422
        val advance = (jdUt - epoch) / periodDays * 360.0
        return AstroMath.norm360(start + if (mode.retrograde) -advance else advance)
    }

    private fun ziQi(jdUt: Double, mode: ZiQiMode): Position {
        val periodDays = mode.periodYears * 365.2422
        val spd = 360.0 / periodDays
        return Position(ziQiLongitude(jdUt, mode), if (mode.retrograde) -spd else spd)
    }
}
