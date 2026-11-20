package zhiqiu.qizheng

/**
 * 二十八宿（回归今宿）。
 *
 * 传统各宿古度之和 365.25，映射到黄道 360°。
 * 宿零点（角宿距星/岁差）使春分点不再硬对齐角宿起点，由排盘时刻算出后**显式传入**，
 * 见 [zeroDeg] 与 [QizhengChart.xiuZeroDeg]。
 * 黄金样例 2026-08-29 命宫丑宫头 → 箕四（对照星命排盘「回归今宿·黄道」）。
 */
object XiuTable {
    data class Xiu(val name: String, val widthAncient: Double, val element: String)

    /**
     * 古度宽度，和 = 365.25。
     * [element]：宿名口诀五行（角木蛟、亢金龙…箕水豹…），盘心「×度命」取此，不用日柱纳音。
     */
    private val raw = listOf(
        Xiu("角", 12.0, "木"), Xiu("亢", 9.0, "金"), Xiu("氐", 15.0, "土"), Xiu("房", 5.0, "日"),
        Xiu("心", 5.0, "月"), Xiu("尾", 18.0, "火"), Xiu("箕", 11.0, "水"),
        Xiu("斗", 26.0, "木"), Xiu("牛", 8.0, "金"), Xiu("女", 12.0, "土"), Xiu("虚", 10.0, "日"),
        Xiu("危", 17.0, "月"), Xiu("室", 16.0, "火"), Xiu("壁", 9.0, "水"),
        Xiu("奎", 16.0, "木"), Xiu("娄", 12.0, "金"), Xiu("胃", 14.0, "土"), Xiu("昴", 11.0, "日"),
        Xiu("毕", 16.0, "月"), Xiu("觜", 2.0, "火"), Xiu("参", 9.0, "水"),
        Xiu("井", 33.0, "木"), Xiu("鬼", 4.0, "金"), Xiu("柳", 15.0, "土"), Xiu("星", 7.0, "日"),
        Xiu("张", 18.0, "月"), Xiu("翼", 18.0, "火"), Xiu("轸", 17.0, "水"),
    )

    private const val AncientSum = 365.25

    /**
     * 黄经总岁差（general precession in longitude）：5029.0966″/儒略世纪。
     * 二十八宿锚定在恒星上，故宿界相对回归春分点每年东移约 50.29″。
     */
    const val PrecessionDegPerCentury = 1.396971

    /**
     * J2000.0 时的今宿零点（角宿起点在回归黄道上的位置，度）。
     *
     * **核对依据（与权威在线排盘「星命排盘 V1.25」xingmingpp.xyz 逐项比对后标定）**：
     * 网站「回归今宿·黄道」以**角宿距星 Spica（α Vir）的现代黄经**为锚，
     * 2026-08-29 反推得其角宿起点黄经 = 204.21°，对应 J2000 零点 ≈ 203.838°
     * （Spica 实测 J2000 黄经约 203.85°，吻合）。
     *
     * 网站的宿界宽度（今宿）与古宿度表不同（如井宿今宽远小于古宽 33°），
     * 故 [startsModern] 直接采用网站反推的**现代宿宽**，而非古宽 × scale。
     */
    const val ZeroAtJ2000 = 203.838

    /** 给定「J2000 起的世纪数」时的今宿零点 */
    fun zeroDeg(centuriesSinceJ2000: Double): Double =
        ZeroAtJ2000 + PrecessionDegPerCentury * centuriesSinceJ2000

    private val scale = 360.0 / AncientSum

    /** 古宿宽累积起点（角宿基准 0，单位：古度，和 = 365.25） */
    private val startsAncient: DoubleArray = run {
        val arr = DoubleArray(raw.size)
        var acc = 0.0
        for (i in raw.indices) {
            arr[i] = acc
            acc += raw[i].widthAncient
        }
        arr
    }

    /**
     * 现代宿宽下的累积起点（角宿基准 0，单位：今度）。
     *
     * 数据来自「星命排盘 V1.25」(xingmingpp.xyz) 反推：
     * 用 12 个宫头落宿 + 11 个星曜落宿反推 17~18 个宿的现代起点（数据自洽，
     * 如星起点 147.65° 多次出现一致），其余 10 个缺失宿按最近已知宿为锚、
     * 按古宿宽比例线性插值补全，并与已知宿自洽（轸起点推算 346.84 ≈ 实测 346.88）。
     * 顺序同 [raw]：角亢氐房心尾箕斗牛女虚危室壁奎娄胃昴毕觜参井鬼柳星张翼轸。
     */
    private val startsModern: DoubleArray = doubleArrayOf(
        0.00, 12.14, 21.25, 39.89, 46.11, 52.32, 67.43, 76.35,
        100.77, 108.30, 119.57, 129.53, 149.65, 165.32, 178.54, 192.65,
        203.36, 215.58, 225.16, 239.10, 240.84, 251.46, 282.68, 286.46,
        303.44, 311.85, 329.84, 346.88,
    )

    /**
     * 《星学大成》固定今度宿宽（角→轸，和=360）。
     * 对照星命排盘 `getcs` 在 `pz=1` 时写入的表。
     */
    val DachengWidths: List<Double> = listOf(
        9.0, 18.0, 13.0, 15.0, 11.0, 15.5, 0.5, 9.0,
        30.0, 3.0, 13.0, 7.0, 17.0, 20.0, 19.0, 12.0,
        10.0, 16.0, 6.0, 6.0, 17.0, 10.0, 22.0, 7.0,
        11.0, 9.0, 16.0, 18.0,
    )

    /**
     * 自定义宿度默认占位表（星命排盘 UI `gong.xiudu`，和=360）。
     * 非实测今宿；选「自定义」且未另传 [QizhengConfig.customXiuWidths] 时用此表。
     */
    val DefaultCustomWidths: List<Double> = listOf(
        8.9, 17.6, 10.4, 14.8, 12.1, 15.8, 1.0, 11.8,
        30.5, 2.9, 15.3, 5.9, 15.0, 18.7, 17.1, 12.8,
        8.9, 16.3, 5.4, 6.4, 18.6, 10.7, 23.8, 7.9,
        10.9, 9.4, 15.3, 15.8,
    )

    private val startsDacheng: DoubleArray = startsFromWidths(DachengWidths)
    private val startsCustomDefault: DoubleArray = startsFromWidths(DefaultCustomWidths)

    private fun startsFromWidths(widths: List<Double>): DoubleArray {
        require(widths.size == 28) { "二十八宿宽须 28 项" }
        val arr = DoubleArray(28)
        var acc = 0.0
        for (i in 0 until 28) {
            arr[i] = acc
            acc += widths[i]
        }
        return arr
    }

    /** 校验自定义宿宽：28 项且和≈360 */
    fun normalizeCustomWidths(widths: List<Double>?): List<Double> {
        if (widths == null || widths.size != 28) return DefaultCustomWidths
        val sum = widths.sum()
        return if (kotlin.math.abs(sum - 360.0) < 0.05) widths else DefaultCustomWidths
    }
    data class Location(
        val name: String,
        /** 宿内古度（0..该宿古宽），对照参考「箕四」「斗19.4」 */
        val degreeInXiu: Double,
        val index: Int,
        /** 宿五行/七政属性：金木水火土日月 → 盘心「水度命」等 */
        val element: String,
    )

    fun names(): List<String> = raw.map { it.name }

    /** [frame] 框架下宿 idx 的角宿基准起点（今度，未加零点） */
    private fun frameStart(idx: Int, frame: XiuFrame, customWidths: List<Double>?): Double = when (frame) {
        XiuFrame.MODERN, XiuFrame.XUANXIAO -> startsModern[idx]
        XiuFrame.ANCIENT_SCALED, XiuFrame.ANCIENT_J2000 -> startsAncient[idx] * scale
        XiuFrame.DACHENG -> startsDacheng[idx]
        XiuFrame.CUSTOM -> {
            val w = normalizeCustomWidths(customWidths)
            if (w === DefaultCustomWidths) startsCustomDefault[idx]
            else startsFromWidths(w)[idx]
        }
    }

    /**
     * 有效宿零点。
     *
     * **玄枵虚中**：今宿宽不变，把虚宿中点锚在**子宫中点**。
     * 本盘地支映射下子宫 = 黄经 300°–330°，中点 **315°**
     * （对照星命排盘 `xl.JS`：`se = 虚中 − 315`；《左传》「玄枵，虚中也」）。
     * 古宿岁差用 J2000 固定锚；其余用排盘时刻岁差零点。
     */
    fun effectiveZero(
        baseZero: Double,
        frame: XiuFrame,
        customWidths: List<Double>? = null,
    ): Double = when (frame) {
        XiuFrame.ANCIENT_J2000 -> ZeroAtJ2000
        XiuFrame.XUANXIAO -> {
            val xu = 10 // 虚（角→轸）
            val rel = startsModern[xu]
            val next = startsModern[xu + 1] // 危
            val w = (if (next < rel) next + 360.0 else next) - rel
            // 315° = 子宫正中（非冬至点 270°；270° 在本映射下是丑宫中）
            AstroMath.norm360(315.0 - (rel + w / 2.0))
        }
        else -> baseZero
    }

    /** 宿界在黄道上的绝对黄经；[equatorial] 时转赤经返回。 */
    fun startOf(
        idx: Int,
        zeroDeg: Double,
        frame: XiuFrame = XiuFrame.MODERN,
        equatorial: Boolean = false,
        customWidths: List<Double>? = null,
    ): Double {
        val z = effectiveZero(zeroDeg, frame, customWidths)
        val lon = frameStart(idx, frame, customWidths) + z
        return if (equatorial) AstroMath.rightAscension(lon) else AstroMath.norm360(lon)
    }

    /** 宿宽（今度，360 制） */
    fun widthAt(
        idx: Int,
        zeroDeg: Double,
        frame: XiuFrame = XiuFrame.MODERN,
        equatorial: Boolean = false,
        customWidths: List<Double>? = null,
    ): Double {
        val nextIdx = if (idx + 1 < raw.size) idx + 1 else 0
        val cur = startOf(idx, zeroDeg, frame, equatorial, customWidths)
        val nextRaw = startOf(nextIdx, zeroDeg, frame, equatorial, customWidths)
        val next = if (nextRaw < cur) nextRaw + 360.0 else nextRaw
        return next - cur
    }

    /**
     * 落宿。
     *
     * [zeroDeg] 今宿零点（岁差），由排盘时刻算出后传入；玄枵虚中 / 古宿岁差会再换算。
     * [customWidths] 仅 [XiuFrame.CUSTOM] 使用。
     */
    fun locate(
        longitude: Double,
        zeroDeg: Double,
        xiuSystem: XiuSystem = XiuSystem.ANCIENT,
        frame: XiuFrame = XiuFrame.MODERN,
        equatorial: Boolean = false,
        customWidths: List<Double>? = null,
    ): Location {
        for (i in 0 until 28) {
            val delta = AstroMath.norm360(longitude - startOf(i, zeroDeg, frame, equatorial, customWidths))
            if (delta < widthAt(i, zeroDeg, frame, equatorial, customWidths)) {
                val degModern = delta
                val deg = if (xiuSystem == XiuSystem.ANCIENT) degModern / scale else degModern
                val x = raw[i]
                return Location(x.name, deg, i, x.element)
            }
        }
        val x = raw[0]
        return Location(
            x.name,
            AstroMath.norm360(longitude - startOf(0, zeroDeg, frame, equatorial, customWidths)) / scale,
            0,
            x.element,
        )
    }

    /** 盘心「×度命」：日/月宿记作日度命/月度命，其余用五行 */
    fun duMingLabel(element: String): String = when (element) {
        "日" -> "日度命"
        "月" -> "月度命"
        else -> "${element}度命"
    }
}
